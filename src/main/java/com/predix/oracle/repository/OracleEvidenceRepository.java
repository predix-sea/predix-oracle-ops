package com.predix.oracle.repository;

import com.predix.oracle.repository.entity.OracleEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OracleEvidenceRepository extends JpaRepository<OracleEvidenceEntity, Long> {

    List<OracleEvidenceEntity> findByMarketIdOrderByFetchedAtDesc(String marketId);
}
