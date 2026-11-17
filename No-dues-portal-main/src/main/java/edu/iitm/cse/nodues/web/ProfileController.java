package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.service.ProfileService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mail.MailException;
import org.springframework.mail.MailAuthenticationException;

@Controller
public class ProfileController {
    private final ProfileService profiles;
    private final RequestController requests;

    public ProfileController(ProfileService profiles, RequestController requests) {
        this.profiles = profiles;
        this.requests = requests;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("user", profiles.get(requests.currentUserId(principal)));
        return "profile/index";
    }

    @PostMapping("/profile")
    public String update(Principal principal, @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String oldPassword,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false, defaultValue = "") String confirmPassword,
            @RequestParam(required = false) MultipartFile picture, Model model) {
        try {
            profiles.update(requests.currentUserId(principal), name, oldPassword,
                    password, confirmPassword, picture);
            return "redirect:/profile?updated";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("user", profiles.get(requests.currentUserId(principal)));
            model.addAttribute("error", exception.getMessage());
            return "profile/index";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotForm() { return "auth/forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email, Model model) {
        try {
            var token = profiles.createResetToken(email);
            model.addAttribute("message", "A password reset link was sent to your email.");
        } catch (MailAuthenticationException exception) {
            model.addAttribute("error",
                    "Email authentication failed. Check MAIL_USERNAME and MAIL_PASSWORD in .env.");
        } catch (IllegalArgumentException | IllegalStateException | MailException exception) {
            model.addAttribute("error", "The reset email could not be sent. Check your email settings.");
        }
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @RequestParam String password, Model model) {
        try {
            profiles.resetPassword(token, password);
            return "redirect:/login?reset";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("token", token);
            model.addAttribute("error", exception.getMessage());
            return "auth/reset-password";
        }
    }
}
