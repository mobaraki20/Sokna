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
    public static final String DB_NAME = "products.db";
    private static final int DB_VERSION = 6;

    public static class Product {
        public long id; public String name; public double purchase; public double retail; public double wholesale; public int stock; public String unit; public int minStock;
        public Product(long id, String name, double purchase, double retail, double wholesale, int stock) { this(id,name,purchase,retail,wholesale,stock,"عدد",0); }
        public Product(long id, String name, double purchase, double retail, double wholesale, int stock, String unit, int minStock) {
            this.id=id; this.name=name; this.purchase=purchase; this.retail=retail; this.wholesale=wholesale; this.stock=stock; this.unit=unit==null?"عدد":unit; this.minStock=minStock;
        }
    }
    public static class Customer {
        public long id; public String name; public String phone; public String address; public double openingBalance;
        public Customer(long id,String name,String phone,String address){this(id,name,phone,address,0);}
        public Customer(long id,String name,String phone,String address,double openingBalance){this.id=id;this.name=name;this.phone=phone;this.address=address;this.openingBalance=openingBalance;}
    }
    public static class Supplier {
        public long id; public String name; public String phone; public String address; public double openingBalance;
        public Supplier(long id,String name,String phone,String address,double openingBalance){this.id=id;this.name=name;this.phone=phone;this.address=address;this.openingBalance=openingBalance;}
    }
    public static class Visitor {
        public long id; public String name; public String phone; public double commissionPercent; public String notes; public boolean active;
        public Visitor(long id,String name,String phone,double commissionPercent,String notes,boolean active){this.id=id;this.name=name;this.phone=phone;this.commissionPercent=commissionPercent;this.notes=notes;this.active=active;}
    }
    public static class SaleLine {
        public long productId; public String productName; public double unitPrice; public int qty; public String saleType;
        public SaleLine(long productId,String productName,double unitPrice,int qty,String saleType){this.productId=productId;this.productName=productName;this.unitPrice=unitPrice;this.qty=qty;this.saleType=saleType;}
    }
    public static class FinanceEntry {
        public long id; public String type,account,partyKind,partyName,description; public double amount; public long createdAt;
        public FinanceEntry(long id,String type,String account,String partyKind,String partyName,double amount,String description,long createdAt){this.id=id;this.type=type;this.account=account;this.partyKind=partyKind;this.partyName=partyName;this.amount=amount;this.description=description;this.createdAt=createdAt;}
    }
    public static class Summary {
        public double sales,purchases,cash,stockValue,receivables,payables; public int lowStock,productCount,customerCount,supplierCount;
    }
    public static class VisitorReport { public String name; public double sales,commission; public VisitorReport(String n,double s,double c){name=n;sales=s;commission=c;} }

    public AppDb(Context context){ super(context,DB_NAME,null,DB_VERSION); }

    @Override public void onConfigure(SQLiteDatabase db){ super.onConfigure(db); db.setForeignKeyConstraintsEnabled(false); }

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,retail REAL NOT NULL DEFAULT 0,wholesale REAL NOT NULL DEFAULT 0,purchase REAL NOT NULL DEFAULT 0,stock INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT 'عدد',min_stock INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT,opening_balance REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_customers_name ON customers(name)");
        db.execSQL("CREATE TABLE suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT,opening_balance REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_suppliers_name ON suppliers(name)");
        db.execSQL("CREATE TABLE visitors(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,commission_percent REAL NOT NULL DEFAULT 0,notes TEXT,active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_no TEXT NOT NULL,customer_id INTEGER,customer_name TEXT,customer_phone TEXT,visitor_id INTEGER,visitor_name TEXT,subtotal REAL NOT NULL DEFAULT 0,discount REAL NOT NULL DEFAULT 0,total REAL NOT NULL DEFAULT 0,paid REAL NOT NULL DEFAULT 0,commission_rate REAL NOT NULL DEFAULT 0,commission_amount REAL NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE sale_items(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER NOT NULL,product_id INTEGER,product_name TEXT NOT NULL,sale_type TEXT,qty INTEGER NOT NULL,unit_price REAL NOT NULL,line_total REAL NOT NULL)");
        db.execSQL("CREATE TABLE purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_no TEXT NOT NULL,supplier_id INTEGER,supplier_name TEXT,subtotal REAL NOT NULL DEFAULT 0,discount REAL NOT NULL DEFAULT 0,total REAL NOT NULL DEFAULT 0,paid REAL NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE purchase_items(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_id INTEGER NOT NULL,product_id INTEGER,product_name TEXT NOT NULL,qty INTEGER NOT NULL,unit_price REAL NOT NULL,line_total REAL NOT NULL)");
        db.execSQL("CREATE TABLE finance_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,account TEXT NOT NULL DEFAULT 'نقد',party_kind TEXT,party_id INTEGER,party_name TEXT,amount REAL NOT NULL,description TEXT,source TEXT NOT NULL DEFAULT 'MANUAL',created_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){
            safe(db,"ALTER TABLE products ADD COLUMN purchase REAL NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 0");
            safe(db,"CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT)"); safe(db,"CREATE INDEX idx_customers_name ON customers(name)");
        }
        if(oldVersion<3){
            safe(db,"CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_no TEXT NOT NULL,customer_id INTEGER,customer_name TEXT,customer_phone TEXT,total REAL NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)");
            safe(db,"CREATE TABLE sale_items(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER NOT NULL,product_id INTEGER,product_name TEXT NOT NULL,sale_type TEXT,qty INTEGER NOT NULL,unit_price REAL NOT NULL,line_total REAL NOT NULL)");
        }
        if(oldVersion<4){
            safe(db,"ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'عدد'"); safe(db,"ALTER TABLE products ADD COLUMN min_stock INTEGER NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE customers ADD COLUMN opening_balance REAL NOT NULL DEFAULT 0");
            safe(db,"CREATE TABLE suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT,opening_balance REAL NOT NULL DEFAULT 0)"); safe(db,"CREATE INDEX idx_suppliers_name ON suppliers(name)");
            safe(db,"CREATE TABLE visitors(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,commission_percent REAL NOT NULL DEFAULT 0,notes TEXT,active INTEGER NOT NULL DEFAULT 1)");
            safe(db,"CREATE TABLE purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_no TEXT NOT NULL,supplier_id INTEGER,supplier_name TEXT,subtotal REAL NOT NULL DEFAULT 0,discount REAL NOT NULL DEFAULT 0,total REAL NOT NULL DEFAULT 0,paid REAL NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)");
            safe(db,"CREATE TABLE purchase_items(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_id INTEGER NOT NULL,product_id INTEGER,product_name TEXT NOT NULL,qty INTEGER NOT NULL,unit_price REAL NOT NULL,line_total REAL NOT NULL)");
            safe(db,"CREATE TABLE finance_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,account TEXT NOT NULL DEFAULT 'نقد',party_kind TEXT,party_id INTEGER,party_name TEXT,amount REAL NOT NULL,description TEXT,source TEXT NOT NULL DEFAULT 'MANUAL',created_at INTEGER NOT NULL)");
        }
        if(oldVersion<5){
            safe(db,"ALTER TABLE sales ADD COLUMN visitor_id INTEGER"); safe(db,"ALTER TABLE sales ADD COLUMN visitor_name TEXT"); safe(db,"ALTER TABLE sales ADD COLUMN subtotal REAL NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE sales ADD COLUMN paid REAL NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE sales ADD COLUMN commission_rate REAL NOT NULL DEFAULT 0"); safe(db,"ALTER TABLE sales ADD COLUMN commission_amount REAL NOT NULL DEFAULT 0");
            safe(db,"UPDATE sales SET subtotal=total WHERE subtotal=0");
        }
        if(oldVersion<6){ safe(db,"CREATE INDEX IF NOT EXISTS idx_finance_created ON finance_entries(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_sales_created ON sales(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_purchases_created ON purchases(created_at)"); }
    }
    private static void safe(SQLiteDatabase db,String sql){ try{db.execSQL(sql);}catch(Exception ignored){} }

    public long productCount(){return count("products");} public long customerCount(){return count("customers");} public long supplierCount(){return count("suppliers");} public long visitorCount(){return count("visitors");}
    private long count(String table){return DatabaseUtils.longForQuery(getReadableDatabase(),"SELECT COUNT(*) FROM "+table,null);}

    public long saveProduct(Product p){
        ContentValues v=new ContentValues(); v.put("name",p.name.trim());v.put("purchase",Math.max(0,p.purchase));v.put("retail",Math.max(0,p.retail));v.put("wholesale",Math.max(0,p.wholesale));v.put("stock",Math.max(0,p.stock));v.put("unit",p.unit==null?"عدد":p.unit.trim());v.put("min_stock",Math.max(0,p.minStock));
        if(p.id>0){getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(p.id)});return p.id;} return getWritableDatabase().insertOrThrow("products",null,v);
    }
    public void deleteProduct(long id){getWritableDatabase().delete("products","id=?",new String[]{String.valueOf(id)});}
    public List<Product> searchProducts(String query){
        ArrayList<Product> out=new ArrayList<>(); String q=query==null?"":query.trim(); Cursor c;
        if(q.isEmpty()) c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock FROM products ORDER BY name COLLATE NOCASE LIMIT 400",null);
        else c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock FROM products WHERE name LIKE ? ORDER BY CASE WHEN name=? THEN 0 WHEN name LIKE ? THEN 1 ELSE 2 END,name COLLATE NOCASE LIMIT 400",new String[]{"%"+q+"%",q,q+"%"});
        try{while(c.moveToNext())out.add(new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7)));}finally{c.close();}return out;
    }
    public Product getProduct(long id){Cursor c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock FROM products WHERE id=?",new String[]{String.valueOf(id)});try{if(!c.moveToFirst())return null;return new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7));}finally{c.close();}}
    public void upsertProducts(List<Product> items){
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{for(Product p:items){Cursor c=d.rawQuery("SELECT id,purchase,stock,unit,min_stock FROM products WHERE name=? COLLATE NOCASE LIMIT 1",new String[]{p.name});long id=0;double oldPurchase=0;int oldStock=0;String unit="عدد";int min=0;try{if(c.moveToFirst()){id=c.getLong(0);oldPurchase=c.getDouble(1);oldStock=c.getInt(2);unit=c.getString(3);min=c.getInt(4);}}finally{c.close();}ContentValues v=new ContentValues();v.put("name",p.name);v.put("retail",p.retail);v.put("wholesale",p.wholesale);v.put("purchase",p.purchase>0?p.purchase:oldPurchase);v.put("stock",p.stock>0?p.stock:oldStock);v.put("unit",unit);v.put("min_stock",min);if(id>0)d.update("products",v,"id=?",new String[]{String.valueOf(id)});else d.insertOrThrow("products",null,v);}d.setTransactionSuccessful();}finally{d.endTransaction();}
    }

    public long saveCustomer(Customer p){ContentValues v=new ContentValues();v.put("name",p.name.trim());v.put("phone",nz(p.phone));v.put("address",nz(p.address));v.put("opening_balance",p.openingBalance);if(p.id>0){getWritableDatabase().update("customers",v,"id=?",new String[]{""+p.id});return p.id;}return getWritableDatabase().insertOrThrow("customers",null,v);}
    public void deleteCustomer(long id){getWritableDatabase().delete("customers","id=?",new String[]{""+id});}
    public List<Customer> searchCustomers(String q){ArrayList<Customer> out=new ArrayList<>();q=q==null?"":q.trim();Cursor c=q.isEmpty()?getReadableDatabase().rawQuery("SELECT id,name,phone,address,opening_balance FROM customers ORDER BY name LIMIT 300",null):getReadableDatabase().rawQuery("SELECT id,name,phone,address,opening_balance FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name LIMIT 300",new String[]{"%"+q+"%","%"+q+"%"});try{while(c.moveToNext())out.add(new Customer(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getDouble(4)));}finally{c.close();}return out;}

    public long saveSupplier(Supplier p){ContentValues v=new ContentValues();v.put("name",p.name.trim());v.put("phone",nz(p.phone));v.put("address",nz(p.address));v.put("opening_balance",p.openingBalance);if(p.id>0){getWritableDatabase().update("suppliers",v,"id=?",new String[]{""+p.id});return p.id;}return getWritableDatabase().insertOrThrow("suppliers",null,v);}
    public void deleteSupplier(long id){getWritableDatabase().delete("suppliers","id=?",new String[]{""+id});}
    public List<Supplier> searchSuppliers(String q){ArrayList<Supplier> out=new ArrayList<>();q=q==null?"":q.trim();Cursor c=q.isEmpty()?getReadableDatabase().rawQuery("SELECT id,name,phone,address,opening_balance FROM suppliers ORDER BY name LIMIT 300",null):getReadableDatabase().rawQuery("SELECT id,name,phone,address,opening_balance FROM suppliers WHERE name LIKE ? OR phone LIKE ? ORDER BY name LIMIT 300",new String[]{"%"+q+"%","%"+q+"%"});try{while(c.moveToNext())out.add(new Supplier(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getDouble(4)));}finally{c.close();}return out;}

    public long saveVisitor(Visitor p){ContentValues v=new ContentValues();v.put("name",p.name.trim());v.put("phone",nz(p.phone));v.put("commission_percent",Math.max(0,p.commissionPercent));v.put("notes",nz(p.notes));v.put("active",p.active?1:0);if(p.id>0){getWritableDatabase().update("visitors",v,"id=?",new String[]{""+p.id});return p.id;}return getWritableDatabase().insertOrThrow("visitors",null,v);}
    public void deleteVisitor(long id){getWritableDatabase().delete("visitors","id=?",new String[]{""+id});}
    public List<Visitor> searchVisitors(String q){ArrayList<Visitor> out=new ArrayList<>();q=q==null?"":q.trim();Cursor c=q.isEmpty()?getReadableDatabase().rawQuery("SELECT id,name,phone,commission_percent,notes,active FROM visitors ORDER BY active DESC,name LIMIT 300",null):getReadableDatabase().rawQuery("SELECT id,name,phone,commission_percent,notes,active FROM visitors WHERE name LIKE ? OR phone LIKE ? ORDER BY active DESC,name LIMIT 300",new String[]{"%"+q+"%","%"+q+"%"});try{while(c.moveToNext())out.add(new Visitor(c.getLong(0),c.getString(1),c.getString(2),c.getDouble(3),c.getString(4),c.getInt(5)==1));}finally{c.close();}return out;}

    public long saveSale(String invoiceNo,Customer customer,Visitor visitor,List<SaleLine> lines,double discount,double paid,String account)throws Exception{
        if(lines==null||lines.isEmpty())throw new Exception("فاکتور خالی است");SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{double subtotal=0;for(SaleLine l:lines){Cursor c=d.rawQuery("SELECT stock FROM products WHERE id=?",new String[]{""+l.productId});int stock;try{if(!c.moveToFirst())throw new Exception("کالا پیدا نشد: "+l.productName);stock=c.getInt(0);}finally{c.close();}if(stock<l.qty)throw new Exception("موجودی کافی نیست: "+l.productName+" ("+stock+")");subtotal+=l.unitPrice*l.qty;}discount=Math.max(0,Math.min(discount,subtotal));double total=subtotal-discount;paid=Math.max(0,Math.min(paid,total));double rate=visitor==null?0:visitor.commissionPercent;double commission=total*rate/100d;ContentValues s=new ContentValues();s.put("invoice_no",invoiceNo);if(customer!=null){s.put("customer_id",customer.id);s.put("customer_name",customer.name);s.put("customer_phone",customer.phone);}if(visitor!=null){s.put("visitor_id",visitor.id);s.put("visitor_name",visitor.name);}s.put("subtotal",subtotal);s.put("discount",discount);s.put("total",total);s.put("paid",paid);s.put("commission_rate",rate);s.put("commission_amount",commission);s.put("created_at",System.currentTimeMillis());long saleId=d.insertOrThrow("sales",null,s);for(SaleLine l:lines){d.execSQL("UPDATE products SET stock=stock-? WHERE id=?",new Object[]{l.qty,l.productId});ContentValues it=new ContentValues();it.put("sale_id",saleId);it.put("product_id",l.productId);it.put("product_name",l.productName);it.put("sale_type",l.saleType);it.put("qty",l.qty);it.put("unit_price",l.unitPrice);it.put("line_total",l.unitPrice*l.qty);d.insertOrThrow("sale_items",null,it);}if(paid>0)addFinanceInternal(d,"RECEIPT",account,"CUSTOMER",customer==null?0:customer.id,customer==null?"فروش نقدی":customer.name,paid,"دریافت بابت فاکتور "+invoiceNo,"SALE");d.setTransactionSuccessful();return saleId;}finally{d.endTransaction();}
    }

    public long savePurchase(String invoiceNo,Supplier supplier,List<SaleLine> lines,double discount,double paid,String account)throws Exception{
        if(lines==null||lines.isEmpty())throw new Exception("فاکتور خالی است");SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{double subtotal=0;for(SaleLine l:lines)subtotal+=l.unitPrice*l.qty;discount=Math.max(0,Math.min(discount,subtotal));double total=subtotal-discount;paid=Math.max(0,Math.min(paid,total));ContentValues h=new ContentValues();h.put("invoice_no",invoiceNo);if(supplier!=null){h.put("supplier_id",supplier.id);h.put("supplier_name",supplier.name);}h.put("subtotal",subtotal);h.put("discount",discount);h.put("total",total);h.put("paid",paid);h.put("created_at",System.currentTimeMillis());long pid=d.insertOrThrow("purchases",null,h);for(SaleLine l:lines){d.execSQL("UPDATE products SET stock=stock+?,purchase=? WHERE id=?",new Object[]{l.qty,l.unitPrice,l.productId});ContentValues it=new ContentValues();it.put("purchase_id",pid);it.put("product_id",l.productId);it.put("product_name",l.productName);it.put("qty",l.qty);it.put("unit_price",l.unitPrice);it.put("line_total",l.unitPrice*l.qty);d.insertOrThrow("purchase_items",null,it);}if(paid>0)addFinanceInternal(d,"PAYMENT",account,"SUPPLIER",supplier==null?0:supplier.id,supplier==null?"خرید نقدی":supplier.name,paid,"پرداخت بابت خرید "+invoiceNo,"PURCHASE");d.setTransactionSuccessful();return pid;}finally{d.endTransaction();}
    }

    public long addFinance(String type,String account,String partyKind,long partyId,String partyName,double amount,String description){return addFinanceInternal(getWritableDatabase(),type,account,partyKind,partyId,partyName,amount,description,"MANUAL");}
    private long addFinanceInternal(SQLiteDatabase d,String type,String account,String partyKind,long partyId,String partyName,double amount,String description,String source){ContentValues v=new ContentValues();v.put("type",type);v.put("account",account==null||account.trim().isEmpty()?"نقد":account.trim());v.put("party_kind",nz(partyKind));if(partyId>0)v.put("party_id",partyId);v.put("party_name",nz(partyName));v.put("amount",Math.max(0,amount));v.put("description",nz(description));v.put("source",source);v.put("created_at",System.currentTimeMillis());return d.insertOrThrow("finance_entries",null,v);}
    public List<FinanceEntry> recentFinance(){ArrayList<FinanceEntry> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,type,account,party_kind,party_name,amount,description,created_at FROM finance_entries ORDER BY created_at DESC LIMIT 100",null);try{while(c.moveToNext())out.add(new FinanceEntry(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getDouble(5),c.getString(6),c.getLong(7)));}finally{c.close();}return out;}

    public Summary summary(){Summary s=new Summary();SQLiteDatabase d=getReadableDatabase();s.productCount=(int)productCount();s.customerCount=(int)customerCount();s.supplierCount=(int)supplierCount();s.sales=scalar(d,"SELECT COALESCE(SUM(total),0) FROM sales");s.purchases=scalar(d,"SELECT COALESCE(SUM(total),0) FROM purchases");s.stockValue=scalar(d,"SELECT COALESCE(SUM(stock*purchase),0) FROM products");s.lowStock=(int)scalar(d,"SELECT COUNT(*) FROM products WHERE min_stock>0 AND stock<=min_stock");s.cash=scalar(d,"SELECT COALESCE(SUM(CASE WHEN type IN ('RECEIPT','INCOME') THEN amount ELSE -amount END),0) FROM finance_entries");s.receivables=scalar(d,"SELECT COALESCE(SUM(opening_balance),0) FROM customers")+scalar(d,"SELECT COALESCE(SUM(total-paid),0) FROM sales")-scalar(d,"SELECT COALESCE(SUM(amount),0) FROM finance_entries WHERE source='MANUAL' AND type='RECEIPT' AND party_kind='CUSTOMER'");s.payables=scalar(d,"SELECT COALESCE(SUM(opening_balance),0) FROM suppliers")+scalar(d,"SELECT COALESCE(SUM(total-paid),0) FROM purchases")-scalar(d,"SELECT COALESCE(SUM(amount),0) FROM finance_entries WHERE source='MANUAL' AND type='PAYMENT' AND party_kind='SUPPLIER'");return s;}
    public List<VisitorReport> visitorReports(){ArrayList<VisitorReport> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(visitor_name,'بدون ویزیتور'),COALESCE(SUM(total),0),COALESCE(SUM(commission_amount),0) FROM sales GROUP BY visitor_id,visitor_name ORDER BY SUM(total) DESC",null);try{while(c.moveToNext())out.add(new VisitorReport(c.getString(0),c.getDouble(1),c.getDouble(2)));}finally{c.close();}return out;}
    public double customerBalance(long id){return scalar(getReadableDatabase(),"SELECT COALESCE(opening_balance,0)+(SELECT COALESCE(SUM(total-paid),0) FROM sales WHERE customer_id="+id+")-(SELECT COALESCE(SUM(amount),0) FROM finance_entries WHERE source='MANUAL' AND type='RECEIPT' AND party_kind='CUSTOMER' AND party_id="+id+") FROM customers WHERE id="+id);}
    public double supplierBalance(long id){return scalar(getReadableDatabase(),"SELECT COALESCE(opening_balance,0)+(SELECT COALESCE(SUM(total-paid),0) FROM purchases WHERE supplier_id="+id+")-(SELECT COALESCE(SUM(amount),0) FROM finance_entries WHERE source='MANUAL' AND type='PAYMENT' AND party_kind='SUPPLIER' AND party_id="+id+") FROM suppliers WHERE id="+id);}
    private double scalar(SQLiteDatabase d,String sql){Cursor c=d.rawQuery(sql,null);try{return c.moveToFirst()?c.getDouble(0):0;}finally{c.close();}}
    private String nz(String s){return s==null?"":s;}
    public void checkpoint(){try{getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)",null).close();}catch(Exception ignored){}}
}
