package train;

import io.ztbeike.ffr4ms.trace.annotation.EnableTracePlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync
@IntegrationComponentScan
@EnableSwagger2
@EnableEurekaClient
@EnableTracePlugin
public class TrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainApplication.class, args);
    }

}
