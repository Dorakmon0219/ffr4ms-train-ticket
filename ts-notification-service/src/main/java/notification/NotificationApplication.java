package notification;

import io.ztbeike.ffr4ms.trace.annotation.EnableTracePlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author fdse
 */
@SpringBootApplication
@EnableSwagger2
@EnableEurekaClient
@EnableTracePlugin
public class NotificationApplication{
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}