package com.example.wristband;

import com.example.wristband.config.PrinterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties(PrinterProperties.class)
public class WristbandPrinterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WristbandPrinterServiceApplication.class, args);
    }
}
