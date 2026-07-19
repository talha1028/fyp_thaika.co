package com.example.madproject.helpers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Renders plain contract text into a paginated A4-ish PDF using Android's built-in PdfDocument. */
public class PdfHelper {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;

    public static byte[] generateContractPdf(String title, String contractText) throws IOException {
        int usableWidth = PAGE_WIDTH - 2 * MARGIN;
        int usableHeight = PAGE_HEIGHT - 2 * MARGIN;

        TextPaint paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setTextSize(11f);

        String fullText = (title != null ? title + "\n\n" : "") + contractText;

        StaticLayout fullLayout = StaticLayout.Builder
                .obtain(fullText, 0, fullText.length(), paint, usableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .build();

        PdfDocument document = new PdfDocument();
        int totalLines = fullLayout.getLineCount();
        int lineIndex = 0;
        int pageNumber = 1;

        while (lineIndex < totalLines) {
            int startLine = lineIndex;
            int startTop = fullLayout.getLineTop(startLine);
            int endLine = startLine;
            while (endLine < totalLines
                    && (fullLayout.getLineBottom(endLine) - startTop) <= usableHeight) {
                endLine++;
            }
            if (endLine == startLine) {
                endLine++; // guarantee progress even if a single line overflows the page
            }

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.save();
            canvas.translate(MARGIN, MARGIN);
            canvas.clipRect(0, 0, usableWidth, usableHeight);
            canvas.translate(0, -startTop);
            fullLayout.draw(canvas);
            canvas.restore();

            document.finishPage(page);
            lineIndex = endLine;
            pageNumber++;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.writeTo(baos);
        document.close();
        return baos.toByteArray();
    }
}
