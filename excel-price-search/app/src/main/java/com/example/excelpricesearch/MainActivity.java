package com.example.excelpricesearch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_XLSX = 1001;
    private static final int SAVE_PDF = 2002;

    private AppDb db;
    private LinearLayout content;
    private Button productsTab, customersTab, invoiceTab;

    private EditText productSearch, customerSearch;
    private TextView productStatus, customerStatus;
    private LinearLayout productResults, customerResults, invoiceItemsContainer;
    private TextView invoiceTotal, selectedCustomerText, invoiceNoText;

    private final ArrayList<AppDb.SaleLine> invoiceItems = new ArrayList<>();
    private AppDb.Customer selectedCustomer;
    private String currentInvoiceNo;

    private ArrayList<AppDb.SaleLine> pendingPdfLines;
    private AppDb.Customer pendingPdfCustomer;
    private String pendingPdfInvoiceNo;
    private long pendingPdfTime;

    private ArrayList<AppDb.SaleLine> lastSaleLines;
    private AppDb.Customer lastSaleCustomer;
    private String lastSaleInvoiceNo;
    private long lastSaleTime;

    private int currentScreen = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(25, 50, 90));
        db = new AppDb(this);
        currentInvoiceNo = newInvoiceNo();
        setContentView(buildUi());
        showProductsScreen();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(14), dp(12), dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.rgb(246, 248, 252));

        TextView title = new TextView(this);
        title.setText("مدیریت فروش و انبار");
        title.setTextSize(23);
        title.setTextColor(Color.rgb(20, 36, 64));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT);
        root.addView(title, lp(-1, -2, 0, 0, 0, 10));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        productsTab = new Button(this);
        productsTab.setText("کالا و انبار");
        productsTab.setTextSize(12);
        productsTab.setOnClickListener(v -> showProductsScreen());
        tabs.addView(productsTab, new LinearLayout.LayoutParams(0, dp(48), 1));

        customersTab = new Button(this);
        customersTab.setText("مشتریان");
        customersTab.setTextSize(12);
        customersTab.setOnClickListener(v -> showCustomersScreen());
        LinearLayout.LayoutParams ct = new LinearLayout.LayoutParams(0, dp(48), 1);
        ct.setMargins(dp(5), 0, 0, 0);
        tabs.addView(customersTab, ct);

        invoiceTab = new Button(this);
        invoiceTab.setText("فاکتور");
        invoiceTab.setTextSize(12);
        invoiceTab.setOnClickListener(v -> showInvoiceScreen());
        LinearLayout.LayoutParams it = new LinearLayout.LayoutParams(0, dp(48), 1);
        it.setMargins(dp(5), 0, 0, 0);
        tabs.addView(invoiceTab, it);

        root.addView(tabs, lp(-1, -2, 0, 0, 0, 10));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        updateInvoiceTab();
        return root;
    }

    private void showProductsScreen() {
        currentScreen = 0;
        content.removeAllViews();

        TextView hint = label("Excel: نام کالا، قیمت خرده، قیمت عمده — ستون ۴ قیمت خرید و ستون ۵ موجودی اختیاری است.", 13, Color.DKGRAY, false);
        content.addView(hint, lp(-1, -2, 0, 0, 0, 8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button add = new Button(this);
        add.setText("+ تعریف کالا");
        add.setOnClickListener(v -> showProductDialog(null));
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button excel = new Button(this);
        excel.setText("ورود / بروزرسانی Excel");
        excel.setOnClickListener(v -> pickFile());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dp(48), 1);
        ep.setMargins(dp(6), 0, 0, 0);
        actions.addView(excel, ep);
        content.addView(actions, lp(-1, -2, 0, 0, 0, 8));

        productSearch = searchBox("جستجوی نام کالا...");
        productSearch.addTextChangedListener(new SimpleWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderProducts(s.toString()); }
        });
        content.addView(productSearch, lp(-1, dp(50), 0, 0, 0, 7));

        productStatus = label("", 13, Color.GRAY, false);
        content.addView(productStatus, lp(-1, -2, 0, 0, 0, 7));

        ScrollView scroll = new ScrollView(this);
        productResults = new LinearLayout(this);
        productResults.setOrientation(LinearLayout.VERTICAL);
        productResults.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(productResults, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        updateProductStatus();
        renderProducts("");
        setTabState(0);
    }

    private void showCustomersScreen() {
        currentScreen = 1;
        content.removeAllViews();

        Button add = new Button(this);
        add.setText("+ تعریف مشتری جدید");
        add.setOnClickListener(v -> showCustomerDialog(null));
        content.addView(add, lp(-1, dp(48), 0, 0, 0, 8));

        customerSearch = searchBox("جستجو با نام یا شماره تماس...");
        customerSearch.addTextChangedListener(new SimpleWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderCustomers(s.toString()); }
        });
        content.addView(customerSearch, lp(-1, dp(50), 0, 0, 0, 7));

        customerStatus = label("", 13, Color.GRAY, false);
        content.addView(customerStatus, lp(-1, -2, 0, 0, 0, 7));

        ScrollView scroll = new ScrollView(this);
        customerResults = new LinearLayout(this);
        customerResults.setOrientation(LinearLayout.VERTICAL);
        customerResults.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(customerResults, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        updateCustomerStatus();
        renderCustomers("");
        setTabState(1);
    }

    private void showInvoiceScreen() {
        currentScreen = 2;
        content.removeAllViews();

        invoiceNoText = label("شماره فاکتور: " + currentInvoiceNo, 13, Color.GRAY, false);
        content.addView(invoiceNoText, lp(-1, -2, 0, 0, 0, 5));

        selectedCustomerText = label(customerText(), 15, Color.rgb(20, 36, 64), true);
        content.addView(selectedCustomerText, lp(-1, -2, 0, 0, 0, 5));

        Button chooseCustomer = new Button(this);
        chooseCustomer.setText("انتخاب مشتری از لیست");
        chooseCustomer.setOnClickListener(v -> showCustomerPicker());
        content.addView(chooseCustomer, lp(-1, dp(46), 0, 0, 0, 7));

        ScrollView scroll = new ScrollView(this);
        invoiceItemsContainer = new LinearLayout(this);
        invoiceItemsContainer.setOrientation(LinearLayout.VERTICAL);
        invoiceItemsContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(invoiceItemsContainer, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        invoiceTotal = label("", 20, Color.rgb(20, 92, 65), true);
        content.addView(invoiceTotal, lp(-1, -2, 0, 7, 0, 5));

        Button save = new Button(this);
        save.setText("ثبت فروش و کسر از موجودی");
        save.setOnClickListener(v -> saveSale());
        content.addView(save, lp(-1, dp(50), 0, 0, 0, 5));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button pdf = new Button(this);
        pdf.setText("صدور PDF");
        pdf.setOnClickListener(v -> requestSavePdf());
        row.addView(pdf, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button clear = new Button(this);
        clear.setText("فاکتور جدید / پاک کردن");
        clear.setOnClickListener(v -> clearInvoice());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(48), 1);
        clp.setMargins(dp(6), 0, 0, 0);
        row.addView(clear, clp);
        content.addView(row, lp(-1, -2, 0, 0, 0, 0));

        renderInvoice();
        setTabState(2);
    }

    private void setTabState(int active) {
        productsTab.setEnabled(active != 0);
        customersTab.setEnabled(active != 1);
        invoiceTab.setEnabled(active != 2);
    }

    private void updateInvoiceTab() {
        if (invoiceTab == null) return;
        int count = 0;
        for (AppDb.SaleLine i : invoiceItems) count += i.qty;
        invoiceTab.setText(count == 0 ? "فاکتور" : "فاکتور (" + count + ")");
    }

    private void updateProductStatus() {
        if (productStatus != null) productStatus.setText("تعداد کالاها: " + number(db.productCount()));
    }

    private void updateCustomerStatus() {
        if (customerStatus != null) customerStatus.setText("تعداد مشتریان: " + number(db.customerCount()));
    }

    private void renderProducts(String q) {
        if (productResults == null) return;
        productResults.removeAllViews();
        List<AppDb.Product> list = db.searchProducts(q);
        if (list.isEmpty()) {
            TextView empty = label(db.productCount() == 0 ? "هنوز کالایی تعریف نشده است." : "کالایی پیدا نشد.", 16, Color.GRAY, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, 0);
            productResults.addView(empty);
            return;
        }
        for (AppDb.Product p : list) productResults.addView(productCard(p));
    }

    private View productCard(AppDb.Product p) {
        LinearLayout card = card();

        TextView name = label(p.name, 18, Color.rgb(20, 36, 64), true);
        card.addView(name);

        card.addView(label("قیمت خرید: " + money(p.purchase), 14, Color.rgb(105, 85, 20), false), lp(-1, -2, 0, 6, 0, 0));
        card.addView(label("قیمت خرده: " + money(p.retail), 15, Color.rgb(25, 110, 75), false), lp(-1, -2, 0, 3, 0, 0));
        card.addView(label("قیمت عمده: " + money(p.wholesale), 15, Color.rgb(35, 80, 150), false), lp(-1, -2, 0, 3, 0, 0));
        int stockColor = p.stock <= 0 ? Color.rgb(180, 45, 45) : Color.rgb(80, 80, 80);
        card.addView(label("موجودی انبار: " + number(p.stock), 15, stockColor, true), lp(-1, -2, 0, 3, 0, 7));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button retail = smallButton("فروش خرده");
        retail.setEnabled(p.stock > reservedQty(p.id));
        retail.setOnClickListener(v -> addToInvoice(p, p.retail, "خرده"));
        row1.addView(retail, new LinearLayout.LayoutParams(0, dp(43), 1));

        Button wholesale = smallButton("فروش عمده");
        wholesale.setEnabled(p.stock > reservedQty(p.id));
        wholesale.setOnClickListener(v -> addToInvoice(p, p.wholesale, "عمده"));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, dp(43), 1);
        wp.setMargins(dp(5), 0, 0, 0);
        row1.addView(wholesale, wp);
        card.addView(row1);

        Button edit = smallButton("ویرایش کالا / موجودی");
        edit.setOnClickListener(v -> showProductDialog(p));
        card.addView(edit, lp(-1, dp(43), 0, 5, 0, 0));
        return card;
    }

    private void showProductDialog(AppDb.Product existing) {
        boolean edit = existing != null;
        LinearLayout form = formLayout();
        EditText name = field("نام کالا", edit ? existing.name : "", InputType.TYPE_CLASS_TEXT);
        EditText purchase = field("قیمت خرید", edit ? plain(existing.purchase) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText retail = field("قیمت خرده", edit ? plain(existing.retail) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText wholesale = field("قیمت عمده", edit ? plain(existing.wholesale) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText stock = field("موجودی انبار", edit ? String.valueOf(existing.stock) : "0", InputType.TYPE_CLASS_NUMBER);
        form.addView(name); form.addView(purchase); form.addView(retail); form.addView(wholesale); form.addView(stock);

        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(edit ? "ویرایش کالا" : "تعریف کالا")
                .setView(sv)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) { toast("نام کالا را وارد کنید"); return; }
                    AppDb.Product p = new AppDb.Product(edit ? existing.id : 0, n, val(purchase), val(retail), val(wholesale), intVal(stock));
                    db.saveProduct(p);
                    toast(edit ? "کالا بروزرسانی شد" : "کالا ثبت شد");
                    if (currentScreen == 0) {
                        updateProductStatus();
                        renderProducts(productSearch == null ? "" : productSearch.getText().toString());
                    }
                });
        if (edit) {
            b.setNeutralButton("حذف کالا", (d, w) -> new AlertDialog.Builder(this)
                    .setMessage("این کالا حذف شود؟")
                    .setNegativeButton("خیر", null)
                    .setPositiveButton("حذف", (x, y) -> {
                        db.deleteProduct(existing.id);
                        for (int i = invoiceItems.size() - 1; i >= 0; i--) if (invoiceItems.get(i).productId == existing.id) invoiceItems.remove(i);
                        updateInvoiceTab();
                        updateProductStatus();
                        renderProducts(productSearch == null ? "" : productSearch.getText().toString());
                    }).show());
        }
        b.show();
    }

    private void renderCustomers(String q) {
        if (customerResults == null) return;
        customerResults.removeAllViews();
        List<AppDb.Customer> list = db.searchCustomers(q);
        if (list.isEmpty()) {
            TextView empty = label(db.customerCount() == 0 ? "هنوز مشتری تعریف نشده است." : "مشتری پیدا نشد.", 16, Color.GRAY, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, 0);
            customerResults.addView(empty);
            return;
        }
        for (AppDb.Customer c : list) customerResults.addView(customerCard(c));
    }

    private View customerCard(AppDb.Customer c) {
        LinearLayout card = card();
        card.addView(label(c.name, 18, Color.rgb(20, 36, 64), true));
        if (c.phone != null && !c.phone.isEmpty()) card.addView(label("تلفن: " + c.phone, 14, Color.DKGRAY, false), lp(-1, -2, 0, 5, 0, 0));
        if (c.address != null && !c.address.isEmpty()) card.addView(label("آدرس: " + c.address, 14, Color.DKGRAY, false), lp(-1, -2, 0, 3, 0, 5));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button select = smallButton("انتخاب برای فاکتور");
        select.setOnClickListener(v -> {
            selectedCustomer = c;
            toast("مشتری برای فاکتور انتخاب شد");
            showInvoiceScreen();
        });
        row.addView(select, new LinearLayout.LayoutParams(0, dp(43), 1));
        Button edit = smallButton("ویرایش");
        edit.setOnClickListener(v -> showCustomerDialog(c));
        LinearLayout.LayoutParams e = new LinearLayout.LayoutParams(0, dp(43), 1);
        e.setMargins(dp(5), 0, 0, 0);
        row.addView(edit, e);
        card.addView(row);
        return card;
    }

    private void showCustomerDialog(AppDb.Customer existing) {
        boolean edit = existing != null;
        LinearLayout form = formLayout();
        EditText name = field("نام مشتری", edit ? existing.name : "", InputType.TYPE_CLASS_TEXT);
        EditText phone = field("شماره تماس", edit ? existing.phone : "", InputType.TYPE_CLASS_PHONE);
        EditText address = field("آدرس", edit ? existing.address : "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        address.setSingleLine(false);
        address.setMinLines(2);
        form.addView(name); form.addView(phone); form.addView(address);
        ScrollView sv = new ScrollView(this); sv.addView(form);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(edit ? "ویرایش مشتری" : "تعریف مشتری")
                .setView(sv)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) { toast("نام مشتری را وارد کنید"); return; }
                    AppDb.Customer c = new AppDb.Customer(edit ? existing.id : 0, n, phone.getText().toString().trim(), address.getText().toString().trim());
                    long id = db.saveCustomer(c);
                    if (!edit) c.id = id;
                    toast(edit ? "مشتری بروزرسانی شد" : "مشتری ثبت شد");
                    if (selectedCustomer != null && edit && selectedCustomer.id == existing.id) selectedCustomer = c;
                    if (currentScreen == 1) {
                        updateCustomerStatus();
                        renderCustomers(customerSearch == null ? "" : customerSearch.getText().toString());
                    }
                });
        if (edit) {
            b.setNeutralButton("حذف مشتری", (d, w) -> new AlertDialog.Builder(this)
                    .setMessage("این مشتری حذف شود؟")
                    .setNegativeButton("خیر", null)
                    .setPositiveButton("حذف", (x, y) -> {
                        db.deleteCustomer(existing.id);
                        if (selectedCustomer != null && selectedCustomer.id == existing.id) selectedCustomer = null;
                        updateCustomerStatus();
                        renderCustomers(customerSearch == null ? "" : customerSearch.getText().toString());
                    }).show());
        }
        b.show();
    }

    private void showCustomerPicker() {
        List<AppDb.Customer> list = db.searchCustomers("");
        String[] names = new String[list.size() + 1];
        names[0] = "بدون مشتری";
        for (int i = 0; i < list.size(); i++) {
            AppDb.Customer c = list.get(i);
            names[i + 1] = c.name + (c.phone == null || c.phone.isEmpty() ? "" : " — " + c.phone);
        }
        new AlertDialog.Builder(this)
                .setTitle("انتخاب مشتری")
                .setItems(names, (d, which) -> {
                    selectedCustomer = which == 0 ? null : list.get(which - 1);
                    if (selectedCustomerText != null) selectedCustomerText.setText(customerText());
                })
                .setNegativeButton("بستن", null)
                .show();
    }

    private String customerText() {
        if (selectedCustomer == null) return "مشتری: انتخاب نشده";
        return "مشتری: " + selectedCustomer.name + (selectedCustomer.phone == null || selectedCustomer.phone.isEmpty() ? "" : " | " + selectedCustomer.phone);
    }

    private int reservedQty(long productId) {
        int n = 0;
        for (AppDb.SaleLine l : invoiceItems) if (l.productId == productId) n += l.qty;
        return n;
    }

    private void addToInvoice(AppDb.Product p, double price, String saleType) {
        int reserved = reservedQty(p.id);
        if (p.stock <= reserved) {
            toast("موجودی کافی نیست. موجودی: " + p.stock);
            return;
        }
        for (AppDb.SaleLine l : invoiceItems) {
            if (l.productId == p.id && l.saleType.equals(saleType) && Math.abs(l.unitPrice - price) < 0.0001) {
                l.qty++;
                updateInvoiceTab();
                toast("به فاکتور اضافه شد");
                if (currentScreen == 0) renderProducts(productSearch == null ? "" : productSearch.getText().toString());
                return;
            }
        }
        invoiceItems.add(new AppDb.SaleLine(p.id, p.name, price, 1, saleType));
        updateInvoiceTab();
        toast("به فاکتور اضافه شد");
        if (currentScreen == 0) renderProducts(productSearch == null ? "" : productSearch.getText().toString());
    }

    private void renderInvoice() {
        if (invoiceItemsContainer == null) return;
        invoiceItemsContainer.removeAllViews();
        if (invoiceItems.isEmpty()) {
            TextView empty = label(lastSaleLines == null ? "فاکتور خالی است. از بخش کالا، محصول را به فاکتور اضافه کنید." : "فاکتور جدید خالی است. آخرین فروش ثبت‌شده برای صدور PDF هنوز در دسترس است.", 15, Color.GRAY, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(25), dp(8), dp(25));
            invoiceItemsContainer.addView(empty);
        } else {
            for (int i = 0; i < invoiceItems.size(); i++) invoiceItemsContainer.addView(invoiceLine(i));
        }
        if (invoiceTotal != null) invoiceTotal.setText("جمع کل: " + money(invoiceGrandTotal()));
        updateInvoiceTab();
    }

    private View invoiceLine(int index) {
        AppDb.SaleLine item = invoiceItems.get(index);
        LinearLayout card = card();
        AppDb.Product current = db.getProduct(item.productId);
        card.addView(label(item.productName + " — " + item.saleType, 17, Color.rgb(20, 36, 64), true));
        card.addView(label("قیمت واحد: " + money(item.unitPrice) + " | جمع: " + money(item.unitPrice * item.qty), 14, Color.DKGRAY, false), lp(-1, -2, 0, 5, 0, 2));
        if (current != null) card.addView(label("موجودی فعلی: " + current.stock, 13, Color.GRAY, false));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button plus = smallButton("+");
        plus.setOnClickListener(v -> {
            AppDb.Product p = db.getProduct(item.productId);
            if (p == null || reservedQty(item.productId) >= p.stock) { toast("بیشتر از موجودی انبار نمی‌توان اضافه کرد"); return; }
            item.qty++;
            renderInvoice();
        });
        row.addView(plus, new LinearLayout.LayoutParams(dp(55), dp(42)));

        TextView qty = label("تعداد: " + item.qty, 16, Color.rgb(20, 36, 64), true);
        qty.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(0, dp(42), 1);
        qp.setMargins(dp(4), 0, dp(4), 0);
        row.addView(qty, qp);

        Button minus = smallButton("−");
        minus.setOnClickListener(v -> {
            if (item.qty > 1) item.qty--; else invoiceItems.remove(item);
            renderInvoice();
            updateInvoiceTab();
        });
        row.addView(minus, new LinearLayout.LayoutParams(dp(55), dp(42)));

        Button del = smallButton("حذف");
        del.setOnClickListener(v -> {
            invoiceItems.remove(item);
            renderInvoice();
            updateInvoiceTab();
        });
        LinearLayout.LayoutParams dpb = new LinearLayout.LayoutParams(dp(72), dp(42));
        dpb.setMargins(dp(5), 0, 0, 0);
        row.addView(del, dpb);
        card.addView(row, lp(-1, -2, 0, 7, 0, 0));
        return card;
    }

    private double invoiceGrandTotal() {
        double total = 0;
        for (AppDb.SaleLine i : invoiceItems) total += i.unitPrice * i.qty;
        return total;
    }

    private ArrayList<AppDb.SaleLine> copyLines(List<AppDb.SaleLine> src) {
        ArrayList<AppDb.SaleLine> out = new ArrayList<>();
        for (AppDb.SaleLine l : src) out.add(new AppDb.SaleLine(l.productId, l.productName, l.unitPrice, l.qty, l.saleType));
        return out;
    }

    private AppDb.Customer copyCustomer(AppDb.Customer c) {
        return c == null ? null : new AppDb.Customer(c.id, c.name, c.phone, c.address);
    }

    private void saveSale() {
        if (invoiceItems.isEmpty()) { toast("فاکتور خالی است"); return; }
        try {
            ArrayList<AppDb.SaleLine> snapshot = copyLines(invoiceItems);
            AppDb.Customer customerSnapshot = copyCustomer(selectedCustomer);
            long now = System.currentTimeMillis();
            db.saveSale(currentInvoiceNo, customerSnapshot, snapshot);
            lastSaleLines = snapshot;
            lastSaleCustomer = customerSnapshot;
            lastSaleInvoiceNo = currentInvoiceNo;
            lastSaleTime = now;
            invoiceItems.clear();
            selectedCustomer = null;
            currentInvoiceNo = newInvoiceNo();
            toast("فروش ثبت شد و موجودی انبار کسر شد");
            renderInvoice();
            if (invoiceNoText != null) invoiceNoText.setText("شماره فاکتور: " + currentInvoiceNo);
            if (selectedCustomerText != null) selectedCustomerText.setText(customerText());
        } catch (Exception e) {
            toast("ثبت فروش انجام نشد: " + e.getMessage());
        }
    }

    private void clearInvoice() {
        invoiceItems.clear();
        selectedCustomer = null;
        currentInvoiceNo = newInvoiceNo();
        renderInvoice();
        if (invoiceNoText != null) invoiceNoText.setText("شماره فاکتور: " + currentInvoiceNo);
        if (selectedCustomerText != null) selectedCustomerText.setText(customerText());
    }

    private void requestSavePdf() {
        if (!invoiceItems.isEmpty()) {
            pendingPdfLines = copyLines(invoiceItems);
            pendingPdfCustomer = copyCustomer(selectedCustomer);
            pendingPdfInvoiceNo = currentInvoiceNo;
            pendingPdfTime = System.currentTimeMillis();
        } else if (lastSaleLines != null && !lastSaleLines.isEmpty()) {
            pendingPdfLines = copyLines(lastSaleLines);
            pendingPdfCustomer = copyCustomer(lastSaleCustomer);
            pendingPdfInvoiceNo = lastSaleInvoiceNo;
            pendingPdfTime = lastSaleTime;
            toast("PDF آخرین فروش ثبت‌شده صادر می‌شود");
        } else {
            toast("فاکتوری برای صدور PDF وجود ندارد");
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE, "invoice_" + pendingPdfInvoiceNo + ".pdf");
        startActivityForResult(i, SAVE_PDF);
    }

    private void writePdf(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("امکان ساخت فایل وجود ندارد");
            InvoicePdf.write(out, pendingPdfInvoiceNo, pendingPdfCustomer, pendingPdfLines, pendingPdfTime);
            runOnUiThread(() -> toast("فایل PDF فاکتور ذخیره شد"));
        } catch (Exception e) {
            runOnUiThread(() -> toast("خطا در ساخت PDF: " + e.getMessage()));
        }
    }

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
            if (productStatus != null) productStatus.setText("در حال خواندن Excel...");
            new Thread(() -> importXlsx(uri)).start();
            return;
        }
        if (requestCode == SAVE_PDF && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            new Thread(() -> writePdf(uri)).start();
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
            List<AppDb.Product> items = XlsxReader.read(tmp);
            tmp.delete();
            if (items.isEmpty()) throw new Exception("کالایی در فایل پیدا نشد");
            db.importProducts(items);
            runOnUiThread(() -> {
                updateProductStatus();
                renderProducts(productSearch == null ? "" : productSearch.getText().toString());
                toast(items.size() + " ردیف Excel بروزرسانی/اضافه شد");
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                updateProductStatus();
                toast("خطا در خواندن Excel: " + e.getMessage());
            });
        }
    }

    private String newInvoiceNo() {
        return String.valueOf(System.currentTimeMillis());
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(11), dp(12), dp(11));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1), Color.rgb(225, 229, 236));
        card.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(9));
        card.setLayoutParams(cp);
        return card;
    }

    private LinearLayout formLayout() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return form;
    }

    private EditText field(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setInputType(inputType);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.setMargins(0, 0, 0, dp(7));
        e.setLayoutParams(p);
        return e;
    }

    private EditText searchBox(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(16);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), Color.rgb(210, 216, 226));
        bg.setCornerRadius(dp(12));
        e.setBackground(bg);
        return e;
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        return b;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private double val(EditText e) {
        return parseNumber(e.getText().toString());
    }

    private int intVal(EditText e) {
        return Math.max(0, (int) Math.round(parseNumber(e.getText().toString())));
    }

    private double parseNumber(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().replace(",", "").replace("٬", "").replace(" ", "");
        StringBuilder x = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '۰' && ch <= '۹') x.append((char) ('0' + ch - '۰'));
            else if (ch >= '٠' && ch <= '٩') x.append((char) ('0' + ch - '٠'));
            else x.append(ch);
        }
        try { return Double.parseDouble(x.toString()); } catch (Exception ignored) { return 0; }
    }

    private String money(double v) {
        return new DecimalFormat("#,###.##").format(v);
    }

    private String plain(double v) {
        if (Math.rint(v) == v) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private String number(long v) {
        return new DecimalFormat("#,###").format(v);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void afterTextChanged(Editable s) {}
    }
}
