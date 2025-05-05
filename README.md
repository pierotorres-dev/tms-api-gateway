# TMS API Gateway

## Descripción

El **API Gateway** es un componente central de la arquitectura de microservicios del _Tyre Management System_ (TMS), un sistema especializado para la administración e inspección de neumáticos para flotas de vehículos pesados (camiones, grúas, stackers). Este servicio actúa como punto de entrada único para todas las peticiones externas, enrutándolas hacia los microservicios correspondientes y proporcionando funcionalidades transversales como seguridad, trazabilidad y observabilidad.

## Características principales

- **Enrutamiento inteligente**: redirección de peticiones a los microservicios apropiados basándose en reglas configurables.
- **Seguridad centralizada**: autenticación JWT y autorización para proteger los endpoints.
- **Rutas públicas/privadas**: configuración flexible para definir qué rutas requieren autenticación.
- **Trazabilidad distribuida**: integración con Zipkin para seguimiento de peticiones a través de múltiples servicios.
- **Observabilidad**: métricas Prometheus y endpoints Actuator para monitorizar el sistema.
- **Logging estructurado**: formato de logs con IDs de trace para facilitar el diagnóstico de problemas.

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.4.5
- Spring Cloud Gateway
- WebFlux (modelo reactivo)
- Micrometer para observabilidad
- Brave & Zipkin para trazabilidad
- Docker & Docker Compose
- Prometheus para métricas
- OpenAPI/Swagger para documentación

## Requisitos previos

- JDK 17 o superior
- Maven 3.8 o superior
- Docker y Docker Compose
- Acceso al Auth Service (servicio de autenticación)

## Instalación y ejecución

### 1. Variables de entorno

Crea un archivo `.env` en la raíz del proyecto y añade:

```dotenv
API_GATEWAY_KEY=secret
AUTH_SERVICE_ROUTE=http://localhost:8080
```

### 2. Ejecución con Docker Compose

```bash
git clone https://github.com/tu-usuario/tms-api-gateway.git
cd tms-api-gateway
docker-compose up -d
```
El API Gateway quedará expuesto en:
http://localhost:8081

### 3. Ejecución local para desarrollo

```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

## Seguridad

- El API Gateway implementa varias capas de seguridad:

- Autenticación JWT: todas las rutas privadas requieren un token JWT válido.

- Validación de tokens: los tokens son verificados contra el Auth Service.

- Rutas públicas configurables: definidas en application.yml, no requieren autenticación.

- API Key interna: uso de claves para autenticar la comunicación entre microservicios.

## Observabilidad

- Actuator endpoints:

    - Health: http://localhost:8081/actuator/health

    - Metrics: http://localhost:8081/actuator/metrics

    - Prometheus: http://localhost:8081/actuator/prometheus

- Trazabilidad distribuida con Zipkin:

    - Levanta Zipkin en http://localhost:9411.

    - Todas las peticiones llevan trace_id y span_id en los logs.