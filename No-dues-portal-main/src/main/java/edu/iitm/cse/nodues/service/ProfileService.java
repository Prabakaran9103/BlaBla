package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.PasswordResetToken;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.repository.PasswordResetTokenRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final PortalEmailService emailService;
    private final Path pictureDirectory;

    public ProfileService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder encoder, PortalEmailService emailService,
            @Value("${app.file-storage-path:data/uploads}") String storage) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.emailService = emailService;
        this.pictureDirectory = Path.of(storage, "profile");
    }

    @Transactional
    public User update(String userId, String name, String oldPassword,
            String password, String confirmPassword, MultipartFile picture) {
        User user = get(userId);
        if (name != null && !name.isBlank()) user.changeName(name.trim());
        if (password != null && !password.isBlank()) {
            if (oldPassword == null || oldPassword.isBlank()
                    || user.getPasswordHash() == null
                    || !encoder.matches(oldPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("The old password is not correct");
            }
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("New password and confirmation do not match");
            }
            if (password.length() < 8) throw new IllegalArgumentException("Password must have at least eight characters");
            user.setPasswordHash(encoder.encode(password));
        }
        if (picture != null && !picture.isEmpty()) {
            try {
                Files.createDirectories(pictureDirectory);
                String fileName = userId + "-" + Path.of(picture.getOriginalFilename()).getFileName();
                Path destination = pictureDirectory.resolve(fileName).normalize();
                if (!destination.startsWith(pictureDirectory)) throw new IllegalArgumentException("Invalid picture name");
                Files.copy(picture.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                user.changeDisplayPictureReference(fileName);
            } catch (Exception exception) {
                throw new IllegalArgumentException("The display picture could not be saved", exception);
            }
        }
        return users.save(user);
    }

    @Transactional
    public PasswordResetToken createResetToken(String email) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("No account was found for this email"));
        PasswordResetToken token = tokens.save(new PasswordResetToken(user.getId()));
        emailService.sendPasswordReset(user.getId(), token.getToken());
        return token;
    }

    @Transactional
    public void resetPassword(String token, String password) {
        PasswordResetToken reset = tokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("This reset link is not valid"));
        if (reset.isExpired()) throw new IllegalArgumentException("This reset link has expired");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must have at least eight characters");
        User user = get(reset.getUserId());
        user.setPasswordHash(encoder.encode(password));
        users.save(user);
        tokens.delete(reset);
    }

    public User get(String id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account was not found"));
    }
}
