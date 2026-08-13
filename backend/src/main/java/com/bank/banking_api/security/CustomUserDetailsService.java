package com.bank.banking_api.security;


import com.bank.banking_api.domain.User;
import com.bank.banking_api.persistence.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        try {
            UUID userId = UUID.fromString(identifier);

            User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return new CustomUserDetails(user);
        } catch (IllegalArgumentException e) {
            return userRepository.findByUsername(identifier)
                    .map(CustomUserDetails::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
    }

}