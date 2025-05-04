package com.dliriotech.tms.apigateway.config.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
@Slf4j
public class RouteTraceFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
                    URI requestUrl = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

                    if (route != null && requestUrl != null) {
                        String routeId = route.getId();
                        String sourceURL = exchange.getRequest().getURI().toString();
                        String targetURL = requestUrl.toString();

                        log.info("Solicitud enrutada: [{}] {} → {}",
                                routeId,
                                sourceURL,
                                targetURL);
                    }
                }));
    }

    @Override
    public int getOrder() {
        return 1;
    }
}