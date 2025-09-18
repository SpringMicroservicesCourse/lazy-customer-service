package tw.fengqing.spring.springbucks.customer;

import tw.fengqing.spring.springbucks.customer.support.CustomConnectionKeepAliveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;



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
		// HttpClient 5.x 的新寫法，避免使用已棄用的方法
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(200);  // 設定最大連線數
		connectionManager.setDefaultMaxPerRoute(20); // 設定每個路由的最大連線數
		connectionManager.setValidateAfterInactivity(TimeValue.ofSeconds(30)); // 空閒連線驗證時間
		
		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.disableAutomaticRetries() // 停用自動重試
				.setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy()) // 自定義 Keep-Alive 策略
				.build();
	}
}
