package com.wms.repository;

import com.wms.entity.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    List<TransferRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByRequestNumber(String requestNumber);
}
