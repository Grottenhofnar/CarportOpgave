package service;

import model.BillOfMaterials;
import model.BomLine;
import model.CarportSpec;
import model.Order;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PdfGenerator {

    private static final PDRectangle PAGE = PDRectangle.A4;
    private static final float MARGIN = 50f;
    private static final DecimalFormat KR;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("da", "DK"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        KR = new DecimalFormat("#,##0.00", s);
    }

    public byte[] build(Order order, BillOfMaterials bom) throws Exception {
        try (PDDocument doc = new PDDocument()) {

            PDPage page1 = new PDPage(PAGE);
            doc.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                float top = PAGE.getHeight() - MARGIN;
                drawHeader(cs, top);
                float y = top - 70;

                y = text(cs, PDType1Font.HELVETICA_BOLD, 16, MARGIN, y, "Ordrebekræftelse #" + order.orderId);
                y -= 6;
                CarportSpec spec = order.spec;
                String dims = fmtMeter(spec.widthCm) + " x " + fmtMeter(spec.lengthCm) + " m";
                y = text(cs, PDType1Font.HELVETICA, 12, MARGIN, y, "Carport med fladt tag · " + dims);
                if (spec.withShed) {
                    y = text(cs, PDType1Font.HELVETICA, 12, MARGIN, y,
                            "Inkl. redskabsrum: " + fmtMeter(spec.shedWidthCm) + " x " + fmtMeter(spec.shedLengthCm) + " m");
                }
                if (order.contact != null) {
                    String navn = safe(order.contact.firstName) + " " + safe(order.contact.lastName);
                    y -= 8;
                    y = text(cs, PDType1Font.HELVETICA, 12, MARGIN, y, "Kunde: " + navn.trim());
                    if (notBlank(order.contact.address))
                        y = text(cs, PDType1Font.HELVETICA, 11, MARGIN, y,
                                safe(order.contact.address) + ", " + safe(order.contact.postalCode) + " " + safe(order.contact.city));
                    if (notBlank(order.contact.email))
                        y = text(cs, PDType1Font.HELVETICA, 11, MARGIN, y, "Email: " + order.contact.email);
                    if (notBlank(order.contact.phone))
                        y = text(cs, PDType1Font.HELVETICA, 11, MARGIN, y, "Telefon: " + order.contact.phone);
                }
                y -= 10;
                BigDecimal price = order.totalPrice == null ? BigDecimal.ZERO : order.totalPrice;
                y = text(cs, PDType1Font.HELVETICA_BOLD, 14, MARGIN, y, "Pris: " + KR.format(price) + " kr.");

                y -= 30;
                drawPlan(cs, MARGIN, y - 260, PAGE.getWidth() - 2 * MARGIN, 250, spec, bom);
            }

            PDPage page2 = new PDPage(PAGE);
            doc.addPage(page2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
                float top = PAGE.getHeight() - MARGIN;
                drawHeader(cs, top);
                float y = top - 70;
                y = text(cs, PDType1Font.HELVETICA_BOLD, 16, MARGIN, y, "Stykliste");
                y -= 14;
                y = drawBomTable(doc, page2, cs, y, bom);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void drawHeader(PDPageContentStream cs, float top) throws Exception {
        text(cs, PDType1Font.HELVETICA_BOLD, 20, MARGIN, top, "Fog · Carport");
        text(cs, PDType1Font.HELVETICA, 10, MARGIN, top - 16, "Værebro Trælast og Byggecenter");
        cs.setLineWidth(1f);
        cs.moveTo(MARGIN, top - 26);
        cs.lineTo(PAGE.getWidth() - MARGIN, top - 26);
        cs.stroke();
    }

    private float text(PDPageContentStream cs, PDType1Font font, float size,
                       float x, float y, String s) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s == null ? "" : s);
        cs.endText();
        return y - (size + 6);
    }

    private void drawPlan(PDPageContentStream cs, float x, float y, float maxW, float maxH,
                          CarportSpec spec, BillOfMaterials bom) throws Exception {
        double L = spec.lengthCm, W = spec.widthCm;
        double scale = Math.min(maxW / L, maxH / W);
        float dw = (float) (L * scale), dh = (float) (W * scale);

        cs.setLineWidth(2f);
        cs.addRect(x, y, dw, dh);
        cs.stroke();

        int gaps = (int) Math.ceil(L / 60.0);
        if (L / gaps >= 59.999) gaps++;
        int rafters = gaps + 1;
        cs.setLineWidth(0.6f);
        for (int i = 0; i < rafters; i++) {
            float sx = x + dw * i / (rafters - 1);
            cs.moveTo(sx, y);
            cs.lineTo(sx, y + dh);
            cs.stroke();
        }

        if (spec.withShed && spec.shedWidthCm > 0 && spec.shedLengthCm > 0) {
            float sl = (float) (spec.shedLengthCm * scale);
            float sw = (float) (spec.shedWidthCm * scale);
            cs.setLineWidth(1.2f);
            cs.addRect(x + dw - sl, y, sl, sw);
            cs.stroke();
        }

        text(cs, PDType1Font.HELVETICA, 11, x, y + dh + 10,
                rafters + " spær · " + fmtMeter(spec.widthCm) + " x " + fmtMeter(spec.lengthCm) + " m");
    }

    private float drawBomTable(PDDocument doc, PDPage page, PDPageContentStream cs,
                               float startY, BillOfMaterials bom) throws Exception {
        float x = MARGIN;
        float right = PAGE.getWidth() - MARGIN;
        float[] cols = { x, x + 250, x + 320, x + 390, right };
        float y = startY;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        row(cs, y, cols, "Materiale", "Længde", "Antal", "Kostpris");
        y -= 6;
        cs.setLineWidth(0.8f);
        cs.moveTo(x, y); cs.lineTo(right, y); cs.stroke();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA, 10);
        for (BomLine l : bom.lines) {
            if (y < MARGIN + 40) break;
            String len = l.lengthCm > 0 ? l.lengthCm + " cm" : "-";
            row(cs, y, cols, clip(l.description, 48), len, String.valueOf(l.quantity),
                    KR.format(l.lineTotal == null ? BigDecimal.ZERO : l.lineTotal) + " kr.");
            y -= 14;
        }

        y -= 4;
        cs.setLineWidth(1.2f);
        cs.moveTo(x, y); cs.lineTo(right, y); cs.stroke();
        y -= 16;
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        row(cs, y, cols, "Materialer i alt (kostpris)", "", "",
                KR.format(bom.total == null ? BigDecimal.ZERO : bom.total) + " kr.");
        return y - 20;
    }

    private void row(PDPageContentStream cs, float y, float[] cols,
                     String c1, String c2, String c3, String c4) throws Exception {
        cell(cs, cols[0], y, c1);
        cell(cs, cols[1], y, c2);
        cell(cs, cols[2], y, c3);
        cell(cs, cols[3], y, c4);
    }

    private void cell(PDPageContentStream cs, float x, float y, String s) throws Exception {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(s == null ? "" : s);
        cs.endText();
    }

    private String fmtMeter(int cm) {
        return new DecimalFormat("0.0#", new DecimalFormatSymbols(new Locale("da", "DK")))
                .format(cm / 100.0);
    }

    private String clip(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String safe(String s) { return s == null ? "" : s; }
}
