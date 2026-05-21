package com.predix.oracle.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSourceRequest {
    @NotBlank
    private String name;
    private String type;
    @NotBlank
    private String baseUrl;
    private Boolean enabled;
    private Integer priority;
}
