package kalman;

// Tracking pattern recognition for the HPS experiment based on an extended Kalman filter
// Algorithm:
//    1. Loop over starting strategies in the downstream layers, each involving at least 3 stereo planes and 2 non-stereo planes
//    2. Fit the starting planes to a line and parabola (or circle?) assuming a uniform field (average field in the starting region)
//    3. Run a Kalman filter back to the first plane
//    4. Smooth back to the starting point; abandon really bad fits
//    5. Restart and repeat (iterate) using the smoothed helix as a start
//    6. Filter to the other end
//    7. Smooth back to the starting layer
//    8. One by one remove each layer from the fit and test the residual. Replace the assigned hit with another if it is better
//    9. Run a Kalman fit and smoothing over all assigned hits starting the filter from the first layer and working downstream 
//   10. Extrapolate the smoothed helix to the vertex and to the Ecal
//   11. Remove hits used in successful fits before going on to the next starting strategy

public class KalmanPatRecHPS {

    public KalmanPatRecHPS() {

    }

}
