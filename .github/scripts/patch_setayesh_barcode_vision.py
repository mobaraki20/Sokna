from pathlib import Path
import re

main_path = Path('app/src/main/java/com/example/excelpricesearch/MainActivity.java')
db_path = Path('app/src/main/java/com/example/excelpricesearch/AppDb.java')
s = main_path.read_text()
db = db_path.read_text()

def repl_once(text, old, new, name):
    if old not in text:
        raise SystemExit(f'missing target {name}')
    return text.replace(old, new, 1)

def replace_between(text, start_sig, next_sig, body, name):
    pattern = re.escape(start_sig) + r'.*?(?=\n\s*' + re.escape(next_sig) + r')'
    out, n = re.subn(pattern, body.rstrip(), text, count=1, flags=re.S)
    if n != 1:
        raise SystemExit(f'missing range {name}')
    return out

# ---------- Database: barcode + image signature ----------
db = repl_once(db, 'private static final int DB_VERSION = 6;', 'private static final int DB_VERSION = 7;', 'db version')

db = replace_between(db,
    '    public static class Product {',
    'public static class Customer {',
    '''    public static class Product {
        public long id; public String name; public double purchase; public double retail; public double wholesale; public int stock; public String unit; public int minStock; public String barcode; public String imageHash;
        public Product(long id, String name, double purchase, double retail, double wholesale, int stock) { this(id,name,purchase,retail,wholesale,stock,"عدد",0,"",""); }
        public Product(long id, String name, double purchase, double retail, double wholesale, int stock, String unit, int minStock) { this(id,name,purchase,retail,wholesale,stock,unit,minStock,"",""); }
        public Product(long id, String name, double purchase, double retail, double wholesale, int stock, String unit, int minStock, String barcode, String imageHash) {
            this.id=id; this.name=name; this.purchase=purchase; this.retail=retail; this.wholesale=wholesale; this.stock=stock; this.unit=unit==null?"عدد":unit; this.minStock=minStock; this.barcode=barcode==null?"":barcode; this.imageHash=imageHash==null?"":imageHash;
        }
    }
''', 'product class')

db = repl_once(db,
    'db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,retail REAL NOT NULL DEFAULT 0,wholesale REAL NOT NULL DEFAULT 0,purchase REAL NOT NULL DEFAULT 0,stock INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT \'عدد\',min_stock INTEGER NOT NULL DEFAULT 0)");',
    'db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,retail REAL NOT NULL DEFAULT 0,wholesale REAL NOT NULL DEFAULT 0,purchase REAL NOT NULL DEFAULT 0,stock INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT \'عدد\',min_stock INTEGER NOT NULL DEFAULT 0,barcode TEXT,image_hash TEXT)");',
    'create products')

db = repl_once(db,
    '        if(oldVersion<6){ safe(db,"CREATE INDEX IF NOT EXISTS idx_finance_created ON finance_entries(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_sales_created ON sales(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_purchases_created ON purchases(created_at)"); }',
    '        if(oldVersion<6){ safe(db,"CREATE INDEX IF NOT EXISTS idx_finance_created ON finance_entries(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_sales_created ON sales(created_at)"); safe(db,"CREATE INDEX IF NOT EXISTS idx_purchases_created ON purchases(created_at)"); }\n        if(oldVersion<7){ safe(db,"ALTER TABLE products ADD COLUMN barcode TEXT"); safe(db,"ALTER TABLE products ADD COLUMN image_hash TEXT"); safe(db,"CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode)"); }',
    'upgrade 7')

db = replace_between(db,
    '    public long saveProduct(Product p){',
    'public void deleteProduct(long id){',
    '''    public long saveProduct(Product p){
        ContentValues v=new ContentValues();
        v.put("name",p.name.trim());v.put("purchase",Math.max(0,p.purchase));v.put("retail",Math.max(0,p.retail));v.put("wholesale",Math.max(0,p.wholesale));v.put("stock",Math.max(0,p.stock));v.put("unit",p.unit==null?"عدد":p.unit.trim());v.put("min_stock",Math.max(0,p.minStock));v.put("barcode",p.barcode==null?"":p.barcode.trim());v.put("image_hash",p.imageHash==null?"":p.imageHash);
        if(p.id>0){getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(p.id)});return p.id;}
        return getWritableDatabase().insertOrThrow("products",null,v);
    }
''', 'save product')

db = replace_between(db,
    '    public List<Product> searchProducts(String query){',
    'public void upsertProducts(List<Product> items){',
    '''    public List<Product> searchProducts(String query){
        ArrayList<Product> out=new ArrayList<>(); String q=query==null?"":query.trim(); Cursor c;
        String cols="id,name,purchase,retail,wholesale,stock,unit,min_stock,COALESCE(barcode,''),COALESCE(image_hash,'')";
        if(q.isEmpty()) c=getReadableDatabase().rawQuery("SELECT "+cols+" FROM products ORDER BY name COLLATE NOCASE LIMIT 400",null);
        else c=getReadableDatabase().rawQuery("SELECT "+cols+" FROM products WHERE name LIKE ? OR barcode LIKE ? ORDER BY CASE WHEN name=? THEN 0 WHEN name LIKE ? THEN 1 ELSE 2 END,name COLLATE NOCASE LIMIT 400",new String[]{"%"+q+"%","%"+q+"%",q,q+"%"});
        try{while(c.moveToNext())out.add(new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9)));}finally{c.close();}return out;
    }
    public Product getProduct(long id){Cursor c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock,COALESCE(barcode,''),COALESCE(image_hash,'') FROM products WHERE id=?",new String[]{String.valueOf(id)});try{if(!c.moveToFirst())return null;return new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9));}finally{c.close();}}
    public Product findProductByBarcode(String barcode){if(barcode==null||barcode.trim().isEmpty())return null;Cursor c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock,COALESCE(barcode,''),COALESCE(image_hash,'') FROM products WHERE barcode=? LIMIT 1",new String[]{barcode.trim()});try{if(!c.moveToFirst())return null;return new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9));}finally{c.close();}}
    public List<Product> productsWithImages(){ArrayList<Product> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,purchase,retail,wholesale,stock,unit,min_stock,COALESCE(barcode,''),COALESCE(image_hash,'') FROM products WHERE image_hash IS NOT NULL AND image_hash<>''",null);try{while(c.moveToNext())out.add(new Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9)));}finally{c.close();}return out;}
    public void updateProductBarcode(long id,String barcode){ContentValues v=new ContentValues();v.put("barcode",barcode==null?"":barcode.trim());getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateProductImageHash(long id,String hash){ContentValues v=new ContentValues();v.put("image_hash",hash==null?"":hash);getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(id)});}
''', 'product queries')

db_path.write_text(db)

# ---------- MainActivity imports / state ----------
s = repl_once(s, 'import android.graphics.Color;\n', 'import android.graphics.Color;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\nimport android.graphics.Canvas;\nimport android.graphics.Paint;\n', 'bitmap imports')
s = repl_once(s, 'import android.net.Uri;\n', 'import android.net.Uri;\nimport android.provider.MediaStore;\n', 'media import')
s = repl_once(s, 'import android.widget.EditText;\n', 'import android.widget.EditText;\nimport android.widget.ImageView;\n', 'imageview import')
s = repl_once(s, 'import java.util.Locale;\n', 'import java.util.Locale;\n\nimport com.google.mlkit.vision.codescanner.GmsBarcodeScanner;\nimport com.google.mlkit.vision.codescanner.GmsBarcodeScanning;\n', 'scanner imports')

s = repl_once(s,
    '    private static final int PICK_XLSX=1001, SAVE_PDF=2002, BACKUP_CREATE=3001, BACKUP_RESTORE=3002;',
    '    private static final int PICK_XLSX=1001, SAVE_PDF=2002, BACKUP_CREATE=3001, BACKUP_RESTORE=3002, PICK_IMAGE_IDENTIFY=4001, CAPTURE_IMAGE_IDENTIFY=4002, PICK_IMAGE_REFERENCE=4003, CAPTURE_IMAGE_REFERENCE=4004;',
    'request codes')
s = repl_once(s,
    '    private long pendingPdfTime;\n',
    '    private long pendingPdfTime;\n    private AppDb.Product pendingImageProduct;\n    private int pendingImageMode=0; // 0=lookup, 1=sale, 2=purchase\n',
    'image state')

# ---------- Animated Mother Accessories splash ----------
s = replace_between(s,
    '    @Override protected void onCreate(Bundle b){',
    'private View buildRoot(){',
    '''    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        db=new AppDb(this);
        saleNo=newNo("F");
        purchaseNo=newNo("K");
        showMotherSplash();
    }

    private void showMotherSplash(){
        getWindow().setStatusBarColor(Color.rgb(18,18,18));
        getWindow().setNavigationBarColor(Color.rgb(18,18,18));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackgroundColor(Color.rgb(18,18,18));
        TextView mark=label("◆",32,Color.rgb(212,175,55),true);mark.setGravity(Gravity.CENTER);mark.setAlpha(0f);mark.setScaleX(.65f);mark.setScaleY(.65f);root.addView(mark,lp(-1,-2,0,0,0,10));
        TextView title=label("اکسسوری مادر",36,Color.rgb(230,195,82),true);title.setGravity(Gravity.CENTER);title.setAlpha(0f);title.setScaleX(.90f);title.setScaleY(.90f);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));root.addView(title);
        TextView line=label("ــــــــــــــــــــــــ",18,Color.rgb(142,111,35),false);line.setGravity(Gravity.CENTER);line.setAlpha(0f);root.addView(line,lp(-1,-2,0,8,0,4));
        TextView sub=label("SYSTEM SETAYESH  •  مدیریت فروش و انبار",12,Color.rgb(190,190,190),false);sub.setGravity(Gravity.CENTER);sub.setAlpha(0f);root.addView(sub);
        setContentView(root);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(180f).setDuration(650).setInterpolator(new DecelerateInterpolator()).start();
        title.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(260).setDuration(700).setInterpolator(new DecelerateInterpolator()).start();
        line.animate().alpha(1f).setStartDelay(620).setDuration(450).start();
        sub.animate().alpha(1f).translationY(-dp(3)).setStartDelay(800).setDuration(500).start();
        uiHandler.postDelayed(()->root.animate().alpha(0f).setDuration(250).withEndAction(this::launchMainUi).start(),2200);
    }

    private void launchMainUi(){
        getWindow().setStatusBarColor(WIN_SURFACE);getWindow().setNavigationBarColor(WIN_SURFACE);int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(android.os.Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);setContentView(buildRoot());showDashboard();
    }
''', 'splash')

# ---------- Product UI ----------
s = replace_between(s,
    '    private void showProducts(){',
    'private void productDialog(AppDb.Product p){',
    '''    private void showProducts(){
        screenTitle("کالا، انبار و بارکد");
        LinearLayout actions=row();actions.addView(btn("+ تعریف کالا",v->productDialog(null)),weight());actions.addView(btn("ورود Excel",v->pickExcel()),weightM());content.addView(actions,lp(-1,-2,0,0,0,7));
        LinearLayout vision=row();vision.addView(btn("شناسایی کالا از عکس",v->chooseIdentifyImage(0)),weight());vision.addView(btn("راهنمای عکس",v->new AlertDialog.Builder(this).setTitle("شناسایی تصویری").setMessage("برای هر کالا ابتدا یک عکس مرجع ثبت کنید. هنگام شناسایی، کالا را تا حد امکان مشابه همان زاویه و با پس‌زمینه ساده داخل کادر قرار دهید.").setPositiveButton("باشه",null).show()),weightM());content.addView(vision,lp(-1,dp(48),0,0,0,7));
        EditText q=searchBox("جستجوی نام یا بارکد...");content.addView(q,lp(-1,dp(48),0,0,0,7));TextView stat=label("تعداد کالاها: "+db.productCount(),13,WIN_MUTED,false);content.addView(stat,lp(-1,-2,0,0,0,5));
        ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sc.addView(list);content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));Runnable render=()->renderProducts(list,q.getText().toString());q.addTextChangedListener(watcher(render));render.run();animateContent();
    }
    private void renderProducts(LinearLayout list,String q){
        list.removeAllViews();
        for(AppDb.Product p:db.searchProducts(q)){
            LinearLayout c=card();
            TextView n=label(p.name+"  |  موجودی: "+p.stock+" "+p.unit,17,p.stock<=p.minStock&&p.minStock>0?Color.rgb(190,55,45):WIN_TEXT,true);c.addView(n);
            c.addView(label("خرید: "+money(p.purchase)+"   |   خرده: "+money(p.retail)+"   |   عمده: "+money(p.wholesale),13,WIN_MUTED,false),lp(-1,-2,0,5,0,2));
            c.addView(label("بارکد: "+(p.barcode==null||p.barcode.isEmpty()?"ثبت نشده":p.barcode)+"   |   عکس مرجع: "+(p.imageHash==null||p.imageHash.isEmpty()?"ندارد":"✓ دارد"),12,WIN_MUTED,false),lp(-1,-2,0,2,0,6));
            LinearLayout a=row();a.addView(btn("ویرایش",v->productDialog(p)),weight());a.addView(btn("نمایش بارکد",v->showBarcode(p)),weightM());c.addView(a,lp(-1,dp(44),0,0,0,5));
            LinearLayout b=row();b.addView(btn("ثبت عکس مرجع",v->chooseReferenceImage(p)),weight());b.addView(btn("حذف",v->confirm("حذف کالا",p.name,()->{db.deleteProduct(p.id);showProducts();})),weightM());c.addView(b,lp(-1,dp(44),0,0,0,0));
            list.addView(c,lp(-1,-2,0,0,0,7));
        }
        if(list.getChildCount()==0)list.addView(empty("کالایی پیدا نشد."));
    }
''', 'products ui')

s = replace_between(s,
    '    private void productDialog(AppDb.Product p){',
    'private void showCustomers(){',
    '''    private void productDialog(AppDb.Product p){
        LinearLayout f=form();
        EditText name=input("نام کالا",InputType.TYPE_CLASS_TEXT),unit=input("واحد (عدد، بسته، کیلو...)",InputType.TYPE_CLASS_TEXT),purchase=num("قیمت خرید"),retail=num("قیمت خرده"),wholesale=num("قیمت عمده"),stock=intNum("موجودی"),min=intNum("حداقل موجودی"),barcode=input("بارکد / کد اسکن",InputType.TYPE_CLASS_TEXT);
        if(p!=null){name.setText(p.name);unit.setText(p.unit);purchase.setText(raw(p.purchase));retail.setText(raw(p.retail));wholesale.setText(raw(p.wholesale));stock.setText(""+p.stock);min.setText(""+p.minStock);barcode.setText(p.barcode);}else unit.setText("عدد");
        addFields(f,name,unit,purchase,retail,wholesale,stock,min,barcode);
        f.addView(btn("تولید بارکد EAN-13",v->barcode.setText(generateEan13())),lp(-1,dp(44),0,0,0,6));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(p==null?"تعریف کالا":"ویرایش کالا").setView(scrollWrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(name.getText().toString().trim().isEmpty()){toast("نام کالا را وارد کنید");return;}
            String bc=barcode.getText().toString().trim();if(bc.isEmpty())bc=generateEan13();
            AppDb.Product clash=db.findProductByBarcode(bc);if(clash!=null&&(p==null||clash.id!=p.id)){toast("این بارکد قبلاً برای «"+clash.name+"» ثبت شده است");return;}
            db.saveProduct(new AppDb.Product(p==null?0:p.id,name.getText().toString().trim(),val(purchase),val(retail),val(wholesale),ival(stock),unit.getText().toString().trim(),ival(min),bc,p==null?"":p.imageHash));
            dlg.dismiss();showProducts();
        }));dlg.show();
    }
''', 'product dialog')

# ---------- Scan/photo buttons in invoices ----------
s = repl_once(s,
    'content.addView(btn("+ افزودن کالا به فاکتور",v->pickProduct(p->lineDialog(p,true))),lp(-1,dp(46),0,0,0,6));',
    'LinearLayout quickSale=row();quickSale.addView(btn("+ انتخاب کالا",v->pickProduct(p->lineDialog(p,true))),weight());quickSale.addView(btn("اسکن بارکد",v->scanBarcode(true)),weightM());content.addView(quickSale,lp(-1,dp(46),0,0,0,5));content.addView(btn("شناسایی کالا از عکس",v->chooseIdentifyImage(1)),lp(-1,dp(44),0,0,0,6));',
    'sale quick actions')
s = repl_once(s,
    'content.addView(btn("+ افزودن کالا به خرید",v->pickProduct(p->lineDialog(p,false))),lp(-1,dp(46),0,0,0,6));',
    'LinearLayout quickBuy=row();quickBuy.addView(btn("+ انتخاب کالا",v->pickProduct(p->lineDialog(p,false))),weight());quickBuy.addView(btn("اسکن بارکد",v->scanBarcode(false)),weightM());content.addView(quickBuy,lp(-1,dp(46),0,0,0,5));content.addView(btn("شناسایی کالا از عکس",v->chooseIdentifyImage(2)),lp(-1,dp(44),0,0,0,6));',
    'purchase quick actions')

# ---------- Image activity results ----------
s = replace_between(s,
    '    @Override protected void onActivityResult(int req,int res,Intent data){',
    'private void importExcel(Uri uri){',
    '''    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(res!=RESULT_OK)return;
        try{
            if(req==CAPTURE_IMAGE_REFERENCE||req==CAPTURE_IMAGE_IDENTIFY){Bitmap bm=data==null||data.getExtras()==null?null:(Bitmap)data.getExtras().get("data");if(bm==null){toast("عکس دریافت نشد");return;}if(req==CAPTURE_IMAGE_REFERENCE)processReferenceImage(bm);else processIdentifyImage(bm);return;}
            if(data==null||data.getData()==null)return;Uri uri=data.getData();
            if(req==PICK_IMAGE_REFERENCE||req==PICK_IMAGE_IDENTIFY){try(InputStream in=getContentResolver().openInputStream(uri)){Bitmap bm=BitmapFactory.decodeStream(in);if(bm==null){toast("تصویر قابل خواندن نیست");return;}if(req==PICK_IMAGE_REFERENCE)processReferenceImage(bm);else processIdentifyImage(bm);}return;}
            if(req==PICK_XLSX)new Thread(()->importExcel(uri)).start();else if(req==SAVE_PDF)new Thread(()->writePdf(uri)).start();else if(req==BACKUP_CREATE)new Thread(()->writeBackup(uri)).start();else if(req==BACKUP_RESTORE)new Thread(()->restoreBackup(uri)).start();
        }catch(Exception e){toast("خطا: "+e.getMessage());}
    }
''', 'activity result')

# ---------- Feature helpers before pickers ----------
insert_marker = '    private void pickCustomer(CustomerPick cb){'
if insert_marker not in s:
    raise SystemExit('missing picker marker')
helpers = r'''    private void scanBarcode(boolean sale){
        GmsBarcodeScanner scanner=GmsBarcodeScanning.getClient(this);
        scanner.startScan().addOnSuccessListener(code->{String raw=code.getRawValue();if(raw==null||raw.trim().isEmpty()){toast("بارکد خوانده نشد");return;}AppDb.Product p=db.findProductByBarcode(raw.trim());if(p==null){new AlertDialog.Builder(this).setTitle("کالا پیدا نشد").setMessage("بارکد "+raw+" در کالاها ثبت نشده است. ابتدا آن را برای یک کالا تعریف کنید.").setPositiveButton("باشه",null).show();return;}addFastProduct(p,sale);}).addOnCanceledListener(()->{}).addOnFailureListener(e->toast("خطای اسکن: "+e.getMessage()));
    }

    private void addFastProduct(AppDb.Product p,boolean sale){
        if(sale&&p.stock<=0){toast("موجودی «"+p.name+"» صفر است");return;}
        ArrayList<AppDb.SaleLine> lines=sale?saleLines:purchaseLines;double price=sale?p.retail:p.purchase;if(price<0)price=0;
        for(AppDb.SaleLine l:lines)if(l.productId==p.id){l.qty++;if(sale)renderSaleLines();else renderPurchaseLines();toast(p.name+" × "+l.qty);return;}
        lines.add(new AppDb.SaleLine(p.id,p.name,price,1,sale?"فروش":"خرید"));if(sale)renderSaleLines();else renderPurchaseLines();toast(p.name+" به فاکتور اضافه شد");
    }

    private void chooseReferenceImage(AppDb.Product p){pendingImageProduct=p;String[] items={"گرفتن عکس با دوربین","انتخاب از گالری"};new AlertDialog.Builder(this).setTitle("عکس مرجع — "+p.name).setItems(items,(d,w)->{if(w==0){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAPTURE_IMAGE_REFERENCE);}else{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE_REFERENCE);}}).setNegativeButton("انصراف",null).show();}
    private void chooseIdentifyImage(int mode){pendingImageMode=mode;String[] items={"گرفتن عکس با دوربین","انتخاب از گالری"};new AlertDialog.Builder(this).setTitle("شناسایی کالا از عکس").setItems(items,(d,w)->{if(w==0){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAPTURE_IMAGE_IDENTIFY);}else{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE_IDENTIFY);}}).setNegativeButton("انصراف",null).show();}
    private void processReferenceImage(Bitmap bm){if(pendingImageProduct==null)return;String h=imageHash(bm);db.updateProductImageHash(pendingImageProduct.id,h);pendingImageProduct.imageHash=h;toast("عکس مرجع برای «"+pendingImageProduct.name+"» ثبت شد");showProducts();}
    private void processIdentifyImage(Bitmap bm){String h=imageHash(bm);List<AppDb.Product> refs=db.productsWithImages();if(refs.isEmpty()){toast("هنوز برای هیچ کالایی عکس مرجع ثبت نشده است");return;}AppDb.Product best=null;int bestD=999;for(AppDb.Product p:refs){int d=hashDistance(h,p.imageHash);if(d<bestD){bestD=d;best=p;}}if(best==null||bestD>20){new AlertDialog.Builder(this).setTitle("تطابق مطمئن پیدا نشد").setMessage("عکس را نزدیک‌تر، روبه‌رو و با پس‌زمینه ساده بگیرید یا عکس مرجع کالا را دوباره ثبت کنید.").setPositiveButton("باشه",null).show();return;}int confidence=Math.max(0,100-(bestD*100/64));AppDb.Product found=best;if(pendingImageMode==1)addFastProduct(found,true);else if(pendingImageMode==2)addFastProduct(found,false);else new AlertDialog.Builder(this).setTitle("کالا شناسایی شد").setMessage(found.name+"\nموجودی: "+found.stock+" "+found.unit+"\nبارکد: "+(found.barcode.isEmpty()?"—":found.barcode)+"\nشباهت تصویری تقریبی: "+confidence+"٪").setPositiveButton("باشه",null).show();}
    private String imageHash(Bitmap source){Bitmap b=Bitmap.createScaledBitmap(source,8,8,true);int[] gray=new int[64];long sum=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){int c=b.getPixel(x,y);int g=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;gray[y*8+x]=g;sum+=g;}int avg=(int)(sum/64);StringBuilder out=new StringBuilder(64);for(int g:gray)out.append(g>=avg?'1':'0');if(b!=source)b.recycle();return out.toString();}
    private int hashDistance(String a,String b){if(a==null||b==null||a.length()!=b.length())return 999;int d=0;for(int i=0;i<a.length();i++)if(a.charAt(i)!=b.charAt(i))d++;return d;}

    private String generateEan13(){long seed=Math.abs(System.currentTimeMillis()%1000000000L);String base="290"+String.format(Locale.US,"%09d",seed);int sum=0;for(int i=0;i<12;i++){int d=base.charAt(i)-'0';sum+=d*((i%2)==0?1:3);}int check=(10-(sum%10))%10;String code=base+check;AppDb.Product old=db.findProductByBarcode(code);if(old!=null)return generateEan13FromSeed(seed+1);return code;}
    private String generateEan13FromSeed(long seed){String base="290"+String.format(Locale.US,"%09d",Math.abs(seed%1000000000L));int sum=0;for(int i=0;i<12;i++){int d=base.charAt(i)-'0';sum+=d*((i%2)==0?1:3);}int check=(10-(sum%10))%10;String code=base+check;return db.findProductByBarcode(code)==null?code:generateEan13FromSeed(seed+1);}
    private String ensureBarcode(AppDb.Product p){if(p.barcode!=null&&!p.barcode.trim().isEmpty())return p.barcode.trim();String b=generateEan13();db.updateProductBarcode(p.id,b);p.barcode=b;return b;}
    private void showBarcode(AppDb.Product p){String code=ensureBarcode(p);LinearLayout box=form();TextView name=label(p.name,17,WIN_TEXT,true);name.setGravity(Gravity.CENTER);box.addView(name,lp(-1,-2,0,0,0,6));ImageView img=new ImageView(this);img.setAdjustViewBounds(true);img.setImageBitmap(ean13Bitmap(code));box.addView(img,lp(-1,dp(210),0,0,0,4));TextView t=label(code,16,WIN_TEXT,true);t.setGravity(Gravity.CENTER);box.addView(t);new AlertDialog.Builder(this).setTitle("بارکد کالا").setView(box).setPositiveButton("بستن",null).show();}
    private Bitmap ean13Bitmap(String code){if(code==null||!code.matches("\\d{13}")){Bitmap x=Bitmap.createBitmap(760,220,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(x);c.drawColor(Color.WHITE);Paint p=new Paint(1);p.setColor(Color.BLACK);p.setTextSize(34);p.setTextAlign(Paint.Align.CENTER);c.drawText(code==null?"":code,380,120,p);return x;}String[] L={"0001101","0011001","0010011","0111101","0100011","0110001","0101111","0111011","0110111","0001011"};String[] G={"0100111","0110011","0011011","0100001","0011101","0111001","0000101","0010001","0001001","0010111"};String[] R={"1110010","1100110","1101100","1000010","1011100","1001110","1010000","1000100","1001000","1110100"};String[] parity={"LLLLLL","LLGLGG","LLGGLG","LLGGGL","LGLLGG","LGGLLG","LGGGLL","LGLGLG","LGLGGL","LGGLGL"};int first=code.charAt(0)-'0';StringBuilder bits=new StringBuilder("101");String par=parity[first];for(int i=1;i<=6;i++){int d=code.charAt(i)-'0';bits.append(par.charAt(i-1)=='L'?L[d]:G[d]);}bits.append("01010");for(int i=7;i<=12;i++)bits.append(R[code.charAt(i)-'0']);bits.append("101");int w=760,h=250,margin=36,barH=175;Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);c.drawColor(Color.WHITE);Paint paint=new Paint();paint.setColor(Color.BLACK);float module=(w-margin*2f)/bits.length();for(int i=0;i<bits.length();i++)if(bits.charAt(i)=='1')c.drawRect(margin+i*module,20,margin+(i+1)*module,20+barH,paint);paint.setTextSize(28);paint.setTextAlign(Paint.Align.CENTER);c.drawText(code,w/2f,230,paint);return bm;}

'''
s = s.replace(insert_marker, helpers + insert_marker, 1)

main_path.write_text(s)
