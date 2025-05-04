package com.dliriotech.tms.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
}