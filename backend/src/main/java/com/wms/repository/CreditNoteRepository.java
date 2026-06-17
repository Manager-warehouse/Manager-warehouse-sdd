package com.wms.repository;

import com.wms.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);
    List<CreditNote> findByDealerId(Long dealerId);
    boolean existsByReceiptId(Long receiptId);
}
