package io.github.danjos.mybankapp.frontui.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class PasswordCaptureFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Only capture password on login POST request
        if ("POST".equals(request.getMethod())) {
            String requestURI = request.getRequestURI();
            log.debug("PasswordCaptureFilter: Processing {} request to {}", request.getMethod(), requestURI);
            
            // Check if this is a login request (Spring Security default is /login)
            if ("/login".equals(requestURI) || requestURI.endsWith("/login")) {
                String password = request.getParameter("password");
                String username = request.getParameter("username");
                
                log.debug("PasswordCaptureFilter: Login request detected. Username: {}, Password present: {}", 
                    username, password != null && !password.isEmpty());
                
                if (password != null && !password.isEmpty()) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("temp_password", password);
                    log.info("Password captured and stored in session for token exchange. Session ID: {}, Username: {}", 
                        session.getId(), username);
                } else {
                    log.warn("PasswordCaptureFilter: Password parameter is null or empty for login request");
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

