package com.wms.repository;

import com.wms.entity.access_control.User;
import com.wms.entity.access_control.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Repository quản lý refresh token — tìm/xóa token theo user hoặc giá trị token (Spec 001). */
@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    void deleteByUser(User user);

    void deleteByUserEmail(String email);
}
