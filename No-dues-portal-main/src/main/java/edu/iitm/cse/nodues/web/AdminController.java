package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.service.AdminService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String admin(Principal principal, Model model) {
        String adminId = adminService.adminIdForEmail(principal.getName());
        model.addAttribute("users", adminService.users(adminId));
        model.addAttribute("settings", adminService.settings(adminId));
        return "admin/index";
    }

    @PostMapping("/admin/users")
    public String addUser(
            Principal principal,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam UserRole role,
            @RequestParam(required = false, defaultValue = "") String password) {
        String adminId = adminService.adminIdForEmail(principal.getName());
        adminService.addUser(adminId, name, email, role, password);
        return "redirect:/admin";
    }

    @PostMapping("/admin/threshold")
    public String threshold(Principal principal, @RequestParam int threshold) {
        String adminId = adminService.adminIdForEmail(principal.getName());
        adminService.changeReminderThreshold(adminId, threshold);
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/role")
    public String changeRole(
            Principal principal,
            @RequestParam String userId,
            @RequestParam UserRole role) {
        String adminId = adminService.adminIdForEmail(principal.getName());
        adminService.changeUserRole(adminId, userId, role);
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/remove")
    public String removeUser(Principal principal, @RequestParam String userId) {
        adminService.removeUser(adminService.adminIdForEmail(principal.getName()), userId);
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/update")
    public String updateUser(Principal principal, @RequestParam String userId,
            @RequestParam String name, @RequestParam String email,
            @RequestParam(required = false) UserRole role) {
        adminService.updateUser(adminService.adminIdForEmail(principal.getName()),
                userId, name, email, role);
        return "redirect:/admin";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String adminError() {
        return "redirect:/admin?error";
    }
}
