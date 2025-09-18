package tw.fengqing.spring.springbucks.customer.controller;

import tw.fengqing.spring.springbucks.customer.integration.CoffeeOrderService;
import tw.fengqing.spring.springbucks.customer.integration.CoffeeService;
import tw.fengqing.spring.springbucks.customer.model.Coffee;
import tw.fengqing.spring.springbucks.customer.model.CoffeeOrder;
import tw.fengqing.spring.springbucks.customer.model.NewOrderRequest;
import tw.fengqing.spring.springbucks.customer.model.OrderState;
import tw.fengqing.spring.springbucks.customer.model.OrderStateRequest;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
// 暫時移除 CircuitBreakerOpenException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
// 移除 Vavr 依賴，使用 Java 8+ 原生方式
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerController {
    @Autowired
    private CoffeeService coffeeService;
    @Autowired
    private CoffeeOrderService coffeeOrderService;
    @Value("${customer.name}")
    private String customer;
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;

    public CustomerController(CircuitBreakerRegistry circuitBreakerRegistry,
                              BulkheadRegistry bulkheadRegistry) {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("menu");
        bulkhead = bulkheadRegistry.bulkhead("menu");
    }

    @GetMapping("/menu")
    public List<Coffee> readMenu() {
        try {
            return Bulkhead.decorateSupplier(bulkhead,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> coffeeService.getAll())).get();
        } catch (BulkheadFullException e) {
            log.warn("Circuit breaker or bulkhead is open, returning empty list", e);
            return Collections.emptyList();
        }
    }

    @PostMapping("/order")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "order")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "order")
    public CoffeeOrder createAndPayOrder() {
        NewOrderRequest orderRequest = NewOrderRequest.builder()
                .customer(customer)
                .items(Arrays.asList("capuccino"))
                .build();
        CoffeeOrder order = coffeeOrderService.create(orderRequest);
        log.info("Create order: {}", order != null ? order.getId() : "-");
        order = coffeeOrderService.updateState(order.getId(),
                OrderStateRequest.builder().state(OrderState.PAID).build());
        log.info("Order is PAID: {}", order);
        return order;
    }

}
