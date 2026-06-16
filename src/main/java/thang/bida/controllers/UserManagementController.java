package thang.bida.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import thang.bida.dto.CreateStaffRequest;
import thang.bida.dto.UpdateStaffRequest;
import thang.bida.model.User;
import thang.bida.services.UserService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    // Danh sách nhân viên (STAFF)
    @GetMapping
    public ResponseEntity<List<User>> getAllStaffs() {
        return ResponseEntity.ok(userService.getAllStaffs());
    }

    // Tạo nhân viên - chỉ nhận 1 role
    @PostMapping
    public ResponseEntity<User> createStaff(@RequestBody CreateStaffRequest req) {
        return ResponseEntity.ok(
                userService.createStaff(
                        req.getPhone(), // Đã sửa: username -> phone
                        req.getEmail(),
                        req.getPassword(),
                        req.getFullName(),
                        req.getAddress(),
                        req.getImageUrl(),
                        req.getRole()));
    }

    // Cập nhật nhân viên
    @PatchMapping("/{id}")
    public ResponseEntity<User> updateStaff(
            @PathVariable Long id,
            @RequestBody UpdateStaffRequest req) {
        return ResponseEntity.ok(
                userService.updateStaff(
                        id,
                        req.getPhone(), // Đã sửa: username -> phone
                        req.getEmail(),
                        req.getPassword(),
                        req.getFullName(),
                        req.getAddress(),
                        req.getImageUrl(),
                        req.getRole()));
    }

    // Khóa / mở tài khoản
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        userService.toggleUserStatus(id, active);
        return ResponseEntity.ok().build();
    }

    // Xóa nhân viên
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return userService.deleteUser(id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}