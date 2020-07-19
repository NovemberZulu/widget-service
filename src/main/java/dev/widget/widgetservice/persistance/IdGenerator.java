package dev.widget.widgetservice.persistance;

import org.springframework.stereotype.Component;

import java.util.UUID;

/*
 * generates unique ids based on GUIDs
 */
@Component
public class IdGenerator {
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
