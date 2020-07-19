package dev.widget.widgetservice.application;

import dev.widget.widgetservice.controllers.WidgetController;
import dev.widget.widgetservice.models.Widget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class WidgetServiceApplicationTests {
    @Autowired
    WidgetController controller;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(controller, notNullValue());
    }

    @Test
    void createWidgetSuccess() {
        // Arrange
        Widget src = randomWidget();

        // Act
        Widget result = restTemplate.postForObject(widgetsUrl(), src, Widget.class);

        // Assert
        assertThat(result.getX(), is(src.getX()));
        assertThat(result.getY(), is(src.getY()));
        assertThat(result.getZ(), is(src.getZ()));
        assertThat(result.getWidth(), is(src.getWidth()));
        assertThat(result.getHeight(), is(src.getHeight()));
        assertThat(result.getId(), not(emptyString()));
        assertThat(result.getLastModification(), notNullValue());
    }

    @Test
    void createWidgetError() {
        // Arrange
        Widget src = randomWidget();
        src.setX(null);
        // Act
        ResponseEntity<Widget> result = restTemplate.postForEntity(widgetsUrl(), src, Widget.class);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getWidgetSuccess() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);

        // Act
        Widget result = restTemplate.getForObject(widgetUrl(created.getId()), Widget.class);

        // Assert
        assertThat(result.getX(), is(src.getX()));
        assertThat(result.getY(), is(src.getY()));
        assertThat(result.getZ(), is(src.getZ()));
        assertThat(result.getWidth(), is(src.getWidth()));
        assertThat(result.getHeight(), is(src.getHeight()));
        assertThat(result.getId(), is(created.getId()));
        assertThat(result.getLastModification(), is(created.getLastModification()));
    }

    @Test
    void getWidgetError() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);

        // Act
        ResponseEntity<Widget> result = restTemplate.getForEntity(widgetUrl(created.getId() + "NO!"), Widget.class);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    void getAllWidgetOnePage() {
        // Arrange
        List<Widget> src = IntStream.range(0, 5).mapToObj(
                i -> randomWidget().toBuilder().z(null).build()
        ).collect(toUnmodifiableList());
        List<Widget> created = src.stream().map(
                w -> restTemplate.postForObject(widgetsUrl(), w, Widget.class)
        ).collect(toUnmodifiableList());

        // Act
        Widget[] result = restTemplate.getForObject(pagedUrl(100, 0), Widget[].class);

        // Assert
        assertThat(result.length, is(created.size()));
        for (int i = 0; i < created.size(); i++) {
            assertThat(result[i], is(created.get(i)));
        }
    }

    @Test
    void getAllWidgetMultiPage() {
        // Arrange
        List<Widget> src = IntStream.range(0, 5).mapToObj(
                i -> randomWidget().toBuilder().z(null).build()
        ).collect(toUnmodifiableList());
        List<Widget> created = src.stream().map(
                w -> restTemplate.postForObject(widgetsUrl(), w, Widget.class)
        ).collect(toUnmodifiableList());

        // Act
        List<Widget[]> result = IntStream.range(0, 5).mapToObj(
                i -> restTemplate.getForObject(pagedUrl(1, i), Widget[].class)
        ).collect(toUnmodifiableList());

        // Assert
        for (int i = 0; i < created.size(); i++) {
            assertThat(result.get(i).length, is(1));
            assertThat(result.get(i)[0], is(created.get(i)));
        }
    }

    @Test
    void getAllWidgetTooBigPage() {
        // Arrange -- nothing!

        // Act&Assert
        Throwable e = assertThrows(RestClientException.class,
                () -> restTemplate.getForObject(pagedUrl(1_000_000, 0), Widget[].class)
        );
    }

    @Test
    void setWidgetSuccess() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);
        Widget update = created.toBuilder().x(created.getX() + 1).build();
        HttpEntity<Widget> updateRequest = new HttpEntity<>(update);

        // Act
        ResponseEntity<Widget> response = restTemplate.exchange(widgetUrl(update.getId()), HttpMethod.PUT,
                updateRequest, Widget.class);

        // Assert
        Widget result = response.getBody();
        assertThat(result.getX(), is(update.getX()));
        assertThat(result.getY(), is(update.getY()));
        assertThat(result.getZ(), is(update.getZ()));
        assertThat(result.getWidth(), is(update.getWidth()));
        assertThat(result.getHeight(), is(update.getHeight()));
        assertThat(result.getId(), is(update.getId()));
        assertThat(result.getLastModification().isAfter(created.getLastModification()), is(true));
    }

    @Test
    void setWidgetError() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);
        Widget update = created.toBuilder().x(created.getX() + 1).build();
        HttpEntity<Widget> updateRequest = new HttpEntity<>(update);

        // Act
        ResponseEntity<Widget> result = restTemplate.exchange(widgetUrl(update.getId() + "NO!"), HttpMethod.PUT,
                updateRequest, Widget.class);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void setWidgetPushesUp() {
        // Arrange
        Widget src1 = randomWidget();
        src1.setZ(1);
        Widget created1 = restTemplate.postForObject(widgetsUrl(), src1, Widget.class);
        Widget src2 = randomWidget();
        src2.setZ(2);
        Widget created2 = restTemplate.postForObject(widgetsUrl(), src2, Widget.class);
        Widget src3 = randomWidget();
        src3.setZ(3);
        Widget created3 = restTemplate.postForObject(widgetsUrl(), src3, Widget.class);
        Widget update = created2.toBuilder().z(1).build();
        HttpEntity<Widget> updateRequest = new HttpEntity<>(update);

        // Act
        ResponseEntity<Widget> response = restTemplate.exchange(widgetUrl(update.getId()), HttpMethod.PUT,
                updateRequest, Widget.class);

        // Assert
        Widget result = response.getBody();
        assertThat(result.getX(), is(update.getX()));
        assertThat(result.getY(), is(update.getY()));
        assertThat(result.getZ(), is(update.getZ()));
        assertThat(result.getWidth(), is(update.getWidth()));
        assertThat(result.getHeight(), is(update.getHeight()));
        assertThat(result.getId(), is(update.getId()));

        // Check current status
        Widget[] widgets = restTemplate.getForObject(pagedUrl(100, 0), Widget[].class);
        assertThat(widgets.length, is(3));

        assertThat(widgets[0].getX(), is(created2.getX()));
        assertThat(widgets[0].getY(), is(created2.getY()));
        assertThat(widgets[0].getZ(), is(1));
        assertThat(widgets[0].getWidth(), is(created2.getWidth()));
        assertThat(widgets[0].getHeight(), is(created2.getHeight()));
        assertThat(widgets[0].getId(), is(created2.getId()));

        assertThat(widgets[1].getX(), is(created1.getX()));
        assertThat(widgets[1].getY(), is(created1.getY()));
        assertThat(widgets[1].getZ(), is(2));
        assertThat(widgets[1].getWidth(), is(created1.getWidth()));
        assertThat(widgets[1].getHeight(), is(created1.getHeight()));
        assertThat(widgets[1].getId(), is(created1.getId()));

        assertThat(widgets[2].getX(), is(created3.getX()));
        assertThat(widgets[2].getY(), is(created3.getY()));
        assertThat(widgets[2].getZ(), is(4));
        assertThat(widgets[2].getWidth(), is(created3.getWidth()));
        assertThat(widgets[2].getHeight(), is(created3.getHeight()));
        assertThat(widgets[2].getId(), is(created3.getId()));
    }

    @Test
    void deleteWidgetSuccess() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);
        HttpEntity<Void> deleteRequest = new HttpEntity<>((Void) null);

        // Act
        ResponseEntity<Widget> response = restTemplate.exchange(widgetUrl(created.getId()), HttpMethod.DELETE,
                deleteRequest, Widget.class);

        // Assert
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
    }

    @Test
    void deleteWidgetError() {
        // Arrange
        Widget src = randomWidget();
        Widget created = restTemplate.postForObject(widgetsUrl(), src, Widget.class);
        HttpEntity<Void> deleteRequest = new HttpEntity<>((Void) null);

        // Act
        ResponseEntity<Widget> response = restTemplate.exchange(widgetUrl(created.getId() + "NO!"), HttpMethod.DELETE,
                deleteRequest, Widget.class);

        // Assert
        assertThat(response.getStatusCode(), is(HttpStatus.GONE));
    }

    private String pagedUrl(int pageSize, int offset) {
        return widgetsUrl() + format("?pageSize=%s&offset=%s", pageSize, offset);
    }

    private String widgetUrl(final String id) {
        return localServerUrl() + format("/widget/%s", id);
    }

    private String widgetsUrl() {
        return localServerUrl() + "/widgets";
    }

    private String localServerUrl() {
        return format("http://localhost:%s", port);
    }

    private Widget randomWidget() {
        return Widget.builder()
                .x(ThreadLocalRandom.current().nextInt())
                .y(ThreadLocalRandom.current().nextInt())
                .z(ThreadLocalRandom.current().nextInt())
                .width(ThreadLocalRandom.current().nextInt())
                .height(ThreadLocalRandom.current().nextInt())
                .build();
    }

}
