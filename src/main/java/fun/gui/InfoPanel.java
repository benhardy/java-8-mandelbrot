package fun.gui;

import fun.Complex;
import fun.Histogram;
import fun.RenderParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.log;

public class InfoPanel extends JPanel {
    private final JLabel xLabel = new JLabel("Real:");
    private final JTextField xField = new JTextField("0");
    private final JLabel yLabel = new JLabel("Imaginary:");
    private final JTextField yField = new JTextField("0");
    private final JLabel scaleLabel = new JLabel("Scale:");
    private final JTextField scaleField = new JTextField("4");
    private final JLabel bailoutLabel = new JLabel("Bailout:");
    private final JTextField bailoutField = new JTextField("100");
    private final JButton goButton = new JButton("Go!");
    private final BufferedImage histogramImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
    private final JPanel histogramPanel = MandelbrotWindow.createDrawPanel(
            histogramImage.getWidth(), histogramImage.getHeight(), histogramImage);

    private static GridBagConstraints labelConstraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 0;
        return constraints;
    }

    private static GridBagConstraints textConstraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;
        return constraints;
    }

    private static GridBagConstraints histogramConstraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;
        constraints.gridheight = 3;
        return constraints;
    }

    public static class ConstraintsBuilder {
        private final GridBagConstraints constraints = new GridBagConstraints();
        public static ConstraintsBuilder at(int x, int y) {
            ConstraintsBuilder cb = new ConstraintsBuilder();
            cb.constraints.gridx = x;
            cb.constraints.gridy = y;
            return cb;
        }
        public ConstraintsBuilder gridSize(int width, int height) {
            constraints.gridwidth = width;
            constraints.gridheight = height;
            return this;
        }
        public ConstraintsBuilder weight(double wx, double wy) {
            constraints.weightx = wx;
            constraints.weighty = wy;
            return this;
        }
        public GridBagConstraints build() {
            return constraints;
        }
        public ConstraintsBuilder leftAlign() {
            constraints.anchor = GridBagConstraints.WEST;
            return this;
        }
        public ConstraintsBuilder rightAlign() {
            constraints.anchor = GridBagConstraints.EAST;
            return this;
        }
        public ConstraintsBuilder fillNone() {
            return fill(GridBagConstraints.NONE);
        }
        public ConstraintsBuilder fillHorizontal() {
            return fill(GridBagConstraints.HORIZONTAL);
        }
        public ConstraintsBuilder fillVertical() {
            return fill(GridBagConstraints.VERTICAL);
        }
        public ConstraintsBuilder fillBoth() {
            return fill(GridBagConstraints.BOTH);
        }

        private ConstraintsBuilder fill(int filling) {
            constraints.fill = filling;
            return this;
        }
    }

    public InfoPanel(MandelbrotWindow window) {
        final GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        this.add(xLabel, labelConstraints(0, 0));
        this.add(xField, textConstraints(1, 0));
        this.add(scaleLabel, labelConstraints(2, 0));
        this.add(scaleField, textConstraints(3, 0));
        this.add(goButton, textConstraints(0, 2));
        this.add(yLabel, labelConstraints(0, 1));
        this.add(yField, textConstraints(1, 1));
        this.add(bailoutLabel, labelConstraints(2, 1));
        this.add(bailoutField, textConstraints(3, 1));
        this.add(histogramPanel, histogramConstraints(4, 0));
        goButton.addActionListener((event) -> window.startNewRendering(getRenderParameters()));
    }

    public RenderParameters getRenderParameters() {
        double x = Double.parseDouble(xField.getText());
        double y = Double.parseDouble(yField.getText());
        double scale = Double.parseDouble(scaleField.getText());
        int bailout = Integer.parseInt(bailoutField.getText());
        return new RenderParameters(new Complex(x, y), scale, bailout);
    }

    public void setFromRenderParameters(RenderParameters parameters) {
        xField.setText(Double.toString(parameters.center.real));
        yField.setText(Double.toString(parameters.center.imaginary));
        scaleField.setText(Double.toString(parameters.width));
        bailoutField.setText(Integer.toString(parameters.bailout));
    }

    public int getBailout() {
        return Integer.valueOf(bailoutField.getText());
    }

    public void updateHistogram(Histogram h) {
        int max = h.max();
        //Histogram bucketed = h.bucketed(v -> v * histogramImage.getWidth() / (max + 1));
        double logMax = log(max);
        Histogram bucketedLog = h.bucketed(v -> (int)(log(v+1) *histogramImage.getWidth() / logMax));

        double logMaxHeight = log(bucketedLog.height() + 1);
        for (int x = 0; x < histogramImage.getWidth(); x++) {
            double logHeight = (int)(bucketedLog.valueAt(x) + 1);
           int height = bucketedLog.valueAt(x) * histogramImage.getHeight() / bucketedLog.height();
//           int height = (int) (logHeight * histogramImage.getHeight() / logMaxHeight);
            for (int y = 0; y < histogramImage.getHeight(); y++) {
                final int color = (y <= height) ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                histogramImage.setRGB(x, histogramImage.getHeight() - 1 - y, color);
            }
        }
        histogramPanel.repaint();
    }
}

