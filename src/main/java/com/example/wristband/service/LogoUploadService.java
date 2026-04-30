package com.example.wristband.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LogoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(LogoUploadService.class);

    private final ZebraPrinterClient zebraPrinterClient;
    private final Map<String, String> hashToImageName = new ConcurrentHashMap<>();

    public LogoUploadService(ZebraPrinterClient zebraPrinterClient) {
        this.zebraPrinterClient = zebraPrinterClient;
    }

    /**
     * Ensures the given base64 image is available on the printer as a GRF image stored on R: (RAM).
     * Returns the image name (without drive prefix or extension) to be used in ^XG.
     * The result is cached by image hash — identical images are uploaded only once per JVM lifetime.
     * Note: the printer's RAM drive is cleared on power cycle; call clearCache() after a printer restart.
     */
    public String ensureImageOnPrinter(String base64Image, String namePrefix) {
        if (base64Image == null || base64Image.isBlank()) {
            return null;
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String hash = sha256Hex(imageBytes);
        return hashToImageName.computeIfAbsent(hash, h -> uploadToPrinter(imageBytes, namePrefix, h));
    }

    /** Clears the upload cache. Call this after a printer power cycle so logos are re-uploaded. */
    public void clearCache() {
        hashToImageName.clear();
        logger.info("Logo upload cache cleared");
    }

    private String uploadToPrinter(byte[] imageBytes, String namePrefix, String hash) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                throw new IllegalArgumentException("Unsupported image format for logo");
            }
            BufferedImage mono = ZplGrfEncoder.toMonochrome(original);
            String imageName = (namePrefix + "_" + hash.substring(0, 8)).toUpperCase();
            String dgCommand = "^XA^DGR:" + imageName + ".GRF," + ZplGrfEncoder.toDgData(mono) + "^XZ";
            zebraPrinterClient.sendToPrinter(dgCommand);
            logger.info("Uploaded logo {} to printer with name {}", namePrefix, imageName);
            return imageName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read logo image", e);
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
