package com.bank.banking_api.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum AccountRole {
    RETAIL_USER,
    ADMIN;

    public GrantedAuthority getGrantedAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + this.name());
    }

    public boolean canAccessAdminPanel() {
        return this == ADMIN;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }
}