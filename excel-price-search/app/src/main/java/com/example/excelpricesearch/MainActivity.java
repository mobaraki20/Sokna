package com.example.excelpricesearch;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends Activity {
    private static final int PICK_XLSX = 1001;
    private static final int SAVE_PDF = 2002;

    private ProductDb db;
    private LinearLayout content;
    private Button productsTab;
    private Button invoiceTab;

    private EditText search;
    private TextView status;
    private LinearLayout results;

    private EditText customerName;
    private EditText customerPhone;
    private LinearLayout invoiceItemsContainer;
    private TextView invoiceTotal;

    private final ArrayList<InvoiceItem> invoiceItems = new ArrayList<>();
    private ArrayList<InvoiceItem> pendingPdfItems;
    private String pendingCustomerName = "";
    private String pendingCustomerPhone = "";
    private String pendingInvoiceNo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(25, 50, 90));
        db = new ProductDb();
        setContentView(buildUi());
        showProductsScreen();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.rgb(246, 248, 252));

        TextView title = new TextView(this);
        title.setText("جستجوی قیمت و فاکتور فروش");
        title.setTextSize(23);
        title.setTextColor(Color.rgb(20, 36, 64));
        title.setGravity(Gravity.RIGHT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, lp(-1, -2, 0, 0, 0, 10));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        productsTab = new Button(this);
        productsTab.setText("کالاها و قیمت");
        productsTab.setOnClickListener(v -> showProductsScreen());
        tabs.addView(productsTab, new LinearLayout.LayoutParams(0, dp(48), 1));

        invoiceTab = new Button(this);
        invoiceTab.setText("فاکتور");
        invoiceTab.setOnClickListener(v -> showInvoiceScreen());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(48), 1);
        ip.setMargins(dp(8), 0, 0, 0);
        tabs.addView(invoiceTab, ip);

        root.addView(tabs, lp(-1, -2, 0, 0, 0, 10));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        updateInvoiceTab();
        return root;
    }

    private void showProductsScreen() {
        content.removeAllViews();

        TextView hint = new TextView(this);
        hint.setText("فایل Excel با سه ستون: نام کالا، قیمت خرده، قیمت عمده");
        hint.setTextSize(14);
        hint.setTextColor(Color.DKGRAY);
        hint.setGravity(Gravity.RIGHT);
        content.addView(hint, lp(-1, -2, 0, 0, 0, 10));

        Button choose = new Button(this);
        choose.setText("انتخاب فایل Excel (.xlsx)");
        choose.setTextSize(15);
        choose.setOnClickListener(v -> pickFile());
        content.addView(choose, lp(-1, dp(50), 0, 0, 0, 10));

        search = new EditText(this);
        search.setHint("جستجو بر اساس نام کالا...");
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        search.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.WHITE);
        searchBg.setStroke(dp(1), Color.rgb(210, 216, 226));
        searchBg.setCornerRadius(dp(12));
        search.setBackground(searchBg);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { renderProducts(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        content.addView(search, lp(-1, dp(52), 0, 0, 0, 8));

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(Color.GRAY);
        status.setGravity(Gravity.RIGHT);
        content.addView(status, lp(-1, -2, 0, 0, 0, 8));

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        updateStatus();
        renderProducts("");
        setTabState(true);
    }

    private void showInvoiceScreen() {
        content.removeAllViews();

        TextView h = new TextView(this);
        h.setText("فاکتور فروش");
        h.setTextSize(21);
        h.setTextColor(Color.rgb(20, 36, 64));
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.setGravity(Gravity.RIGHT);
        content.addView(h, lp(-1, -2, 0, 0, 0, 8));

        customerName = new EditText(this);
        customerName.setHint("نام مشتری (اختیاری)");
        customerName.setSingleLine(true);
        customerName.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        customerName.setPadding(dp(12), 0, dp(12), 0);
        content.addView(customerName, lp(-1, dp(48), 0, 0, 0, 6));

        customerPhone = new EditText(this);
        customerPhone.setHint("شماره تماس مشتری (اختیاری)");
        customerPhone.setSingleLine(true);
        customerPhone.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        customerPhone.setPadding(dp(12), 0, dp(12), 0);
        content.addView(customerPhone, lp(-1, dp(48), 0, 0, 0, 8));

        ScrollView scroll = new ScrollView(this);
        invoiceItemsContainer = new LinearLayout(this);
        invoiceItemsContainer.setOrientation(LinearLayout.VERTICAL);
        invoiceItemsContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(invoiceItemsContainer, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        invoiceTotal = new TextView(this);
        invoiceTotal.setTextSize(20);
        invoiceTotal.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        invoiceTotal.setTextColor(Color.rgb(20, 92, 65));
        invoiceTotal.setGravity(Gravity.RIGHT);
        invoiceTotal.setPadding(0, dp(8), 0, dp(6));
        content.addView(invoiceTotal, lp(-1, -2, 0, 0, 0, 4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button pdf = new Button(this);
        pdf.setText("صدور PDF");
        pdf.setOnClickListener(v -> requestSavePdf());
        actions.addView(pdf, new LinearLayout.LayoutParams(0, dp(50), 1));

        Button clear = new Button(this);
        clear.setText("پاک کردن فاکتور");
        clear.setOnClickListener(v -> {
            invoiceItems.clear();
            renderInvoice();
            updateInvoiceTab();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(50), 1);
        cp.setMargins(dp(8), 0, 0, 0);
        actions.addView(clear, cp);

        content.addView(actions, lp(-1, -2, 0, 0, 0, 0));

        renderInvoice();
        setTabState(false);
    }

    private void setTabState(boolean productsActive) {
        if (productsTab == null || invoiceTab == null) return;
        productsTab.setEnabled(!productsActive);
        invoiceTab.setEnabled(productsActive);
    }

    private void updateInvoiceTab() {
        if (invoiceTab == null) return;
        int count = 0;
        for (InvoiceItem i : invoiceItems) count += i.qty;
        invoiceTab.setText(count == 0 ? "فاکتور" : "فاکتور (" + count + ")");
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        startActivityForResult(i, PICK_XLSX);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_XLSX && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (status != null) status.setText("در حال خواندن فایل...");
            new Thread(() -> importXlsx(uri)).start();
            return;
        }
        if (requestCode == SAVE_PDF && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            new Thread(() -> writeInvoicePdf(uri)).start();
        }
    }

    private void importXlsx(Uri uri) {
        try {
            File tmp = new File(getCacheDir(), "import_" + System.currentTimeMillis() + ".xlsx");
            try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(tmp)) {
                if (in == null) throw new Exception("امکان باز کردن فایل وجود ندارد");
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            List<Product> products = Xlsx.read(tmp);
            tmp.delete();
            if (products.isEmpty()) throw new Exception("هیچ کالایی در سه ستون اول پیدا نشد");
            db.replaceAll(products);
            runOnUiThread(() -> {
                updateStatus();
                if (search != null) search.setText("");
                renderProducts("");
                Toast.makeText(this, products.size() + " کالا با موفقیت وارد شد", Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                updateStatus();
                Toast.makeText(this, "خطا در خواندن Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void updateStatus() {
        if (status == null) return;
        long count = db.count();
        status.setText(count == 0 ? "هنوز فایل اکسل وارد نشده است." : "تعداد کالاهای ذخیره‌شده: " + formatNumber(String.valueOf(count)));
    }

    private void renderProducts(String q) {
        if (results == null) return;
        results.removeAllViews();
        List<Product> list = db.search(q == null ? "" : q.trim());
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(db.count() == 0 ? "ابتدا فایل Excel را انتخاب کنید." : "کالایی پیدا نشد.");
            empty.setTextSize(16);
            empty.setTextColor(Color.GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(36), 0, 0);
            results.addView(empty);
            return;
        }
        for (Product p : list) results.addView(productCard(p));
    }

    private View productCard(Product p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(225, 229, 236));
        card.setBackground(bg);

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(18);
        name.setTextColor(Color.rgb(20, 36, 64));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setGravity(Gravity.RIGHT);
        card.addView(name);

        TextView retail = priceLine("قیمت خرده: ", p.retail, Color.rgb(25, 110, 75));
        card.addView(retail, lp(-1, -2, 0, 7, 0, 0));
        TextView wholesale = priceLine("قیمت عمده: ", p.wholesale, Color.rgb(35, 80, 150));
        card.addView(wholesale, lp(-1, -2, 0, 4, 0, 7));

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button addRetail = new Button(this);
        addRetail.setText("افزودن خرده");
        addRetail.setTextSize(13);
        addRetail.setOnClickListener(v -> addToInvoice(p, p.retail, "خرده"));
        addRow.addView(addRetail, new LinearLayout.LayoutParams(0, dp(45), 1));

        Button addWholesale = new Button(this);
        addWholesale.setText("افزودن عمده");
        addWholesale.setTextSize(13);
        addWholesale.setOnClickListener(v -> addToInvoice(p, p.wholesale, "عمده"));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, dp(45), 1);
        wp.setMargins(dp(6), 0, 0, 0);
        addRow.addView(addWholesale, wp);
        card.addView(addRow, lp(-1, -2, 0, 6, 0, 0));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        return card;
    }

    private TextView priceLine(String label, String value, int color) {
        TextView t = new TextView(this);
        t.setText(label + formatNumber(value));
        t.setTextSize(16);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        return t;
    }

    private void addToInvoice(Product p, String rawPrice, String priceType) {
        double price = parseNumber(rawPrice);
        if (price <= 0) {
            Toast.makeText(this, "قیمت این کالا معتبر نیست.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (InvoiceItem i : invoiceItems) {
            if (i.name.equals(p.name) && i.priceType.equals(priceType) && Math.abs(i.unitPrice - price) < 0.001) {
                i.qty++;
                updateInvoiceTab();
                Toast.makeText(this, "تعداد کالا در فاکتور افزایش یافت.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        invoiceItems.add(new InvoiceItem(p.name, priceType, price, 1));
        updateInvoiceTab();
        Toast.makeText(this, "کالا به فاکتور اضافه شد.", Toast.LENGTH_SHORT).show();
    }

    private void renderInvoice() {
        if (invoiceItemsContainer == null || invoiceTotal == null) return;
        invoiceItemsContainer.removeAllViews();
        if (invoiceItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("فاکتور خالی است.\nاز بخش «کالاها و قیمت» محصول اضافه کنید.");
            empty.setTextSize(16);
            empty.setTextColor(Color.GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(40), dp(8), dp(40));
            invoiceItemsContainer.addView(empty);
            invoiceTotal.setText("جمع کل: 0");
            return;
        }
        for (int index = 0; index < invoiceItems.size(); index++) invoiceItemsContainer.addView(invoiceItemCard(index));
        invoiceTotal.setText("جمع کل: " + formatMoney(invoiceGrandTotal()));
    }

    private View invoiceItemCard(int index) {
        InvoiceItem item = invoiceItems.get(index);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(225, 229, 236));
        card.setBackground(bg);

        TextView name = new TextView(this);
        name.setText(item.name);
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setTextColor(Color.rgb(20, 36, 64));
        name.setGravity(Gravity.RIGHT);
        card.addView(name);

        TextView info = new TextView(this);
        info.setText("نوع قیمت: " + item.priceType + "   |   قیمت واحد: " + formatMoney(item.unitPrice));
        info.setTextSize(14);
        info.setTextColor(Color.DKGRAY);
        info.setGravity(Gravity.RIGHT);
        card.addView(info, lp(-1, -2, 0, 5, 0, 5));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button plus = new Button(this);
        plus.setText("+");
        plus.setOnClickListener(v -> { item.qty++; renderInvoice(); updateInvoiceTab(); });
        controls.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(44)));

        TextView qty = new TextView(this);
        qty.setText(String.valueOf(item.qty));
        qty.setTextSize(18);
        qty.setGravity(Gravity.CENTER);
        qty.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        controls.addView(qty, new LinearLayout.LayoutParams(dp(60), dp(44)));

        Button minus = new Button(this);
        minus.setText("−");
        minus.setOnClickListener(v -> {
            if (item.qty > 1) item.qty--; else invoiceItems.remove(item);
            renderInvoice(); updateInvoiceTab();
        });
        controls.addView(minus, new LinearLayout.LayoutParams(dp(58), dp(44)));

        TextView lineTotal = new TextView(this);
        lineTotal.setText("جمع: " + formatMoney(item.lineTotal()));
        lineTotal.setTextSize(16);
        lineTotal.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        lineTotal.setTextColor(Color.rgb(25, 110, 75));
        lineTotal.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        controls.addView(lineTotal, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button remove = new Button(this);
        remove.setText("حذف");
        remove.setTextSize(12);
        remove.setOnClickListener(v -> { invoiceItems.remove(item); renderInvoice(); updateInvoiceTab(); });
        controls.addView(remove, new LinearLayout.LayoutParams(dp(70), dp(44)));
        card.addView(controls);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(p);
        return card;
    }

    private double invoiceGrandTotal() {
        double total = 0;
        for (InvoiceItem i : invoiceItems) total += i.lineTotal();
        return total;
    }

    private void requestSavePdf() {
        if (invoiceItems.isEmpty()) {
            Toast.makeText(this, "فاکتور خالی است.", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingPdfItems = new ArrayList<>();
        for (InvoiceItem i : invoiceItems) pendingPdfItems.add(new InvoiceItem(i.name, i.priceType, i.unitPrice, i.qty));
        pendingCustomerName = customerName == null ? "" : customerName.getText().toString().trim();
        pendingCustomerPhone = customerPhone == null ? "" : customerPhone.getText().toString().trim();
        pendingInvoiceNo = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "invoice-" + pendingInvoiceNo + ".pdf");
        startActivityForResult(i, SAVE_PDF);
    }

    private void writeInvoicePdf(Uri uri) {
        try {
            if (pendingPdfItems == null || pendingPdfItems.isEmpty()) throw new Exception("اطلاعات فاکتور موجود نیست");
            PdfDocument pdf = new PdfDocument();
            final int pageWidth = 595;
            final int pageHeight = 842;
            final int margin = 36;
            final int rowHeight = 30;

            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.rgb(20, 36, 64));
            titlePaint.setTextSize(24);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.RIGHT);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(11);
            textPaint.setTextAlign(Paint.Align.RIGHT);

            Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boldPaint.setColor(Color.BLACK);
            boldPaint.setTextSize(11);
            boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            boldPaint.setTextAlign(Paint.Align.RIGHT);

            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(1);

            int pageNo = 1;
            PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create());
            Canvas canvas = page.getCanvas();
            int y = drawPdfHeader(canvas, titlePaint, textPaint, boldPaint, linePaint, margin, pageWidth, pendingInvoiceNo, pendingCustomerName, pendingCustomerPhone);

            double grandTotal = 0;
            int rowNo = 1;
            for (InvoiceItem item : pendingPdfItems) {
                if (y + rowHeight > pageHeight - 70) {
                    drawPdfPageFooter(canvas, textPaint, pageWidth, pageHeight, pageNo);
                    pdf.finishPage(page);
                    pageNo++;
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create());
                    canvas = page.getCanvas();
                    y = drawPdfTableHeader(canvas, boldPaint, linePaint, margin, pageWidth, 55);
                }
                grandTotal += item.lineTotal();
                float right = pageWidth - margin;
                float xTotal = right;
                float xUnit = right - 95;
                float xQty = right - 190;
                float xType = right - 245;
                float xName = right - 305;
                float baseY = y + 20;

                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(formatMoney(item.lineTotal()), xTotal, baseY, textPaint);
                canvas.drawText(formatMoney(item.unitPrice), xUnit, baseY, textPaint);
                canvas.drawText(String.valueOf(item.qty), xQty, baseY, textPaint);
                canvas.drawText(item.priceType, xType, baseY, textPaint);
                canvas.drawText(ellipsize(item.name, 23), xName, baseY, textPaint);
                textPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(String.valueOf(rowNo), margin, baseY, textPaint);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, linePaint);
                y += rowHeight;
                rowNo++;
            }

            if (y + 55 > pageHeight - 50) {
                drawPdfPageFooter(canvas, textPaint, pageWidth, pageHeight, pageNo);
                pdf.finishPage(page);
                pageNo++;
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create());
                canvas = page.getCanvas();
                y = 70;
            }
            boldPaint.setTextSize(16);
            boldPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("جمع کل: " + formatMoney(grandTotal), pageWidth - margin, y + 35, boldPaint);
            boldPaint.setTextSize(11);
            drawPdfPageFooter(canvas, textPaint, pageWidth, pageHeight, pageNo);
            pdf.finishPage(page);

            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("امکان ایجاد فایل وجود ندارد");
                pdf.writeTo(out);
            }
            pdf.close();
            runOnUiThread(() -> Toast.makeText(this, "فایل PDF فاکتور با موفقیت ذخیره شد.", Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "خطا در ساخت PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private int drawPdfHeader(Canvas canvas, Paint titlePaint, Paint textPaint, Paint boldPaint, Paint linePaint, int margin, int pageWidth, String invoiceNo, String customer, String phone) {
        int right = pageWidth - margin;
        canvas.drawText("فاکتور فروش", right, 55, titlePaint);
        String date = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US).format(new Date());
        canvas.drawText("شماره فاکتور: " + invoiceNo, right, 80, textPaint);
        canvas.drawText("تاریخ: " + date, right, 98, textPaint);
        int y = 118;
        if (!customer.isEmpty()) { canvas.drawText("مشتری: " + customer, right, y, textPaint); y += 18; }
        if (!phone.isEmpty()) { canvas.drawText("تماس: " + phone, right, y, textPaint); y += 18; }
        return drawPdfTableHeader(canvas, boldPaint, linePaint, margin, pageWidth, y + 8);
    }

    private int drawPdfTableHeader(Canvas canvas, Paint boldPaint, Paint linePaint, int margin, int pageWidth, int y) {
        float right = pageWidth - margin;
        float baseY = y + 20;
        boldPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("جمع", right, baseY, boldPaint);
        canvas.drawText("قیمت واحد", right - 95, baseY, boldPaint);
        canvas.drawText("تعداد", right - 190, baseY, boldPaint);
        canvas.drawText("نوع", right - 245, baseY, boldPaint);
        canvas.drawText("کالا", right - 305, baseY, boldPaint);
        boldPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("ردیف", margin, baseY, boldPaint);
        boldPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawLine(margin, y + 30, pageWidth - margin, y + 30, linePaint);
        return y + 30;
    }

    private void drawPdfPageFooter(Canvas canvas, Paint textPaint, int pageWidth, int pageHeight, int pageNo) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(9);
        textPaint.setColor(Color.GRAY);
        canvas.drawText("صفحه " + pageNo, pageWidth / 2f, pageHeight - 24, textPaint);
        textPaint.setTextSize(11);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }

    private String ellipsize(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private double parseNumber(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().replace(",", "").replace("٬", "").replace(" ", "");
        s = convertPersianDigits(s);
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private String convertPersianDigits(String s) {
        if (s == null) return "";
        char[] fa = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        char[] ar = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
        String out = s;
        for (int i = 0; i < 10; i++) {
            out = out.replace(fa[i], (char)('0' + i));
            out = out.replace(ar[i], (char)('0' + i));
        }
        return out;
    }

    private String formatNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        String s = convertPersianDigits(raw.trim().replace(",", "").replace("٬", ""));
        try {
            double d = Double.parseDouble(s);
            if (Math.rint(d) == d) return new DecimalFormat("#,###").format((long) d);
            return new DecimalFormat("#,##0.##").format(d);
        } catch (Exception e) { return raw; }
    }

    private String formatMoney(double value) {
        if (Math.rint(value) == value) return new DecimalFormat("#,###").format((long) value);
        return new DecimalFormat("#,##0.##").format(value);
    }

    static class Product {
        final String name, retail, wholesale;
        Product(String n, String r, String w) { name = n; retail = r; wholesale = w; }
    }

    static class InvoiceItem {
        final String name, priceType;
        final double unitPrice;
        int qty;
        InvoiceItem(String name, String priceType, double unitPrice, int qty) {
            this.name = name; this.priceType = priceType; this.unitPrice = unitPrice; this.qty = qty;
        }
        double lineTotal() { return unitPrice * qty; }
    }

    class ProductDb extends SQLiteOpenHelper {
        ProductDb() { super(MainActivity.this, "products.db", null, 1); }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, retail TEXT, wholesale TEXT)");
            db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        }
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
        long count() { return DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM products", null); }
        void replaceAll(List<Product> items) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                d.delete("products", null, null);
                for (Product p : items) {
                    ContentValues v = new ContentValues();
                    v.put("name", p.name); v.put("retail", p.retail); v.put("wholesale", p.wholesale);
                    d.insertOrThrow("products", null, v);
                }
                d.setTransactionSuccessful();
            } finally { d.endTransaction(); }
        }
        List<Product> search(String q) {
            ArrayList<Product> out = new ArrayList<>();
            Cursor c;
            if (q.isEmpty()) {
                c = getReadableDatabase().rawQuery("SELECT name,retail,wholesale FROM products ORDER BY name COLLATE NOCASE LIMIT 150", null);
            } else {
                c = getReadableDatabase().rawQuery("SELECT name,retail,wholesale FROM products WHERE name LIKE ? ORDER BY name COLLATE NOCASE LIMIT 150", new String[]{"%" + q + "%"});
            }
            try {
                while (c.moveToNext()) out.add(new Product(c.getString(0), c.getString(1), c.getString(2)));
            } finally { c.close(); }
            return out;
        }
    }

    static class Xlsx {
        static List<Product> read(File file) throws Exception {
            try (ZipFile zip = new ZipFile(file)) {
                ArrayList<String> shared = readShared(zip);
                ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
                if (sheet == null) {
                    for (java.util.Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                        ZipEntry z = e.nextElement();
                        if (z.getName().startsWith("xl/worksheets/") && z.getName().endsWith(".xml")) { sheet = z; break; }
                    }
                }
                if (sheet == null) throw new Exception("Worksheet پیدا نشد");
                Document doc = parse(zip.getInputStream(sheet));
                NodeList rows = doc.getElementsByTagName("row");
                ArrayList<Product> result = new ArrayList<>();
                for (int i = 0; i < rows.getLength(); i++) {
                    Element row = (Element) rows.item(i);
                    String[] v = new String[]{"", "", ""};
                    NodeList cells = row.getElementsByTagName("c");
                    for (int j = 0; j < cells.getLength(); j++) {
                        Element c = (Element) cells.item(j);
                        int col = columnIndex(c.getAttribute("r"));
                        if (col >= 0 && col < 3) v[col] = cellValue(c, shared);
                    }
                    String name = clean(v[0]);
                    if (name.isEmpty()) continue;
                    if (i == 0 && isHeader(v)) continue;
                    result.add(new Product(name, clean(v[1]), clean(v[2])));
                }
                return result;
            }
        }

        static boolean isHeader(String[] v) {
            String a = clean(v[0]); String b = clean(v[1]); String c = clean(v[2]);
            return (a.contains("نام") && a.contains("کالا")) || b.contains("خرده") || c.contains("عمده");
        }

        static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim(); }

        static ArrayList<String> readShared(ZipFile zip) throws Exception {
            ArrayList<String> out = new ArrayList<>();
            ZipEntry e = zip.getEntry("xl/sharedStrings.xml");
            if (e == null) return out;
            Document d = parse(zip.getInputStream(e));
            NodeList sis = d.getElementsByTagName("si");
            for (int i = 0; i < sis.getLength(); i++) {
                NodeList ts = ((Element) sis.item(i)).getElementsByTagName("t");
                StringBuilder s = new StringBuilder();
                for (int j = 0; j < ts.getLength(); j++) s.append(ts.item(j).getTextContent());
                out.add(s.toString());
            }
            return out;
        }

        static String cellValue(Element c, ArrayList<String> shared) {
            String type = c.getAttribute("t");
            if ("inlineStr".equals(type)) {
                NodeList ts = c.getElementsByTagName("t");
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < ts.getLength(); i++) s.append(ts.item(i).getTextContent());
                return s.toString();
            }
            NodeList vs = c.getElementsByTagName("v");
            if (vs.getLength() == 0) return "";
            String v = vs.item(0).getTextContent();
            if ("s".equals(type)) {
                try {
                    int idx = Integer.parseInt(v);
                    return idx >= 0 && idx < shared.size() ? shared.get(idx) : "";
                } catch (Exception ignored) { return ""; }
            }
            return v;
        }

        static int columnIndex(String ref) {
            int col = 0; int i = 0;
            while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
                col = col * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1); i++;
            }
            return col - 1;
        }

        static Document parse(InputStream in) throws Exception {
            try (InputStream input = in) {
                DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                f.setFeature("http://xml.org/sax/features/external-general-entities", false);
                f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                f.setExpandEntityReferences(false);
                return f.newDocumentBuilder().parse(input);
            }
        }
    }
}
