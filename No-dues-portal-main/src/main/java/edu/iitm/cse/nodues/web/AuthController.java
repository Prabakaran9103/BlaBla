package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class AuthController {

    private final UserRegistrationService registrationService;
    private final boolean googleEnabled;

    public AuthController(
            UserRegistrationService registrationService,
            @Value("${app.google.enabled:false}") boolean googleEnabled) {
        this.registrationService = registrationService;
        this.googleEnabled = googleEnabled;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleEnabled", googleEnabled);
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("authForm", new AuthForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("authForm") AuthForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) return "auth/register";
        try {
            registrationService.register(form.getName(), form.getEmail(), form.getPassword());
            return "redirect:/login?registered";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "auth/register";
        }
    }
}
