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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        if (username == null || username.trim().isEmpty()) {
            log.error("Username is null or empty when trying to get user data");
            throw new RuntimeException("Имя пользователя не указано");
        }
        try {
            // Get user profile
            String userUrl = gatewayUrl + "/api/accounts/users/username/" + username;
            log.debug("Fetching user profile from: {}", userUrl);
            var userProfileResponse = restTemplate.exchange(userUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> userProfile = userProfileResponse.getBody();
            
            // Get user accounts (to get balance)
            String accountsUrl = gatewayUrl + "/api/accounts/username/" + username;
            log.info("Fetching accounts from: {} for user: {}", accountsUrl, username);
            List<Map<String, Object>> accounts = null;
            try {
                var accountsResponse = restTemplate.exchange(accountsUrl, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                accounts = accountsResponse.getBody();
                log.info("Accounts response status: {}, accounts count: {}", 
                    accountsResponse.getStatusCode(), accounts != null ? accounts.size() : 0);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("HTTP error fetching accounts for user: {}. Status: {}, Response: {}", 
                    username, e.getStatusCode(), e.getResponseBodyAsString(), e);
                // Continue with empty accounts list - user might not have an account yet
                accounts = null;
            } catch (Exception e) {
                log.error("Error fetching accounts for user: {}", username, e);
                // Continue with empty accounts list
                accounts = null;
            }
            
            if (userProfile == null) {
                throw new RuntimeException("User not found: " + username);
            }
            
            // Combine data into UserDataDTO
            UserDataDTO userData = new UserDataDTO();
            userData.setId(getLongValue(userProfile.get("id")));
            userData.setUsername((String) userProfile.get("username"));
            userData.setFirstName((String) userProfile.get("firstName"));
            userData.setLastName((String) userProfile.get("lastName"));
            userData.setEmail((String) userProfile.get("email"));
            
            // Parse birthDate
            if (userProfile.get("birthDate") != null) {
                String birthDateStr = userProfile.get("birthDate").toString();
                userData.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            }
            
            // Get balance from first account (or sum all accounts)
            BigDecimal totalBalance = BigDecimal.ZERO;
            Long accountId = null;
            if (accounts != null && !accounts.isEmpty()) {
                Map<String, Object> firstAccount = accounts.get(0);
                accountId = getLongValue(firstAccount.get("id"));
                Object balanceObj = firstAccount.get("balance");
                if (balanceObj != null) {
                    if (balanceObj instanceof Number) {
                        totalBalance = BigDecimal.valueOf(((Number) balanceObj).doubleValue());
                    } else {
                        totalBalance = new BigDecimal(balanceObj.toString());
                    }
                    log.info("Found balance {} for user: {}, accountId: {}", totalBalance, username, accountId);
                } else {
                    log.warn("Balance is null in account data for user: {}", username);
                }
            } else {
                log.warn("No accounts found for user: {}. Balance will be 0.00", username);
            }
            
            userData.setAccountId(accountId);
            userData.setBalance(totalBalance);
            
            return userData;
        } catch (Exception e) {
            log.error("Error getting user data for: {}", username, e);
            throw new RuntimeException("Ошибка получения данных пользователя: " + e.getMessage());
        }
    }
    
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
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
            String url = gatewayUrl + "/api/accounts/users/username/" + username + "/profile";
            
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
            String url = gatewayUrl + "/api/accounts/users/username/" + username + "/password";
            
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
        if (username == null || username.trim().isEmpty()) {
            log.error("Username is null or empty for deposit operation");
            throw new RuntimeException("Имя пользователя не указано");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid amount for deposit: {}", amount);
            throw new RuntimeException("Неверная сумма для пополнения");
        }
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
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error processing deposit for username: {}", username, e);
            throw new RuntimeException("Ошибка пополнения счета: " + e.getMessage());
        }
    }

    public void withdraw(String username, BigDecimal amount) {
        if (username == null || username.trim().isEmpty()) {
            log.error("Username is null or empty for withdraw operation");
            throw new RuntimeException("Имя пользователя не указано");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid amount for withdrawal: {}", amount);
            throw new RuntimeException("Неверная сумма для снятия");
        }
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
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error processing withdrawal for username: {}", username, e);
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
            String url = gatewayUrl + "/api/accounts/users/register";
            
            // Split name into first and last name
            String[] nameParts = name.trim().split("\\s+", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            // Generate email from username, removing spaces
            String cleanUsername = username.replaceAll("\\s+", "");
            String email = cleanUsername + "@bank.local";
            
            Map<String, String> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("password", password);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            userData.put("email", email);
            userData.put("birthDate", birthdate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(userData, headers);
            
            restTemplate.postForObject(url, request, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("HTTP error registering user: {}. Status: {}, Response: {}", 
                username, e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // Try to extract validation errors from response
            String errorMessage = "Ошибка регистрации";
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> errorResponse = mapper.readValue(e.getResponseBodyAsString(), Map.class);
                
                // Check if there are detailed validation errors
                if (errorResponse.containsKey("errors") && errorResponse.get("errors") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) errorResponse.get("errors");
                    if (!errors.isEmpty()) {
                        errorMessage = String.join("; ", errors);
                    }
                } else if (errorResponse.containsKey("error")) {
                    errorMessage = errorResponse.get("error").toString();
                }
            } catch (Exception parseException) {
                log.warn("Could not parse error response, using default message", parseException);
                // Use the raw response if parsing fails
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.isEmpty()) {
                    errorMessage = "Ошибка регистрации: " + responseBody;
                }
            }
            
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            log.error("Error registering user: {}", username, e);
            throw new RuntimeException("Ошибка регистрации: " + e.getMessage());
        }
    }

    private Long getAccountId(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.error("Username is null or empty when trying to get account ID");
            throw new RuntimeException("Имя пользователя не указано");
        }
        try {
            UserDataDTO userData = getUserData(username);
            if (userData == null) {
                log.error("User data is null for username: {}", username);
                throw new RuntimeException("Пользователь не найден: " + username);
            }
            Long accountId = userData.getAccountId();
            if (accountId == null) {
                log.info("No account found for user: {}. Creating new account automatically.", username);
                // Auto-create an account for the user
                accountId = createAccountForUser(userData.getId());
                log.info("Account created successfully for user: {}. Account ID: {}", username, accountId);
            }
            return accountId;
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error getting account ID for username: {}", username, e);
            throw new RuntimeException("Ошибка получения ID счета: " + e.getMessage());
        }
    }

    private Long createAccountForUser(Long userId) {
        try {
            String url = gatewayUrl + "/api/accounts/users/" + userId + "/accounts";
            log.debug("Creating account for user ID: {} at URL: {}", userId, url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(new HashMap<>(), headers);
            
            var response = restTemplate.exchange(url, HttpMethod.POST, request, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("message")) {

                String message = responseBody.get("message").toString();
                String[] parts = message.split(": ");
                if (parts.length > 1) {
                    return Long.parseLong(parts[parts.length - 1]);
                }
            }
            
            String accountsUrl = gatewayUrl + "/api/accounts/users/" + userId + "/accounts";
            var accountsResponse = restTemplate.exchange(accountsUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            List<Map<String, Object>> accounts = accountsResponse.getBody();
            if (accounts != null && !accounts.isEmpty()) {
                // Get last created account
                Map<String, Object> latestAccount = accounts.get(accounts.size() - 1);
                return getLongValue(latestAccount.get("id"));
            }
            
            throw new RuntimeException("Failed to create account: Unable to retrieve account ID");
        } catch (Exception e) {
            log.error("Error creating account for user ID: {}", userId, e);
            throw new RuntimeException("Ошибка создания счета: " + e.getMessage());
        }
    }
}
