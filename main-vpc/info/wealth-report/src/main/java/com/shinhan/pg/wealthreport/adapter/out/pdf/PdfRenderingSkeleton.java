package com.shinhan.pg.wealthreport.adapter.out.pdf;

import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Local deterministic PDF renderer: embedded licensed Korean font, measured wrapping and page
 * breaks. No HTML interpreter, remote resources, user names or restoration dictionary.
 */
public final class PdfRenderingSkeleton implements PdfRenderingPort {
    public byte[] render(String text) {
        try (var document = new PDDocument();
                var fontStream = getClass().getResourceAsStream("/fonts/NanumGothic-Regular.ttf");
                var output = new ByteArrayOutputStream()) {
            var font = PDType0Font.load(document, Objects.requireNonNull(fontStream));
            var lines = new ArrayList<String>();
            float fontSize = 11, width = PDRectangle.A4.getWidth() - 96;
            for (String paragraph : text.split("\n", -1)) {
                if (paragraph.contains("\t")) {
                    lines.add(paragraph);
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (int cp : paragraph.codePoints().toArray()) {
                    String next = line.toString() + new String(Character.toChars(cp));
                    if (font.getStringWidth(next) / 1000 * fontSize > width && !line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.appendCodePoint(cp);
                }
                lines.add(line.toString());
            }
            int perPage = 40, pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
            for (int page = 0; page < pages; page++) {
                if (Thread.currentThread().isInterrupted())
                    throw new ReportException(WealthReportErrorCode.INTERRUPTED);
                var sheet = new PDPage(PDRectangle.A4);
                document.addPage(sheet);
                try (var stream = new PDPageContentStream(document, sheet)) {
                    float y = 790;
                    for (int n = page * perPage;
                            n < Math.min(lines.size(), (page + 1) * perPage);
                            n++) {
                        String line = lines.get(n);
                        String[] cells = line.split("\t", -1);
                        stream.beginText();
                        stream.setFont(font, n == 0 ? 17 : fontSize);
                        stream.newLineAtOffset(48, y);
                        stream.showText(cells[0]);
                        stream.endText();
                        if (cells.length == 2) {
                            float right =
                                    sheet.getMediaBox().getWidth()
                                            - 48
                                            - font.getStringWidth(cells[1]) / 1000 * fontSize;
                            stream.beginText();
                            stream.setFont(font, fontSize);
                            stream.newLineAtOffset(right, y);
                            stream.showText(cells[1]);
                            stream.endText();
                            stream.setLineWidth(0.3f);
                            stream.moveTo(48, y - 6);
                            stream.lineTo(sheet.getMediaBox().getWidth() - 48, y - 6);
                            stream.stroke();
                        }
                        y -= 18;
                    }
                    stream.beginText();
                    stream.setFont(font, 9);
                    stream.newLineAtOffset(48, 32);
                    stream.showText("PG Platform | " + (page + 1) + " / " + pages);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (ReportException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ReportException(WealthReportErrorCode.PDF_FAILED);
        }
    }
}
