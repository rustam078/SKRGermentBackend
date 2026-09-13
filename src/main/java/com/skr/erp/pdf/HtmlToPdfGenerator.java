package com.skr.erp.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;

/**
 * Renders an XHTML string (a stored template with placeholders already filled)
 * into a PDF byte array using OpenHTMLtoPDF.
 */
public class HtmlToPdfGenerator {

    private HtmlToPdfGenerator() {
    }

    public static byte[] render(String xhtml) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF from HTML template", e);
        }
    }
}
