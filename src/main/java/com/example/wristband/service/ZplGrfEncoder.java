package com.example.wristband.service;

import java.awt.image.BufferedImage;

/**
 * Shared utility for converting BufferedImages to ZPL GRF hex data.
 * Centralises the monochrome conversion and pixel encoding used by both
 * LogoUploadService (^DG printer uploads) and Base64ToGrfConverter (inline preview).
 */
public class ZplGrfEncoder {

    private ZplGrfEncoder() {}

    /**
     * Converts a source image to 1-bit monochrome using a single luminance-threshold pass.
     * Pixels with luminance < 128 become black, all others white.
     */
    public static BufferedImage toMonochrome(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage mono = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int luminance = ((rgb >> 16 & 0xFF) * 30 + (rgb >> 8 & 0xFF) * 59 + (rgb & 0xFF) * 11) / 100;
                mono.setRGB(x, y, luminance < 128 ? 0x000000 : 0xFFFFFF);
            }
        }
        return mono;
    }

    /**
     * Encodes a monochrome image as ZPL GRF data for a ^DG upload command.
     * Returns "totalBytes,bytesPerRow,hexData" — plug directly into:
     *   ^XA^DGR:NAME.GRF,{result}^XZ
     */
    public static String toDgData(BufferedImage mono) {
        int width = mono.getWidth();
        int height = mono.getHeight();
        int bytesPerRow = (width + 7) / 8;
        int totalBytes = bytesPerRow * height;
        return totalBytes + "," + bytesPerRow + "," + encodeHex(mono, width, height);
    }

    /**
     * Encodes a monochrome image as an inline ^GFA command string.
     * Returns "^GFA,totalBytes,totalBytes,bytesPerRow,hexData" for use inside a label, followed by ^FS.
     */
    public static String toGfaCommand(BufferedImage mono) {
        int width = mono.getWidth();
        int height = mono.getHeight();
        int bytesPerRow = (width + 7) / 8;
        int totalBytes = bytesPerRow * height;
        return "^GFA," + totalBytes + "," + totalBytes + "," + bytesPerRow + "," + encodeHex(mono, width, height);
    }

    private static String encodeHex(BufferedImage mono, int width, int height) {
        int bytesPerRow = (width + 7) / 8;
        StringBuilder data = new StringBuilder(bytesPerRow * height * 2);
        for (int y = 0; y < height; y++) {
            int bitIndex = 0;
            int currentByte = 0;
            for (int x = 0; x < width; x++) {
                int rgb = mono.getRGB(x, y);
                int luminance = ((rgb >> 16 & 0xFF) * 30 + (rgb >> 8 & 0xFF) * 59 + (rgb & 0xFF) * 11) / 100;
                currentByte <<= 1;
                if (luminance < 128) currentByte |= 1;
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
        return data.toString();
    }
}
