package com.bank.banking_api.config;

import com.bank.banking_api.security.CustomUserDetailsService;
import com.bank.banking_api.security.JwtTokenProvider;
import com.bank.banking_api.service.RateLimiterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final RateLimiterFilter rateLimiterFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtTokenProvider tokenProvider, RateLimiterFilter rateLimiterFilter, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.tokenProvider = tokenProvider;
        this.rateLimiterFilter = rateLimiterFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RateLimiterService rateLimiterService, RateLimiterFilter rateLimiterFilter) throws Exception {
        http
                .headers(headers -> headers
                        //1 Prevent Clickjacking: Completely forbid anyone from putting your API/site in an <iframe>
                        .frameOptions(frameOption -> frameOption.deny())

                        //2 Enforce HTTPS(HSTS): Tells browsers to Only talk to your server over HTTPS for the next year
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)   // 365 days in second
                        )

                        //3 Content Security Policy (CSP): Restrict where scripts/styles/images can be loaded from
                        //Essential for stopping malicious scripts from running in the browser
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'self'; frame-ancestors 'none';")
                        )

                        //4 Referrer Policy: Do not leak your internal API URLs when users click external links
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )

                        //5 Permissions Policy: Disable browser features your banking API doesn't need( like camera/microphone)
                        .permissionsPolicyHeader(permission -> permission
                                .policy("camera=(), microphone=(), geolocation=()")
                        )
                )

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}