# Proyecto Génesis (Java 17 + Spring Boot + PostgreSQL)

Servicio REST que ejecuta un cálculo de complejidad media (integración numérica por Monte Carlo), consume CPU según el parámetro `samplesN`,
persiste inputs + resultados + métricas, y soporta CRUD con **borrado lógico** (deshabilitar).

## Requisitos
- Java 17
- Maven 3.9+
- PostgreSQL accesible (por defecto apunta a `192.168.4.46:5432`)

## Configuración de BD
Edita `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<HOST>:5432/genesis
    username: genesis
    password: genesis
```

## Ejecutar
```bash
mvn spring-boot:run
```

Health:
- http://localhost:8080/actuator/health

## Endpoints
- POST   `/api/v1/calculations`  (ejecuta + persiste)
- GET    `/api/v1/calculations`  (lista activos)
- GET    `/api/v1/calculations?includeDisabled=true`
- GET    `/api/v1/calculations/{id}`
- PUT    `/api/v1/calculations/{id}` (re-ejecuta y actualiza parámetros)
- DELETE `/api/v1/calculations/{id}` (soft delete -> disabled=true)
- POST   `/api/v1/calculations/{id}/enable` (rehabilita)

### Ejemplo POST
```bash
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{"functionType":"GAUSSIAN_SIN","a":-2,"b":2,"samplesN":2000000,"seed":42}'
```

## Docker (opcional)
En `docker/docker-compose.yml` hay un ejemplo para levantar PostgreSQL localmente:
```bash
cd docker
docker compose up -d
```
Luego cambia el `application.yml` a `localhost:5432`.
