package com.predix.oracle.config.security;

import com.predix.oracle.config.PredixOracleProperties;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER_TOKEN = "X-Oracle-Admin-Token";
    public static final String ATTR_ACTOR = "oracleActor";

    private final PredixOracleProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.GET.matches(request.getMethod())
                && request.getRequestURI().contains("/evidences")
                && !request.getRequestURI().contains("/collect")) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && request.getRequestURI().contains("/sources")) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && request.getRequestURI().contains("/jobs")) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && request.getRequestURI().contains("/uma/")) {
            return true;
        }

        String token = request.getHeader(HEADER_TOKEN);
        if (token == null || token.isBlank() || !token.equals(properties.getAuthToken())) {
            throw new OracleException(OracleErrorCode.UNAUTHORIZED, "Invalid or missing admin token");
        }
        request.setAttribute(ATTR_ACTOR, "MANUAL");
        return true;
    }
}
