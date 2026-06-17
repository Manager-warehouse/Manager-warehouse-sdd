package com.wms.repository;

import com.wms.entity.DocumentSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select documentSequence from DocumentSequence documentSequence "
            + "where documentSequence.sequenceKey = :sequenceKey")
    Optional<DocumentSequence> findBySequenceKeyForUpdate(@Param("sequenceKey") String sequenceKey);
}
