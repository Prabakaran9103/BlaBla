package edu.iitm.cse.nodues.config;

import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    public GoogleLoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = googleUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?googleError");
            return;
        }
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (!normalizedEmail.contains("@")) {
            response.sendRedirect("/login?googleError");
            return;
        }
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            if (!normalizedEmail.endsWith("@smail.iitm.ac.in")) {
                return null;
            }
            String name = googleUser.getAttribute("name");
            return userRepository.save(new User(
                    name == null || name.isBlank() ? normalizedEmail : name, normalizedEmail,
                    UserRole.STUDENT, LoginMethod.GOOGLE));
        });
        if (user == null || !user.isActive()) {
            clearAuthentication(request);
            response.sendRedirect("/login?googleDenied");
            return;
        }
        var localUser = org.springframework.security.core.userdetails.User
                .withUsername(normalizedEmail)
                .password("")
                .roles(user.getRole().name())
                .build();
        var localAuthentication = new UsernamePasswordAuthenticationToken(
                localUser, null, localUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(localAuthentication);
        new HttpSessionSecurityContextRepository().saveContext(
                SecurityContextHolder.getContext(), request, response);
        response.sendRedirect("/dashboard");
    }

    private void clearAuthentication(HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
