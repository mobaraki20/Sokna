package com.example.excelpricesearch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppDb extends SQLiteOpenHelper {
    public static class Product {
        public long id;
        public String name;
        public double purchase;
        public double retail;
        public double wholesale;
        public int stock;

        public Product(long id, String name, double purchase, double retail, double wholesale, int stock) {
            this.id = id;
            this.name = name;
            this.purchase = purchase;
            this.retail = retail;
            this.wholesale = wholesale;
            this.stock = stock;
        }
    }

    public static class Customer {
        public long id;
        public String name;
        public String phone;
        public String address;

        public Customer(long id, String name, String phone, String address) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.address = address;
        }
    }

    public static class SaleLine {
        public long productId;
        public String productName;
        public double unitPrice;
        public int qty;
        public String saleType;

        public SaleLine(long productId, String productName, double unitPrice, int qty, String saleType) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.qty = qty;
            this.saleType = saleType;
        }
    }

    public AppDb(Context context) {
        super(context, "products.db", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, retail REAL NOT NULL DEFAULT 0, wholesale REAL NOT NULL DEFAULT 0, purchase REAL NOT NULL DEFAULT 0, stock INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, address TEXT)");
        db.execSQL("CREATE INDEX idx_customers_name ON customers(name)");
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_no TEXT NOT NULL, customer_id INTEGER, customer_name TEXT, customer_phone TEXT, total REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE sale_items(id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER, product_name TEXT NOT NULL, sale_type TEXT, qty INTEGER NOT NULL, unit_price REAL NOT NULL, line_total REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE products ADD COLUMN purchase REAL NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, address TEXT)"); } catch (Exception ignored) {}
            try { db.execSQL("CREATE INDEX idx_customers_name ON customers(name)"); } catch (Exception ignored) {}
        }
        if (oldVersion < 3) {
            try { db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_no TEXT NOT NULL, customer_id INTEGER, customer_name TEXT, customer_phone TEXT, total REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)"); } catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE sale_items(id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER, product_name TEXT NOT NULL, sale_type TEXT, qty INTEGER NOT NULL, unit_price REAL NOT NULL, line_total REAL NOT NULL)"); } catch (Exception ignored) {}
        }
    }

    public long productCount() {
        return DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM products", null);
    }

    public long customerCount() {
        return DatabaseUtils.longForQuery(getReadableDatabase(), "SELECT COUNT(*) FROM customers", null);
    }

    public long saveProduct(Product p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name.trim());
        v.put("purchase", Math.max(0, p.purchase));
        v.put("retail", Math.max(0, p.retail));
        v.put("wholesale", Math.max(0, p.wholesale));
        v.put("stock", Math.max(0, p.stock));
        if (p.id > 0) {
            getWritableDatabase().update("products", v, "id=?", new String[]{String.valueOf(p.id)});
            return p.id;
        }
        return getWritableDatabase().insertOrThrow("products", null, v);
    }

    public void deleteProduct(long id) {
        getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Product> searchProducts(String query) {
        ArrayList<Product> out = new ArrayList<>();
        Cursor c;
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            c = getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock FROM products ORDER BY name COLLATE NOCASE LIMIT 300", null);
        } else {
            c = getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock FROM products WHERE name LIKE ? ORDER BY CASE WHEN name=? THEN 0 WHEN name LIKE ? THEN 1 ELSE 2 END, name COLLATE NOCASE LIMIT 300", new String[]{"%" + q + "%", q, q + "%"});
        }
        try {
            while (c.moveToNext()) {
                out.add(new Product(c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getInt(5)));
            }
        } finally { c.close(); }
        return out;
    }

    public Product getProduct(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock FROM products WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (!c.moveToFirst()) return null;
            return new Product(c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getInt(5));
        } finally { c.close(); }
    }

    public void replaceProducts(List<Product> items) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            d.delete("products", null, null);
            for (Product p : items) {
                ContentValues v = new ContentValues();
                v.put("name", p.name);
                v.put("purchase", Math.max(0, p.purchase));
                v.put("retail", Math.max(0, p.retail));
                v.put("wholesale", Math.max(0, p.wholesale));
                v.put("stock", Math.max(0, p.stock));
                d.insertOrThrow("products", null, v);
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
    }

    public long saveCustomer(Customer c) {
        ContentValues v = new ContentValues();
        v.put("name", c.name.trim());
        v.put("phone", c.phone == null ? "" : c.phone.trim());
        v.put("address", c.address == null ? "" : c.address.trim());
        if (c.id > 0) {
            getWritableDatabase().update("customers", v, "id=?", new String[]{String.valueOf(c.id)});
            return c.id;
        }
        return getWritableDatabase().insertOrThrow("customers", null, v);
    }

    public void deleteCustomer(long id) {
        getWritableDatabase().delete("customers", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Customer> searchCustomers(String query) {
        ArrayList<Customer> out = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        Cursor c;
        if (q.isEmpty()) {
            c = getReadableDatabase().rawQuery("SELECT id,name,phone,address FROM customers ORDER BY name COLLATE NOCASE LIMIT 300", null);
        } else {
            c = getReadableDatabase().rawQuery("SELECT id,name,phone,address FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name COLLATE NOCASE LIMIT 300", new String[]{"%" + q + "%", "%" + q + "%"});
        }
        try {
            while (c.moveToNext()) out.add(new Customer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)));
        } finally { c.close(); }
        return out;
    }

    public boolean canSell(List<SaleLine> lines) {
        SQLiteDatabase d = getReadableDatabase();
        for (SaleLine line : lines) {
            Cursor c = d.rawQuery("SELECT stock FROM products WHERE id=?", new String[]{String.valueOf(line.productId)});
            try {
                if (!c.moveToFirst() || c.getInt(0) < line.qty) return false;
            } finally { c.close(); }
        }
        return true;
    }

    public long saveSale(String invoiceNo, Customer customer, List<SaleLine> lines) throws Exception {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            double total = 0;
            for (SaleLine line : lines) {
                Cursor c = d.rawQuery("SELECT stock FROM products WHERE id=?", new String[]{String.valueOf(line.productId)});
                int stock;
                try {
                    if (!c.moveToFirst()) throw new Exception("کالا پیدا نشد: " + line.productName);
                    stock = c.getInt(0);
                } finally { c.close(); }
                if (stock < line.qty) throw new Exception("موجودی کافی نیست: " + line.productName + " (موجودی " + stock + ")");
                total += line.unitPrice * line.qty;
            }

            ContentValues sale = new ContentValues();
            sale.put("invoice_no", invoiceNo);
            if (customer != null && customer.id > 0) sale.put("customer_id", customer.id);
            sale.put("customer_name", customer == null ? "" : customer.name);
            sale.put("customer_phone", customer == null ? "" : customer.phone);
            sale.put("total", total);
            sale.put("created_at", System.currentTimeMillis());
            long saleId = d.insertOrThrow("sales", null, sale);

            for (SaleLine line : lines) {
                d.execSQL("UPDATE products SET stock=stock-? WHERE id=?", new Object[]{line.qty, line.productId});
                ContentValues item = new ContentValues();
                item.put("sale_id", saleId);
                item.put("product_id", line.productId);
                item.put("product_name", line.productName);
                item.put("sale_type", line.saleType);
                item.put("qty", line.qty);
                item.put("unit_price", line.unitPrice);
                item.put("line_total", line.unitPrice * line.qty);
                d.insertOrThrow("sale_items", null, item);
            }
            d.setTransactionSuccessful();
            return saleId;
        } finally { d.endTransaction(); }
    }
}
