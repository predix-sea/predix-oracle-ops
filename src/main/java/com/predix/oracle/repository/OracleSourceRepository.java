package com.predix.oracle.repository;

import com.predix.oracle.repository.entity.OracleSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OracleSourceRepository extends JpaRepository<OracleSourceEntity, Long> {

    List<OracleSourceEntity> findByEnabledTrueOrderByPriorityAsc();
}
