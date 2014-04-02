package fun;

import fun.gui.MandelbrotWindow;


public class Mandelbrot {

    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        MandelbrotWindow window = new MandelbrotWindow(width, height);
        window.setVisible(true);
    }



}
