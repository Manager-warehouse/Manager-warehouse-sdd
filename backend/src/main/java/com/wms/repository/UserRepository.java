package com.wms.repository;


import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository truy vấn bảng users — tìm theo email, mã NV, kiểm tra trùng lặp (Spec 001). */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByCode(String code);
    List<User> findByRole(UserRole role);
}
