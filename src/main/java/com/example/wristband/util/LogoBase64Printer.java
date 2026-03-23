package com.example.wristband.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class LogoBase64Printer {

    public static void main(String[] args) throws Exception {
        Path path = Path.of("src/main/resources/images/stup-logo.jpeg");
        byte[] bytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println(base64);
    }
}

