package com.wms.repository;

import com.wms.entity.User;
import com.wms.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByCode(String code);
    Optional<User> findByRefreshTokenHash(String refreshTokenHash);
    List<User> findByRole(UserRole role);
}
