package com.predix.oracle.repository.entity;

import com.predix.oracle.domain.TxStatus;
import com.predix.oracle.domain.UmaActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "uma_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UmaTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_id", nullable = false, length = 64)
    private String marketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private UmaActionType actionType;

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(name = "assertion_id", length = 128)
    private String assertionId;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_status", nullable = false, length = 32)
    private TxStatus txStatus;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_receipt", columnDefinition = "jsonb")
    private Map<String, Object> rawReceipt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
