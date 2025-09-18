package tw.fengqing.spring.springbucks.customer.integration;

import tw.fengqing.spring.springbucks.customer.model.CoffeeOrder;
import tw.fengqing.spring.springbucks.customer.model.OrderState;
import tw.fengqing.spring.springbucks.customer.model.OrderStateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Slf4j
public class NotificationListener {
    @Autowired
    private CoffeeOrderService orderService;
    @Value("${customer.name}")
    private String customer;

    @Bean
    public Consumer<Long> notifyOrders() {
        return id -> {
            CoffeeOrder order = orderService.getOrder(id);
            if (order != null && OrderState.BREWED == order.getState()) {
                log.info("Order {} is READY, I'll take it.", id);
                orderService.updateState(id,
                        OrderStateRequest.builder().state(OrderState.TAKEN).build());
            } else {
                log.warn("Order {} is NOT READY. Current state: {}. Why are you notify me?", 
                        id, order != null ? order.getState() : "null");
            }
        };
    }
}
