package com.fintech.demo.order.security;

import com.fintech.demo.order.common.NotFoundException;
import com.fintech.demo.order.domain.Role;
import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 【職責】從 SecurityContext 解析目前登入用戶（含 DB id／角色）。
 * 【技巧】讀多用 @Transactional(readOnly=true)；寫入走預設交易。
 * 【概念】Service 是 Demo 最常說明的「流程編排」層。
 */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || "anonymousUser".equals(auth.getName())) {
            throw new NotFoundException("not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    public Long requireUserId() {
        return requireUser().getId();
    }

    public boolean isAdmin() {
        return requireUser().getRole() == Role.ADMIN;
    }
}
