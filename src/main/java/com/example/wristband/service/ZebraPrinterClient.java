package com.example.wristband.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class ZebraPrinterClient {

    private static final Logger logger = LoggerFactory.getLogger(ZebraPrinterClient.class);

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public ZebraPrinterClient(
            @Value("${printer.host}") String host,
            @Value("${printer.port}") int port,
            @Value("${printer.connect-timeout-ms:3000}") int connectTimeoutMillis,
            @Value("${printer.read-timeout-ms:3000}") int readTimeoutMillis) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public void sendToPrinter(String zpl) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            try (OutputStream os = socket.getOutputStream()) {
                // Stuur eerst een korte "clear/cancel" naar de printer zodat
                // eventuele resterende data in de buffer niet interfereert met dit label.
                // Dit voorkomt het .NET-probleem waarbij de buffer na een aantal bandjes volloopt.
                String clearSequence = "^XA^JA^XZ\n";
                os.write(clearSequence.getBytes(StandardCharsets.UTF_8));

                // Daarna het effectieve ZPL-label versturen
                os.write(zpl.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            logger.info("Sent ZPL to printer at {}:{}", host, port);
        } catch (IOException e) {
            logger.error("Failed to send ZPL to printer at {}:{} - {}", host, port, e.getMessage(), e);
            throw new RuntimeException("Failed to send to printer", e);
        }
    }
}

