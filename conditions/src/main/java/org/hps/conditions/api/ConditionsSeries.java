package org.hps.conditions.api;

import java.util.ArrayList;

/**
 * This class represents a series of collections containing <tt>ConditionsObjects</tt>. It is used to conditions
 * collections when there are multiple ones with the same key that are valid for the current run.
 *
 * @param <ObjectType> the type of the conditions object
 * @param <CollectionType> the type of the conditions collection
 */
@SuppressWarnings({"serial"})
// FIXME: This class should possibly be removed. It doesn't provide much functionality.
public class ConditionsSeries<ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>>
        extends ArrayList<ConditionsObjectCollection<ObjectType>> {
}
