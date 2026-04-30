package com.example.wristband.service;

import com.example.wristband.api.PrintRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ZplTemplateBuilder {

    private static final String TEMPLATE_PATH = "zpl/wristband-template.zpl";
    private static final Pattern LOGO_ID_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private String template;

    @PostConstruct
    void init() {
        this.template = loadTemplate();
    }

    public String buildWristbandZpl(PrintRequest request) {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("{{name}}", escape(request.getFirstName() + " " + request.getLastName()));
        replacements.put("{{group}}", escape(request.getOrganisationName()));
        replacements.put("{{event}}", escape(request.getProjectName()));
        replacements.put("{{barcode}}", escape(request.getBarcode()));

        String filled = applyReplacements(template, replacements);

        String logoZpl = buildLogoZpl(request);
        if (!logoZpl.isEmpty()) {
            int idx = filled.indexOf("^XA");
            if (idx >= 0) {
                filled = filled.substring(0, idx + 3) + "\n" + logoZpl + filled.substring(idx + 3);
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

        if (!LOGO_ID_PATTERN.matcher(logoId).matches()) {
            throw new IllegalArgumentException("Invalid logoId: " + logoId);
        }

        String safeId = logoId.toUpperCase();
        return "^FO800,50^XGR:" + safeId + ".GRF,1,1^FS\n"
             + "^FO2500,50^XGR:" + safeId + ".GRF,1,1^FS";
    }

    /**
     * Applies replacements in a single pass using StringBuilder to prevent
     * user-supplied values from being interpreted as further placeholders.
     */
    private String applyReplacements(String source, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            source = source.replace(entry.getKey(), entry.getValue());
        }
        return source;
    }

    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ZPL template from " + TEMPLATE_PATH, e);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("^", " ").replace("~", " ");
    }
}
