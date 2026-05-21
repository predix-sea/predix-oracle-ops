package com.predix.oracle.audit;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.repository.ResolutionAuditRepository;
import com.predix.oracle.repository.entity.ResolutionAuditEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final ResolutionAuditRepository auditRepository;

    @Transactional
    public void record(String marketId, String phase, ActorType actor, String action,
                       Map<String, Object> input, Map<String, Object> output) {
        ResolutionAuditEntity audit = ResolutionAuditEntity.builder()
                .marketId(marketId)
                .phase(phase)
                .actorType(actor)
                .action(action)
                .inputSnapshot(input)
                .outputSnapshot(output)
                .build();
        auditRepository.save(audit);
    }
}
