package com.example.excelpricesearch;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends Activity {
    private static final int PICK_XLSX = 1001;
    private ProductDb db;
    private EditText search;
    private TextView status;
    private LinearLayout results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(25, 50, 90));
        db = new ProductDb();
        setContentView(buildUi());
        updateStatus();
        renderProducts("");
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(12));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.rgb(246, 248, 252));

        TextView title = new TextView(this);
        title.setText("جستجوی قیمت کالا");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(20, 36, 64));
        title.setGravity(Gravity.RIGHT);
        title.setTypeface(null, 1);
        root.addView(title, lp(-1, -2, 0, 0, 0, 12));

        TextView hint = new TextView(this);
        hint.setText("فایل Excel با سه ستون: نام کالا، قیمت خرده، قیمت عمده");
        hint.setTextSize(14);
        hint.setTextColor(Color.DKGRAY);
        hint.setGravity(Gravity.RIGHT);
        root.addView(hint, lp(-1, -2, 0, 0, 0, 14));

        Button choose = new Button(this);
        choose.setText("انتخاب فایل Excel (.xlsx)");
        choose.setTextSize(16);
        choose.setOnClickListener(v -> pickFile());
        root.addView(choose, lp(-1, dp(52), 0, 0, 0, 14));

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
        root.addView(search, lp(-1, dp(52), 0, 0, 0, 10));

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(Color.GRAY);
        status.setGravity(Gravity.RIGHT);
        root.addView(status, lp(-1, -2, 0, 0, 0, 10));

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
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
            status.setText("در حال خواندن فایل...");
            new Thread(() -> importXlsx(uri)).start();
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
                search.setText("");
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
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(225, 229, 236));
        card.setBackground(bg);

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextSize(18);
        name.setTextColor(Color.rgb(20, 36, 64));
        name.setTypeface(null, 1);
        name.setGravity(Gravity.RIGHT);
        card.addView(name);

        TextView retail = priceLine("قیمت خرده: ", p.retail, Color.rgb(25, 110, 75));
        card.addView(retail, lp(-1, -2, 0, 8, 0, 0));

        TextView wholesale = priceLine("قیمت عمده: ", p.wholesale, Color.rgb(35, 80, 150));
        card.addView(wholesale, lp(-1, -2, 0, 4, 0, 0));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cp);
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

    private String formatNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        String s = raw.trim().replace(",", "");
        try {
            double d = Double.parseDouble(s);
            if (Math.rint(d) == d) return new DecimalFormat("#,###").format((long) d);
            return new DecimalFormat("#,##0.##").format(d);
        } catch (Exception e) { return raw; }
    }

    static class Product {
        final String name, retail, wholesale;
        Product(String n, String r, String w) { name = n; retail = r; wholesale = w; }
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
                f.setNamespaceAware(false);
                try { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignored) {}
                return f.newDocumentBuilder().parse(input);
            }
        }
    }
}
