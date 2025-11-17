package io.github.danjos.mybankapp.frontui.config;

import io.github.danjos.mybankapp.frontui.service.OAuth2TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2TokenService oAuth2TokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        
        // Get password from session (stored by the filter before authentication)
        HttpSession session = request.getSession();
        String password = (String) session.getAttribute("temp_password");
        
        if (password != null) {
            log.info("User {} logged in successfully, obtaining OAuth2 token", username);
            
            // Obtain OAuth2 token
            String accessToken = oAuth2TokenService.getAccessToken(username, password);
            
            if (accessToken != null) {
                // Store token in session
                session.setAttribute("oauth2_access_token", accessToken);
                log.info("OAuth2 token stored in session for user: {}", username);
            } else {
                log.warn("Failed to obtain OAuth2 token for user: {}", username);
            }
            
            // Remove temporary password from session
            session.removeAttribute("temp_password");
        } else {
            log.warn("Password not found in session for user: {}", username);
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

