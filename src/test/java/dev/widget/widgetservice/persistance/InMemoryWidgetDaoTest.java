package dev.widget.widgetservice.persistance;

import dev.widget.widgetservice.models.Widget;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryWidgetDaoTest {
    @Mock
    IdGenerator idGenerator;

    // used to spy on DAO's internal state
    ArrayList<Widget> widgets;
    Map<String, Widget> lookup;

    @Mock
    Clock clock;

    InMemoryWidgetDao sut;

    @BeforeEach
    void before() {
        widgets = new ArrayList<>();
        lookup = new HashMap<>();
        sut = new InMemoryWidgetDao(idGenerator, widgets, lookup, clock);
    }

    @Test
    void createCreatesWidget() {
        // Arrange
        String id = RandomStringUtils.randomAlphanumeric(10);
        when(idGenerator.generateId()).thenReturn(id);
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);

        Instant past = now.minusSeconds(100);
        String wrongId = id + "NO!";
        Widget src = new Widget(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt(),
                ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt(),
                ThreadLocalRandom.current().nextInt(),
                past, wrongId);

        // Act
        Widget result = sut.create(src);

        // Assert
        assertThat(result.getX(), is(src.getX()));
        assertThat(result.getY(), is(src.getY()));
        assertThat(result.getZ(), is(src.getZ()));
        assertThat(result.getWidth(), is(src.getWidth()));
        assertThat(result.getHeight(), is(src.getHeight()));
        assertThat(result.getLastModification(), is(now));
        assertThat(result.getId(), is(id));
        verify(idGenerator).generateId();
        verify(clock).instant();

        // sanity check
        assertThat(result.getLastModification(), not(past));
        assertThat(result.getId(), not(wrongId));

        // spy on internals
        assertThat(widgets, contains(result));
        assertThat(lookup, hasEntry(id, result));
    }

    @Test
    void createStoresWidgets() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();

        // Act
        IntStream.range(0, 100).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);

        // Assert
        // Spy on internals
        assertThat(widgets.size(), is(100));
        for (int i = 0; i < widgets.size(); ++i) {
            Widget w = widgets.get(i);
            assertThat(w.getZ(), is(i));
            assertThat(lookup, hasEntry(w.getId(), w));
        }
    }

    @Test
    void createSetsZtoZeroWhenNoWidgets() {
        // Arrange
        Widget src = new Widget();

        // Act
        Widget result = sut.create(src);

        // Assert
        assertThat(result.getZ(), is(0));
    }

    @Test
    void createMovesToForeground() {
        // Arrange
        IntStream.range(0, 100).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);
        Widget src = new Widget();

        // Act
        Widget result = sut.create(src);

        // Assert
        assertThat(result.getZ(), is(100));
    }

    @Test
    void createCreatesKeepZWhenSet() {
        // Arrange
        int z = ThreadLocalRandom.current().nextInt();
        Widget src = Widget.builder().z(z).build();

        // Act
        Widget result = sut.create(src);

        // Assert
        assertThat(result.getZ(), is(z));
    }

    @Test
    void createPushesExistingUp() {
        // Arrange
        IntStream.range(0, 100).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);
        Widget src = Widget.builder().z(50).build();

        // Act
        Widget result = sut.create(src);

        // Assert
        assertThat(result.getZ(), is(50));

        // Spy on internals
        assertThat(widgets.size(), is(101));
        for (int i = 0; i < widgets.size(); ++i) {
            assertThat(widgets.get(i).getZ(), is(i));
        }
    }

    @Test
    void getFindsWidgetById() {
        // Arrange
        IntStream.range(0, 100).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);
        Widget existing = sut.create(Widget.builder().z(50).build());

        // Act
        Widget result = sut.get(existing.getId());

        // Assert
        assertThat(result, sameInstance(existing));
    }

    @Test
    void getAllReturnPagedData() {
        // Arrange
        IntStream.range(0, 199).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);

        // Act
        List<List<Widget>> result = IntStream.range(0, 20)
                .mapToObj(i -> sut.getAll(10, i * 10))
                .collect(toUnmodifiableList());

        // Assert
        assertThat(result.size(), is(20));
        for (int i = 0; i < result.size(); ++i) {
            List<Widget> page = result.get(i);
            if (i != result.size() - 1) {
                assertThat(page.size(), is(10));
            } else {
                assertThat(page.size(), is(9));
            }
            for (int j = 0; j < page.size(); ++j) {
                assertThat(page.get(j).getZ(), is(10 * i + j));
            }
        }
    }

    @Test
    void updateReturnsNullIfNotFound() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();
        Widget existing = sut.create(new Widget());
        Widget notExisting = existing.toBuilder().id(existing.getId() + "NO!").build();

        // Act
        Widget result = sut.update(notExisting);

        // Assert
        assertThat(result, nullValue());
    }

    @Test
    void updateUpdatesData() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();
        Widget existing = sut.create(Widget.builder().x(1).y(2).z(3).width(4).build());
        Widget newExisting = Widget.builder().x(11).y(12).z(3).id(existing.getId()).build();
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);

        // Act
        Widget result = sut.update(newExisting);

        // Assert
        assertThat(result.getX(), is(newExisting.getX()));
        assertThat(result.getY(), is(newExisting.getY()));
        assertThat(result.getZ(), is(newExisting.getZ()));
        assertThat(result.getWidth(), is(existing.getWidth()));
        assertThat(result.getId(), is(newExisting.getId()));
        assertThat(result.getLastModification(), is(now));

        // Spy on internal
        assertThat(widgets.size(), is(1));
        assertThat(widgets.get(0), sameInstance(result));
        assertThat(lookup, hasEntry(result.getId(), result));
    }

    @Test
    void updatePushesOthersUp() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();
        IntStream.range(0, 100).mapToObj(z -> Widget.builder().z(z).build()).forEach(sut::create);
        Widget existing = widgets.get(50);
        Widget newExisting = Widget.builder().x(11).y(12).z(0).id(existing.getId()).build();
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);

        // Act
        Widget result = sut.update(newExisting);

        // Assert
        assertThat(result.getLastModification(), is(now));

        // Spy on internals
        assertThat(widgets.size(), is(100));
        assertThat(widgets.get(0).getLastModification(), is(now));
        assertThat(widgets.get(0).getId(), is(result.getId()));
        for (int i = 0; i < widgets.size(); ++i) {
            if (i < 50) {
                assertThat(widgets.get(i).getZ(), is(i));
            } else if (i > 50) {
                assertThat(widgets.get(i).getZ(), is(i + 1));
            }
        }
    }

    @Test
    void deleteDeletesWidget() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();
        Widget existing = sut.create(new Widget());
        // sanity check
        assertThat(widgets.size(), is(1));
        assertThat(lookup, hasEntry(existing.getId(), existing));

        // Act
        boolean result = sut.delete(existing.getId());

        // Assert
        assertThat(result, is(true));

        // Spy on internals
        assertThat(widgets.size(), is(0));
        assertThat(lookup.size(), is(0));
    }

    @Test
    void deleteReturnsFalseOnNotExitsing() {
        // Arrange
        when(idGenerator.generateId()).thenCallRealMethod();
        Widget existing = sut.create(new Widget());
        sut.delete(existing.getId());

        // Act
        boolean result = sut.delete(existing.getId());

        // Assert
        assertThat(result, is(false));

        // Spy on internals
        assertThat(widgets.size(), is(0));
        assertThat(lookup.size(), is(0));
    }
}