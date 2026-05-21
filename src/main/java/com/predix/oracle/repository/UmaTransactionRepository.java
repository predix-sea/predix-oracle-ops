package com.predix.oracle.repository;

import com.predix.oracle.repository.entity.UmaTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UmaTransactionRepository extends JpaRepository<UmaTransactionEntity, Long> {

    List<UmaTransactionEntity> findByMarketIdOrderByCreatedAtDesc(String marketId);

    Optional<UmaTransactionEntity> findFirstByAssertionIdOrderByCreatedAtDesc(String assertionId);
}
