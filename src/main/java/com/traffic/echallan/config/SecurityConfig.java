package com.traffic.echallan.config;

import com.traffic.echallan.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> repo.findByUsername(username)
                .filter(com.traffic.echallan.entity.User::isActive)
                .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getUsername())
                        .password(u.getPasswordHash()).roles(u.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JSON 401 without WWW-Authenticate, so browsers never show the ugly Basic-auth popup
        AuthenticationEntryPoint entry = (req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"message\":\"Authentication required or invalid credentials\"}");
        };
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.authenticationEntryPoint(entry))
            .exceptionHandling(e -> e.authenticationEntryPoint(entry))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/*.html", "/js/**", "/css/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/penalty-rules/**", "/api/violations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/penalty-rules/**", "/api/violations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "OFFICER")
                .anyRequest().permitAll());
        return http.build();
    }
}
