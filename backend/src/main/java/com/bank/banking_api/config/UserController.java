package com.bank.banking_api.config;

import com.bank.banking_api.security.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {
    @GetMapping("/users/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(new UserDto(
                userDetails.getUserId().toString(),
                userDetails.getUsername(),
                userDetails.getRole()
        ));
    }


    public record UserDto(
            String userId,
            String username,
            String role
    ) {
    }
}
