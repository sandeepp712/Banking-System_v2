//package com.bank.banking_api.config;
//
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE) // This ensures it runs FIRST
//public class CorsFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletResponse response = (HttpServletResponse) res;
//        HttpServletRequest request = (HttpServletRequest) req;
//
//        // Allow ALL origins (for development)
//        response.setHeader("Access-Control-Allow-Origin", "*");
//
//        // Allow these HTTP methods
//        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
//
//        // Allow these headers
//        response.setHeader("Access-Control-Allow-Headers", "*");
//
//        // Allow credentials (cookies, auth headers)
//        response.setHeader("Access-Control-Allow-Credentials", "true");
//
//        // Max age for pre-flight requests
//        response.setHeader("Access-Control-Max-Age", "3600");
//
//        // Handle pre-flight OPTIONS request
//        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//            response.setStatus(HttpServletResponse.SC_OK);
//        } else {
//            chain.doFilter(req, res);
//        }
//    }
//
//    @Override
//    public void init(FilterConfig filterConfig) {}
//
//    @Override
//    public void destroy() {}
//}