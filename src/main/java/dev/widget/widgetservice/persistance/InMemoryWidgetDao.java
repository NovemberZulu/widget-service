package dev.widget.widgetservice.persistance;

import dev.widget.widgetservice.models.Widget;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.*;

import static java.util.stream.Collectors.toUnmodifiableList;

/*
 * Simple storage using ArrayList as storage backend.
 * Widgets are stored ordered by Z, Map provides quick lookup by id functionality.
 * Avoids race conditions in the simplest way with synchronized methods.
 * Since call semantic requires mass changes in widgets as a result of a single call
 * (i.e. a new /modified widget push other widgets up),
 * it is not as bad as it sounds.
 * As long as both List and Map is in-memory storage this should be fast enough
 */
@Component
public class InMemoryWidgetDao implements WidgetDao<Widget> {
    private final IdGenerator idGenerator;
    private final ArrayList<Widget> widgets; // we want to make sure add(index, element) is supported
    private final Map<String, Widget> lookup;
    private final Clock clock;

    public InMemoryWidgetDao(final IdGenerator idGenerator, final ArrayList<Widget> widgets,
                             final Map<String, Widget> lookup, final Clock clock) {
        this.idGenerator = idGenerator;
        this.widgets = widgets;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Override
    public synchronized Widget create(final Widget src) {
        final Widget result = src.toBuilder().id(idGenerator.generateId()).build();
        if (result.getZ() == null) {
            result.setZ(foreground());
        }
        result.setLastModification(clock.instant());
        insert(result);
        lookup.put(result.getId(), result);
        return result;
    }

    @Override
    public synchronized Widget get(String id) {
        return lookup.get(id);
    }

    @Override
    public synchronized List<Widget> getAll(int pageSize, int offset) {
        return widgets.stream().skip(offset).limit(pageSize).collect(toUnmodifiableList());
    }

    @Override
    public synchronized Widget update(Widget entity) {
        final Widget target = lookup.get(entity.getId());
        if (target == null) {
            return null;
        }
        final Widget result = Widget.merge(target, entity);
        result.setLastModification(clock.instant());
        if (target.getZ().equals(result.getZ())) { // no changes in Z; just update data
            int i = searchByZ(result);
            widgets.set(i, result);
        } else { // Z changed
            delete(result.getId());
            insert(result);
        }
        lookup.put(result.getId(), result);
        return result;
    }

    @Override
    public synchronized boolean delete(String id) {
        final Widget widget = lookup.get(id);
        if (widget != null) {
            int i = searchByZ(widget); // Z is unique
            widgets.remove(i);
            lookup.remove(id);
            return true;
        } else {
            return false;
        }
    }

    /*
     * returns current foreground z-index
     */
    private int foreground() {
        return widgets.size() == 0 ? 0 : widgets.get(widgets.size() - 1).getZ() + 1;
    }

    /*
     * search for widget with the same Z as given
     */
    private int searchByZ(final Widget widget) {
        return Collections.binarySearch(widgets, widget, Comparator.comparingInt(Widget::getZ));
    }

    /*
     * inserts widget pushing other widgets up
     */
    private void insert(final Widget widget) {
        int i = searchByZ(widget);
        if (i >= 0) { //found
            widgets.add(i, widget);
            widgets.stream().skip(i + 1).forEach(w -> w.setZ(w.getZ() + 1)); // push up
        } else { //not found
            widgets.add(-i - 1, widget);
        }
    }
}
