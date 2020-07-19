package dev.widget.widgetservice.controllers;

import dev.widget.widgetservice.application.configuration.AppConfiguration;
import dev.widget.widgetservice.models.Widget;
import dev.widget.widgetservice.persistance.WidgetDao;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetControllerTest {
    private static final String WIDGET = "/widget/{id}";

    @Mock
    WidgetDao<Widget> dao;

    @Mock
    AppConfiguration config;

    @Mock
    Widget src;

    @Mock
    Widget widget;

    @Mock
    UriComponentsBuilder uriBuilder;

    @Mock
    UriComponents uriComponents;

    URI uri;

    @Mock
    List<Widget> widgets;

    @InjectMocks
    private WidgetController sut;

    @BeforeEach
    void before() throws URISyntaxException {
        // URI is final
        uri = new URI(format("https://%s/", RandomStringUtils.randomAlphanumeric(10)));
    }

    @Test
    void createWidgetCreatesWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(true);
        when(dao.create(src)).thenReturn(widget);
        when(widget.getId()).thenReturn(id);
        when(uriBuilder.path(WIDGET)).thenReturn(uriBuilder);
        when(uriBuilder.buildAndExpand(id)).thenReturn(uriComponents);
        when(uriComponents.toUri()).thenReturn(uri);

        // Act
        ResponseEntity result = sut.createWidget(src, uriBuilder);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(result.getHeaders().getLocation(), is(uri));
        assertThat(result.getBody(), sameInstance(widget));

        verify(src).isValidSource();
        verify(dao).create(same(src));
        verify(widget).getId();
        verify(uriBuilder).path(WIDGET);
        verify(uriBuilder).buildAndExpand(id);
        verify(uriComponents).toUri();
    }

    @Test
    void createWidgetReturnErrorOnInvalidSource() {
        // Arrange
        when(src.isValidSource()).thenReturn(false);

        // Act
        ResponseEntity result = sut.createWidget(src, uriBuilder);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        verify(src).isValidSource();
    }

    @Test
    void createWidgetHandlesNullSource() {
        // Arrange -- nothing!

        // Act
        ResponseEntity result = sut.createWidget(null, uriBuilder);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void GetWidgetGetsWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(dao.get(id)).thenReturn(widget);

        // Act
        ResponseEntity result = sut.getWidget(id);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widget));

        verify(dao).get(id);
    }

    @Test
    void GetWidgetReturnsNotFoundWhenWidgetNotFound() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(dao.get(id)).thenReturn(null);

        // Act
        ResponseEntity result = sut.getWidget(id);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.NOT_FOUND));

        verify(dao).get(id);
    }

    @Test
    void GetWidgetAllWidgetsReturnAllWidgets() {
        // Arrange
        int pageSize = ThreadLocalRandom.current().nextInt();
        int offset = ThreadLocalRandom.current().nextInt();
        when(config.getMaxPageSize()).thenReturn(pageSize + 1);
        when(dao.getAll(pageSize, offset)).thenReturn(widgets);
        when(widgets.size()).thenReturn(2);

        // Act
        ResponseEntity result = sut.getAllWidgets(pageSize, offset);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widgets));

        verify(config).getMaxPageSize();
        verify(dao).getAll(pageSize, offset);
        verify(widgets).size();
    }

    @Test
    void GetWidgetAllWidgetsUsesDefaultPageSize() {
        // Arrange
        int pageSize = ThreadLocalRandom.current().nextInt();
        int offset = ThreadLocalRandom.current().nextInt();
        when(config.getDefaultPageSize()).thenReturn(pageSize);
        when(config.getMaxPageSize()).thenReturn(pageSize + 1);
        when(dao.getAll(pageSize, offset)).thenReturn(widgets);
        when(widgets.size()).thenReturn(2);

        // Act
        ResponseEntity result = sut.getAllWidgets(null, offset);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widgets));

        verify(config).getMaxPageSize();
        verify(dao).getAll(pageSize, offset);
        verify(widgets).size();
    }

    @Test
    void GetWidgetAllWidgetsUErrorsOnTooBigPageSize() {
        // Arrange
        int pageSize = ThreadLocalRandom.current().nextInt();
        when(config.getMaxPageSize()).thenReturn(pageSize - 1);

        // Act
        ResponseEntity result = sut.getAllWidgets(pageSize, 0);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));

        verify(config, atLeastOnce()).getMaxPageSize();
    }

    @Test
    void GetWidgetAllWidgetsUsesZeroOffsetByDefault() {
        // Arrange
        int pageSize = ThreadLocalRandom.current().nextInt();
        when(config.getMaxPageSize()).thenReturn(pageSize + 1);
        when(dao.getAll(pageSize, 0)).thenReturn(widgets);
        when(widgets.size()).thenReturn(2);

        // Act
        ResponseEntity result = sut.getAllWidgets(pageSize, null);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widgets));

        verify(config).getMaxPageSize();
        verify(dao).getAll(pageSize, 0);
        verify(widgets).size();
    }

    @Test
    void GetWidgetAllWidgetsReturnNoContentWhenNoWidgets() {
        // Arrange
        int pageSize = ThreadLocalRandom.current().nextInt();
        int offset = ThreadLocalRandom.current().nextInt();
        when(config.getMaxPageSize()).thenReturn(pageSize + 1);
        when(dao.getAll(pageSize, offset)).thenReturn(widgets);
        when(widgets.size()).thenReturn(0);

        // Act
        ResponseEntity result = sut.getAllWidgets(pageSize, offset);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.NO_CONTENT));

        verify(config).getMaxPageSize();
        verify(dao).getAll(pageSize, offset);
        verify(widgets).size();
    }

    @Test
    void setWidgetUpdatesWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(true);
        when(src.getId()).thenReturn(id);
        when(dao.update(src)).thenReturn(widget);

        // Act
        ResponseEntity result = sut.setWidget(id, src);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widget));

        verify(src).isValidSource();
        verify(src, atLeastOnce()).getId();
        verify(dao).update(same(src));
    }

    @Test
    void setWidgetErrorsWhenNotFound() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(true);
        when(src.getId()).thenReturn(id);
        when(dao.update(src)).thenReturn(null);

        // Act
        ResponseEntity result = sut.setWidget(id, src);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.NOT_FOUND));

        verify(src).isValidSource();
        verify(src, atLeastOnce()).getId();
        verify(dao).update(same(src));
    }

    @Test
    void setWidgetSetsIdFromPath() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(true);
        when(src.getId()).thenReturn(id);
        when(dao.update(src)).thenReturn(widget);

        // Act
        ResponseEntity result = sut.setWidget(id, src);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), sameInstance(widget));

        verify(src).setId(id);
    }

    @Test
    void setWidgetErrorsOnIdMismatch() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(true);
        when(src.getId()).thenReturn(id + "NO!");

        // Act
        ResponseEntity result = sut.setWidget(id, src);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void setWidgetErrorsOnInvalidSource() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(src.isValidSource()).thenReturn(false);

        // Act
        ResponseEntity result = sut.setWidget(id, src);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void setWidgetHandlesNullInput() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);

        // Act
        ResponseEntity result = sut.setWidget(id, null);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void removeWidgetRemovesWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(dao.delete(id)).thenReturn(true);

        // Act
        ResponseEntity result = sut.removeWidget(id);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.NO_CONTENT));

        verify(dao).delete(id);
    }

    @Test
    void removeWidgetErrorsOnNotMissingWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(dao.delete(id)).thenReturn(false);

        // Act
        ResponseEntity result = sut.removeWidget(id);

        // Assert
        assertThat(result.getStatusCode(), is(HttpStatus.GONE));

        verify(dao).delete(id);
    }
}