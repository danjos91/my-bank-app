package io.github.danjos.mybankapp.frontui.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class PasswordCaptureFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Only capture password on login POST request
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            String password = request.getParameter("password");
            if (password != null && !password.isEmpty()) {
                HttpSession session = request.getSession(true);
                session.setAttribute("temp_password", password);
                log.debug("Password captured and stored in session");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

