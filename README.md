# E-commerce con Saga coreografiada (Java 21 + Spring Boot 4) en Azure — capa gratuita

Microservicios orientados a eventos que procesan pedidos con el **patrón Saga coreografiado**:
cada servicio ejecuta su transacción local y publica un evento; si algo falla, se emiten
eventos de compensación.

```
Cliente ──► API Gateway (JWT + rate limit) ──► Order Service ──outbox──► RabbitMQ
                                                                         │   ▲
                                         Inventory Service ◄─────────────┤   │
                                         Payment Service (Circuit Breaker)◄──┘
```

## Módulos

| Módulo | Puerto local | Responsabilidad |
|---|---|---|
| `gateway` | 9080 (docker) / 8080 | Login JWT, validación, rate limiting por IP (Bucket4j), enrutamiento |
| `order-service` | 8081 | Ciclo de vida del pedido, inicia la Saga |
| `inventory-service` | 8082 | Reserva atómica de stock, compensación, expiración de reservas |
| `payment-service` | 8083 | Cobro contra pasarela simulada con Resilience4j |
| `saga-common` | — | Eventos, Transactional Outbox, idempotencia, topología RabbitMQ |

## Flujo de eventos

| Evento | Publica | Consume | Resultado |
|---|---|---|---|
| `OrderCreated` | Order | Inventory | Reserva stock |
| `StockReserved` | Inventory | Payment | Cobra |
| `StockReservationFailed` | Inventory | Order | `CANCELLED_OUT_OF_STOCK` |
| `StockReservationExpired` | Inventory | Order | `CANCELLED_EXPIRED` |
| `PaymentProcessed` | Payment | Order, Inventory | `CONFIRMED`, reserva definitiva |
| `PaymentFailed` | Payment | Order, Inventory | `CANCELLED_PAYMENT_FAILED`, stock devuelto |

## Garantías de consistencia

- **Transactional Outbox**: el cambio de negocio y su evento se guardan en la misma transacción;
  `OutboxRelay` los publica (`FOR UPDATE SKIP LOCKED` para soportar varias réplicas).
- **Idempotencia**: cada consumidor registra el `eventId` en `processed_events`.
- **Reintentos + DLQ**: 3 intentos con backoff; luego el mensaje va a `<cola>.dlq`.
- **Sin sobreventa**: `UPDATE ... SET available = available - ? WHERE available >= ?`.

## Ejecutar en local

Requisitos: Docker y JDK 21.

```bash
docker compose up -d --build          # todo el sistema
# o solo infraestructura para correr los servicios desde el IDE:
docker compose up -d postgres rabbitmq
```

- Gateway: http://localhost:9080
- RabbitMQ: http://localhost:15672 (guest / guest)

```bash
# 1. Login (usuarios demo: demo / ana, contraseña demo123)
curl -X POST localhost:9080/auth/login -H "Content-Type: application/json" \
     -d '{"username":"demo","password":"demo123"}'

# 2. Crear pedido (responde 202 con estado PENDING_PAYMENT)
curl -X POST localhost:9080/api/orders -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"items":[{"productId":"KB-01","quantity":2}]}'

# 3. Consultar el estado final
curl localhost:9080/api/orders/<id> -H "Authorization: Bearer <token>"
```

Escenarios de prueba con el catálogo de ejemplo:

| Pedido | Resultado esperado |
|---|---|
| `KB-01`, `MOUSE-01` | `CONFIRMED` |
| `LAPTOP-01` (1899 > límite 1000) | `CANCELLED_PAYMENT_FAILED`, stock devuelto |
| `MONITOR-01` × 50 | `CANCELLED_OUT_OF_STOCK` |
| `LAST-UNIT` × 10 en paralelo | 1 `CONFIRMED`, 9 `CANCELLED_OUT_OF_STOCK` |
| `GATEWAY_FAILURE_RATE=1.0 docker compose up -d payment-service` | El Circuit Breaker se abre tras 5 fallos |

## Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev             # http://localhost:5173 (redirige /api y /auth al Gateway en :9080)
```

Login, catálogo, carrito y "Mis pedidos": el estado del pedido se actualiza en vivo mientras avanza la Saga.
En Azure se publica en Static Web Apps con `VITE_API_URL` apuntando al Gateway.

## Tests

```bash
./mvnw verify           # Windows: mvnw.cmd verify  (requiere Docker: usa Testcontainers)
cd frontend && npm test
```

| Módulo | Qué se prueba |
|---|---|
| inventory-service | Concurrencia (10 compras de 1 unidad), compensación, idempotencia, todo-o-nada, expiración de reservas |
| order-service | API (202, validación, aislamiento por cliente), Outbox, transiciones de estado ante cada evento |
| payment-service | Aprobación, rechazo, no cobrar dos veces, apertura del Circuit Breaker |
| gateway | Login JWT, 401, reemplazo de `X-User-Id` falsificado, rate limiting por IP |
| frontend | Login, catálogo, cálculo del carrito |

Los tests de integración levantan PostgreSQL y RabbitMQ reales con Testcontainers
(configuración compartida en el test-jar de `saga-common`).

## CI/CD (GitHub Actions)

`.github/workflows/ci.yml`: en cada push o pull request ejecuta los tests del backend y del frontend y,
si está configurado `SONAR_TOKEN`, el análisis de SonarCloud. En push a `main` publica las 4 imágenes
en GitHub Container Registry (`ghcr.io/<usuario>/ecommerce-<servicio>`).
