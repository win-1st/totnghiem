package thang.bida.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import thang.bida.dto.ApiResponse;
import thang.bida.dto.ChangePasswordRequest;
import thang.bida.dto.ForgotPasswordRequest;
import thang.bida.dto.LoginDto;
import thang.bida.dto.RegisterDto;
import thang.bida.dto.ResetPasswordRequest;
import thang.bida.dto.VerifyOtpRequest;
import thang.bida.model.ERole;
import thang.bida.model.Role;
import thang.bida.model.User;
import thang.bida.repository.RoleRepository;
import thang.bida.repository.UserRepository;
import thang.bida.security.jwt.JwtUtils;
import thang.bida.services.PasswordResetService;
import thang.bida.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    // ✅ Health check
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running! Auth controller is healthy.");
    }

    // ✅ Register - CHỈ GIỮ 1 METHOD NÀY
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
        System.out.println("=== REGISTER ===");
        System.out.println("Username: " + registerDto.getUsername());
        System.out.println("Email: " + registerDto.getEmail());
        System.out.println("FullName: " + registerDto.getFullName());
        System.out.println("Phone: " + registerDto.getPhone());

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            return new ResponseEntity<>("Username is taken!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            return new ResponseEntity<>("Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        // ✅ THÊM CÁC FIELD NÀY
        user.setFullName(registerDto.getFullName());
        user.setPhone(registerDto.getPhone());
        user.setAddress(registerDto.getAddress());
        if (registerDto.getImageUrl() != null) {
            user.setImageUrl(registerDto.getImageUrl());
        }

        Set<Role> roles = new HashSet<>();

        if (registerDto.getRoles() == null || registerDto.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Error: USER Role is not found."));
            roles.add(userRole);
            System.out.println("Assigning default CUSTOMER role");
        } else {
            for (String roleName : registerDto.getRoles()) {
                try {
                    String roleEnumName = "ROLE_" + roleName.toUpperCase();
                    ERole roleEnum = ERole.valueOf(roleEnumName);
                    Role role = roleRepository.findByName(roleEnum)
                            .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " is not found."));
                    roles.add(role);
                    System.out.println("Assigning role: " + roleName);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid role requested: " + roleName);
                    return new ResponseEntity<>("Invalid role: " + roleName, HttpStatus.BAD_REQUEST);
                }
            }
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        System.out.println("User registered successfully with ID: " + savedUser.getId());
        System.out.println("Assigned roles: " + roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));

        return new ResponseEntity<>("User registered success!", HttpStatus.OK);
    }

    // ✅ Login với refresh token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        try {
            System.out.println("🔐 LOGIN ATTEMPT ==================================");
            System.out.println("Username: " + loginDto.getUsername());

            // Kiểm tra user
            User user = userRepository.findByUsername(loginDto.getUsername())
                    .orElseThrow(() -> {
                        System.out.println("❌ USER NOT FOUND: " + loginDto.getUsername());
                        return new RuntimeException("User not found");
                    });

            System.out.println("✅ User found: " + user.getUsername());
            System.out.println("👤 Full Name: " + user.getFullName());
            System.out.println("👥 Roles count: " + user.getRoles().size());
            user.getRoles().forEach(role -> System.out.println("   - Role: " + role.getName()));

            // Authentication
            System.out.println("🔄 Attempting authentication...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            System.out.println("🎉 Authentication SUCCESS!");

            // Generate tokens
            System.out.println("🔑 Generating JWT token...");
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            System.out.println("✅ JWT Token generated, length: " + jwt.length());
            System.out.println("✅ Refresh Token generated, length: " + refreshToken.length());

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            System.out.println("✅ Login successful! User: " + userDetails.getUsername());
            System.out.println("✅ Full Name: " + user.getFullName());
            System.out.println("✅ Roles: " + roles);
            System.out.println("==================================================");

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("refreshToken", refreshToken);
            response.put("type", "Bearer");
            response.put("id", userDetails.getId());
            response.put("username", userDetails.getUsername());
            response.put("fullName", user.getFullName());
            response.put("email", userDetails.getEmail());
            response.put("roles", roles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ LOGIN FAILED: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==================================================");
            return new ResponseEntity<>("Invalid username or password! Error: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // ✅ Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Refresh token is required");
            }

            System.out.println("🔄 REFRESH TOKEN ATTEMPT ===========================");

            if (!jwtUtils.validateJwtToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            if (jwtUtils.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            System.out.println("✅ Valid refresh token for user: " + username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> (GrantedAuthority) () -> role.getName().name())
                    .collect(Collectors.toList());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, authorities);

            String newAccessToken = jwtUtils.generateJwtToken(authentication);
            String newRefreshToken = jwtUtils.generateRefreshToken(authentication);

            System.out.println("✅ Token refresh successful!");
            System.out.println("==================================================");

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ REFRESH TOKEN FAILED: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token refresh failed: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("imageUrl", user.getImageUrl());
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("roles", user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken() {
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.sendPasswordResetEmail(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không tìm thấy email hoặc có lỗi xảy ra. Vui lòng thử lại."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
            BindingResult bindingResult) {

        System.out.println("=== VERIFY OTP CONTROLLER ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("OTP: " + request.getOtp());

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation error: " + errorMessage));
        }

        try {
            var tokenOptional = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());

            if (tokenOptional.isPresent()) {
                System.out.println("OTP verification successful. Token: " + tokenOptional.get());
                return ResponseEntity.ok(new ApiResponse(true,
                        "Xác thực OTP thành công",
                        Map.of("token", tokenOptional.get())));
            } else {
                System.out.println("OTP verification failed");
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        "Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            System.err.println("Error in verifyOtp controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Token không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.resendOtp(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đã gửi lại mã OTP. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không thể gửi lại mã OTP. Vui lòng thử lại."));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TokenResponse {
        private String token;
    }
}