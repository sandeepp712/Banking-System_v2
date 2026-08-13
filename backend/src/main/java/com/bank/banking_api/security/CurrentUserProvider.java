package com.bank.banking_api.security;


import com.bank.banking_api.exception.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider{
    /**
     * Returns the authenticated user's ID.
     * This is the ONLY place in the codebase that reads SecurityContextHolder
     * for business logic purposes.
     *
     * @throws UnauthenticatedException if no user is authenticated
     */
    public UUID getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()){
            throw new UnauthenticatedException("Authentication Failed") {
            };
        }

        if(authentication.getPrincipal() instanceof CustomUserDetails User){
            return User.getUserId();
        }

        throw new UnauthenticatedException("Authentication Failed"+authentication.getPrincipal().getClass().getName());

    }

    /**
     * Returns the authenticated user's role.
     * Useful for authorization decisions.
     */
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException("No authenticated user in current context");
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("ROLE_ANONYMOUS");
    }
}