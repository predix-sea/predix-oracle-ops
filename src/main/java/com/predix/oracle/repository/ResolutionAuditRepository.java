package com.predix.oracle.repository;

import com.predix.oracle.repository.entity.ResolutionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResolutionAuditRepository extends JpaRepository<ResolutionAuditEntity, Long> {

    List<ResolutionAuditEntity> findByMarketIdOrderByCreatedAtDesc(String marketId);
}
