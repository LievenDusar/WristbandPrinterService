package com.example.wristband.api;

import com.example.wristband.service.PrintService;
import com.example.wristband.service.PrinterUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrintController.class)
class PrintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrintService printService;

    @Test
    void printWristband_validRequest_returns202() throws Exception {
        doNothing().when(printService).printWristband(any());

        mockMvc.perform(post("/api/print/wristband")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void printWristband_missingBarcode_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(PrintRequest.builder()
                .firstName("Jan").lastName("Peeters")
                .organisationName("Org").projectName("Event")
                .build());

        mockMvc.perform(post("/api/print/wristband")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void printWristband_barcodeTooLong_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(PrintRequest.builder()
                .barcode("A".repeat(49))
                .firstName("Jan").lastName("Peeters")
                .organisationName("Org").projectName("Event")
                .build());

        mockMvc.perform(post("/api/print/wristband")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void printWristband_printerUnavailable_returns503() throws Exception {
        doThrow(new PrinterUnavailableException("localhost", 9100, new RuntimeException("refused")))
                .when(printService).printWristband(any());

        mockMvc.perform(post("/api/print/wristband")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Printer unavailable"));
    }

    @Test
    void printWristband_unexpectedException_returns500WithNoStackTrace() throws Exception {
        doThrow(new RuntimeException("unexpected"))
                .when(printService).printWristband(any());

        mockMvc.perform(post("/api/print/wristband")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void previewWristband_returnsTextPlain() throws Exception {
        when(printService.previewWristbandZpl(any())).thenReturn("^XA^XZ");

        mockMvc.perform(post("/api/print/wristband/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(PrintRequest.builder()
                .barcode("1234567890")
                .firstName("Jan")
                .lastName("Peeters")
                .organisationName("Chiro X")
                .projectName("Pukkelpop 2026")
                .build());
    }
}
