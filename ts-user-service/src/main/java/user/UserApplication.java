package user;

import io.ztbeike.ffr4ms.trace.annotation.EnableTracePlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author fdse
 */
@SpringBootApplication
@EnableEurekaClient
@EnableTracePlugin
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
