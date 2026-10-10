package com.example.excelpricesearch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoicePdf {
    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final int M = 36;

    public static void write(OutputStream out, String invoiceNo, AppDb.Customer customer, List<AppDb.SaleLine> lines, long createdAt) throws Exception {
        PdfDocument doc = new PdfDocument();
        try {
            double grandTotal = 0;
            for (AppDb.SaleLine l : lines) grandTotal += l.unitPrice * l.qty;

            int index = 0;
            int pageNo = 1;
            while (index < lines.size() || pageNo == 1) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create();
                PdfDocument.Page page = doc.startPage(info);
                Canvas c = page.getCanvas();
                c.drawColor(Color.WHITE);

                Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                linePaint.setColor(Color.rgb(210, 215, 225));
                linePaint.setStrokeWidth(1f);

                TextPaint title = tp(22, Color.rgb(20, 36, 64), true);
                TextPaint body = tp(12, Color.DKGRAY, false);
                TextPaint bold = tp(12, Color.rgb(20, 36, 64), true);
                TextPaint totalPaint = tp(16, Color.rgb(20, 92, 65), true);

                int y = M;
                y += drawRtl(c, "فاکتور فروش", title, M, y, PAGE_W - 2 * M);
                y += 8;

                String date = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US).format(new Date(createdAt));
                y += drawRtl(c, "شماره فاکتور: " + invoiceNo, bold, M, y, PAGE_W - 2 * M);
                y += drawRtl(c, "تاریخ: " + date, body, M, y, PAGE_W - 2 * M);
                if (customer != null) {
                    if (customer.name != null && !customer.name.trim().isEmpty()) y += drawRtl(c, "مشتری: " + customer.name, body, M, y, PAGE_W - 2 * M);
                    if (customer.phone != null && !customer.phone.trim().isEmpty()) y += drawRtl(c, "تلفن: " + customer.phone, body, M, y, PAGE_W - 2 * M);
                    if (customer.address != null && !customer.address.trim().isEmpty()) y += drawRtl(c, "آدرس: " + customer.address, body, M, y, PAGE_W - 2 * M);
                }
                y += 8;
                c.drawLine(M, y, PAGE_W - M, y, linePaint);
                y += 14;

                int rowNo = index + 1;
                while (index < lines.size()) {
                    AppDb.SaleLine l = lines.get(index);
                    double lineTotal = l.unitPrice * l.qty;
                    String row = rowNo + ") " + l.productName + "  |  " + l.saleType + "  |  تعداد: " + l.qty + "  |  فی: " + money(l.unitPrice) + "  |  جمع: " + money(lineTotal);
                    int h = measureRtl(row, body, PAGE_W - 2 * M);
                    if (y + h + 60 > PAGE_H - M) break;
                    y += drawRtl(c, row, body, M, y, PAGE_W - 2 * M);
                    y += 7;
                    c.drawLine(M, y, PAGE_W - M, y, linePaint);
                    y += 9;
                    index++;
                    rowNo++;
                }

                if (index >= lines.size()) {
                    y += 10;
                    y += drawRtl(c, "جمع کل: " + money(grandTotal), totalPaint, M, y, PAGE_W - 2 * M);
                } else {
                    drawRtl(c, "ادامه در صفحه بعد", body, M, PAGE_H - M - 24, PAGE_W - 2 * M);
                }

                doc.finishPage(page);
                pageNo++;
                if (lines.isEmpty()) break;
            }
            doc.writeTo(out);
        } finally {
            doc.close();
        }
    }

    private static TextPaint tp(float size, int color, boolean bold) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
        return p;
    }

    private static int drawRtl(Canvas canvas, String text, TextPaint paint, int x, int y, int width) {
        StaticLayout layout = StaticLayout.Builder.obtain(text == null ? "" : text, 0, text == null ? 0 : text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .build();
        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
        return layout.getHeight();
    }

    private static int measureRtl(String text, TextPaint paint, int width) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .build().getHeight();
    }

    public static String money(double value) {
        return new DecimalFormat("#,###.##").format(value);
    }
}
