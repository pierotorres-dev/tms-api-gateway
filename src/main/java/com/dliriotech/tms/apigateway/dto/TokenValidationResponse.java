package com.dliriotech.tms.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private Integer userId;
    private Integer empresaId;
    private String role;
    private Set<String> allowedMethods;
}

