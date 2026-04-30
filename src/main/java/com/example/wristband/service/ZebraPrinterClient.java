package com.example.wristband.service;

import com.example.wristband.config.PrinterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class ZebraPrinterClient {

    private static final Logger logger = LoggerFactory.getLogger(ZebraPrinterClient.class);
    private static final String CLEAR_SEQUENCE = "^XA^JA^XZ\n";

    private final PrinterProperties props;

    public ZebraPrinterClient(PrinterProperties props) {
        this.props = props;
    }

    @Retryable(retryFor = PrinterUnavailableException.class, maxAttempts = 2,
               backoff = @Backoff(delay = 500))
    public void sendToPrinter(String zpl) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(props.getHost(), props.getPort()),
                    props.getConnectTimeoutMs());
            socket.setSoTimeout(props.getReadTimeoutMs());

            // Do NOT close the OutputStream directly — closing it also closes the socket.
            // Instead, flush and then half-close the write side via shutdownOutput().
            OutputStream os = socket.getOutputStream();
            os.write(CLEAR_SEQUENCE.getBytes(StandardCharsets.UTF_8));
            os.write(zpl.getBytes(StandardCharsets.UTF_8));
            os.flush();

            // Half-close the write side, then drain any bytes the printer sends back.
            // This prevents TCP RST packets from causing the printer to discard buffered data.
            socket.shutdownOutput();
            drainInput(socket);

            logger.info("Sent ZPL to printer at {}:{}", props.getHost(), props.getPort());
        } catch (IOException e) {
            logger.error("Failed to send ZPL to printer at {}:{} — {}",
                    props.getHost(), props.getPort(), e.getMessage(), e);
            throw new PrinterUnavailableException(props.getHost(), props.getPort(), e);
        }
    }

    private void drainInput(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[256];
            while (in.read(buf) != -1) { /* drain */ }
        } catch (IOException ignored) {
            // Printer may close without sending anything — that's fine
        }
    }
}
