package com.bank.banking_api.config;

import com.bank.banking_api.security.CustomUserDetailsService;
import com.bank.banking_api.security.JwtTokenProvider;
import com.bank.banking_api.service.RateLimiterService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 1. Explicitly list your frontend URL (Vite is usually 5173, React is 3000)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // 2. CRITICAL: This MUST be true for HttpOnly cookies to work across domains/ports
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RateLimiterService rateLimiterService, RateLimiterFilter rateLimiterFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        //1 Prevent Clickjacking: Completely forbid anyone from putting your API/site in an <iframe>
                        .frameOptions(frameOption -> frameOption.deny())

                        //2 Enforce HTTPS(HSTS): Tells browsers to Only talk to your server over HTTPS for the next year
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)   // 365 days in second
                        )

                        //3 Content Security Policy (CSP): Restrict where scripts/styles/images can be loaded from
                        //Essential for stopping malicious scripts from running in the browser
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'self'; frame-ancestors 'none';"))

                        //4 Referrer Policy: Do not leak your internal API URLs when users click external links
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        //5 Permissions Policy: Disable browser features your banking API doesn't need( like camera/microphone)
                        .permissionsPolicyHeader(permission -> permission.policy("camera=(), microphone=(), geolocation=()")))

                .csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint((request, response, authException) -> {
//
//                            // 1. Log it as a WARN, not an ERROR.
//                            // This prevents waking up the on-call engineer for a simple expired token.
//                            Logger log = LoggerFactory.getLogger("SecurityAudit");
//                            log.warn("Authentication failed for path {}: {}", request.getRequestURI(), authException.getMessage());
//
//                            // 2. Set the correct HTTP Status
//                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
//                            response.setContentType("application/json");
//                            response.setCharacterEncoding("UTF-8");
//
//                            // 3. Write a clean JSON response
//                            Map<String, Object> errorBody = new HashMap<>();
//                            errorBody.put("errorCode", "UNAUTHORIZED");
//                            errorBody.put("message", authException.getMessage());
//                            errorBody.put("status", 401);
//                            errorBody.put("path", request.getRequestURI());
//
//                            new ObjectMapper().writeValue(response.getOutputStream(), errorBody);
//                        })
//                )


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()

                        // require a valid JWT for accounts
                        .requestMatchers("/api/accounts/**").authenticated()
                        .requestMatchers("/api/transfers/**").authenticated()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}