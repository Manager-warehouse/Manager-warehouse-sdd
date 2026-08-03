package com.wms.repository;


import com.wms.entity.billing_payment.DebitNote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    boolean existsByDebitNoteNumber(String debitNoteNumber);

    Optional<DebitNote> findByReceiptId(Long receiptId);
}
