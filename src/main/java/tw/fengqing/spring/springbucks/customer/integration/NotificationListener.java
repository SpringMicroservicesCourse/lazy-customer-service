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

/**
 * 通知客戶監聽器 - 處理通知客戶消息
 * 使用 Spring Cloud Stream 函數式編程模型
 */
@Component
@Slf4j
public class NotificationListener {
    @Autowired
    private CoffeeOrderService orderService;
    @Value("${customer.name}")
    private String customer;

        /**
     * 接收通知客戶消息的函數式 Bean
     * 接收訂單 ID，更新訂單狀態
     * 如果訂單狀態為 BREWED，則更新訂單狀態為 TAKEN
     * 如果訂單狀態為 NOT READY，則發出警告
     * @return 通知客戶處理函數
     */
    @Bean
    public Consumer<Long> notifyOrders() {
        return id -> {
            CoffeeOrder order = orderService.getOrder(id);
            if (order != null && OrderState.BREWED == order.getState()) {
                log.info("Order {} is READY, I'll take it.", id);
                orderService.updateState(id,
                        OrderStateRequest.builder().state(OrderState.TAKEN).build());
            } else {
                log.warn("Order {} is NOT READY. Why are you notify me?", id);
            }
        };
    }
}
