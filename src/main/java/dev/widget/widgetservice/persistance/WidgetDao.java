package dev.widget.widgetservice.persistance;

import java.util.List;

public interface WidgetDao<T> {
    /*
     * creates and persist entity using src as base
     */
    public T create(final T src);

    /*
     * loads entity by id
     */
    T get (final String id);

    /*
     * loads all entities, sorted in natural order using page size and offset
     */
    List<T> getAll(final int pageSize, final int offset);

    /*
     * updates entity
     */
    T update(final T entity);

    /*
     * deletes entity by id. Return true id entity existed.
     */
    boolean delete(final String id);
}
