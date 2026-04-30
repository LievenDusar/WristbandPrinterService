package com.example.wristband.service;

import com.example.wristband.api.PrintRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZplTemplateBuilderTest {

    private ZplTemplateBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ZplTemplateBuilder();
        builder.init();
    }

    @Test
    void allPlaceholdersReplaced() {
        PrintRequest request = request("Jan", "Peeters", "Chiro X", "Pukkelpop 2026", "1234567890");
        String zpl = builder.buildWristbandZpl(request);

        assertThat(zpl).doesNotContain("{{name}}", "{{group}}", "{{event}}", "{{barcode}}");
        assertThat(zpl).contains("Jan Peeters");
        assertThat(zpl).contains("Chiro X");
        assertThat(zpl).contains("Pukkelpop 2026");
        assertThat(zpl).contains("1234567890");
    }

    @Test
    void caretInNameIsStripped() {
        PrintRequest request = request("Jan^Bad", "Peeters", "Org", "Event", "BAR");
        String zpl = builder.buildWristbandZpl(request);
        assertThat(zpl).doesNotContain("^Bad");
        assertThat(zpl).contains("Jan Bad Peeters");
    }

    @Test
    void tildeInNameIsStripped() {
        PrintRequest request = request("Jan", "Peeters~Evil", "Org", "Event", "BAR");
        String zpl = builder.buildWristbandZpl(request);
        assertThat(zpl).doesNotContain("~Evil");
    }

    @Test
    void placeholderValueInUserDataDoesNotCauseSecondPassSubstitution() {
        // If a user's name literally contains "{{group}}" it must not be substituted
        PrintRequest request = request("{{group}}", "Test", "RealOrg", "Event", "BAR");
        String zpl = builder.buildWristbandZpl(request);
        // "{{group}}" in the name field gets escaped (the braces are harmless in ZPL),
        // but the group placeholder itself must have been filled with "RealOrg"
        assertThat(zpl).contains("RealOrg");
        assertThat(zpl).doesNotContain("{{group}}");
    }

    @Test
    void noLogoZplWhenNoLogoIds() {
        PrintRequest request = request("Jan", "Peeters", "Org", "Event", "BAR");
        String zpl = builder.buildWristbandZpl(request);
        assertThat(zpl).doesNotContain("^XG");
    }

    @Test
    void sponsorLogoIdProducesTwoXgReferences() {
        PrintRequest request = PrintRequest.builder()
                .barcode("BAR")
                .firstName("Jan")
                .lastName("Peeters")
                .organisationName("Org")
                .projectName("Event")
                .sponsorLogoId("STUPLOGO")
                .build();
        String zpl = builder.buildWristbandZpl(request);
        long count = zpl.chars().filter(c -> c == 'X').mapToObj(c -> (char) c)
                .filter(c -> zpl.indexOf("^XGR:STUPLOGO.GRF") >= 0).count();
        assertThat(zpl).contains("^XGR:STUPLOGO.GRF,1,1");
        assertThat(countOccurrences(zpl, "^XGR:STUPLOGO.GRF,1,1")).isEqualTo(2);
    }

    @Test
    void invalidLogoIdThrowsIllegalArgument() {
        PrintRequest request = PrintRequest.builder()
                .barcode("BAR")
                .firstName("Jan")
                .lastName("P")
                .organisationName("Org")
                .projectName("Event")
                .sponsorLogoId("bad logo id!")
                .build();
        assertThatThrownBy(() -> builder.buildWristbandZpl(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid logoId");
    }

    private PrintRequest request(String first, String last, String org, String event, String barcode) {
        return PrintRequest.builder()
                .barcode(barcode)
                .firstName(first)
                .lastName(last)
                .organisationName(org)
                .projectName(event)
                .build();
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
