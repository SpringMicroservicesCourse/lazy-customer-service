package tw.fengqing.spring.springbucks.customer;

import tw.fengqing.spring.springbucks.customer.support.CustomConnectionKeepAliveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
@EnableDiscoveryClient
@EnableFeignClients
@EnableAspectJAutoProxy
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }


	@Bean
	public CloseableHttpClient httpClient() {
		// 整合連線池管理器和 HttpClient 配置
		return HttpClients.custom()
				.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
						.setMaxConnTotal(200) // 最大連線數
						.setMaxConnPerRoute(20) // 每個路由最大連線數
						.setDefaultConnectionConfig(ConnectionConfig.custom()
								.setTimeToLive(TimeValue.ofSeconds(30)) // 連線存活時間
								.build())
						.build())
				.evictIdleConnections(TimeValue.ofSeconds(30)) // 空閒連線清理
				.disableAutomaticRetries() // 停用自動重試
				.setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy()) // 自定義 Keep-Alive 策略
				.build();
	}

}
