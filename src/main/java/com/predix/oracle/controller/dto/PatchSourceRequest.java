package com.predix.oracle.controller.dto;

import lombok.Data;

@Data
public class PatchSourceRequest {
    private String type;
    private String baseUrl;
    private Integer priority;
}
