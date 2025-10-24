# lazy-customer-service

> Event-driven customer service with Spring Cloud Stream, RabbitMQ routing key-based message delivery, and Resilience4j protection

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.2-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-orange.svg)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive demonstration of **event-driven microservices** using Spring Cloud Stream, featuring routing key-based message delivery, multi-instance deployment with precise message routing, OpenFeign client service invocation, and Resilience4j circuit breaker and bulkhead protection.

## Features

- **Event-Driven Architecture**: Consumer-only message processing with Spring Cloud Stream
- **Routing Key-Based Delivery**: Precise message routing based on customer name
- **Multi-Instance Support**: Independent message consumption per instance
- **Declarative HTTP Client**: OpenFeign for service communication
- **Service Resilience**: Circuit Breaker and Bulkhead patterns via Resilience4j
- **Service Discovery**: Consul integration for service registration
- **Dynamic Customer Naming**: Port-based customer identification
- **Connection Pooling**: Apache HttpClient 5 with optimized pool settings
- **Monetary Calculations**: Joda Money for precise money handling
- **Functional Programming**: Vavr utilities for functional style
- **AspectJ Support**: AOP for cross-cutting concerns
- **35 Actuator Endpoints**: Comprehensive monitoring and management

## Tech Stack

- Spring Boot 3.4.5
- Spring Cloud Stream 4.x
- Spring Cloud OpenFeign
- Spring Cloud Consul Discovery
- RabbitMQ 3.11+
- Consul 1.4.5
- Resilience4j Spring Boot 3
- Java 21
- Joda Money 2.0.2
- Vavr 0.10.4
- AspectJ Weaver
- Apache HttpClient 5
- Apache Commons Lang3
- Lombok
- Maven 3.8+

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven 3.8+ (or use included Maven Wrapper)
- **RabbitMQ 3.11+** running on localhost:5672
- **Consul 1.4.5+** running on localhost:8500
- **busy-waiter-service** running (upstream service)
- **rabbitmq-barista-service** running (for complete workflow)

### Quick Start

**Step 1: Start Infrastructure**

```bash
# Start RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=spring \
  -e RABBITMQ_DEFAULT_PASS=spring \
  rabbitmq:3.11-management

# Start Consul
docker run -d --name consul \
  -p 8500:8500 \
  -p 8600:8600/udp \
  consul:1.4.5

# Start MariaDB
docker run -d --name mariadb \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=springbucks \
  -e MYSQL_USER=springbucks \
  -e MYSQL_PASSWORD=springbucks \
  mariadb:10.6
```

**Step 2: Start Dependency Services**

```bash
# Start rabbitmq-barista-service
cd ../rabbitmq-barista-service
./mvnw spring-boot:run

# Start busy-waiter-service
cd ../busy-waiter-service
./mvnw spring-boot:run
```

**Step 3: Run lazy-customer-service**

```bash
# Single instance (port 8090)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/lazy-customer-service-0.0.1-SNAPSHOT.jar --server.port=8090
```

**Step 4: Run Multiple Instances (for routing test)**

```bash
# Terminal 1: Customer instance 1 (port 8090)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Terminal 2: Customer instance 2 (port 8091)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"
```

**Step 5: Verify Service**

```bash
# Health check
curl http://localhost:8090/actuator/health | jq

# Check Consul registration
curl -s http://localhost:8500/v1/catalog/service/customer-service | \
  jq '.[] | {ID: .ServiceID, Port: .ServicePort}'

# Expected output:
# { "ID": "customer-service-8090", "Port": 8090 }
# { "ID": "customer-service-8091", "Port": 8091 }
```

## Configuration

### Application Properties

```properties
# Server configuration (dynamic port)
server.port=0

# Customer name (port-based for uniqueness)
customer.name=spring-${server.port}
# - Instance 8090: spring-8090
# - Instance 8091: spring-8091

# Feign client timeouts
feign.client.config.default.connect-timeout=500
feign.client.config.default.read-timeout=500

# Consul service discovery
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.prefer-ip-address=true

# Circuit Breaker configuration (Resilience4j 2.x)
resilience4j.circuitbreaker.instances.order.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.order.wait-duration-in-open-state=5s
resilience4j.circuitbreaker.instances.order.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.order.sliding-window-size=5
resilience4j.circuitbreaker.instances.order.minimum-number-of-calls=3

# Bulkhead configuration
resilience4j.bulkhead.instances.order.max-concurrent-calls=1
resilience4j.bulkhead.instances.order.max-wait-time=5

# RabbitMQ connection
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=spring
spring.rabbitmq.password=spring

# Spring Cloud Stream function definition
spring.cloud.function.definition=notifyOrders

# Input binding - receive order completion notifications
spring.cloud.stream.bindings.notifyOrders-in-0.destination=notifyOrders
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.durable-subscription=true
```

**Key Configuration Points:**

| Property | Value | Purpose |
|----------|-------|---------|
| `customer.name` | `spring-${server.port}` | Unique customer identifier per instance |
| `binding-routing-key` | `${customer.name}` | Bind specific routing key for message filtering |
| `group` | `customer-service-${server.port}` | Isolated queue per instance |
| `durable-subscription` | `true` | Queue survives broker restart |

### Bootstrap Properties

```properties
# Application name (for Consul registration)
spring.application.name=customer-service
```

## API Endpoints

### Customer Operations

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/customer/order` | Place new order | Order details with state PAID |
| GET | `/actuator/health` | Health check | Service health status |
| GET | `/actuator/bindings` | Stream bindings | Message channel bindings |

**Examples:**

```bash
# Place order (single-step: create + pay)
curl -X POST http://localhost:8090/customer/order | jq

# Expected response:
# {
#   "id": 1,
#   "customer": "spring-8090",
#   "items": [{"name": "capuccino", "price": 125.00}],
#   "state": "PAID",
#   "total": 118.75,
#   "createTime": "2025-10-21T13:53:52.000+08:00"
# }
```

## Message Flow

### Complete Order Workflow

```
1. Customer Service places order
   - POST /customer/order
   - Creates order via Feign → Waiter Service
   - Waiter returns order with state INIT
   ↓
2. Customer Service pays order
   - PUT /order/{id} with state PAID (via Feign)
   - Waiter updates state to PAID
   - Waiter sends message to newOrders Exchange
   ↓
3. Barista Service receives message
   - Processes coffee (5 seconds)
   - Updates order state to BREWED
   - Sends completion to finishedOrders Exchange
   ↓
4. Waiter Service receives completion
   - Queries order to get customer name
   - Builds message with customer name as Header
   - Sends to notifyOrders Exchange
   ↓
5. RabbitMQ routes message
   - Routing key: customer name (spring-8090)
   - Routes to queue bound with matching key
   ↓
6. Customer Service receives notification
   - Only instance with matching routing key receives message
   - Updates order state to TAKEN via Feign
   ↓
7. Workflow complete
```

## Key Components

### NotificationListener (Message Consumer)

```java
@Component
@Slf4j
public class NotificationListener {
    @Autowired
    private CoffeeOrderService orderService;
    
    @Value("${customer.name}")
    private String customer;

    /**
     * Listen for order completion notifications
     * 1. Receive order ID from Waiter Service
     * 2. Filter by routing key (only receive own messages)
     * 3. Query order details via Feign
     * 4. Update order state to TAKEN if BREWED
     * 
     * Key: Each customer instance uses different routing key
     * - RabbitMQ routes message to corresponding customer queue
     * 
     * @return Order notification handler function
     */
    @Bean
    public Consumer<Long> notifyOrders() {
        return id -> {
            log.info("Order [{}] is finished.", id);
            
            // Query order details via Feign
            CoffeeOrder order = orderService.getOrder(id);
            log.info("Get Order: {}", order);
            
            // Confirm order state is BREWED
            if (order != null && order.getState() == OrderState.BREWED) {
                log.info("Order [{}] is BREWED, I will take it.", id);
                
                // Update order state to TAKEN via Feign
                OrderStateRequest request = OrderStateRequest.builder()
                        .state(OrderState.TAKEN)
                        .build();
                orderService.updateState(id, request);
            } else {
                log.info("Order [{}] is not BREWED yet, state: {}", 
                    id, order != null ? order.getState() : "null");
            }
        };
    }
}
```

**Key Points:**

| Component | Purpose |
|-----------|---------|
| `@Value("${customer.name}")` | Customer name for routing key binding |
| `Consumer<Long>` | Message consumer receiving order ID |
| Message filtering | Only receive messages via RabbitMQ routing key |
| State confirmation | Only process orders in BREWED state |

### CustomerController (Feign Client)

```java
@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerController {
    @Autowired
    private CoffeeService coffeeService;
    
    @Autowired
    private CoffeeOrderService orderService;

    /**
     * Place new order (create + pay in one step)
     * 1. Get coffee list via Feign
     * 2. Create order via Feign (state: INIT)
     * 3. Update order to PAID via Feign
     * 4. Wait for order completion notification (async)
     * 
     * @return Order details with state PAID
     */
    @PostMapping("/order")
    public CoffeeOrder create() {
        // Get coffee list
        List<Coffee> coffees = coffeeService.getAll();
        log.info("Get {} coffees", coffees.size());
        
        // Create order
        NewOrderRequest orderRequest = NewOrderRequest.builder()
                .customer(customer)
                .items(Collections.singletonList(coffees.get(0).getName()))
                .build();
        CoffeeOrder order = orderService.create(orderRequest);
        log.info("Create order: {}", order.getId());
        
        // Pay order
        OrderStateRequest stateRequest = OrderStateRequest.builder()
                .state(OrderState.PAID)
                .build();
        order = orderService.updateState(order.getId(), stateRequest);
        log.info("Order is PAID: {}", order);
        
        return order;
    }
}
```

**Key Points:**

| Component | Purpose |
|-----------|---------|
| `coffeeService.getAll()` | Feign call to Waiter Service to get coffee list |
| `orderService.create()` | Feign call to create order (state: INIT) |
| `orderService.updateState()` | Feign call to update state to PAID |
| Single-step ordering | Create and pay in one API call |

## Routing Key Mechanism

### Consumer Configuration

**lazy-customer-service (Customer 8090):**

```properties
# Bind specific routing key (customer name)
customer.name=spring-8090
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=spring-8090
```

**Result:**
- Queue: `notifyOrders.customer-service-8090` 
- Routing Key: `spring-8090`
- Only receives messages with routing key `spring-8090`

**lazy-customer-service (Customer 8091):**

```properties
# Bind specific routing key (customer name)
customer.name=spring-8091
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=spring-8091
```

**Result:**
- Queue: `notifyOrders.customer-service-8091`
- Routing Key: `spring-8091`
- Only receives messages with routing key `spring-8091`

### Message Routing Flow

```
Waiter Service sends message:
  Header: { customer: "spring-8090" }
       ↓
  RabbitMQ Producer (routing-key-expression: headers.customer)
       ↓
  Routing Key: "spring-8090"
       ↓
  notifyOrders Exchange
       ↓ (routes based on routing key)
       ├──> Queue: notifyOrders.customer-service-8090 ✅
       └──> Queue: notifyOrders.customer-service-8091 ❌
       ↓
  Customer Service (8090) receives message
  Customer Service (8091) does NOT receive message
```

## Startup Logs

### Bootstrap Phase

```log
# 1. Application initialization
2025-10-21T14:15:32.125+08:00  INFO [main] CustomerServiceApplication : No active profile set

# 2. OpenFeign client scanning
2025-10-21T14:15:32.458+08:00  INFO [main] FeignClientFactoryBean : Registered FeignClient: CoffeeService
2025-10-21T14:15:32.461+08:00  INFO [main] FeignClientFactoryBean : Registered FeignClient: CoffeeOrderService

# 3. Spring Cloud Stream integration
2025-10-21T14:15:32.783+08:00  INFO [main] GenericScope : BeanFactory id=45f3a2c8-b123-4d5e-9a8f-1234567890ab

# 4. Tomcat initialization (dynamic port)
2025-10-21T14:15:33.015+08:00  INFO [main] TomcatWebServer : Tomcat initialized with port 0 (http)

# 5. Spring Cloud Stream channel subscription
2025-10-21T14:15:33.456+08:00  INFO [main] DirectWithAttributesChannel : Channel 'customer-service.notifyOrders-in-0' has 1 subscriber(s).

# 6. RabbitMQ connection
2025-10-21T14:15:33.789+08:00  INFO [main] CachingConnectionFactory : Created new connection: rabbitConnectionFactory#abc123:0/SimpleConnection@xyz789 [delegate=amqp://spring@127.0.0.1:5672/, localPort=52341]

# 7. RabbitMQ queue declaration
2025-10-21T14:15:33.892+08:00  INFO [main] RabbitExchangeQueueProvisioner : declaring queue for inbound: notifyOrders.customer-service-8090, bound to: notifyOrders

# 8. Message listener started
2025-10-21T14:15:33.945+08:00  INFO [main] AmqpInboundChannelAdapter : started bean 'inbound.notifyOrders.customer-service-8090'

# 9. Tomcat actual port
2025-10-21T14:15:34.012+08:00  INFO [main] TomcatWebServer : Tomcat started on port 8090 (http)

# 10. Consul service registration
2025-10-21T14:15:34.015+08:00  INFO [main] ConsulServiceRegistry : Registering service with consul: NewService{id='customer-service-8090', name='customer-service', ...port=8090...}

# 11. Startup complete
2025-10-21T14:15:34.123+08:00  INFO [main] CustomerServiceApplication : Started CustomerServiceApplication in 2.145 seconds
```

**Log Analysis:**

| Step | Event | Details |
|------|-------|---------|
| **1-3** | Spring initialization | Profile, Feign clients, integration components |
| **6** | RabbitMQ connection | Connected to localhost:5672 |
| **7** | Queue declaration | Queue: `notifyOrders.customer-service-8090` |
| **9** | Dynamic port | Actual port: 8090 (server.port=0) |
| **10** | Consul registration | Service ID: `customer-service-8090`, Port: 8090 |
| **11** | Startup time | **2.145 seconds** total |

### Order Processing Logs

```log
# 1. HTTP Request: Place order
2025-10-21T14:16:05.234  INFO [nio-8090-exec-1] CustomerController : Get 5 coffees

# 2. Feign: Create order
2025-10-21T14:16:05.289  INFO [nio-8090-exec-1] CustomerController : Create order: 1

# 3. Feign: Update order to PAID
2025-10-21T14:16:05.345  INFO [nio-8090-exec-1] CustomerController : Order is PAID: CoffeeOrder(id=1, customer=spring-8090, state=PAID...)

# 4. Message Listener: Receive notification (async thread, after ~5s)
2025-10-21T14:16:10.456  INFO [tomer-service-1] NotificationListener : Order [1] is finished.
                               ↑ Message listener thread (async)

# 5. Feign: Query order details
2025-10-21T14:16:10.512  INFO [tomer-service-1] NotificationListener : Get Order: CoffeeOrder(id=1, state=BREWED...)

# 6. Feign: Update order to TAKEN
2025-10-21T14:16:10.567  INFO [tomer-service-1] NotificationListener : Order [1] is BREWED, I will take it.
```

**Thread Analysis:**

| Thread Name | Type | Purpose |
|-------------|------|---------|
| `nio-8090-exec-1` | HTTP request thread | Handle REST API requests |
| `tomer-service-1` | Message listener thread | Handle RabbitMQ messages (async) |

**Timing Analysis:**
- Place order → PAID: **~111ms** (3 Feign calls)
- PAID → Notification received: **~5100ms** (Barista processing time)
- Notification → TAKEN: **~111ms** (Feign query + update)
- Total workflow: **~5322ms**

## Resilience4j Configuration

### Circuit Breaker

```properties
# Order service circuit breaker
resilience4j.circuitbreaker.instances.order.failure-rate-threshold=50
# Open circuit if 50% of calls fail

resilience4j.circuitbreaker.instances.order.wait-duration-in-open-state=5s
# Wait 5 seconds before trying half-open state

resilience4j.circuitbreaker.instances.order.sliding-window-size=5
# Evaluate last 5 calls

resilience4j.circuitbreaker.instances.order.minimum-number-of-calls=3
# Need at least 3 calls before evaluation
```

### Bulkhead

```properties
# Order service bulkhead
resilience4j.bulkhead.instances.order.max-concurrent-calls=1
# Allow only 1 concurrent call

resilience4j.bulkhead.instances.order.max-wait-time=5
# Wait maximum 5 seconds for permission
```

### Test Circuit Breaker

```bash
# Stop waiter-service to trigger circuit breaker
docker stop busy-waiter-service

# Place order (will fail after 3 attempts)
curl -X POST http://localhost:8090/customer/order

# Check circuit breaker health
curl http://localhost:8090/actuator/health | jq '.components.circuitBreakers'

# Expected output:
# {
#   "status": "UP",
#   "details": {
#     "order": {
#       "status": "OPEN",  ← Circuit is open
#       "failureRate": "100.0%",
#       "slowCallRate": "0.0%"
#     }
#   }
# }
```

## Monitoring

### View Stream Bindings

```bash
# View all bindings
curl http://localhost:8090/actuator/bindings | jq

# Expected output:
# {
#   "notifyOrders-in-0": {
#     "group": "customer-service-8090",
#     "bindingDestination": "notifyOrders"
#   }
# }
```

### View RabbitMQ Queues

```bash
# Query customer queue
curl -u spring:spring \
  http://localhost:15672/api/queues/%2F/notifyOrders.customer-service-8090 | \
  jq '{name, messages, consumers, consumer_details}'

# Expected output:
# {
#   "name": "notifyOrders.customer-service-8090",
#   "messages": 0,
#   "consumers": 1,
#   "consumer_details": [
#     {
#       "consumer_tag": "...",
#       "channel_details": {...}
#     }
#   ]
# }
```

### View Consul Service

```bash
# Query customer-service in Consul
curl -s http://localhost:8500/v1/catalog/service/customer-service | \
  jq '.[] | {ID: .ServiceID, Port: .ServicePort, Address: .ServiceAddress}'

# Expected output (2 instances):
# { "ID": "customer-service-8090", "Port": 8090, "Address": "192.168.1.100" }
# { "ID": "customer-service-8091", "Port": 8091, "Address": "192.168.1.100" }
```

## Common Issues

### Issue 1: Message Not Received

**Symptom:**
```
Waiter sends notification but customer doesn't receive it
```

**Solutions:**

```bash
# 1. Verify routing key binding
curl -u spring:spring \
  http://localhost:15672/api/bindings/%2F/e/notifyOrders/q/notifyOrders.customer-service-8090 | \
  jq '.routing_key'

# Expected: "spring-8090"

# 2. Check customer.name property
curl http://localhost:8090/actuator/env | jq '.propertySources[] | select(.name=="applicationConfig: [classpath:/application.properties]") | .properties."customer.name"'

# Expected: "spring-8090"

# 3. Verify message header
# In Waiter Service OrderListener.java:
Message<Long> message = MessageBuilder.withPayload(id)
        .setHeader("customer", order.getCustomer())  // Must set this!
        .build();
```

### Issue 2: Circuit Breaker Not Opening

**Symptom:**
```
Service fails but circuit breaker stays CLOSED
```

**Root Cause:** Using old Resilience4j 1.x configuration

**Solutions:**

```properties
# ✅ Correct (Resilience4j 2.x)
resilience4j.circuitbreaker.instances.order.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.order.sliding-window-size=5
resilience4j.circuitbreaker.instances.order.minimum-number-of-calls=3

# ❌ Wrong (Resilience4j 1.x - won't work)
resilience4j.circuitbreaker.backends.order.ring-buffer-size-in-closed-state=5
resilience4j.circuitbreaker.backends.order.ring-buffer-size-in-half-open-state=3
```

### Issue 3: Multiple Instances Receive Same Message

**Symptom:**
```
All customer instances receive same notification (no routing)
```

**Root Cause:** Routing key not configured

**Solutions:**

```properties
# ✅ Verify customer-side configuration
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}

# ✅ Verify waiter-side configuration
spring.cloud.stream.rabbit.bindings.notifyOrders-out-0.producer.routing-key-expression=headers.customer

# ✅ Verify message has customer header (in Waiter OrderListener)
Message<Long> message = MessageBuilder.withPayload(id)
        .setHeader("customer", order.getCustomer())  // Required!
        .build();
```

### Issue 4: Feign Client Timeout

**Error:**
```
feign.RetryableException: Read timed out executing POST http://waiter-service/order/
```

**Solutions:**

```properties
# Increase timeouts (default: 500ms)
feign.client.config.default.connect-timeout=2000
feign.client.config.default.read-timeout=5000

# Or configure per-client
feign.client.config.waiter-service.read-timeout=5000
```

## Best Practices

### 1. Routing Key Configuration

**✅ Recommended: Dynamic customer name**

```properties
customer.name=spring-${server.port}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}
```

**❌ Not Recommended: Hard-coded customer name**

```properties
customer.name=spring-8090  # Won't work for multiple instances
```

### 2. Consumer Group Configuration

**✅ Recommended: Port-based group for isolation**

```properties
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}
# Result: Each instance has independent queue
```

**❌ Not Recommended: Shared group (load balancing)**

```properties
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service
# Result: Messages load-balanced across instances (not desired here)
```

### 3. Feign Client Configuration

**✅ Recommended: Configure timeouts and retry**

```properties
feign.client.config.default.connect-timeout=500
feign.client.config.default.read-timeout=500
feign.client.config.default.logger-level=basic
```

### 4. Circuit Breaker Configuration

**✅ Recommended: Use Resilience4j 2.x parameters**

```properties
resilience4j.circuitbreaker.instances.order.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.order.sliding-window-size=5
resilience4j.circuitbreaker.instances.order.minimum-number-of-calls=3
```

## Architecture

### Message Routing Architecture

```
┌──────────────────────────────────────────────────────────┐
│                 Customer Service (8090)                  │
│  ┌────────────────────────────────────────────────────┐  │
│  │         CustomerController                         │  │
│  │  - POST /customer/order                            │  │
│  │    └─> Feign: CoffeeService.getAll()              │  │
│  │    └─> Feign: CoffeeOrderService.create()         │  │
│  │    └─> Feign: CoffeeOrderService.updateState()    │  │
│  └─────────────┬──────────────────────────────────────┘  │
│                │                                          │
│  ┌─────────────▼──────────────────────────────────────┐  │
│  │         NotificationListener                       │  │
│  │  - Consumer<Long> notifyOrders()                   │  │
│  │    └─> Receive order ID                           │  │
│  │    └─> Feign: CoffeeOrderService.getOrder(id)     │  │
│  │    └─> Feign: CoffeeOrderService.updateState()    │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
                 ↑
                 │ (Routing Key: spring-8090)
                 │
           ┌─────┴─────┐
           │ notifyOrders │
           │  Exchange   │
           │  (Direct)   │
           └─────┬─────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
    ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌─────────────┐
│ Queue   │ │ Queue   │ │   Waiter    │
│ (8090)  │ │ (8091)  │ │   Service   │
│         │ │         │ │  (Producer) │
└─────────┘ └─────────┘ └─────────────┘
```

### Multi-Instance Deployment

| Instance | Port | Customer Name | Queue | Routing Key |
|----------|------|---------------|-------|-------------|
| **Instance 1** | 8090 | spring-8090 | notifyOrders.customer-service-8090 | spring-8090 |
| **Instance 2** | 8091 | spring-8091 | notifyOrders.customer-service-8091 | spring-8091 |

## Testing

### Manual Testing - Single Instance

```bash
# Prerequisite: All services running
# - rabbitmq-barista-service
# - busy-waiter-service
# - lazy-customer-service (port 8090)

# Step 1: Place order
curl -X POST http://localhost:8090/customer/order | jq

# Step 2: Observe logs
# Customer Service (8090):
#   "Get 5 coffees"
#   "Create order: 1"
#   "Order is PAID: ..."
#   (wait ~5 seconds)
#   "Order [1] is finished."
#   "Order [1] is BREWED, I will take it."

# Step 3: Verify final state
curl http://localhost:65440/order/1 | jq '.state'
# Expected: "TAKEN"
```

### Manual Testing - Multi-Instance Routing

```bash
# Prerequisite: 2 customer instances running (8090, 8091)

# Step 1: Place order from customer 8090
curl -X POST http://localhost:8090/customer/order | jq

# Step 2: Observe logs
# Customer 8090:
#   "Create order: 1"
#   "Order is PAID: ..."
#   (wait ~5s)
#   "Order [1] is finished." ✅
#   "Order [1] is BREWED, I will take it." ✅

# Customer 8091:
#   (No log output - doesn't receive message) ✅

# Step 3: Place order from customer 8091
curl -X POST http://localhost:8091/customer/order | jq

# Step 4: Observe logs
# Customer 8090:
#   (No log output - doesn't receive message) ✅

# Customer 8091:
#   "Create order: 2"
#   "Order is PAID: ..."
#   (wait ~5s)
#   "Order [2] is finished." ✅
#   "Order [2] is BREWED, I will take it." ✅
```

### Automated Testing

```bash
# Run unit tests
./mvnw test

# Run with coverage
./mvnw clean test jacoco:report
```

## Best Practices Demonstrated

1. **Event-Driven Architecture**: Decoupled communication via RabbitMQ
2. **Routing Key-Based Delivery**: Precise message targeting per instance
3. **Multi-Instance Support**: Scalable deployment with isolated consumption
4. **Declarative HTTP Client**: Type-safe service communication with Feign
5. **Service Resilience**: Circuit Breaker and Bulkhead protection
6. **Service Discovery**: Dynamic service location with Consul
7. **Dynamic Configuration**: Port-based customer identification
8. **Connection Pooling**: Optimized HTTP connection management
9. **Functional Programming**: Consumer function-based message handling
10. **Monitoring**: Comprehensive Actuator endpoints

## Advanced Topics

### 1. Custom Error Handling

```java
@Bean
public Consumer<Long> notifyOrders() {
    return id -> {
        try {
            // Process order...
        } catch (FeignException.ServiceUnavailable e) {
            log.error("Service unavailable, will retry later", e);
            throw e;  // Let Spring Cloud Stream retry
        } catch (Exception e) {
            log.error("Unexpected error processing order {}", id, e);
            // Send to DLQ
        }
    };
}
```

### 2. Message Retry Configuration

```properties
# Retry configuration
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.max-attempts=3
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.back-off-initial-interval=1000
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.back-off-multiplier=2

# Dead Letter Queue
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.auto-bind-dlq=true
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.republish-to-dlq=true
```

### 3. Feign Interceptor for Tracing

```java
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Add trace ID to request
            String traceId = MDC.get("trace-id");
            if (traceId != null) {
                template.header("X-Trace-Id", traceId);
            }
        };
    }
}
```

## References

- [Spring Cloud Stream Documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [RabbitMQ Routing Keys](https://www.rabbitmq.com/tutorials/tutorial-four-spring-amqp.html)
- [Spring Cloud Consul](https://docs.spring.io/spring-cloud-consul/docs/current/reference/html/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Resilience4j Bulkhead](https://resilience4j.readme.io/docs/bulkhead)

## License

MIT License - see [LICENSE](LICENSE) file for details.

## About Us

我們主要專注在敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。喜歡把先進技術和實務經驗結合，打造好用又靈活的軟體解決方案。近來也積極結合 AI 技術，推動自動化工作流，讓開發與運維更有效率、更智慧。持續學習與分享，希望能一起推動軟體開發的創新和進步。

## Contact

**風清雲談** - 專注於敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。

- 🌐 官方網站：[風清雲談部落格](https://blog.fengqing.tw/)
- 📘 Facebook：[風清雲談粉絲頁](https://www.facebook.com/profile.php?id=61576838896062)
- 💼 LinkedIn：[Chu Kuo-Lung](https://www.linkedin.com/in/chu-kuo-lung)
- 📺 YouTube：[雲談風清頻道](https://www.youtube.com/channel/UCXDqLTdCMiCJ1j8xGRfwEig)
- 📧 Email：[fengqing.tw@gmail.com](mailto:fengqing.tw@gmail.com)

---

**⭐ If this project helps you, please give it a Star!**
