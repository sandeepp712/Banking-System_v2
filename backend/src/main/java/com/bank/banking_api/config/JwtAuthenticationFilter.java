package com.bank.banking_api.config;

import com.bank.banking_api.exception.JwtTokenExpiredException;
import com.bank.banking_api.exception.JwtTokenInvalidException;
import com.bank.banking_api.security.CustomUserDetailsService;
import com.bank.banking_api.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException,JwtTokenExpiredException, JwtTokenInvalidException {
        String token = resolveToken(request);
        System.out.println(" JWT FILTER TRIGGERED! Token: " + token);

        if (token != null) {
            try {
                // Validate the tokens and extracts claims
                String userId = tokenProvider.getUserIdFromToken(token).toString();

                // Load user and set context
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtTokenExpiredException | JwtTokenInvalidException e) {
                throw e;
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}