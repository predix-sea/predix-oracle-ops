package com.predix.oracle.repository.entity;

import com.predix.oracle.domain.ActorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "resolution_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_id", nullable = false, length = 64)
    private String marketId;

    @Column(nullable = false, length = 64)
    private String phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Column(nullable = false, length = 128)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> inputSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> outputSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
