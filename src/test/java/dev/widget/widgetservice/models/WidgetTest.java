package dev.widget.widgetservice.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

class WidgetTest {

    private void assertValidSource(Integer x, Integer y, Integer z, Integer w, Integer h, String id, Instant modified,
                                   boolean result) {
        // Act
        Widget sut = new Widget(x, y, z, w, h, modified, id);

        // Assert
        assertThat(sut.isValidSource(), is(result));
    }

    @Test
    void isValidSource() { // not much more than truth table checks for these tests, really
        // Arrange
        Instant now = Instant.now();

        // happy paths
        assertValidSource(1, 2, 3, 4, 5, "id", now, true);
        assertValidSource(1, 2, 3, 4, 5, "id", null, true);
        assertValidSource(1, 2, 3, 4, 5, "", null, true);
        assertValidSource(1, 2, 3, 4, 5, null, null, true);
        assertValidSource(1, 2, null, 4, 5, null, null, true);

        // unhappy paths
        assertValidSource(null, 2, 3, 4, 5, "id", now, false);
        assertValidSource(1, null, 3, 4, 5, "id", now, false);
        assertValidSource(1, 2, 3, null, 5, "id", now, false);
        assertValidSource(1, 2, 3, 4, null, "id", now, false);
    }

    @Test
    void merge() { // just check the truth table
        // Arrange
        Instant now = Instant.now();
        Instant past = now.minusSeconds(1);

        // Overwrites X if not null
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getX(), is(6));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(null, 7, 8, 9, 10, now, "source")
        ).getX(), is(1));

        // Overwrites Y if not null
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getY(), is(7));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, null, 8, 9, 10, now, "source")
        ).getY(), is(2));

        // Overwrites Z if not null
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getZ(), is(8));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, null, 9, 10, now, "source")
        ).getZ(), is(3));

        // Overwrites weights if not null
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getWidth(), is(9));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, null, 10, now, "source")
        ).getWidth(), is(4));

        // Overwrites heights if not null
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getHeight(), is(10));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, null, now, "source")
        ).getHeight(), is(5));

        // Keep last modification date now matter what
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getLastModification(), sameInstance(past));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, null, "source")
        ).getLastModification(), sameInstance(past));

        // Keeps ID no matter what
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "source")
        ).getId(), is("target"));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, "")
        ).getId(), is("target"));
        assertThat(Widget.merge(
                new Widget(1, 2, 3, 4, 5, past, "target"),
                new Widget(6, 7, 8, 9, 10, now, null)
        ).getId(), is("target"));
    }
}