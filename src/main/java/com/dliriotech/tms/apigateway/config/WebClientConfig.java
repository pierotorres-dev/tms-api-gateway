package com.dliriotech.tms.apigateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${service.auth-service-url}")
    private String authServiceUrl;

    @Value("${service.fleet-service-url}")
    private String fleetServiceUrl;

    @Value("${service.tyre-service-url}")
    private String tyreServiceUrl;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient authServiceWebClient() {
        // More generous connection provider settings
        ConnectionProvider provider = ConnectionProvider.builder("auth-pool")
                .maxConnections(200)
                .maxIdleTime(Duration.ofSeconds(60))  // Longer idle time
                .evictInBackground(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // Wait longer for connections
                .lifo()  // Last-in-first-out for better connection reuse
                .build();

        // More generous timeout settings
        return getWebClient(provider, authServiceUrl);
    }

    @Bean
    public WebClient fleetServiceWebClient() {
        // More generous connection provider settings
        ConnectionProvider provider = ConnectionProvider.builder("fleet-pool")
                .maxConnections(200)
                .maxIdleTime(Duration.ofSeconds(60))  // Longer idle time
                .evictInBackground(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // Wait longer for connections
                .lifo()  // Last-in-first-out for better connection reuse
                .build();

        // More generous timeout settings
        return getWebClient(provider, fleetServiceUrl);
    }

    @Bean
    public WebClient tyreServiceWebClient() {
        // More generous connection provider settings
        ConnectionProvider provider = ConnectionProvider.builder("tyre-pool")
                .maxConnections(200)
                .maxIdleTime(Duration.ofSeconds(60))  // Longer idle time
                .evictInBackground(Duration.ofSeconds(120))
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // Wait longer for connections
                .lifo()  // Last-in-first-out for better connection reuse
                .build();

        // More generous timeout settings
        return getWebClient(provider, tyreServiceUrl);
    }

    private WebClient getWebClient(ConnectionProvider provider, String serviceUrl) {
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 10 seconds connect timeout
                .responseTimeout(Duration.ofSeconds(15))  // 15 seconds response timeout
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)))
                .keepAlive(true)
                .wiretap(log.isDebugEnabled());  // Enable detailed connection logging in debug mode

        return WebClient.builder()
                .baseUrl(serviceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }
}