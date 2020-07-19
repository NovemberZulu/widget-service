package dev.widget.widgetservice.models;

import lombok.Builder;
import lombok.Value;

/*
 * Used to provide human-readable error responses
 */
@Value
@Builder
public class ErrorResponse {
    String message;
}
