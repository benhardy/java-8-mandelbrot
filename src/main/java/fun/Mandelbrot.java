package fun;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.*;

public class Mandelbrot {
    private static final int EXECUTORS = 4;

    public static class Window extends JFrame {
        private final BufferedImage image;
        private final JPanel drawPanel;
        private final AtomicReference<Rendering> renderingRef;

        public Window(int imageWidth, int imageHeight) throws IOException, InterruptedException {
            image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            setPreferredSize(new Dimension(600, 600));
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            drawPanel = createDrawPanel(imageWidth, imageHeight, image);
            JScrollPane scroller = new JScrollPane(drawPanel);
            scroller.setMinimumSize(new Dimension(400, 400));
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(scroller, BorderLayout.CENTER);
            renderingRef = new AtomicReference<>(
                    new Rendering(0, 0, 4, imageWidth, imageHeight, 50000, image).startRendering()
            );
            drawPanel.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    System.out.println("event: "+event.getX()+","+event.getY());
                    boolean zoomOut = event.isMetaDown();
                    Rendering oldRender = renderingRef.get();
                    oldRender.stop();
                    double newCenterX = oldRender.getXLeft() + oldRender.getDelta() * event.getX();
                    double newCenterY = oldRender.getYTop() + oldRender.getDelta() * event.getY();
                    double newWidth= oldRender.width * (zoomOut ? 3.0 : 0.333);
                    renderingRef.set(
                            new Rendering(newCenterX, newCenterY, newWidth, imageHeight, imageWidth, 50000, image)
                                    .startRendering());

                }

                @Override
                public void mousePressed(MouseEvent e) {  }

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {   }

                @Override
                public void mouseExited(MouseEvent e) {}
            });
            Runnable task = () -> drawPanel.repaint();
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(task, 1L, 1L, TimeUnit.SECONDS);
        }

        public BufferedImage getImage() {
            return image;
        }

        private static JPanel createDrawPanel(int imageWidth, int imageHeight, final BufferedImage image) {
            final Dimension prefSize = new Dimension(imageHeight, imageWidth);
            return new JPanel() {
                public void paintComponent() {
                    paint(getGraphics());
                }
                public void paint(Graphics g) {
                    g.drawImage(image, 0, 0, null);
                }
                boolean isOpague() { return true; }
                @Override
                public Dimension getPreferredSize() { return prefSize; }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[3]);
        int height = Integer.parseInt(args[4]);
        Window window = new Window(width, height);
        window.show();
    }

    public static class Rendering {
        private final int xPixels, yPixels;
        private final int bailout;
        private final double delta;
        private final double xLeft;
        private final double yTop;
        private final BufferedImage image;
        private final int totalPixels;
        private final AtomicInteger processed = new AtomicInteger(0);
        private final double width;
        private final ExecutorService runner = Executors.newFixedThreadPool(EXECUTORS);
        private final ExecutorService collector = Executors.newSingleThreadExecutor();

        public Rendering(final double xCenter, final double yCenter, final double width,
                         final int xPixels, final int yPixels, int bailout,
                         BufferedImage image) {
            this.xPixels = xPixels;
            this.yPixels = yPixels;
            this.totalPixels = xPixels * yPixels;
            this.bailout = bailout;
            double height = yPixels * width / xPixels;
            this.delta = width / xPixels;
            this.xLeft = xCenter - width / 2;
            this.yTop = yCenter - height / 2;
            this.image = image;
            this.width = width;
        }

        public void stop() {
            runner.shutdownNow();
            collector.shutdownNow();
        }

        public Rendering startRendering() {

            for (int offset = 0; offset < EXECUTORS; offset++) {
                System.out.println("setting up executor " + offset);
                runner.submit(taskForOffset(offset));
            }
            /*
            runner.shutdown();
            runner.awaitTermination(1, TimeUnit.HOURS);
            collector.shutdown();
            collector.awaitTermination(1, TimeUnit.HOURS);
//            ImageIO.write(image, "png", new File("mandelbrot.png"));
            System.out.println("processed " + processed.get() + " pixels");
            */
            return this;
        }

        private Runnable taskForOffset(int offset) {
            return () -> {
                for (int pixelNumber = offset; pixelNumber < totalPixels; pixelNumber += EXECUTORS) {
                    final int xPixel = pixelNumber % xPixels;
                    final int yPixel = pixelNumber / xPixels;
                    final double cx = xLeft + delta * xPixel;
                    final double cy = yTop + delta * yPixel;

                    int iterations = 0;
                    double x = cx;
                    double y = cy;
                    double xSquared = x * x;
                    double ySquared = y * y;
                    while (iterations < bailout && (xSquared + ySquared) < 4.0) {
                        double xNew = xSquared - ySquared + cx;
                        double yNew = 2 * x * y + cy;
                        x = xNew;
                        y = yNew;
                        iterations++;
                        xSquared = x * x;
                        ySquared = y * y;
                    }
                    final int rgb = colorForResult(iterations, xSquared, ySquared);
                    collector.submit(pointColoringTask(xPixel, yPixel, rgb));
                }
            };
        }

        private Runnable pointColoringTask(int xPixel, int yPixel, int rgb) {
            return () -> {
                image.setRGB(xPixel, yPixel, rgb);
                int sofar = processed.incrementAndGet();
                if (sofar % 1000 == 0) {
                    System.out.println("Done " + sofar);
                }
            };
        }

        private int colorForResult(int iterations, double x, double y) {
            final int rgb;
            if (iterations < bailout) {
                final double zn = sqrt(x + y);
                final double nu = log(log(zn) / log(2)) / log(2);
                final double fIteration = iterations + 1 - nu;
//                final int red = 0; //(int) (abs(atan2(y, x) * 255 / PI));

                int blue = sawtooth(fIteration / 3);
                int red = sawtooth(fIteration);
                int green = sawtooth(fIteration / 31);
                rgb = (red << 8 | green) << 8 | blue;
            } else {
                rgb = 0;
            }
            return rgb;
        }

        private static int sawtooth(double v) {
            return abs(((int) v & 0x1FE) -0xFF);
        }

        public double getDelta() {
            return delta;
        }

        public double getXLeft() {
            return xLeft;
        }

        public double getYTop() {
            return yTop;
        }
        public double getWidth() {
            return width;
        }
    }
}
