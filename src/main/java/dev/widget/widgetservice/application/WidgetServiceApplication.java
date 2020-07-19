package dev.widget.widgetservice.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "dev.widget.widgetservice")
public class WidgetServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WidgetServiceApplication.class, args);
	}

}
