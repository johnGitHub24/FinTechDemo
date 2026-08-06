package com.fintech.demo.order.security;

import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【職責】自 users 表載入帳密與 ROLE_。
 * 【技巧】讀多用 @Transactional(readOnly=true)；寫入走預設交易。
 * 【概念】Service 是 Demo 最常說明的「流程編排」層。
 */
@Service
public class DemoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DemoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        String roleName = "ROLE_" + entity.getRole().name();
        return User.withUsername(entity.getUsername())
                .password(entity.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(roleName)))
                .build();
    }
}
