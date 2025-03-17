package eureka;

import io.ztbeike.ffr4ms.registry.EnableRegistryPlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
@EnableRegistryPlugin
public class EurekaMain {
    public static void main(String[] args) {
        SpringApplication.run(EurekaMain.class, args);
    }
}
