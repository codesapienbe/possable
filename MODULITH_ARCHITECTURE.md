# Spring Modulith Architecture

## Overview

This application is structured as a **modular monolith** using **Spring Modulith**, which enforces module boundaries and enables event-driven communication between modules. This architecture provides better maintainability, testability, and the potential for future microservice extraction if needed.

## Module Structure

### 1. Order Module (`com.possable.order`)

**Responsibility**: Order creation, management, and lifecycle

**Public API**: `OrderFacade`

**Events Published**:
- `OrderCreatedEvent` - Published when an order is created
- `OrderCompletedEvent` - Published when an order status changes to COMPLETED

**Events Consumed**: None

**Dependencies**: None (core module)

**Key Components**:
- `OrderModuleService` (internal) - Business logic for orders
- `OrderFacade` - Public API for other modules
- `OrderCreatedEvent`, `OrderCompletedEvent` - Domain events

---

### 2. Checkout Module (`com.possable.checkout`)

**Responsibility**: Payment processing and checkout operations

**Public API**: `CheckoutFacade`

**Events Published**:
- `PaymentCompletedEvent` - Published when a payment is completed

**Events Consumed**:
- `OrderCompletedEvent` - Triggers payment processing workflow

**Dependencies**: `order` (via events only)

**Key Components**:
- `CheckoutModuleService` (internal) - Payment processing logic
- `CheckoutFacade` - Public API for payment operations
- `PaymentCompletedEvent` - Domain event

---

### 3. Print Module (`com.possable.print`)

**Responsibility**: Printer management, print templates, and print job processing

**Public API**: `PrintFacade`

**Events Published**: None

**Events Consumed**:
- `OrderCreatedEvent` - Auto-creates print jobs for kitchen/bar printers
- `PrintJobRequestedEvent` - Creates print jobs on demand

**Dependencies**: `order` (via events only)

**Key Components**:
- `PrintModuleService` (internal) - Print job processing logic
- `PrintFacade` - Public API for print operations
- `PrintJobRequestedEvent` - Domain event for manual print requests

---

### 4. Employee Module (`com.possable.employee`)

**Responsibility**: Employee management and authentication

**Public API**: `EmployeeFacade`

**Events Published**: None

**Events Consumed**: None

**Dependencies**: None (independent module)

**Key Components**:
- `EmployeeModuleService` (internal) - Employee business logic
- `EmployeeFacade` - Public API for employee operations

---

### 5. Customer Module (`com.possable.customer`)

**Responsibility**: Customer-related operations and notifications

**Public API**: `CustomerFacade`

**Events Published**: None

**Events Consumed**:
- `OrderCreatedEvent` - Sends customer notifications
- `PaymentCompletedEvent` - Sends customer receipts

**Dependencies**: `order`, `checkout` (via events only)

**Key Components**:
- `CustomerModuleService` (internal) - Customer notification logic
- `CustomerFacade` - Public API for customer operations

---

## Event-Driven Communication

### Benefits

1. **Loose Coupling**: Modules don't directly depend on each other's implementation
2. **Asynchronous Processing**: Events can be processed asynchronously
3. **Event Sourcing Ready**: Events are persisted using `spring-modulith-events-jpa`
4. **Testability**: Each module can be tested in isolation
5. **Scalability**: Easy to move modules to separate services later

### Event Flow Examples

#### Order Creation Flow

```
User creates order
    ↓
OrderController → OrderFacade → OrderModuleService
    ↓
OrderCreatedEvent published
    ↓
    ├──→ PrintModuleService listens → Creates print jobs
    └──→ CustomerModuleService listens → Sends notification
```

#### Payment Completion Flow

```
Payment processed
    ↓
CheckoutModuleService
    ↓
PaymentCompletedEvent published
    ↓
CustomerModuleService listens → Sends receipt
```

### Event Persistence

Spring Modulith automatically persists events to the database using JPA. This provides:

- **Guaranteed Delivery**: Events are not lost if a consumer fails
- **Replay Capability**: Failed events can be replayed
- **Audit Trail**: Complete history of domain events

## Module Boundaries

### Internal vs Public

- **Internal packages** (`*.internal`): Implementation details, not accessible to other modules
- **Public packages** (module root): Only facade classes and events are exposed

### Dependency Rules

1. Modules communicate **only through events** or public facades
2. No direct access to internal services from other modules
3. Controllers are **not** part of any module (they're in `com.possable.controller`)
4. Models and repositories can be shared (transitioning to module-specific entities recommended)

## Testing Strategy

### Module Testing

```java
@ApplicationModuleTest
class OrderModuleTest {
    // Test order module in isolation
}
```

### Integration Testing

```java
@SpringBootTest
class ModuleIntegrationTest {
    // Test inter-module communication via events
}
```

### Event Testing

```java
@Test
void testOrderCreatedEventPublished() {
    orderFacade.createOrder(items, notes);
    
    // Verify event was published
    verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
}
```

## Logging Standards

All modules use structured logging with consistent format:

```json
{
  "message": "order_created",
  "order_id": "123",
  "component": "order-module",
  "timestamp": "2025-10-01T10:00:00Z"
}
```

### Log Levels

- **ERROR**: Actionable failures requiring immediate attention
- **WARN**: Concerning situations (e.g., missing templates)
- **INFO**: Business events (order created, payment completed)
- **DEBUG**: Technical details (event published)

## Migration from Legacy Code

The application is currently in transition from the old service-based architecture to the new modular architecture:

### Current State

- ✅ Module packages created
- ✅ Event classes defined
- ✅ Facades implemented
- ✅ Controllers updated to use facades
- ⏳ Old `OrderService`, `PaymentService`, `EmployeeService` still exist for backward compatibility
- ⏳ Tests still reference old services

### Next Steps

1. Update all tests to use facades instead of services
2. Move entities to module-specific packages
3. Move repositories to module internal packages
4. Remove legacy service classes
5. Add module verification tests
6. Document API contracts for each facade

## Benefits of This Architecture

1. **Maintainability**: Clear module boundaries make code easier to understand and modify
2. **Testability**: Each module can be tested independently
3. **Scalability**: Modules can be extracted into microservices if needed
4. **Team Productivity**: Different teams can work on different modules with minimal conflicts
5. **Domain-Driven Design**: Modules align with business domains
6. **Event Sourcing**: Built-in support for event-driven architecture

## Monitoring and Observability

All domain events are logged and can be monitored through:

- Application logs (`application.log`)
- Actuator endpoints (`/actuator/health`, `/actuator/metrics`)
- Prometheus metrics (via Micrometer)
- Grafana dashboards (see `grafana/dashboards/`)

## Further Reading

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html) 