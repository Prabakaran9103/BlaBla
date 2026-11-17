package edu.iitm.cse.nodues.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain webSecurity(
            HttpSecurity http,
            @Value("${app.google.enabled:false}") boolean googleEnabled,
            GoogleLoginSuccessHandler googleLoginSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/forgot-password",
                                "/reset-password", "/error", "/css/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        if (googleEnabled) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .successHandler(googleLoginSuccessHandler)
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?googleError")));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
