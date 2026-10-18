package com.hotel.booking.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomServiceCsvTest {

    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        List<List<String>> rows = RoomService.parseCsv("a,b,c\n1,\"x, y\",\"say \"\"hi\"\"\"\n");
        assertEquals(2, rows.size());
        assertEquals(List.of("a", "b", "c"), rows.get(0));
        assertEquals(List.of("1", "x, y", "say \"hi\""), rows.get(1));
    }

    @Test
    void ignoresBlankLinesAndHandlesWindowsLineEndings() {
        List<List<String>> rows = RoomService.parseCsv("h1,h2\r\n\r\nv1,v2\r\n");
        assertEquals(2, rows.size());
        assertEquals(List.of("v1", "v2"), rows.get(1));
    }
}
