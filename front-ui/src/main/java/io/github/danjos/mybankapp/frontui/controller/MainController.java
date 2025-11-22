package io.github.danjos.mybankapp.frontui.controller;

import io.github.danjos.mybankapp.frontui.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final BankService bankService;

    @GetMapping("/")
    public String home() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return "redirect:/login";
            }

            String username = auth.getName();
            log.info("Loading main page for user: {}", username);

            // Get user data
            var userData = bankService.getUserData(username);
            model.addAttribute("login", username);
            model.addAttribute("name", userData.getName());
            model.addAttribute("birthdate", userData.getBirthDate());
            model.addAttribute("balance", userData.getBalance());

            // Get users list for transfers
            var users = bankService.getAllUsers();
            model.addAttribute("users", users);

            // Flash attributes for errors are automatically added to model by Spring
            // They will be null if not set, which is the expected behavior

            return "main";
        } catch (Exception e) {
            log.error("Error loading main page", e);
            model.addAttribute("errorMessage", "Ошибка загрузки данных: " + e.getMessage());
            return "main";
        }
    }

    @PostMapping("/user/{login}/editUserAccount")
    public String editUserAccount(@PathVariable String login,
                                 @RequestParam String name,
                                 @RequestParam String birthdate,
                                 RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating user account for: {}", login);
            bankService.updateUserProfile(login, name, birthdate);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");
        } catch (Exception e) {
            log.error("Error updating user account", e);
            redirectAttributes.addFlashAttribute("userAccountErrors", List.of(e.getMessage()));
        }
        return "redirect:/main";
    }

    @PostMapping("/user/{login}/editPassword")
    public String editPassword(@PathVariable String login,
                              @RequestParam String password,
                              @RequestParam String confirm_password,
                              RedirectAttributes redirectAttributes) {
        try {
            log.info("Updating password for user: {}", login);
            
            if (!password.equals(confirm_password)) {
                redirectAttributes.addFlashAttribute("passwordErrors", 
                    List.of("Пароли не совпадают"));
                return "redirect:/main";
            }

            bankService.updatePassword(login, password);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно изменен");
        } catch (Exception e) {
            log.error("Error updating password", e);
            redirectAttributes.addFlashAttribute("passwordErrors", List.of(e.getMessage()));
        }
        return "redirect:/main";
    }

    @PostMapping("/user/{login}/cash")
    public String cashOperation(@PathVariable String login,
                               @RequestParam String action,
                               @RequestParam BigDecimal value,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Processing cash operation for user: {}, action: {}, value: {}", 
                    login, action, value);

            if ("PUT".equals(action)) {
                bankService.deposit(login, value);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Средства успешно зачислены на счет");
            } else if ("GET".equals(action)) {
                bankService.withdraw(login, value);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Средства успешно сняты со счета");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Неверная операция");
            }
        } catch (Exception e) {
            log.error("Error processing cash operation", e);
            redirectAttributes.addFlashAttribute("cashErrors", List.of(e.getMessage()));
        }
        return "redirect:/main";
    }

    @PostMapping("/user/{login}/transfer")
    public String transfer(@PathVariable String login,
                          @RequestParam String to_login,
                          @RequestParam BigDecimal value,
                          RedirectAttributes redirectAttributes) {
        try {
            log.info("Processing transfer from {} to {} for amount {}", 
                    login, to_login, value);

            if (login.equals(to_login)) {
                redirectAttributes.addFlashAttribute("transferOtherErrors", 
                    List.of("Нельзя переводить средства самому себе"));
                return "redirect:/main";
            }

            bankService.transfer(login, to_login, value);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Перевод успешно выполнен");
        } catch (Exception e) {
            log.error("Error processing transfer", e);
            redirectAttributes.addFlashAttribute("transferOtherErrors", List.of(e.getMessage()));
        }
        return "redirect:/main";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль");
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String login,
                        @RequestParam String password,
                        @RequestParam String confirm_password,
                        @RequestParam String name,
                        @RequestParam String birthdate,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            log.info("Processing registration for user: {}", login);

            // Client-side validation
            List<String> validationErrors = new java.util.ArrayList<>();
            
            if (password == null || password.length() < 6) {
                validationErrors.add("Пароль должен содержать минимум 6 символов");
            }
            
            if (password != null && confirm_password != null && !password.equals(confirm_password)) {
                validationErrors.add("Пароли не совпадают");
            }
            
            if (name == null || name.trim().isEmpty()) {
                validationErrors.add("Фамилия и имя обязательны для заполнения");
            } else {
                String[] nameParts = name.trim().split("\\s+");
                if (nameParts.length < 2) {
                    validationErrors.add("Пожалуйста, укажите и фамилию, и имя (через пробел)");
                }
            }
            
            if (!validationErrors.isEmpty()) {
                model.addAttribute("errors", validationErrors);
                model.addAttribute("login", login);
                model.addAttribute("name", name);
                model.addAttribute("birthdate", birthdate);
                return "signup";
            }

            bankService.registerUser(login, password, name, birthdate);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Регистрация успешно завершена. Теперь вы можете войти в систему.");
            return "redirect:/main";
        } catch (Exception e) {
            log.error("Error during registration", e);
            model.addAttribute("errors", List.of(e.getMessage()));
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            return "signup";
        }
    }
}
