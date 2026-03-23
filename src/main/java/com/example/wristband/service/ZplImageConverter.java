package com.example.wristband.service;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ZplImageConverter {

    public static String toZplGFA(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int bytesPerRow = (width + 7) / 8;
        int totalBytes = bytesPerRow * height;

        StringBuilder hex = new StringBuilder(totalBytes * 2);

        for (int y = 0; y < height; y++) {
            for (int byteIndex = 0; byteIndex < bytesPerRow; byteIndex++) {
                int value = 0;

                for (int bit = 0; bit < 8; bit++) {
                    int x = byteIndex * 8 + bit;
                    value <<= 1;

                    if (x < width) {
                        int rgb = image.getRGB(x, y) & 0xFFFFFF;
                        boolean isBlack = rgb == 0x000000;
                        if (isBlack) {
                            value |= 1;
                        }
                    }
                }

                hex.append(String.format("%02X", value));
            }
        }

        return "^GFA," + totalBytes + "," + totalBytes + "," + bytesPerRow + "," + hex;
    }

    public static BufferedImage toMonochrome(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage mono = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int gray = (r * 30 + g * 59 + b * 11) / 100;
                int bw = gray < 160 ? 0x000000 : 0xFFFFFF;

                mono.setRGB(x, y, bw);
            }
        }

        return mono;
    }

    public static BufferedImage resize(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        return resized;
    }

    public static BufferedImage rotate90CounterClockwise(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        BufferedImage rotated = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = rotated.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, height, width);

            // rotate CCW
            g.translate(0, width);
            g.rotate(Math.toRadians(-90));

            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }

        return rotated;
    }
}