package com.example.wristband.service;

import com.example.wristband.config.PrinterProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZebraPrinterClientTest {

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private ZebraPrinterClient client;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0); // OS-assigned port
        executor = Executors.newSingleThreadExecutor();

        PrinterProperties props = new PrinterProperties();
        props.setHost("localhost");
        props.setPort(serverSocket.getLocalPort());
        props.setConnectTimeoutMs(2000);
        props.setReadTimeoutMs(2000);
        client = new ZebraPrinterClient(props);
    }

    @AfterEach
    void tearDown() throws Exception {
        serverSocket.close();
        executor.shutdownNow();
    }

    @Test
    void happyPath_sendsClearSequenceThenZpl() throws Exception {
        StringBuilder received = new StringBuilder();

        executor.submit(() -> {
            try (Socket conn = serverSocket.accept();
                 OutputStream out = conn.getOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = conn.getInputStream().read(buf)) != -1) {
                    received.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        });

        client.sendToPrinter("^XA^XZ");

        Thread.sleep(200); // let the server thread finish reading
        assertThat(received.toString()).startsWith("^XA^JA^XZ\n");
        assertThat(received.toString()).endsWith("^XA^XZ");
    }

    @Test
    void connectionRefused_throwsPrinterUnavailableException() throws Exception {
        serverSocket.close(); // nothing listening

        PrinterProperties props = new PrinterProperties();
        props.setHost("localhost");
        props.setPort(serverSocket.getLocalPort());
        props.setConnectTimeoutMs(500);
        props.setReadTimeoutMs(500);
        ZebraPrinterClient failClient = new ZebraPrinterClient(props);

        assertThatThrownBy(() -> failClient.sendToPrinter("^XA^XZ"))
                .isInstanceOf(PrinterUnavailableException.class);
    }
}
