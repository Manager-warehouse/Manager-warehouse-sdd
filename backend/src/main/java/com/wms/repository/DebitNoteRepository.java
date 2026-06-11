package com.wms.repository;

import com.wms.entity.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    boolean existsByDebitNoteNumber(String debitNoteNumber);

    Optional<DebitNote> findByReceiptId(Long receiptId);
}
