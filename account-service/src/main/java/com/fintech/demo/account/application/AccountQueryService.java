package com.fintech.demo.account.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.PositionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 【職責】讀側查詢＋可選 Redis 快取；入帳後淘汰 cache key。
 * 【技巧】ObjectProvider&lt;StringRedisTemplate&gt;：redis 關閉時無 bean 也不炸；TTL 見 fintech.redis.ttl-seconds。
 * 【概念】Cache-aside：miss 打 DB 再寫入；寫路徑（applyTrade）必須 delete，避免髒讀。
 */
@Service
public class AccountQueryService {

    private static final Logger log = LoggerFactory.getLogger(AccountQueryService.class);

    private final AccountLedgerService ledgerService;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final boolean redisEnabled;

    public AccountQueryService(
            AccountLedgerService ledgerService,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            @Value("${fintech.redis.enabled:true}") boolean redisEnabled,
            @Value("${fintech.redis.ttl-seconds:600}") long ttlSeconds) {
        this.ledgerService = ledgerService;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /**
     * 【職責】以 cache-aside 模式讀取帳戶資料。
     * 【技巧】Redis 命中時反序列化回 DTO；miss 或失敗時查帳本並回填快取。
     * 【概念】讀側快取降低資料庫壓力，但帳本仍是唯一真實來源。
     */
    public AccountDto getAccount(Long userId) {
        String key = accountKey(userId);
        if (redisEnabled) {
            String cached = getCache(key);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, AccountDto.class);
                } catch (JsonProcessingException e) {
                    log.warn("account cache decode failed userId={}", userId);
                }
            }
        }
        AccountDto dto = ledgerService.getAccount(userId);
        putCache(key, dto);
        return dto;
    }

    /**
     * 【職責】以 cache-aside 模式讀取使用者持倉。
     * 【技巧】使用獨立 positions key 與 TypeReference 還原泛型清單。
     * 【概念】帳戶與持倉分 key 可在資料更新後精確失效。
     */
    public List<PositionDto> listPositions(Long userId) {
        String key = positionsKey(userId);
        if (redisEnabled) {
            String cached = getCache(key);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, new TypeReference<List<PositionDto>>() {
                    });
                } catch (JsonProcessingException e) {
                    log.warn("positions cache decode failed userId={}", userId);
                }
            }
        }
        List<PositionDto> list = ledgerService.listPositions(userId);
        putCache(key, list);
        return list;
    }

    /** 入帳後清除 account／positions 快取。 */
    public void evict(Long userId) {
        if (!redisEnabled || userId == null) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        redis.delete(accountKey(userId));
        redis.delete(positionsKey(userId));
        log.debug("evicted cache userId={}", userId);
    }

    /**
     * 【職責】安全讀取 Redis 字串快取值。
     * 【技巧】Redis bean 不存在或連線失敗時記錄 warn 並回傳 null 讓呼叫端查 DB。
     * 【概念】快取是最佳化而非可用性單點，失敗不可阻斷帳戶查詢。
     */
    private String getCache(String key) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            return redis.opsForValue().get(key);
        } catch (Exception ex) {
            log.warn("redis get failed key={}: {}", key, ex.getMessage());
            return null;
        }
    }

    /**
     * 【職責】將讀模型序列化後寫入具有 TTL 的 Redis 快取。
     * 【技巧】僅在啟用與 bean 可用時寫入，序列化或連線失敗只記錄 warn。
     * 【概念】短 TTL 與顯式失效共同控制快取資料的陳舊程度。
     */
    private void putCache(String key, Object value) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("redis put failed key={}: {}", key, ex.getMessage());
        }
    }

    static String accountKey(Long userId) {
        return "account:" + userId;
    }

    static String positionsKey(Long userId) {
        return "positions:" + userId;
    }
}
