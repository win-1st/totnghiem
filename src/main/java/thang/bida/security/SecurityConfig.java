package thang.bida.security;

import thang.bida.security.jwt.AuthEntryPointJwt;
import thang.bida.security.jwt.AuthTokenFilter;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Autowired
        private AuthEntryPointJwt unauthorizedHandler;

        @Autowired
        private AuthTokenFilter authTokenFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                                                .permitAll()

                                                // Categories - GET cho phép nhiều role, POST/PUT/DELETE chỉ ADMIN
                                                .requestMatchers(HttpMethod.GET, "/api/categories/**")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/categories/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/categories/**")
                                                .hasRole("ADMIN")

                                                // Products - GET cho phép nhiều role, POST/PUT/DELETE chỉ ADMIN
                                                .requestMatchers(HttpMethod.GET, "/api/products/**")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                                                // Tables
                                                .requestMatchers("/api/tables/**").hasAnyRole("ADMIN", "STAFF")

                                                // Orders
                                                .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "STAFF")

                                                // Users - chỉ ADMIN
                                                .requestMatchers("/api/users/**").hasRole("ADMIN")

                                                // Dashboard - chỉ ADMIN
                                                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                                                // Các request còn lại yêu cầu authenticated
                                                .anyRequest().authenticated());

                http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "http://localhost:3001"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setExposedHeaders(Arrays.asList("Authorization"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}