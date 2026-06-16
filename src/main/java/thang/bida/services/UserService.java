package thang.bida.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import thang.bida.model.Role;
import thang.bida.model.User;
import thang.bida.dto.RegisterRequest;
import thang.bida.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(RegisterRequest signUpRequest) {
        User user = new User();
        user.setPhone(signUpRequest.getPhone()); // Dùng phone
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setAddress(signUpRequest.getAddress());
        user.setImageUrl(signUpRequest.getImageUrl());

        if (signUpRequest.getRole() != null && !signUpRequest.getRole().isEmpty()) {
            try {
                Role role = Role.valueOf(signUpRequest.getRole().toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                user.setRole(Role.CUSTOMER);
            }
        } else {
            user.setRole(Role.CUSTOMER);
        }

        user.setIsActive(true);

        return userRepository.save(user);
    }

    public List<User> getAllStaffs() {
        return userRepository.findByRole(Role.STAFF);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createStaff(String phone, String email, String password, String fullName,
            String address, String imageUrl, String roleName) {

        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setAddress(address);
        user.setImageUrl(imageUrl);
        user.setIsActive(true);

        Role role;
        try {
            role = Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ: " + roleName);
        }
        user.setRole(role);

        return userRepository.save(user);
    }

    public User updateStaff(Long id, String phone, String email, String password,
            String fullName, String address, String imageUrl, String roleName) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));

        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        user.setPhone(phone);
        user.setEmail(email);
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setFullName(fullName);
        user.setAddress(address);
        if (imageUrl != null) {
            user.setImageUrl(imageUrl);
        }

        if (roleName != null && !roleName.isEmpty()) {
            try {
                Role role = Role.valueOf(roleName.toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Role không hợp lệ: " + roleName);
            }
        }

        return userRepository.save(user);
    }

    public User toggleUserStatus(Long id, Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));
        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}