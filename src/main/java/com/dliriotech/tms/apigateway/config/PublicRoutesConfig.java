package com.dliriotech.tms.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class PublicRoutesConfig {
    private List<String> publicPaths = new ArrayList<>();

    public boolean isPublic(String path) {
        return publicPaths.stream()
                .anyMatch(publicPath -> {
                    if (publicPath.endsWith("/**")) {
                        String prefix = publicPath.substring(0, publicPath.length() - 3);
                        return path.startsWith(prefix);
                    }
                    return path.equals(publicPath);
                });
    }
}