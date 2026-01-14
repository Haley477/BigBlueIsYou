package Particle;

import org.joml.Vector2f;

import java.util.Random;

/// <summary>
/// Expands upon some of the features the Random class does:
///
/// * nextRange : Generate a random number within some range
/// * nextGaussian : Generate a normally distributed random number
///
/// </summary>
public class MyRandom extends Random {

    /// <summary>
    /// Generates a random number in the range or [Min,Max]
    /// </summary>
    public float nextRange(float min, float max) {
        return org.joml.Math.lerp(min, max, (float) this.nextDouble());
    }

    /// <summary>
    /// Generate a random vector about a unit circle
    /// </summary>
    public Vector2f nextCircleVector() {
        float angle = (float) (this.nextDouble() * 2.0 * Math.PI);
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);

        return new Vector2f(x, y);
    }

    /// <summary>
    /// Generate a normally distributed random number.  Derived from a Wiki reference on
    /// how to do this.
    /// </summary>
    public double nextGaussian(double mean, double stdDev) {
        if (this.usePrevious) {
            this.usePrevious = false;
            return mean + y2 * stdDev;
        }
        this.usePrevious = true;

        double x1 = 0.0;
        double x2 = 0.0;
        double y1 = 0.0;
        double z = 0.0;

        do {
            x1 = 2.0 * this.nextDouble() - 1.0;
            x2 = 2.0 * this.nextDouble() - 1.0;
            z = (x1 * x1) + (x2 * x2);
        }
        while (z >= 1.0);

        z = Math.sqrt((-2.0 * Math.log(z)) / z);
        y1 = x1 * z;
        y2 = x2 * z;

        return mean + y1 * stdDev;
    }

    /// <summary>
    /// Keep this around to optimize gaussian calculation performance.
    /// </summary>
    private double y2;
    private boolean usePrevious = false;
}
