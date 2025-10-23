package io.github.danjos.mybankapp.frontui.service;

import io.github.danjos.mybankapp.frontui.dto.UserDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    public UserDataDTO getUserData(String username) {
        try {
            String url = gatewayUrl + "/api/accounts/user/" + username;
            return restTemplate.getForObject(url, UserDataDTO.class);
        } catch (Exception e) {
            log.error("Error getting user data for: {}", username, e);
            throw new RuntimeException("Ошибка получения данных пользователя: " + e.getMessage());
        }
    }

    public List<UserDataDTO> getAllUsers() {
        try {
            String url = gatewayUrl + "/api/accounts/users";
            return restTemplate.exchange(url, HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<UserDataDTO>>() {}).getBody();
        } catch (Exception e) {
            log.error("Error getting all users", e);
            throw new RuntimeException("Ошибка получения списка пользователей: " + e.getMessage());
        }
    }

    public void updateUserProfile(String username, String name, String birthdate) {
        try {
            String url = gatewayUrl + "/api/accounts/user/" + username + "/profile";
            
            Map<String, String> profileData = new HashMap<>();
            profileData.put("name", name);
            profileData.put("birthdate", birthdate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(profileData, headers);
            
            restTemplate.put(url, request);
        } catch (Exception e) {
            log.error("Error updating user profile for: {}", username, e);
            throw new RuntimeException("Ошибка обновления профиля: " + e.getMessage());
        }
    }

    public void updatePassword(String username, String password) {
        try {
            String url = gatewayUrl + "/api/accounts/user/" + username + "/password";
            
            Map<String, String> passwordData = new HashMap<>();
            passwordData.put("password", password);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(passwordData, headers);
            
            restTemplate.put(url, request);
        } catch (Exception e) {
            log.error("Error updating password for: {}", username, e);
            throw new RuntimeException("Ошибка изменения пароля: " + e.getMessage());
        }
    }

    public void deposit(String username, BigDecimal amount) {
        try {
            String url = gatewayUrl + "/api/cash/deposit";
            
            Map<String, Object> depositData = new HashMap<>();
            depositData.put("accountId", getAccountId(username));
            depositData.put("amount", amount);
            depositData.put("description", "Пополнение через веб-интерфейс");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(depositData, headers);
            
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Error processing deposit for: {}", username, e);
            throw new RuntimeException("Ошибка пополнения счета: " + e.getMessage());
        }
    }

    public void withdraw(String username, BigDecimal amount) {
        try {
            String url = gatewayUrl + "/api/cash/withdraw";
            
            Map<String, Object> withdrawalData = new HashMap<>();
            withdrawalData.put("accountId", getAccountId(username));
            withdrawalData.put("amount", amount);
            withdrawalData.put("description", "Снятие через веб-интерфейс");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(withdrawalData, headers);
            
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Error processing withdrawal for: {}", username, e);
            throw new RuntimeException("Ошибка снятия средств: " + e.getMessage());
        }
    }

    public void transfer(String fromUsername, String toUsername, BigDecimal amount) {
        try {
            String url = gatewayUrl + "/api/transfers";
            
            Map<String, Object> transferData = new HashMap<>();
            transferData.put("fromAccountId", getAccountId(fromUsername));
            transferData.put("toAccountId", getAccountId(toUsername));
            transferData.put("amount", amount);
            transferData.put("description", "Перевод через веб-интерфейс");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(transferData, headers);
            
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Error processing transfer from {} to {}", fromUsername, toUsername, e);
            throw new RuntimeException("Ошибка перевода: " + e.getMessage());
        }
    }

    public void registerUser(String username, String password, String name, String birthdate) {
        try {
            String url = gatewayUrl + "/api/accounts/register";
            
            Map<String, String> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("password", password);
            userData.put("firstName", name.split(" ")[0]);
            userData.put("lastName", name.split(" ").length > 1 ? name.split(" ")[1] : "");
            userData.put("email", username + "@bank.local");
            userData.put("birthDate", birthdate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(userData, headers);
            
            restTemplate.postForObject(url, request, Void.class);
        } catch (Exception e) {
            log.error("Error registering user: {}", username, e);
            throw new RuntimeException("Ошибка регистрации: " + e.getMessage());
        }
    }

    private Long getAccountId(String username) {
        try {
            UserDataDTO userData = getUserData(username);
            return userData.getAccountId();
        } catch (Exception e) {
            log.error("Error getting account ID for: {}", username, e);
            throw new RuntimeException("Ошибка получения ID счета: " + e.getMessage());
        }
    }
}
