package com.predix.oracle.service;

import com.predix.oracle.controller.dto.CreateSourceRequest;
import com.predix.oracle.controller.dto.PatchSourceRequest;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.repository.OracleSourceRepository;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OracleSourceService {

    private final OracleSourceRepository repository;

    public List<OracleSourceEntity> listAll() {
        return repository.findAll();
    }

    @Transactional
    public OracleSourceEntity create(CreateSourceRequest request) {
        OracleSourceEntity entity = OracleSourceEntity.builder()
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : "HTTP_API")
                .baseUrl(request.getBaseUrl())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .build();
        return repository.save(entity);
    }

    @Transactional
    public OracleSourceEntity patch(Long id, PatchSourceRequest request) {
        OracleSourceEntity entity = get(id);
        if (request.getBaseUrl() != null) entity.setBaseUrl(request.getBaseUrl());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getType() != null) entity.setType(request.getType());
        return repository.save(entity);
    }

    @Transactional
    public OracleSourceEntity setEnabled(Long id, boolean enabled) {
        OracleSourceEntity entity = get(id);
        entity.setEnabled(enabled);
        return repository.save(entity);
    }

    public OracleSourceEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new OracleException(OracleErrorCode.NOT_FOUND, "Source not found"));
    }
}
