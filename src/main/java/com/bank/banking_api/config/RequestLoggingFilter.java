package com.bank.banking_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
//        System.out.println("🚨 REQUEST LOGGING FILTER IS RUNNING! 🚨");
        try {
            //1 Check if the client already send a request
            //This is crucial for distributed tracing across multiple microservice
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.trim().isEmpty()) {
                //2. If not, generate a new UUID for this request
                requestId = UUID.randomUUID().toString();
            }

            //3. Put it in the MDC. now all logs on this thread will include it.(ThreadLocal)
            MDC.put(MDC_REQUEST_ID_KEY, requestId);

            // Optional: Add the generated ID to the response header so the frontend can log it too
            response.setHeader(REQUEST_ID_HEADER, requestId);
//            log.info("Filter check: MDC.get(requestId) is currently -> {}",MDC.get("requestId"));

            //4. Continue the chain
            filterChain.doFilter(request, response);
        }finally {
            //5. Critical: clear the MDC when the request is done
//            log.info("Filter cleanup: clearing mdc -> {}",MDC.get(MDC_REQUEST_ID_KEY));
            MDC.clear();
        }
    }
}