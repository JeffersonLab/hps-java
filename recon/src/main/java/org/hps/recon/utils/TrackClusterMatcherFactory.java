package org.hps.recon.utils;

/**
 * This is a convenience class for creating specific Track-to-Cluster matching
 * algorithms via their name in the package org.hps.recon.utils. They must
 * implement the {@link TrackClusterMatcher} interface.
 *
 * @see TrackClusterMatcher
 * @see AbstractTrackClusterMatcher
 * @see TrackClusterMatcherNSigma
 * @see TrackClusterMatcherMinDistance
 */
public final class TrackClusterMatcherFactory {

    /**
     * Dont instantiate this class.
     */
    private TrackClusterMatcherFactory() {

    }

    /**
     * Create a Track to Cluster matching algorithm
     * @param name The name of the matching algorithm
     * @return The matching algorithm
     * @throws IllegalArgumentException if there is no matcher found with name.
     */
    public static TrackClusterMatcher create(String name) {

        TrackClusterMatcher matcher = null;
        try {
            if (TrackClusterMatcherNSigma.class.getSimpleName().equals(name))
                matcher = new TrackClusterMatcherNSigma();
            else if (TrackClusterMatcherMinDistance.class.getSimpleName().equals(name))
                matcher = new TrackClusterMatcherMinDistance();
        }
        catch (Exception e){
            //No matcher by the given name has been found :(
            throw new IllegalArgumentException("Unknown Mactcher algorithm " + name + " cannot be instantiated.", e);
        }
        return matcher;
    }
}
