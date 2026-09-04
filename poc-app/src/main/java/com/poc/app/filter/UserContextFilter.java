package com.poc.app.filter;

import com.poc.common.context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class UserContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String userRoles = request.getHeader("X-User-Roles");
        String userPermissions = request.getHeader("X-User-Permissions");
        String username = request.getHeader("X-User-Name");

        if (userId != null) {
            UserContext.set(userId, userRoles, userPermissions, username);
            log.debug("UserContext set: userId={}, username={}, roles={}, permissions={}",
                    userId, username, userRoles, userPermissions);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
