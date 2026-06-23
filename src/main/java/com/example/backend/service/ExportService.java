package com.example.backend.service;

import com.example.backend.model.JobOpening;
import com.example.backend.model.ScreeningRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    // ════════════════════════════════════════════════════════════
    // CSV export
    // ════════════════════════════════════════════════════════════

    public String generateCsv(JobOpening job, List<ScreeningRecord> records) {
        StringBuilder sb = new StringBuilder();

        sb.append("Rank,File Name,Score,Classification,Years Experience,")
                .append("Matched Skills,Missing Skills,Summary\n");

        int rank = 1;
        for (ScreeningRecord r : records) {
            sb.append(rank++).append(',');
            sb.append(escapeCsv(r.getFileName())).append(',');
            sb.append(r.getScore()).append(',');
            sb.append(r.getClassification()).append(',');
            sb.append(r.getYearsExperience()).append(',');
            sb.append(escapeCsv(String.join("; ", r.getMatchedSkills()))).append(',');
            sb.append(escapeCsv(String.join("; ", r.getMissingSkills()))).append(',');
            sb.append(escapeCsv(r.getSummary())).append('\n');
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ════════════════════════════════════════════════════════════
    // PDF export — built with PDFBox (Apache licensed, free to use)
    // ════════════════════════════════════════════════════════════

    private static final float MARGIN = 40;
    private static final float ROW_HEIGHT = 22;
    private static final float[] COL_WIDTHS = {30, 150, 45, 80, 35, 175};
    private static final String[] HEADERS =
            {"#", "File name", "Score", "Fit", "Yrs", "Matched skills"};

    public byte[] generatePdf(JobOpening job, List<ScreeningRecord> records) throws IOException {

        try (PDDocument document = new PDDocument()) {

            Cursor cursor = newPage(document);

            cursor.content.beginText();
            cursor.content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            cursor.content.newLineAtOffset(MARGIN, cursor.y);
            cursor.content.showText("Candidate ranking - " + job.getTitle());
            cursor.content.endText();
            cursor.y -= 30;

            drawTableHeader(cursor);

            int rank = 1;
            for (ScreeningRecord r : records) {

                if (cursor.y < MARGIN + ROW_HEIGHT) {
                    cursor.content.close();
                    cursor = newPage(document);
                    drawTableHeader(cursor);
                }

                String matched = String.join(", ", r.getMatchedSkills());
                if (matched.length() > 38) {
                    matched = matched.substring(0, 35) + "...";
                }

                String[] row = {
                        String.valueOf(rank++),
                        truncate(r.getFileName(), 24),
                        String.valueOf(r.getScore()),
                        shortLabel(r.getClassification()),
                        String.valueOf(r.getYearsExperience()),
                        matched
                };

                drawRow(cursor, row, false);
            }

            cursor.content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    // Mutable holder — tracks the current page's content stream and
    // y-position as rows get drawn down the page.
    private static class Cursor {
        PDPageContentStream content;
        float y;
        Cursor(PDPageContentStream content, float y) {
            this.content = content;
            this.y = y;
        }
    }

    private Cursor newPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float startY = page.getMediaBox().getHeight() - MARGIN;
        return new Cursor(content, startY);
    }

    private void drawTableHeader(Cursor cursor) throws IOException {
        drawRow(cursor, HEADERS, true);
    }

    private void drawRow(Cursor cursor, String[] values, boolean isHeader) throws IOException {
        float x = MARGIN;
        float fontSize = isHeader ? 9 : 8.5f;
        var font = isHeader
                ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        for (int i = 0; i < values.length; i++) {
            cursor.content.beginText();
            cursor.content.setFont(font, fontSize);
            cursor.content.newLineAtOffset(x, cursor.y);
            cursor.content.showText(values[i] == null ? "" : values[i]);
            cursor.content.endText();
            x += COL_WIDTHS[i];
        }

        cursor.y -= ROW_HEIGHT;

        if (isHeader) {
            cursor.content.moveTo(MARGIN, cursor.y + 6);
            cursor.content.lineTo(MARGIN + sum(COL_WIDTHS), cursor.y + 6);
            cursor.content.setLineWidth(0.5f);
            cursor.content.stroke();
        }
    }

    private float sum(float[] values) {
        float total = 0;
        for (float v : values) total += v;
        return total;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max - 3) + "..." : value;
    }

    private String shortLabel(String classification) {
        return switch (classification) {
            case "STRONG_FIT"   -> "Strong";
            case "POSSIBLE_FIT" -> "Possible";
            case "NOT_FIT"      -> "Not fit";
            default             -> classification;
        };
    }
}
