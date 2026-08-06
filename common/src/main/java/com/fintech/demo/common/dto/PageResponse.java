package com.fintech.demo.common.dto;

import java.util.List;

/**
 * 【職責】通用分頁回應（對齊 TradingPagingList／TradingCRUD）。
 * 【技巧】外層與巢狀 {@link PageMeta} 皆用 Java {@code record}；{@code of} 工廠集中組裝。
 * 【概念】為何用 record？分頁回應是查詢當下的不可變快照（API 回應契約），避免組完後被改 data／meta。
 *         對照：JPA Entity、需 {@code setXxx} 的 Request → class + Lombok。
 */
public record PageResponse<T>(List<T> data, PageMeta meta) {

    public static <T> PageResponse<T> of(List<T> data, int page, int size, long total) {
        return new PageResponse<>(data, PageMeta.of(page, size, total));
    }

    /**
     * 【職責】分頁中繼資料。
     * 【技巧】巢狀 record。
     * 【概念】meta 同樣是不可變值物件，與 data 同屬 API 契約。
     */
    public record PageMeta(int page, int size, long total) {
        public static PageMeta of(int page, int size, long total) {
            return new PageMeta(page, size, total);
        }
    }
}
