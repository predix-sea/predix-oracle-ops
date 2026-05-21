package com.predix.oracle.service;

import com.predix.oracle.controller.dto.CreateSourceRequest;
import com.predix.oracle.controller.dto.PatchSourceRequest;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.repository.OracleSourceRepository;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OracleSourceServiceTest {

    @Mock private OracleSourceRepository repository;
    @InjectMocks private OracleSourceService service;

    @Test
    void createsSource() {
        CreateSourceRequest req = new CreateSourceRequest();
        req.setName("feed");
        req.setBaseUrl("http://x");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OracleSourceEntity saved = service.create(req);
        assertThat(saved.getName()).isEqualTo("feed");
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    void getNotFoundThrows() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(OracleException.class);
    }

    @Test
    void patchesSource() {
        OracleSourceEntity entity = OracleSourceEntity.builder().id(1L).name("a").baseUrl("old").build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PatchSourceRequest patch = new PatchSourceRequest();
        patch.setBaseUrl("new");
        assertThat(service.patch(1L, patch).getBaseUrl()).isEqualTo("new");
    }
}
