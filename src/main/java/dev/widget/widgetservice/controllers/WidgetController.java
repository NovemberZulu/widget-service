package dev.widget.widgetservice.controllers;

import dev.widget.widgetservice.application.configuration.AppConfiguration;
import dev.widget.widgetservice.models.ErrorResponse;
import dev.widget.widgetservice.models.Widget;
import dev.widget.widgetservice.persistance.WidgetDao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

@RestController
@Component
public class WidgetController {
    private final static String WIDGETS_PATH = "/widgets";
    private final static String WIDGET_PATH = "/widget/{id}";

    private final WidgetDao<Widget> dao;
    private final AppConfiguration config;

    public WidgetController(final WidgetDao<Widget> dao, final AppConfiguration config) {
        this.dao = dao;
        this.config = config;
    }

    @PostMapping(WIDGETS_PATH)
    public ResponseEntity createWidget(@RequestBody final Widget src, final UriComponentsBuilder uriBuilder) {
        final Widget input = Optional.ofNullable(src).orElseGet(Widget::new);
        if (!input.isValidSource()) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder().message("Request missing required params").build()
            );
        }
        final Widget result = dao.create(src);
        return ResponseEntity.created(
                uriBuilder.path(WIDGET_PATH).buildAndExpand(result.getId()).toUri()
        ).body(result);
    }

    @GetMapping(WIDGET_PATH)
    public ResponseEntity getWidget(@PathVariable("id") final String id) {
        final Widget result = dao.get(id);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.builder().message(format("Widget [%s] not found", id)).build()
            );
        }
    }

    @GetMapping(WIDGETS_PATH)
    public ResponseEntity getAllWidgets(@RequestParam(name = "pageSize", required = false) final Integer pageSize,
                                        @RequestParam(name = "offset", required = false) final Integer offset) {
        final int requestedPageSize = Optional.ofNullable(pageSize).orElseGet(config::getDefaultPageSize);
        final int requestedOffset = Optional.ofNullable(offset).orElse(0);
        if (requestedPageSize > config.getMaxPageSize()) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder()
                            .message(format(
                                    "Requested page size %s is more than maximum page size %s",
                                    requestedPageSize, config.getMaxPageSize()))
                            .build()
            );
        }
        final List<Widget> result = dao.getAll(requestedPageSize, requestedOffset);
        if (result.size() == 0) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @PutMapping(WIDGET_PATH)
    public ResponseEntity setWidget(@PathVariable("id") final String id, @RequestBody final Widget widget) {
        final Widget input = Optional.ofNullable(widget).orElseGet(Widget::new);
        if (!input.isValidSource()) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder().message("Request missing required params").build()
            );
        }
        if ((input.getId() != null) && !Objects.equals(input.getId(), id)) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder()
                            .message(format(
                                    "Widget ID [%s] from request path doesn't match widget ID [%s] from request body",
                                    input.getId(), id))
                            .build()
            );
        }
        input.setId(id);
        final Widget result = dao.update(widget);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.builder().message(format("Widget ID [%s] not found", id)).build()
            );
        }
    }

    @DeleteMapping(WIDGET_PATH)
    public ResponseEntity removeWidget(@PathVariable("id") final String id) {
        if (dao.delete(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
    }
}