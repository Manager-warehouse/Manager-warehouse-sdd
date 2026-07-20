package com.wms.repository;

import com.wms.entity.CreditNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    boolean existsByCreditNoteNumber(String creditNoteNumber);
    boolean existsByReceiptId(Long receiptId);
    Optional<CreditNote> findByReceiptId(Long receiptId);
    List<CreditNote> findByDealerId(Long dealerId);
}
