package thang.bida.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import thang.bida.dto.ApiResponse;
import thang.bida.dto.ChangePasswordRequest;
import thang.bida.dto.ForgotPasswordRequest;
import thang.bida.dto.ResetPasswordRequest;
import thang.bida.dto.VerifyOtpRequest;
import thang.bida.model.User;
import thang.bida.payload.request.LoginRequest;
import thang.bida.payload.request.SignupRequest;
import thang.bida.payload.response.JwtResponse;
import thang.bida.payload.response.MessageResponse;
import thang.bida.repository.UserRepository;
import thang.bida.security.jwt.JwtUtils;
import thang.bida.services.PasswordResetService;
import thang.bida.services.UserDetailsImpl;
import thang.bida.services.UserService;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final UserDetailsService userDetailsService; // ← THÊM DÒNG NÀY

    public AuthController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService,
            UserService userService,
            UserDetailsService userDetailsService) { // ← THÊM THAM SỐ
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.userService = userService;
        this.userDetailsService = userDetailsService; // ← THÊM DÒNG NÀY
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running! Auth controller is healthy.");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        userService.registerUser(signUpRequest);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // SỬA ĐOẠN NÀY
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFullName(),
                userDetails.getEmail(),
                roles));
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
        response.put("role", user.getRole() != null ? user.getRole().name() : "CUSTOMER");

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
}