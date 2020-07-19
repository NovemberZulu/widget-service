package dev.widget.widgetservice.application.configuration;

import dev.widget.widgetservice.models.Widget;
import dev.widget.widgetservice.persistance.InMemoryWidgetDao;
import dev.widget.widgetservice.persistance.WidgetDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@Configuration
public class SpringConfiguration {

    /*
     * this is where we select underlying persistence implementation
     */
    @Bean
    @Primary
    WidgetDao<Widget> widgetDao(final InMemoryWidgetDao dao) {
        return dao;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
