package com.example.wristband.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Pure image converter: takes a base64 PNG/JPEG and returns ZPL ^DG data
 * for use as a GRF image, without talking to any printer.
 */
@Component
public class Base64ToGrfConverter {

    /**
     * Converts a base64-encoded image to a ZPL ^DG command string.
     *
     * @param base64Image   Base64 string (PNG/JPEG, no data-URL prefix).
     * @param imageName     Name of the image (without drive prefix or extension), e.g. "STUPLOGO".
     * @return A full ZPL command: ^XA^DG{name}.GRF,totalBytes,bytesPerRow,height,{data}^XZ
     */
    public String toGrf(String base64Image, String imageName) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("base64Image must not be null or blank");
        }
        if (imageName == null || imageName.isBlank()) {
            throw new IllegalArgumentException("imageName must not be null or blank");
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image from base64", e);
        }
        if (original == null) {
            throw new IllegalArgumentException("Unsupported image format for base64 logo");
        }

        BufferedImage mono = toMonochrome(original);
        GrfData grfData = buildGfaFromImage(mono);

        String upperName = imageName.toUpperCase();
        return "^XA^DG" + upperName + ".GRF," +
                grfData.totalBytes + "," +
                grfData.bytesPerRow + "," +
                grfData.height + "," +
                grfData.hexData +
                "^XZ";
    }

    private BufferedImage toMonochrome(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage mono = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = mono.createGraphics();
        try {
            g2d.drawImage(src, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return mono;
    }

    private GrfData buildGfaFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int bytesPerRow = (width + 7) / 8;
        int totalBytes = bytesPerRow * height;

        StringBuilder data = new StringBuilder(totalBytes * 2);

        for (int y = 0; y < height; y++) {
            int bitIndex = 0;
            int currentByte = 0;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = (int) (0.3 * ((rgb >> 16) & 0xFF)
                        + 0.59 * ((rgb >> 8) & 0xFF)
                        + 0.11 * (rgb & 0xFF));
                boolean isBlack = gray < 128;

                currentByte <<= 1;
                if (isBlack) {
                    currentByte |= 1;
                }

                bitIndex++;
                if (bitIndex == 8) {
                    data.append(String.format("%02X", currentByte & 0xFF));
                    bitIndex = 0;
                    currentByte = 0;
                }
            }

            if (bitIndex > 0) {
                currentByte <<= (8 - bitIndex);
                data.append(String.format("%02X", currentByte & 0xFF));
            }
        }

        GrfData result = new GrfData();
        result.totalBytes = totalBytes;
        result.bytesPerRow = bytesPerRow;
        result.height = height;
        result.hexData = data.toString();
        return result;
    }

    private static class GrfData {
        int totalBytes;
        int bytesPerRow;
        int height;
        String hexData;
    }
}

