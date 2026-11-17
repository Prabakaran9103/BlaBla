package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final NoDuesRequestRepository requestRepository;

    public HomeController(
            UserRepository userRepository,
            NoDuesRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("studentCount", userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.STUDENT).count());
        model.addAttribute("staffCount", userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.STAFF).count());
        model.addAttribute("facultyCount", userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.FACULTY).count());
        model.addAttribute("requestCount", requestRepository.count());
        return "home";
    }
}
