package thang.bida.config;

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

        @Bean
        public AuthTokenFilter authenticationJwtTokenFilter() {
                return new AuthTokenFilter();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ========== PUBLIC ==========
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/ws/**").permitAll()
                                                .requestMatchers("/api/reservations/create").permitAll()
                                                .requestMatchers("/api/reservations/check-availability").permitAll()
                                                .requestMatchers("/api/promotions/active").permitAll()
                                                .requestMatchers("/api/promotions/current").permitAll()
                                                .requestMatchers("/api/promotions/public/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                                .requestMatchers("/api/products/non-time-based").permitAll()
                                                .requestMatchers("/api/promotions/*/products").permitAll()
                                                // PayOS webhook (public)
                                                .requestMatchers("/api/payos/webhook").permitAll()
                                                .requestMatchers("/api/payos/check-status/**").permitAll()

                                                // OPTIONS cho tất cả
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // ========== AUTHENTICATED ==========
                                                .requestMatchers("/api/chat/**").authenticated()

                                                // ✅ THÊM: API lấy lịch sử đặt bàn của customer
                                                .requestMatchers("/api/reservations/my-reservations").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/reservations/phone/**")
                                                .hasAnyRole("ADMIN", "STAFF", "MANAGER")

                                                .requestMatchers(HttpMethod.GET, "/api/tables/status/**").permitAll()

                                                // ========== CUSTOMER + STAFF + ADMIN ==========
                                                .requestMatchers(HttpMethod.GET, "/api/products/**")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/categories/**")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/product-types/**")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/tables/**")
                                                .hasAnyRole("ADMIN", "STAFF")

                                                .requestMatchers(HttpMethod.GET, "/api/promotions/active")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")
                                                .requestMatchers(HttpMethod.GET, "/api/promotions/current")
                                                .hasAnyRole("ADMIN", "STAFF", "CUSTOMER")

                                                // ✅ THÊM: Cho phép STAFF xem sản phẩm trong khuyến mãi
                                                .requestMatchers(HttpMethod.GET, "/api/promotions/*/products")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers(HttpMethod.GET, "/api/promotions/*/available-products")
                                                .hasAnyRole("ADMIN", "STAFF")

                                                .requestMatchers(HttpMethod.GET, "/api/time-based/**")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/customer-points/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/auth/me").authenticated()

                                                // ========== STAFF + ADMIN ==========
                                                .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/bills/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/payos/create").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers(HttpMethod.GET, "/api/dashboard/**")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers(HttpMethod.POST, "/api/tables/**")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers(HttpMethod.PUT, "/api/tables/**")
                                                .hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers(HttpMethod.PATCH, "/api/tables/**")
                                                .hasAnyRole("ADMIN", "STAFF")

                                                // ========== ADMIN ONLY ==========
                                                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/categories/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/categories/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/tables/**").hasRole("ADMIN")

                                                // ✅ SỬA: Chỉ các method POST/PUT/DELETE mới yêu cầu ADMIN
                                                // GET thì STAFF vẫn xem được
                                                .requestMatchers(HttpMethod.POST, "/api/promotions/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/promotions/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/promotions/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/promotions/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers(HttpMethod.PUT, "/api/time-based/**").hasRole("ADMIN")
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/dashboard/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/dashboard/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/dashboard/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/inventory/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/product-types/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/product-types/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/product-types/**")
                                                .hasRole("ADMIN")

                                                .anyRequest().authenticated());

                http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

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
                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);

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