package dev.widget.widgetservice.persistance;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IdGeneratorTest {
    public static final int GENERATE_COUNT = 1_000_000;

    @Test
    void generateId() {
        // Try to verify IDs are all unique

        // Arrange
        IdGenerator sut = new IdGenerator();

        // Act
        Set<String> ids = IntStream.range(0, GENERATE_COUNT)
                .mapToObj(i -> sut.generateId())
                .collect(toUnmodifiableSet());

        // Assert
        assertThat(ids.size(), is(GENERATE_COUNT));
    }
}