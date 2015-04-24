package org.hps.conditions.api;

import java.util.Comparator;

public interface ConditionsObjectCollection<ObjectType extends ConditionsObject> extends Iterable<ObjectType>,
        DatabaseObject {

    boolean add(final ObjectType object) throws ConditionsObjectException;

    ObjectType get(final int index);

    int getCollectionId();

    // FIXME: Perhaps this should not be in the interface.
    void setCollectionId(int id);

    int size();

    // FIXME: It might be better if this was external to this interface.
    void sort(final Comparator<ObjectType> comparator);

    // FIXME: It might be better if this was external to this interface.
    ConditionsObjectCollection<ObjectType> sorted(final Comparator<ObjectType> comparator);

    boolean contains(Object object);

    void addAll(ConditionsObjectCollection<ObjectType> collection);
}
