package dev.widget.widgetservice.application.configuration;

import dev.widget.widgetservice.models.Widget;
import dev.widget.widgetservice.persistance.InMemoryWidgetDao;
import dev.widget.widgetservice.persistance.WidgetDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;


@ExtendWith(MockitoExtension.class)
class SpringConfigurationTest {
    @Mock
    InMemoryWidgetDao inMemoryWidgetDao;

    @InjectMocks
    SpringConfiguration sut;

    @Test
    void widgetDao() {
        // Arrange -- nothing!

        // Act
        WidgetDao<Widget> result = sut.widgetDao(inMemoryWidgetDao);

        // Assert
        assertThat(result, sameInstance(inMemoryWidgetDao)); // no logic in this method currently
    }

    @Test
    void clock() {
        // Arrange -- nothing!

        // Act
        Clock result = sut.clock();

        // Assert
        assertThat(result, notNullValue()); // just a clock
    }
}