package fun;

/**
 * The core of the Mandelbrot algorithm.
 *
 * Find the number of iterations taken for a point Z when squared and added to C to exceed length 2 from the origin.
 */
public final class IterationResult {
    public final int iterations;
    public final double xSquared;
    public final double ySquared;

    public IterationResult(final int iterations, final double xSquared, final double ySquared) {
        this.iterations = iterations;
        this.xSquared = xSquared;
        this.ySquared = ySquared;
    }

    public static IterationResult of(final double cx, final double cy, final int bailout) {
        int iterations = 0;
        double zx = cx;
        double zy = cy;
        double zxSquared = zx * zx;
        double zySquared = zy * zy;
        while (iterations < bailout && (zxSquared + zySquared) < 4.0) {
            zy = 2 * zx * zy + cy;
            zx = zxSquared - zySquared + cx;
            iterations++;
            zxSquared = zx * zx;
            zySquared = zy * zy;
        }
        return new IterationResult(iterations, zxSquared, zySquared);
    }
}