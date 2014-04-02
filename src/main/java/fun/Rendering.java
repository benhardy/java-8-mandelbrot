package fun;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.*;
import static java.lang.Math.abs;
import static java.lang.Math.log;

public class Rendering {
    private final RenderParameters renderParameters;
    private final Dimension imageSize;
    private final double delta;
    private final Complex topLeft;
    private final Graphics graphics;
    private final AtomicInteger progress = new AtomicInteger(0);

    private final ExecutorService boss = Executors.newSingleThreadExecutor(),
            calculators = Executors.newWorkStealingPool(),
            painter = Executors.newSingleThreadExecutor();

    private final PriorityBlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<>(1000);
    private final Histogram iterationHistogram = new Histogram();

    public Rendering(RenderParameters renderParameters, BufferedImage image) {
        this.renderParameters = renderParameters;
        this.imageSize = new Dimension(image.getWidth(), image.getHeight());
        double height = imageSize.height * renderParameters.width / imageSize.width;
        this.delta = renderParameters.width / imageSize.width;
        this.topLeft = new Complex(
                renderParameters.center.real - renderParameters.width / 2,
                renderParameters.center.imaginary - height / 2
        );
        this.graphics = image.getGraphics();

    }

    public void stop() {
        calculators.shutdownNow();
        painter.shutdownNow();
        boss.shutdownNow();
        workQueue.clear();
    }

    public Rendering startRendering() {
        int initialScaleLog2 = (int) ceil((log(max(imageSize.width, imageSize.height)) / log(2)));
        workQueue.add(taskForPosition(0, 0, 1 << initialScaleLog2, false, 0));
        boss.submit((Runnable) () -> {
            while (true) {
                try {
                    calculators.submit(workQueue.take());
                } catch (InterruptedException e) {
                }
            }
        });
        return this;
    }

    public Complex whereWasClick(MouseEvent event) {
        double newCenterX = topLeft.real + delta * event.getX();
        double newCenterY = topLeft.imaginary + delta * event.getY();
        return new Complex(newCenterX, newCenterY);
    }

    public int getQueueLength() {
        return workQueue.size();
    }

    public Histogram getHistogram() {
        return iterationHistogram;
    }

    static abstract class PrioritizedRunnable implements Runnable, Comparable<PrioritizedRunnable> {
        private final int priority;

        public PrioritizedRunnable(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(PrioritizedRunnable o) {
            return this.priority - o.priority;
        }
    }

    private Runnable taskForPosition(
            int xPixel, int yPixel, int scale, final boolean doneTopLeft, int suggestedPriority) {
        return new PrioritizedRunnable(suggestedPriority) {
            @Override
            public void run() {
                int priority = suggestedPriority;
                if (!doneTopLeft) {
                    priority = calculatePoint(xPixel, yPixel, scale);
                }
                if (scale <= 1) {
                    return;
                }
                int newScale = scale / 2;

                workQueue.add(taskForPosition(xPixel, yPixel, newScale, true, priority));
                int newX = xPixel + newScale;
                int newY = yPixel + newScale;
                if (newX < imageSize.width) {
                    workQueue.add(taskForPosition(newX, yPixel, newScale, false, priority));
                    if (newY < imageSize.height) {
                        workQueue.add(taskForPosition(newX, newY, newScale, false, priority));
                    }
                }
                if (newY < imageSize.height) {
                    workQueue.add(taskForPosition(xPixel, newY, newScale, false, priority));
                }
            }
        };
    }

    private int calculatePoint(int xPixel, int yPixel, int scale) {
        final double cx = topLeft.real + delta * xPixel;
        final double cy = topLeft.imaginary + delta * yPixel;

        final IterationResult iterationResult = IterationResult.of(cx, cy, renderParameters.bailout);
        final Color color = colorForResult(iterationResult);
        painter.submit(pointColoringTask(xPixel, yPixel, color, scale));
        iterationHistogram.increment(iterationResult.iterations);
        return iterationResult.iterations;
    }

    private Runnable pointColoringTask(int xPixel, int yPixel, Color color, int scale) {
        return () -> {
            final int width = max(0, min(scale, imageSize.width - xPixel));
            final int height = max(0, min(scale, imageSize.height - yPixel));
            graphics.setColor(color);
            graphics.fillRect(xPixel, yPixel, width, height);
            progress.incrementAndGet();
        };
    }

    private Color colorForResult(final IterationResult result) {
        if (result.iterations < renderParameters.bailout) {
            final double zn = sqrt(result.xSquared + result.ySquared);
            final double nu = log(log(zn) / log(2)) / log(2);
            final double fIteration = result.iterations + 1 - nu;

            final int blue = sawtooth(fIteration / 3);
            final int red = sawtooth(fIteration);
            final int green = sawtooth(fIteration / 31);
            return new Color(red, green, blue);
        } else {
            return Color.BLACK;
        }

    }

    private static int sawtooth(double v) {
        return abs(((int) v & 0x1FE) - 0xFF);
    }

    public double getWidth() {
        return renderParameters.width;
    }

    public int getProgress() {
        return progress.get();
    }
}
