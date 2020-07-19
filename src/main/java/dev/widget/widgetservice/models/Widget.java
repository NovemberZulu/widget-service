package dev.widget.widgetservice.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Widget {
    private Integer x;
    private Integer y;
    private Integer z;
    private Integer width;
    private Integer height;
    private Instant lastModification;
    private String id;

    /*
     * true if this object can be used as a valid source, i.e. has x, y, weight, height
     */
    @JsonIgnore
    public boolean isValidSource() {
        return (getX() != null) && (getY() != null) && (getWidth() != null) && (getHeight() != null);
    }

    /*
     * merge source on top of target, skipping null fields, ID, and last modification
     */
    public static Widget merge(final Widget target, final Widget source) {
        return Widget.builder()
                .x(merge(target.getX(), source.getX()))
                .y(merge(target.getY(), source.getY()))
                .z(merge(target.getZ(), source.getZ()))
                .width(merge(target.getWidth(), source.getWidth()))
                .height(merge(target.getHeight(), source.getHeight()))
                .lastModification(target.getLastModification())
                .id(target.getId())
                .build();
    }

    /*
     * merge source on top of target, if source is not null
     */
    private static Integer merge(final Integer target, final Integer source) {
        return source != null ? source : target;
    }
}
