package io.github.danjos.mybankapp.authserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private DataSource dataSource;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT username, password, first_name, last_name FROM accounts_schema.users WHERE username = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String password = resultSet.getString("password");
                        String firstName = resultSet.getString("first_name");
                        String lastName = resultSet.getString("last_name");
                        
                        return User.builder()
                                .username(username)
                                .password(password)
                                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                                .build();
                    } else {
                        throw new UsernameNotFoundException("User not found: " + username);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while loading user", e);
        }
    }
}
