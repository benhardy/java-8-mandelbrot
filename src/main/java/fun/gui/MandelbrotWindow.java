package fun.gui;


import fun.Complex;
import fun.RenderParameters;
import fun.Rendering;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MandelbrotWindow extends JFrame {
    private final BufferedImage image;
    private final JPanel drawPanel;
    private final AtomicReference<Rendering> renderingRef;
    private final InfoPanel infoPanel = new InfoPanel(this);
    private final ControlPanel controlPanel;

    public MandelbrotWindow(int imageWidth, int imageHeight) throws IOException, InterruptedException {
        image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        setPreferredSize(new Dimension(600, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        drawPanel = createDrawPanel(imageWidth, imageHeight, image);
        controlPanel = new ControlPanel(image);
        JScrollPane scroller = new JScrollPane(drawPanel);
        scroller.setMinimumSize(new Dimension(400, 400));
        scroller.setPreferredSize(new Dimension(400, 400));

        this.renderingRef = new AtomicReference<>(
                new Rendering(RenderParameters.INITIAL, image).startRendering()
        );

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(scroller, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        getContentPane().setPreferredSize(new Dimension(imageWidth, imageHeight));
        getContentPane().setMinimumSize(new Dimension(400, 400));
        this.pack();

        drawPanel.addMouseListener(clickListener());
        Runnable task = () -> {
            drawPanel.repaint();
            controlPanel.setProgress(renderingRef.get().getProgress());
            controlPanel.setQueueLength(renderingRef.get().getQueueLength());
            infoPanel.updateHistogram(renderingRef.get().getHistogram());
        };
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(task, 1L, 1L, TimeUnit.SECONDS);
    }

    private MouseListener clickListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent event) {
                boolean zoomOut = event.isMetaDown();
                Rendering oldRender = renderingRef.get();
                Complex center = oldRender.whereWasClick(event);
                double newWidth = oldRender.getWidth() * (zoomOut ? 3.0 : 0.333);
                int newBail = infoPanel.getBailout();
                RenderParameters parameters = new RenderParameters(center, newWidth, newBail);
                startNewRendering(parameters);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };
    }

    public static JPanel createDrawPanel(int imageWidth, int imageHeight, final BufferedImage image) {
        final Dimension prefSize = new Dimension(imageWidth, imageHeight);
        return new JPanel() {
            public void paintComponent() {
                paint(getGraphics());
            }

            public void paint(Graphics g) {
                g.drawImage(image, 0, 0, null);
            }

            boolean isOpague() {
                return true;
            }

            @Override
            public Dimension getPreferredSize() {
                return prefSize;
            }
        };
    }

    public void startNewRendering(RenderParameters renderParameters) {
        Rendering oldRender = renderingRef.get();
        oldRender.stop();
        controlPanel.setProgress(0);
        controlPanel.setQueueLength(0);
        renderingRef.set(new Rendering(renderParameters, image).startRendering());
        infoPanel.setFromRenderParameters(renderParameters);
    }
}
