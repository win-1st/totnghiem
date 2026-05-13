package thang.bida.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import thang.bida.model.ERole;
import thang.bida.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findByRolesNameIn(@Param("roleNames") List<ERole> roleNames);

    // === THÊM CÁC METHOD MỚI ===

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
    List<User> findByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") ERole roleName);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    Long countByRolesNameIn(@Param("roleNames") List<ERole> roleNames);

    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:name%")
    List<User> findByFullNameContaining(@Param("name") String name);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.isActive = :isActive AND r.name IN :roleNames")
    List<User> findByIsActiveAndRolesNameIn(@Param("isActive") Boolean isActive,
            @Param("roleNames") List<ERole> roleNames);

    // Method để kiểm tra xem email có tồn tại với user khác không (dùng cho update)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id != :userId")
    Boolean existsByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);

    // Method để kiểm tra xem username có tồn tại với user khác không (dùng cho
    // update)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.id != :userId")
    Boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("userId") Long userId);

    long countByCreatedAtAfter(LocalDateTime time);

    long countByRoles_Name(ERole name);

    Long countByIsActiveTrue();

    List<User> findTop5ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime cutoff);

    // Thêm method để lấy số người dùng theo khoảng thời gian
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}