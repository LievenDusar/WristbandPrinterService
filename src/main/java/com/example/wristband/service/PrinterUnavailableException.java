package com.example.wristband.service;

public class PrinterUnavailableException extends RuntimeException {

    public PrinterUnavailableException(String host, int port, Throwable cause) {
        super("Printer at " + host + ":" + port + " is unavailable", cause);
    }
}
