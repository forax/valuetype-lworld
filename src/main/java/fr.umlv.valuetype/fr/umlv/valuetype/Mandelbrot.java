package fr.umlv.valuetype;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Mandelbrot {
    public static void main(String[] args) throws IOException {
        var width = 1920;
        var height = 1080;
        var max = 1000;
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var black = 0;
        var colors = new int[max];
        for (var i = 0; i < max; i++) {
            colors[i] = Color.HSBtoRGB(i/256f, 1, i/(i+8f));
        }

        var start = System.nanoTime();
        for (var row = 0; row < height; row++) {
            for (var col = 0; col < width; col++) {
                var complex = new Complex((col - width/2)*4.0/width, (row - height/2)*4.0/width);
                var point = new Complex(0.0, 0.0);
                
                var iteration = 0;
                while (point.squareDistance() < 4 && iteration < max) {
                  point = point.square().add(complex);
                  iteration++;
                } 
                if (iteration < max) {
                  image.setRGB(col, row, colors[iteration]);
                } else {
                  image.setRGB(col, row, black);
                }
            }
        }
        var end = System.nanoTime();

        ImageIO.write(image, "png", new File("mandelbrot.png"));
        System.out.println("generated in " + (end - start)  / 1_000_000);
    }
}