package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.service.NotificationService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestController requestController;

    public NotificationController(
            NotificationService notificationService,
            RequestController requestController) {
        this.notificationService = notificationService;
        this.requestController = requestController;
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        String userId = requestController.currentUserId(principal);
        model.addAttribute("notifications", notificationService.listForUser(userId));
        return "notifications/list";
    }

    @PostMapping("/notifications/{notificationId}/read")
    public String markRead(@PathVariable String notificationId, Principal principal) {
        notificationService.markRead(notificationId, requestController.currentUserId(principal));
        return "redirect:/notifications";
    }
}
