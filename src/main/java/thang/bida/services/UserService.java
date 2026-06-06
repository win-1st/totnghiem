package thang.bida.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import thang.bida.model.Role;
import thang.bida.model.User;
import thang.bida.payload.request.SignupRequest;
import thang.bida.repository.UserRepository;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(SignupRequest signUpRequest) {
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setPhone(signUpRequest.getPhone());
        user.setAddress(signUpRequest.getAddress());
        user.setImageUrl(signUpRequest.getImageUrl());

        // XỬ LÝ ROLE TỪ REQUEST
        Set<String> requestRoles = signUpRequest.getRole();
        if (requestRoles != null && !requestRoles.isEmpty()) {
            String roleStr = requestRoles.iterator().next();
            try {
                Role role = Role.valueOf(roleStr.toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                user.setRole(Role.CUSTOMER); // Mặc định nếu role không hợp lệ
            }
        } else {
            user.setRole(Role.CUSTOMER); // Mặc định là CUSTOMER
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

    public User createStaff(String username, String email, String password, String fullName,
            String phone, String address, String imageUrl, String roleName) {

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhone(phone);
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

    public User updateStaff(Long id, String username, String email, String password,
            String fullName, String phone, String address, String imageUrl, String roleName) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + id));

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        user.setUsername(username);
        user.setEmail(email);
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setFullName(fullName);
        user.setPhone(phone);
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