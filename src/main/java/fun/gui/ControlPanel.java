package fun.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ControlPanel extends JPanel {
    private final JProgressBar progressBar;
    private final BufferedImage image;
    private final  JLabel queueLengthLabel = new JLabel("");

    public ControlPanel(final BufferedImage image) {
        this.image = image;
        progressBar = new JProgressBar(0, image.getWidth() * image.getHeight());
        setLayout(new FlowLayout());
        final Button saveButton = new Button("Save");
        saveButton.addActionListener(this::saveImage);
        add(saveButton);
        add(progressBar);
        add(queueLengthLabel);
    }

    private void saveImage(ActionEvent event) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String stamp = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss").format(now);
            ImageIO.write(image, "png", new File("mandelbrot-" + stamp + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProgress(int progress) {
        this.progressBar.setValue(progress);
    }

    public void setQueueLength(int queueLength) {
        this.queueLengthLabel.setText(Integer.toString(queueLength));
    }
}
