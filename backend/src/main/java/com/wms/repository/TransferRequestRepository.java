package com.wms.repository;


import com.wms.entity.warehouse_transfer.TransferRequest;
import com.wms.enums.warehouse_transfer.TransferRequestStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    List<TransferRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByRequestNumber(String requestNumber);
    boolean existsBySourceWarehouseIdAndDestinationWarehouseIdAndNeededByDateAndStatusIn(
            Long sourceWarehouseId,
            Long destinationWarehouseId,
            LocalDate neededByDate,
            List<TransferRequestStatus> statuses);

    boolean existsBySourceWarehouseIdAndDestinationWarehouseIdAndNeededByDateAndStatusInAndIdNot(
            Long sourceWarehouseId,
            Long destinationWarehouseId,
            LocalDate neededByDate,
            List<TransferRequestStatus> statuses,
            Long id);
}
