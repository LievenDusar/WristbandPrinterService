package com.example.wristband.service;

import com.example.wristband.api.PrintRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ZplTemplateBuilder {

    private static final String TEMPLATE_PATH = "zpl/wristband-template.zpl";

    private String template;

    public String buildWristbandZpl(PrintRequest request) {
        if (template == null) {
            template = loadTemplate();
        }

        String fullName = request.getFirstName() + " " + request.getLastName();
        String group = request.getOrganisationName();
        String event = request.getProjectName();
        String barcode = request.getBarcode();

        String filled = template
                .replace("{{name}}", escape(fullName))
                .replace("{{group}}", escape(group))
                .replace("{{event}}", escape(event))
                .replace("{{barcode}}", escape(barcode));

        // Logo (zoals in de oude .NET code: hetzelfde logo 2x plaatsen)
        String logoZpl = buildLogoZpl(request);

        if (!logoZpl.isEmpty()) {
            // Injecteer logo-ZPL direct na ^XA zodat alles in één label blijft
            int idx = filled.indexOf("^XA");
            if (idx >= 0) {
                String before = filled.substring(0, idx + 3); // inclusief ^XA
                String after = filled.substring(idx + 3);
                filled = before + "\n" + logoZpl + after;
            } else {
                filled = logoZpl + filled;
            }
        }

        return filled;
    }

    private String buildLogoZpl(PrintRequest request) {
        String logoId = null;
        if (request.getSponsorLogoId() != null && !request.getSponsorLogoId().isBlank()) {
            logoId = request.getSponsorLogoId();
        } else if (request.getEventLogoId() != null && !request.getEventLogoId().isBlank()) {
            logoId = request.getEventLogoId();
        }

        if (logoId == null) {
            return "";
        }

        StringBuilder zpl = new StringBuilder();
        // Eerste logo ~ GraphicWrite(50, 800, "LOGO", "E3")
        zpl.append("^FO800,50^XG")
                .append(escape(logoId))
                .append(".GRF,1,1^FS\n");

        // Tweede logo ~ GraphicWrite(50, 2500, "LOGO", "E3")
        zpl.append("^FO2500,50^XG")
                .append(escape(logoId))
                .append(".GRF,1,1^FS");

        return zpl.toString();
    }

    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ZPL template from " + TEMPLATE_PATH, e);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        // Escape ZPL reserved characters if needed
        return value.replace("^", " ");
    }
}

