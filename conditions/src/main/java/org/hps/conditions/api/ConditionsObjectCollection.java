package org.hps.conditions.api;

import java.util.Comparator;

public interface ConditionsObjectCollection<ObjectType extends ConditionsObject> extends Iterable<ObjectType>,
        DatabaseObject {

    boolean add(final ObjectType object) throws ConditionsObjectException;

    ObjectType get(final int index);

    int getCollectionId();

    void setCollectionId(int id);

    int size();

    void sort(final Comparator<ObjectType> comparator);

    ConditionsObjectCollection<ObjectType> sorted(final Comparator<ObjectType> comparator);

    boolean contains(Object object);

    void addAll(ConditionsObjectCollection<ObjectType> collection);
}
