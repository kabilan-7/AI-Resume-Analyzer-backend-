package com.example.backend.service;

import com.example.backend.model.JobOpening;
import com.example.backend.model.ScreeningRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

        sb.append("Rank,File Name,Score,Classification,")
                .append("Years Experience,Matched Skills,Missing Skills,Summary\n");

        int rank = 1;
        for (ScreeningRecord r : records) {
            sb.append(rank++).append(',');
            sb.append(escapeCsv(r.getFileName())).append(',');
            sb.append(r.getScore()).append(',');
            sb.append(r.getClassification()).append(',');
            sb.append(r.getYearsExperience()).append(',');
            sb.append(escapeCsv(
                    r.getMatchedSkills() != null
                            ? String.join("; ", r.getMatchedSkills()) : "")).append(',');
            sb.append(escapeCsv(
                    r.getMissingSkills() != null
                            ? String.join("; ", r.getMissingSkills()) : "")).append(',');
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
    // PDF export — PDFBox 2.x API
    //
    // Key differences from 3.x:
    //   3.x: new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    //   2.x: PDType1Font.HELVETICA_BOLD   (static constant)
    //
    //   3.x: PDPageContentStream(doc, page)  (constructor differs)
    //   2.x: PDPageContentStream(doc, page)  (same here, fine)
    // ════════════════════════════════════════════════════════════

    private static final float MARGIN     = 40f;
    private static final float ROW_HEIGHT = 22f;
    private static final float[] COL_WIDTHS = {30f, 150f, 45f, 80f, 35f, 175f};
    private static final String[] HEADERS =
            {"#", "File name", "Score", "Fit", "Yrs", "Matched skills"};

    public byte[] generatePdf(JobOpening job, List<ScreeningRecord> records)
            throws IOException {

        try (PDDocument document = new PDDocument()) {

            Cursor cursor = newPage(document);

            // Title
            cursor.content.beginText();
            cursor.content.setFont(PDType1Font.HELVETICA_BOLD, 16);
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

                String matched = r.getMatchedSkills() != null
                        ? String.join(", ", r.getMatchedSkills()) : "";
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

    // Mutable cursor — tracks y-position and content stream per page
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
        PDPageContentStream content =
                new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.OVERWRITE, true);
        float startY = page.getMediaBox().getHeight() - MARGIN;
        return new Cursor(content, startY);
    }

    private void drawTableHeader(Cursor cursor) throws IOException {
        drawRow(cursor, HEADERS, true);
        // Draw separator line under header
        cursor.content.setLineWidth(0.5f);
        cursor.content.moveTo(MARGIN, cursor.y + 6);
        cursor.content.lineTo(MARGIN + colSum(), cursor.y + 6);
        cursor.content.stroke();
    }

    private void drawRow(Cursor cursor, String[] values, boolean header)
            throws IOException {

        float x = MARGIN;
        PDType1Font font = header
                ? PDType1Font.HELVETICA_BOLD
                : PDType1Font.HELVETICA;
        float fontSize = header ? 9f : 8.5f;

        for (int i = 0; i < values.length && i < COL_WIDTHS.length; i++) {
            String text = values[i] == null ? "" : values[i];
            cursor.content.beginText();
            cursor.content.setFont(font, fontSize);
            cursor.content.newLineAtOffset(x, cursor.y);
            cursor.content.showText(text);
            cursor.content.endText();
            x += COL_WIDTHS[i];
        }

        cursor.y -= ROW_HEIGHT;
    }

    private float colSum() {
        float total = 0;
        for (float w : COL_WIDTHS) total += w;
        return total;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max - 3) + "..." : value;
    }

    private String shortLabel(String classification) {
        if (classification == null) return "";
        return switch (classification) {
            case "STRONG_FIT"   -> "Strong";
            case "POSSIBLE_FIT" -> "Possible";
            case "NOT_FIT"      -> "Not fit";
            default             -> classification;
        };
    }
}