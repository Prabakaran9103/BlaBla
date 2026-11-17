package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole());
        return switch (user.getRole()) {
            case ADMIN -> "dashboard/admin";
            case STAFF -> "dashboard/staff";
            case FACULTY -> "dashboard/faculty";
            case STUDENT -> "dashboard/student";
        };
    }
}
