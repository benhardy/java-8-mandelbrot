package fun;


public final class RenderParameters {
    public final Complex center;
    public final double width;
    public final int bailout;

    public RenderParameters(Complex center, double width, int bailout) {
        this.center = center;
        this.width = width;
        this.bailout = bailout;
    }

    public static RenderParameters INITIAL = new RenderParameters(Complex.ORIGIN, 4, 100);
}
