# lazy-customer-service

> Event-driven customer service with Spring Cloud Stream and RabbitMQ routing key-based message delivery

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.2-blue.svg)](https://spring.io/projects/spring-cloud)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-orange.svg)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive demonstration of **event-driven microservices** using Spring Cloud Stream, featuring routing key-based message delivery, multi-instance deployment with precise message routing, Feign client service invocation, and Resilience4j circuit breaker protection.

## Features

- Spring Cloud Stream function-based messaging model
- RabbitMQ routing key-based message delivery
- Multi-instance deployment with isolated message consumption
- Feign declarative HTTP client for service invocation
- Resilience4j circuit breaker and bulkhead protection
- Consul service discovery and registration
- Dynamic customer naming based on server port
- Order state management (INIT → PAID → BREWED → TAKEN)
- Apache HttpClient 5 with connection pooling
- Joda Money for precise monetary calculations
- Vavr functional programming utilities

## Tech Stack

- Spring Boot 3.4.5
- Spring Cloud Stream 4.x
- Spring Cloud OpenFeign
- RabbitMQ 3.11+
- Consul 1.4.5
- Resilience4j Spring Boot 3
- Java 21
- Joda Money 2.0.2
- Vavr 0.10.4
- AspectJ Weaver
- Apache HttpClient 5
- Lombok
- Maven 3.8+

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven 3.8+ (or use included Maven Wrapper)
- **RabbitMQ 3.11+** running on localhost:5672
- **Consul 1.4.5+** running on localhost:8500
- **busy-waiter-service** running (dependency service)
- **rabbitmq-barista-service** running (for complete workflow)

### Quick Start

**Step 1: Start RabbitMQ**

```bash
# Using Docker (recommended)
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=spring \
  -e RABBITMQ_DEFAULT_PASS=spring \
  rabbitmq:3.11-management

# Verify RabbitMQ is running
curl http://localhost:15672
# Login: spring / spring
```

**Step 2: Start Consul**

```bash
# Using Docker (recommended)
docker run -d --name consul \
  -p 8500:8500 \
  -p 8600:8600/udp \
  consul:1.4.5

# Verify Consul is running
curl http://localhost:8500/v1/status/leader
# Expected: "127.0.0.1:8300"
```

**Step 3: Start MariaDB**

```bash
docker run -d --name mariadb \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=springbucks \
  -e MYSQL_USER=springbucks \
  -e MYSQL_PASSWORD=springbucks \
  mariadb:10.6

# Verify MariaDB is running
docker exec -it mariadb mysql -uspringbucks -pspringbucks -e "SELECT 1"
```

**Step 4: Start rabbitmq-barista-service**

```bash
# In rabbitmq-barista-service directory
cd ../rabbitmq-barista-service
./mvnw spring-boot:run
```

**Step 5: Start busy-waiter-service**

```bash
# In busy-waiter-service directory
cd ../busy-waiter-service
./mvnw spring-boot:run
```

**Step 6: Run lazy-customer-service (Single Instance)**

```bash
# Method 1: Using Maven Wrapper
./mvnw spring-boot:run

# Method 2: Using Maven with specific port
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Method 3: Using JAR
./mvnw clean package -DskipTests
java -jar target/lazy-customer-service-0.0.1-SNAPSHOT.jar --server.port=8090
```

**Step 7: Run Multiple Instances (Multi-Instance Test)**

```bash
# Terminal 1: Start customer instance 1 (port 8090)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Terminal 2: Start customer instance 2 (port 8091)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"
```

**Step 8: Verify Service**

```bash
# Health check
curl http://localhost:8090/actuator/health

# Check Consul service registration
curl -s http://localhost:8500/v1/catalog/service/customer-service | jq '.[] | {ID: .ServiceID, Port: .ServicePort}'

# Expected output:
# { "ID": "customer-service-8090", "Port": 8090 }
# { "ID": "customer-service-8091", "Port": 8091 }
```

## Configuration

### Application Properties

```properties
# Server configuration (dynamic port allocation)
server.port=0

# Customer name configuration (includes port number for uniqueness)
# - Instance 1: spring-8090
# - Instance 2: spring-8091
customer.name=spring-${server.port}

# RabbitMQ connection configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=spring
spring.rabbitmq.password=spring

# Spring Cloud Stream function definition
# Define consumer function: notifyOrders (receive order completion notification)
spring.cloud.function.definition=notifyOrders

# Input binding configuration - receive order completion notification
spring.cloud.stream.bindings.notifyOrders-in-0.destination=notifyOrders
# Group name includes port number to ensure each instance has independent queue
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}
# Key configuration: bind routing key to customer name, only receive messages for this customer
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.durable-subscription=true
```

**Configuration Explanation:**

| Property | Value | Purpose |
|----------|-------|---------|
| `customer.name` | `spring-${server.port}` | Dynamic customer name for routing |
| `binding-routing-key` | `${customer.name}` | Bind specific routing key for precise delivery |
| `group` | `customer-service-${server.port}` | Ensure instance isolation |
| `durable-subscription` | `true` | Persistent subscription to avoid message loss |

### Bootstrap Properties

```properties
# Application name
spring.application.name=customer-service

# Consul service discovery
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.prefer-ip-address=true

# Feign configuration
spring.cloud.openfeign.client.config.default.connect-timeout=5000
spring.cloud.openfeign.client.config.default.read-timeout=5000

# Resilience4j circuit breaker configuration
resilience4j.circuitbreaker.instances.order.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.order.wait-duration-in-open-state=5000
resilience4j.circuitbreaker.instances.order.sliding-window-size=10

# Resilience4j bulkhead configuration
resilience4j.bulkhead.instances.order.max-concurrent-calls=5
resilience4j.bulkhead.instances.order.max-wait-duration=1000
```

## Message Routing Mechanism

### RabbitMQ Exchange Architecture

| Exchange Name | Type | Purpose | Routing Key | Participating Services |
|---------------|------|---------|-------------|------------------------|
| `newOrders` | Direct | New order notification | Default | Waiter Service → Barista Service |
| `finishedOrders` | Direct | Order completion notification | Default | Barista Service → Waiter Service |
| `notifyOrders` | Direct | Customer notification | Customer name (e.g., spring-8090) | Waiter Service → Customer Service |

### Message Flow

```
1. Customer Service (8090) creates order via Feign → Waiter Service
   ↓
2. Waiter Service updates order state to PAID, sends message to newOrders Exchange
   ↓
3. Barista Service receives order ID from newOrders
   ↓
4. Barista Service processes coffee (5 seconds), updates state to BREWED
   ↓
5. Barista Service sends completion notification to finishedOrders Exchange
   ↓
6. Waiter Service receives completion notification from finishedOrders
   ↓
7. Waiter Service sends message to notifyOrders Exchange with customer name as Header
   ↓
8. RabbitMQ routes message to corresponding customer queue based on routing key
   ↓
9. Customer Service (8090) receives notification (only messages with routing key "spring-8090")
   ↓
10. Customer Service updates order state to TAKEN, workflow complete
```

### Multi-Instance Routing

**Scenario: 2 customer instances running**

```
┌─────────────────────┐         ┌─────────────────────┐
│ Customer (8090)     │         │ Customer (8091)     │
│ Routing Key:        │         │ Routing Key:        │
│   spring-8090       │         │   spring-8091       │
└──────────┬──────────┘         └──────────┬──────────┘
           │                               │
           └───────────┐       ┌───────────┘
                       │       │
            ┌──────────▼───────▼──────────┐
            │  notifyOrders Exchange       │
            │  (Direct type)               │
            └──────────┬───────┬──────────┘
                       │       │
         Routing Key:  │       │  Routing Key:
         spring-8090   │       │  spring-8091
                       │       │
        ┌──────────────▼───┐ ┌─▼──────────────┐
        │ Queue:           │ │ Queue:          │
        │ notifyOrders.    │ │ notifyOrders.   │
        │ customer-        │ │ customer-       │
        │ service-8090     │ │ service-8091    │
        └──────────────────┘ └─────────────────┘
             ↓                      ↓
        Instance 8090          Instance 8091
        receives only          receives only
        order 1 messages       order 2 messages
```

**Key Benefits:**
- ✅ Precise message delivery (no broadcast)
- ✅ Instance isolation
- ✅ Horizontal scaling support
- ✅ Independent queue per instance

## API Endpoints

### Customer Operations

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/customer/order` | Create new order | - (auto-creates capuccino order) |

**Examples:**

```bash
# Create order (instance 8090)
curl -X POST http://localhost:8090/customer/order

# Expected output:
# {
#   "id": 1,
#   "customer": "spring-8090",
#   "items": [
#     {
#       "id": 3,
#       "name": "capuccino",
#       "price": 125.00,
#       ...
#     }
#   ],
#   "state": "PAID",
#   "discount": 95,
#   "total": 118.75,
#   "waiter": "springbucks-uuid"
# }
```

```bash
# Create order (instance 8091)
curl -X POST http://localhost:8091/customer/order

# Expected output:
# {
#   "id": 2,
#   "customer": "spring-8091",  ← Different customer name
#   ...
# }
```

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check status |
| `/actuator/circuitbreakers` | Circuit breaker status |
| `/actuator/bulkheads` | Bulkhead status |
| `/actuator/metrics` | Application metrics |
| `/actuator/bindings` | Stream bindings information |

**Examples:**

```bash
# Health check
curl http://localhost:8090/actuator/health | jq

# Check circuit breaker status
curl http://localhost:8090/actuator/circuitbreakers | jq

# View stream bindings
curl http://localhost:8090/actuator/bindings | jq
```

## Multi-Instance Testing

### Test Routing Key Precision

**Step 1: Start Multiple Instances**

```bash
# Terminal 1
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Terminal 2
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"
```

**Step 2: Create Orders from Different Instances**

```bash
# Create order from instance 8090
curl -X POST http://localhost:8090/customer/order

# Create order from instance 8091
curl -X POST http://localhost:8091/customer/order
```

**Step 3: Observe Logs**

**Instance 8090 Log:**
```log
2025-10-21T13:53:52.825  INFO [nio-8090-exec-2] CustomerController  : Create order: 1
2025-10-21T13:53:52.893  INFO [nio-8090-exec-2] CustomerController  : Order is PAID: ...customer=spring-8090...
2025-10-21T13:53:53.046  INFO [-service-8090-1] NotificationListener : Order 1 is READY, I'll take it.
                                   ↑ Message listener thread receives order 1 notification
```

**Instance 8091 Log:**
```log
2025-10-21T13:57:08.407  INFO [nio-8091-exec-2] CustomerController  : Create order: 2
2025-10-21T13:57:08.424  INFO [nio-8091-exec-2] CustomerController  : Order is PAID: ...customer=spring-8091...
2025-10-21T13:57:08.472  INFO [-service-8091-1] NotificationListener : Order 2 is READY, I'll take it.
                                   ↑ Message listener thread receives order 2 notification
```

**Expected Results:**
- ✅ Order 1 notification only received by 8090 instance
- ✅ Order 2 notification only received by 8091 instance
- ✅ Two instances do not interfere with each other
- ✅ Precise routing based on customer name

**Step 4: Verify RabbitMQ Queues**

```bash
# Open RabbitMQ Management Console
open http://localhost:15672
# Login: spring / spring

# Query queues via API
curl -u spring:spring http://localhost:15672/api/queues | jq '.[] | select(.name | contains("notifyOrders"))'

# Expected queues:
# - notifyOrders.customer-service-8090
# - notifyOrders.customer-service-8091
```

## Startup Logs

### Bootstrap Phase

```log
# 1. Bootstrap: Application initialization
2025-10-21T13:53:11.986+08:00  INFO [main] CustomerServiceApplication : No active profile set, falling back to 1 default profile: "default"

# 2. Spring Cloud Stream: Create message channels
2025-10-21T13:53:12.522+08:00  INFO [main] faultConfiguringBeanFactoryPostProcessor : No bean named 'errorChannel' has been explicitly defined. Therefore, a default PublishSubscribeChannel will be created.

# 3. GenericScope: Support dynamic bean refresh
2025-10-21T13:53:12.618+08:00  INFO [main] GenericScope : BeanFactory id=1a0a246e-9f76-3f98-8da7-2fa149baec8a

# 4. Tomcat: Initialize with port 8090
2025-10-21T13:53:13.085+08:00  INFO [main] TomcatWebServer : Tomcat initialized with port 8090 (http)

# 5. Feign: Create client for waiter-service
2025-10-21T13:53:13.892+08:00  INFO [main] FeignClientFactoryBean : For 'waiter-service' URL not provided. Will try picking an instance via load-balancing.

# 6. Spring Cloud Stream: Subscribe to notifyOrders channel
2025-10-21T13:53:14.951+08:00  INFO [main] DirectWithAttributesChannel : Channel 'customer-service-1.notifyOrders-in-0' has 1 subscriber(s).

# 7. RabbitMQ: Declare queue with routing key
2025-10-21T13:53:15.574+08:00  INFO [main] RabbitExchangeQueueProvisioner : declaring queue for inbound: notifyOrders.customer-service-8090, bound to: notifyOrders

# 8. RabbitMQ: Connect to broker
2025-10-21T13:53:15.605+08:00  INFO [main] CachingConnectionFactory : Created new connection: rabbitConnectionFactory#4b024fb2:0/SimpleConnection@566c6b91 [delegate=amqp://spring@127.0.0.1:5672/, localPort=65475]

# 9. Spring Cloud Stream: Start message listener
2025-10-21T13:53:15.656+08:00  INFO [main] AmqpInboundChannelAdapter : started bean 'inbound.notifyOrders.customer-service-8090'

# 10. Consul: Service registration
2025-10-21T13:53:15.666+08:00  INFO [main] ConsulServiceRegistry : Registering service with consul: NewService{id='customer-service-8090', name='customer-service', ...port=8090...}

# 11. Application startup completed
2025-10-21T13:53:15.744+08:00  INFO [main] CustomerServiceApplication : Started CustomerServiceApplication in 4.173 seconds (process running for 4.422)
```

**Log Analysis:**

| Step | Event | Details |
|------|-------|---------|
| **1** | Profile selection | Using default profile |
| **4** | Tomcat port | Port 8090 assigned |
| **5** | Feign client | Discover waiter-service via load balancing |
| **7** | Queue declaration | `notifyOrders.customer-service-8090` bound to routing key `spring-8090` |
| **8** | RabbitMQ connection | Connected to localhost:5672 |
| **10** | Consul registration | Service ID: `customer-service-8090`, Port: 8090 |
| **11** | Startup time | **4.173 seconds** total |

### Order Processing Logs

```log
# 1. HTTP Request: Create order
2025-10-21T13:53:52.825  INFO [nio-8090-exec-2] CustomerController : Create order: 1

# 2. HTTP Request: Order paid
2025-10-21T13:53:52.893  INFO [nio-8090-exec-2] CustomerController : Order is PAID: CoffeeOrder(id=1, customer=spring-8090, ...)

# 3. Message Listener: Receive order completion notification (different thread)
2025-10-21T13:53:53.046  INFO [-service-8090-1] NotificationListener : Order 1 is READY, I'll take it.
                                   ↑ Message listener thread (async)
```

**Thread Analysis:**

| Thread Name | Type | Purpose |
|-------------|------|---------|
| `nio-8090-exec-2` | HTTP request thread | Handle REST API requests |
| `-service-8090-1` | Message listener thread | Handle RabbitMQ messages |

**Key Observation:** HTTP processing and message listening run in **separate threads**, demonstrating asynchronous event-driven architecture.

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
     * Order completion notification listener
     * Uses function-based programming model
     * Only receives messages matching this customer's routing key
     * 
     * @return Consumer<Long> Order ID processing function
     */
    @Bean
    public Consumer<Long> notifyOrders() {
        return id -> {
            log.info("Order [{}] is finished.", id);
            
            // Query order details via Feign
            CoffeeOrder order = orderService.getOrder(id);
            log.info("Get Order: {}", order);
            
            // Verify order state is BREWED (coffee completed)
            if (order != null && order.getState() == OrderState.BREWED) {
                log.info("Order [{}] is BREWED, I will take it.", id);
                
                // Update order state to TAKEN (picked up)
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
| `@Bean Consumer<Long>` | Function-based message consumer |
| `@Value("${customer.name}")` | Customer name for logging |
| Message filtering | Via RabbitMQ routing key mechanism |
| State verification | Only process BREWED orders |

### CustomerController (REST API)

```java
@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerController {
    @Autowired
    private CoffeeOrderService orderService;
    
    @Value("${customer.name}")
    private String customer;

    /**
     * Create new order
     * Order state automatically set to PAID after creation
     * Processing starts via Waiter Service
     */
    @PostMapping("/order")
    @Bulkhead(name = "order", type = Bulkhead.Type.THREADPOOL)
    public CoffeeOrder createOrder() {
        // Create order request with dynamic customer name
        NewOrderRequest newOrder = NewOrderRequest.builder()
                .customer(customer)  // Use dynamic customer name: spring-8090
                .items(Arrays.asList(
                        CoffeeOrderItemRequest.builder()
                                .coffee("capuccino")
                                .build()))
                .build();
        
        log.info("Create order: {}", newOrder);
        
        // Create order via Feign (calls Waiter Service)
        CoffeeOrder order = orderService.createOrder(newOrder);
        log.info("Order is PAID: {}", order);
        
        return order;
    }
}
```

**Resilience4j Protection:**
- `@Bulkhead(type = THREADPOOL)`: Isolate failure in separate thread pool
- Prevents Waiter Service failure from blocking Customer Service

## Resilience4j Configuration

### Circuit Breaker

**Configuration:**

```properties
# Circuit breaker configuration
resilience4j.circuitbreaker.instances.order.failure-rate-threshold=50
# Open circuit when failure rate exceeds 50%

resilience4j.circuitbreaker.instances.order.wait-duration-in-open-state=5000
# Wait 5 seconds before attempting half-open state

resilience4j.circuitbreaker.instances.order.sliding-window-size=10
# Use last 10 calls to calculate failure rate
```

**Test Circuit Breaker:**

```bash
# Stop waiter-service to trigger circuit breaker
# Then try creating order
curl -X POST http://localhost:8090/customer/order

# Expected: Circuit breaker will open after several failures
# Subsequent requests will fail fast without calling waiter-service
```

### Bulkhead

**Configuration:**

```properties
# Bulkhead configuration
resilience4j.bulkhead.instances.order.max-concurrent-calls=5
# Maximum 5 concurrent calls

resilience4j.bulkhead.instances.order.max-wait-duration=1000
# Wait maximum 1 second for available slot
```

**Test Bulkhead:**

```bash
# Send 10 concurrent requests
for i in {1..10}; do
  curl -X POST http://localhost:8090/customer/order &
done

# Expected: First 5 succeed, remaining 5 either wait or fail
```

## Monitoring

### View RabbitMQ Queue Status

```bash
# Query specific queue
curl -u spring:spring http://localhost:15672/api/queues/%2F/notifyOrders.customer-service-8090 | jq '{name, messages, consumers}'

# Expected output:
# {
#   "name": "notifyOrders.customer-service-8090",
#   "messages": 0,
#   "consumers": 1
# }
```

### View Consul Service Status

```bash
# Query customer-service instances
curl -s http://localhost:8500/v1/catalog/service/customer-service | jq '.[] | {ID: .ServiceID, Port: .ServicePort, Health: .Checks[0].Status}'

# Expected output:
# {
#   "ID": "customer-service-8090",
#   "Port": 8090,
#   "Health": "passing"
# }
# {
#   "ID": "customer-service-8091",
#   "Port": 8091,
#   "Health": "passing"
# }
```

## Common Issues

### Issue 1: Message Not Received

**Symptom:**
```
Order created but no "Order is READY" log
```

**Solutions:**

```bash
# 1. Verify RabbitMQ queue exists with correct routing key
curl -u spring:spring http://localhost:15672/api/queues/%2F/notifyOrders.customer-service-8090 | jq '.bindings'

# Expected: binding-routing-key = "spring-8090"

# 2. Check consumer group configuration
# Each instance must use unique group name:
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}

# 3. Verify routing key matches customer name
# application.properties:
customer.name=spring-${server.port}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}
```

### Issue 2: Duplicate Message Processing

**Symptom:**
```
Both 8090 and 8091 instances receive same message
```

**Root Cause:** Using same consumer group

**Solutions:**

```bash
# ❌ Wrong: Same group name for all instances
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service

# ✅ Correct: Unique group name per instance
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}
```

### Issue 3: Feign Client Connection Failed

**Error:**
```
feign.RetryableException: Connection refused executing GET http://waiter-service/order/1
```

**Solutions:**

```bash
# 1. Verify waiter-service is registered in Consul
curl -s http://localhost:8500/v1/catalog/service/waiter-service

# 2. Check Feign client configuration
# bootstrap.properties should have:
spring.cloud.consul.discovery.enabled=true

# 3. Test service discovery
curl -s http://localhost:8500/v1/catalog/service/waiter-service | jq '.[] | {Port: .ServicePort}'
```

### Issue 4: Circuit Breaker Stays Open

**Symptom:**
```
All requests fail even after waiter-service recovers
```

**Solutions:**

```bash
# 1. Check circuit breaker state
curl http://localhost:8090/actuator/circuitbreakers | jq

# 2. Wait for automatic recovery (wait-duration-in-open-state)
# Default: 5 seconds

# 3. Or restart customer-service
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"
```

## Best Practices

### 1. Routing Key Configuration

**✅ Recommended: Dynamic routing key**

```properties
# Use dynamic customer name based on port
customer.name=spring-${server.port}
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=${customer.name}
```

**❌ Not Recommended: Hard-coded routing key**

```properties
# Hard-coded routing key (not scalable)
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=spring-8090
```

### 2. Consumer Group Configuration

**✅ Recommended: Unique group per instance**

```properties
# Each instance has isolated queue
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-${server.port}
```

**⚠️ Caution: Shared group for load balancing**

```properties
# All instances share same queue (load balancing mode)
# Use this only when messages can be processed by any instance
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service
```

### 3. Resilience4j Configuration

**Circuit Breaker:**
```properties
# ✅ Recommended: Reasonable failure threshold
resilience4j.circuitbreaker.instances.order.failure-rate-threshold=50

# ✅ Recommended: Appropriate wait duration
resilience4j.circuitbreaker.instances.order.wait-duration-in-open-state=5000
```

**Bulkhead:**
```properties
# ✅ Recommended: Limit concurrent calls
resilience4j.bulkhead.instances.order.max-concurrent-calls=5

# ✅ Recommended: Short wait duration
resilience4j.bulkhead.instances.order.max-wait-duration=1000
```

### 4. Message Durability

**✅ Recommended: Enable durable subscription**

```properties
# Ensure messages not lost on consumer restart
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.durable-subscription=true
```

### 5. Error Handling

**Add Retry Configuration:**

```properties
# Retry on message processing failure
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.max-attempts=3
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.back-off-initial-interval=1000
spring.cloud.stream.bindings.notifyOrders-in-0.consumer.back-off-multiplier=2
```

**Add Dead Letter Queue:**

```properties
# Enable DLQ for failed messages
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.auto-bind-dlq=true
spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.republish-to-dlq=true
```

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Customer Services                     │
│  ┌────────────────────┐      ┌────────────────────┐     │
│  │ Instance 8090      │      │ Instance 8091      │     │
│  │ customer=spring-   │      │ customer=spring-   │     │
│  │          8090      │      │          8091      │     │
│  └──────────┬─────────┘      └──────────┬─────────┘     │
└─────────────┼─────────────────────────────┼─────────────┘
              │ Feign Client                │
              │ (via Consul)                │
              ▼                             ▼
┌─────────────────────────────────────────────────────────┐
│              Waiter Service (Port 65440)                 │
│  - Receive order creation requests                      │
│  - Send newOrders message to Barista                    │
│  - Receive finishedOrders notification                  │
│  - Send notifyOrders with routing key (customer name)   │
└─────────────┬─────────────────────┬─────────────────────┘
              │                     │
              ▼                     ▼
     ┌────────────────┐    ┌────────────────┐
     │ RabbitMQ       │    │ Barista Service│
     │ - newOrders    │    │ - Process      │
     │ - finishedOrders│   │   orders       │
     │ - notifyOrders │    │ - Send         │
     │   (routing key)│    │   completion   │
     └────────────────┘    └────────────────┘
```

### Message Routing Flow

```
Customer (8090)
   │ POST /customer/order
   ▼
Waiter Service
   │ updateState(PAID)
   ├──> newOrders Exchange
   │       │
   │       ▼
   │   Barista Service
   │       │ Process coffee (5s)
   │       │ updateState(BREWED)
   │       ▼
   │   finishedOrders Exchange
   │       │
   │       ▼
   │   Waiter Service
   │       │ finishedOrders Consumer
   │       │
   │       ├──> notifyOrders Exchange
   │            │ (routing-key: spring-8090)
   │            ▼
   │        RabbitMQ Routing
   │            │
   │            ├──> Queue: notifyOrders.customer-service-8090
   │            │         ↓
   │            │     Customer (8090) ✅ Receives
   │            │
   │            └──> Queue: notifyOrders.customer-service-8091
   │                      ↓
   │                  Customer (8091) ⛔ Does NOT receive
   ▼
Order state: TAKEN
Workflow complete
```

## Testing

### Manual Testing

**Test Single Instance:**

```bash
# Start single instance
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Create order
curl -X POST http://localhost:8090/customer/order | jq

# Expected flow:
# 1. Order created with state INIT
# 2. Order updated to PAID
# 3. Wait ~5 seconds (Barista processing)
# 4. Receive notification: "Order is READY, I'll take it."
# 5. Order state updated to TAKEN
```

**Test Multi-Instance Routing:**

```bash
# Terminal 1: Start instance 8090
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8090"

# Terminal 2: Start instance 8091
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8091"

# Terminal 3: Create order on 8090
curl -X POST http://localhost:8090/customer/order

# Expected:
# - Instance 8090 log: "Order 1 is READY, I'll take it."
# - Instance 8091 log: (no output for order 1)

# Terminal 4: Create order on 8091
curl -X POST http://localhost:8091/customer/order

# Expected:
# - Instance 8091 log: "Order 2 is READY, I'll take it."
# - Instance 8090 log: (no output for order 2)
```

### Automated Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify
```

## Best Practices Demonstrated

1. **Event-Driven Architecture**: Use messaging for asynchronous communication
2. **Routing Key Precision**: Implement precise message delivery to specific instances
3. **Function-Based Model**: Leverage Spring Cloud Stream 4.x function model
4. **Resilience Pattern**: Circuit breaker and bulkhead for fault tolerance
5. **Service Discovery**: Dynamic service location via Consul
6. **Multi-Instance Support**: Horizontal scaling with message isolation
7. **Dynamic Configuration**: Port-based customer naming for flexibility
8. **Durable Messaging**: Persistent subscriptions to prevent message loss
9. **Thread Separation**: Async message processing separate from HTTP handling
10. **Monitoring Integration**: Comprehensive Actuator endpoints

## Advanced Topics

### 1. Custom Message Headers

```java
// In Waiter Service: Send message with custom headers
Message<Long> message = MessageBuilder.withPayload(orderId)
        .setHeader("customer", order.getCustomer())
        .setHeader("priority", "high")
        .build();
streamBridge.send("notifyOrders-out-0", message);
```

### 2. Message Error Handling

```java
@Bean
public Consumer<Long> notifyOrders() {
    return id -> {
        try {
            // Process message
            processOrder(id);
        } catch (Exception e) {
            log.error("Failed to process order {}", id, e);
            // Message will be sent to DLQ if configured
            throw e;
        }
    };
}
```

### 3. Load Balancing Mode

```properties
# Use shared consumer group for load balancing
# All instances share same queue, messages distributed round-robin
spring.cloud.stream.bindings.notifyOrders-in-0.group=customer-service-shared

# Remove routing key binding for broadcast
# spring.cloud.stream.rabbit.bindings.notifyOrders-in-0.consumer.binding-routing-key=
```

## References

- [Spring Cloud Stream Documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Spring Cloud Stream Function Model](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#spring-cloud-stream-overview-using-spring-cloud-stream)
- [RabbitMQ Routing Tutorial](https://www.rabbitmq.com/tutorials/tutorial-four-spring-amqp.html)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Consul Service Discovery](https://www.consul.io/docs/discovery)

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
