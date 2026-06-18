package com.wms.repository;

import com.wms.entity.Invoice;
import com.wms.enums.InvoiceStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByDealerIdAndStatusInAndDueDateBefore(Long dealerId,
                                                        Iterable<InvoiceStatus> statuses,
                                                        LocalDate dueDate);
}
