package com.predix.oracle.audit;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.repository.ResolutionAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private ResolutionAuditRepository repository;
    @InjectMocks private AuditService auditService;

    @Test
    void persistsAudit() {
        auditService.record("m1", "REQUEST", ActorType.SYSTEM, "TEST",
                Map.of("in", 1), Map.of("out", 2));
        verify(repository).save(any());
    }
}
