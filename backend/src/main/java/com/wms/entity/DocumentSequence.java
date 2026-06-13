package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_sequences")
@Getter
@Setter
public class DocumentSequence {

    @Id
    @Column(name = "sequence_key", length = 50)
    private String sequenceKey;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
