package com.dliriotech.tms.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class UriRequest {
    private String uri;

    private String method;
}