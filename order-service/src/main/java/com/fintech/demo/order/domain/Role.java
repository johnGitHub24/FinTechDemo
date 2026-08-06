package com.fintech.demo.order.domain;

/**
 * 【職責】JWT／授權角色（USER／ADMIN）。
 * 【技巧】種子資料先落地角色，Security 以 ROLE_ 前綴對應。
 * 【概念】RBAC：USER 做自己的單；ADMIN 可看全站審計。
 */
public enum Role {
    USER,
    ADMIN
}
