package thang.bida.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import thang.bida.model.Role;
import thang.bida.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm theo phone
    Optional<User> findByPhone(String phone);

    // Tìm theo email
    Optional<User> findByEmail(String email);

    // Kiểm tra tồn tại
    Boolean existsByPhone(String phone);

    Boolean existsByEmail(String email);

    // Tìm user theo role
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findUsersByRole(@Param("role") Role role);

    // Tìm user active theo role
    List<User> findByIsActiveTrueAndRole(Role role);

    // Tìm user theo tên (like)
    List<User> findByFullNameContaining(String name);

    // Tìm user mới nhất
    List<User> findTop5ByOrderByCreatedAtDesc();

    // Thống kê
    long countByRole(Role role);

    long countByCreatedAtAfter(LocalDateTime time);

    Long countByIsActiveTrue();

    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Kiểm tra email tồn tại với user khác (dùng cho update)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id != :userId")
    Boolean existsByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);

    // Kiểm tra phone tồn tại với user khác (dùng cho update)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phone = :phone AND u.id != :userId")
    Boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("userId") Long userId);
}