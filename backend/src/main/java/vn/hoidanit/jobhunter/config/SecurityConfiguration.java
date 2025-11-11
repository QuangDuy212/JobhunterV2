package vn.hoidanit.jobhunter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                    CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {

        // Danh sách các endpoint được phép truy cập công khai
        String[] whileList = {
                "/", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/register",
                "/storage/**", "/api/v1/email/**",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/ws/**", "/socket.io/**"
        };

        http
            // Tắt CSRF vì không cần thiết cho WebSocket và REST API
            .csrf(c -> c.disable())
            // Cấu hình CORS nếu cần
            .cors(Customizer.withDefaults())
            // Cấu hình các quy tắc xác thực
            .authorizeHttpRequests(authz -> authz
                // Cho phép truy cập công khai các endpoint trong whileList
                .requestMatchers(whileList).permitAll()
                // Cho phép truy cập công khai các endpoint GET cụ thể
                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                // Yêu cầu xác thực cho tất cả các endpoint khác
                .anyRequest().authenticated()
            )
            // Cấu hình OAuth2 Resource Server (JWT)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            // Vô hiệu hóa form login
            .formLogin(f -> f.disable())
            // Cấu hình session ở chế độ stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}