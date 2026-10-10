from pathlib import Path
import re

p = Path('app/src/main/java/com/example/excelpricesearch/MainActivity.java')
s = p.read_text()

def replace_between(start_sig, next_sig, body):
    global s
    pattern = re.escape(start_sig) + r'.*?(?=\n\s*' + re.escape(next_sig) + r')'
    s, n = re.subn(pattern, body.rstrip(), s, count=1, flags=re.S)
    if n != 1:
        raise SystemExit(f'patch target not found: {start_sig}')

def replace_once(old, new):
    global s
    if old not in s:
        raise SystemExit(f'replace target not found: {old[:80]}')
    s = s.replace(old, new, 1)

# Lightweight Fluent / Windows 11 imports and palette.
replace_once('import android.content.Intent;\n', 'import android.content.Intent;\nimport android.content.res.ColorStateList;\n')
replace_once('import android.graphics.drawable.GradientDrawable;\n', 'import android.graphics.drawable.GradientDrawable;\nimport android.graphics.drawable.RippleDrawable;\n')
replace_once('import android.os.Bundle;\n', 'import android.os.Bundle;\nimport android.os.Handler;\nimport android.os.Looper;\n')
replace_once('import android.view.View;\n', 'import android.view.View;\nimport android.view.animation.DecelerateInterpolator;\n')
replace_once(
    '    private AppDb db;\n',
    '''    private AppDb db;
    private static final int WIN_BG = Color.rgb(243,243,243);
    private static final int WIN_SURFACE = Color.rgb(251,251,251);
    private static final int WIN_CARD = Color.WHITE;
    private static final int WIN_TEXT = Color.rgb(28,28,28);
    private static final int WIN_MUTED = Color.rgb(96,96,96);
    private static final int WIN_BORDER = Color.rgb(225,225,225);
    private static final int WIN_ACCENT = Color.rgb(0,103,192);
    private static final int WIN_ACCENT_SOFT = Color.rgb(230,242,252);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
'''
)

replace_between(
    '    @Override protected void onCreate(Bundle b){',
    'private View buildRoot(){',
    '''    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(WIN_SURFACE);
        getWindow().setNavigationBarColor(WIN_SURFACE);
        int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if(android.os.Build.VERSION.SDK_INT>=26) flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        db=new AppDb(this);
        saleNo=newNo("F");
        purchaseNo=newNo("K");
        setContentView(buildRoot());
        showDashboard();
    }
'''
)

replace_between(
    '    private View buildRoot(){',
    'private void showDashboard(){',
    '''    private View buildRoot(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12),dp(10),dp(12),dp(10));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(WIN_BG);

        LinearLayout brand=new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(16),dp(10),dp(16),dp(10));
        brand.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        brand.setBackground(roundBg(WIN_SURFACE,WIN_BORDER,16));
        brand.setElevation(dp(1));

        LinearLayout names=new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView title=label("System Setayesh",19,WIN_TEXT,true);
        title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        TextView sub=label("مدیریت بازرگانی و حسابداری",11,WIN_MUTED,false);
        names.addView(title);
        names.addView(sub,lp(-1,-2,0,1,0,0));
        brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));

        TextView badge=label("Windows 11 UI",10,WIN_ACCENT,true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10),dp(5),dp(10),dp(5));
        badge.setBackground(roundBg(WIN_ACCENT_SOFT,0,20));
        brand.addView(badge);

        root.addView(brand,lp(-1,-2,0,0,0,10));

        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        return root;
    }
'''
)

replace_between(
    '    private void showDashboard(){',
    'private void showProducts(){',
    '''    private void showDashboard(){
        content.removeAllViews();
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        sc.setClipToPadding(false);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(0,0,0,dp(10));
        sc.addView(box,new ScrollView.LayoutParams(-1,-2));
        content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

        AppDb.Summary s=db.summary();

        LinearLayout hero=card();
        hero.setBackground(roundBg(WIN_ACCENT,0,18));
        hero.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView h=label("داشبورد مدیریتی",22,Color.WHITE,true);
        h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        hero.addView(h);
        TextView hs=label("نمای سریع وضعیت کسب‌وکار",12,Color.rgb(225,240,252),false);
        hero.addView(hs,lp(-1,-2,0,2,0,8));
        TextView cash=label("مانده نقد و بانک   "+money(s.cash),17,Color.WHITE,true);
        hero.addView(cash);
        box.addView(hero,lp(-1,-2,0,0,0,10));

        LinearLayout r1=row();
        r1.addView(metric("فروش کل",money(s.sales)),weight());
        r1.addView(metric("خرید کل",money(s.purchases)),weightM());
        box.addView(r1,lp(-1,-2,0,0,0,8));

        LinearLayout r2=row();
        r2.addView(metric("مطالبات",money(s.receivables)),weight());
        r2.addView(metric("بدهی‌ها",money(s.payables)),weightM());
        box.addView(r2,lp(-1,-2,0,0,0,8));

        LinearLayout r3=row();
        r3.addView(metric("ارزش موجودی",money(s.stockValue)),weight());
        r3.addView(metric("کمبود موجودی",String.valueOf(s.lowStock)),weightM());
        box.addView(r3,lp(-1,-2,0,0,0,14));

        box.addView(sectionTitle("عملیات اصلی"),lp(-1,-2,0,0,0,7));
        addMenuRow(box,"کالا و انبار",v->showProducts(),"فروش",v->showSale());
        addMenuRow(box,"خرید",v->showPurchase(),"مشتریان",v->showCustomers());
        addMenuRow(box,"تأمین‌کنندگان",v->showSuppliers(),"ویزیتورها",v->showVisitors());
        addMenuRow(box,"دریافت و پرداخت",v->showFinance(),"گزارش‌ها",v->showReports());
        addMenuRow(box,"بکاپ و بازیابی",v->showBackup(),"ورود Excel",v->pickExcel());

        TextView counts=label("کالا "+s.productCount+"   •   مشتری "+s.customerCount+"   •   تأمین‌کننده "+s.supplierCount,12,WIN_MUTED,false);
        counts.setGravity(Gravity.CENTER);
        box.addView(counts,lp(-1,-2,0,10,0,6));
        animateContent();
    }
'''
)

# Keep the dialog fixes while the source is migrated.
replace_between(
    '    private void productDialog(AppDb.Product p){',
    'private void showCustomers(){',
    '''    private void productDialog(AppDb.Product p){
        LinearLayout f=form();
        EditText name=input("نام کالا",InputType.TYPE_CLASS_TEXT),unit=input("واحد (عدد، بسته، کیلو...)",InputType.TYPE_CLASS_TEXT),purchase=num("قیمت خرید"),retail=num("قیمت خرده"),wholesale=num("قیمت عمده"),stock=intNum("موجودی"),min=intNum("حداقل موجودی");
        if(p!=null){name.setText(p.name);unit.setText(p.unit);purchase.setText(raw(p.purchase));retail.setText(raw(p.retail));wholesale.setText(raw(p.wholesale));stock.setText(""+p.stock);min.setText(""+p.minStock);}else unit.setText("عدد");
        addFields(f,name,unit,purchase,retail,wholesale,stock,min);
        new AlertDialog.Builder(this).setTitle(p==null?"تعریف کالا":"ویرایش کالا").setView(scrollWrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
            if(name.getText().toString().trim().isEmpty()){toast("نام کالا را وارد کنید");return;}
            db.saveProduct(new AppDb.Product(p==null?0:p.id,name.getText().toString().trim(),val(purchase),val(retail),val(wholesale),ival(stock),unit.getText().toString().trim(),ival(min)));
            showProducts();
        }).show();
    }
'''
)

replace_between(
    '    private void customerDialog(AppDb.Customer c){',
    'private void showSuppliers(){',
    '''    private void customerDialog(AppDb.Customer c){
        LinearLayout f=form();EditText name=input("نام مشتری",1),phone=input("شماره تماس",3),address=input("آدرس",1),opening=num("مانده اول دوره (طلب از مشتری)");
        if(c!=null){name.setText(c.name);phone.setText(c.phone);address.setText(c.address);opening.setText(raw(c.openingBalance));}
        addFields(f,name,phone,address,opening);
        new AlertDialog.Builder(this).setTitle(c==null?"مشتری جدید":"ویرایش مشتری").setView(scrollWrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
            if(name.getText().toString().trim().isEmpty()){toast("نام را وارد کنید");return;}
            db.saveCustomer(new AppDb.Customer(c==null?0:c.id,name.getText().toString().trim(),phone.getText().toString(),address.getText().toString(),val(opening)));
            showCustomers();
        }).show();
    }
'''
)

replace_between(
    '    private void supplierDialog(AppDb.Supplier s){',
    'private void showVisitors(){',
    '''    private void supplierDialog(AppDb.Supplier s){
        LinearLayout f=form();EditText name=input("نام تأمین‌کننده",1),phone=input("شماره تماس",3),address=input("آدرس",1),opening=num("مانده اول دوره (بدهی به تأمین‌کننده)");
        if(s!=null){name.setText(s.name);phone.setText(s.phone);address.setText(s.address);opening.setText(raw(s.openingBalance));}
        addFields(f,name,phone,address,opening);
        new AlertDialog.Builder(this).setTitle(s==null?"تأمین‌کننده جدید":"ویرایش تأمین‌کننده").setView(scrollWrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
            if(name.getText().toString().trim().isEmpty()){toast("نام را وارد کنید");return;}
            db.saveSupplier(new AppDb.Supplier(s==null?0:s.id,name.getText().toString().trim(),phone.getText().toString(),address.getText().toString(),val(opening)));
            showSuppliers();
        }).show();
    }
'''
)

replace_between(
    '    private void visitorDialog(AppDb.Visitor s){',
    'private void showSale(){',
    '''    private void visitorDialog(AppDb.Visitor s){
        LinearLayout f=form();EditText name=input("نام ویزیتور",1),phone=input("شماره تماس",3),commission=num("درصد پورسانت"),notes=input("توضیحات",1);
        Button active=btn(s==null||s.active?"وضعیت: فعال":"وضعیت: غیرفعال",null);final boolean[] on={s==null||s.active};
        active.setOnClickListener(v->{on[0]=!on[0];active.setText(on[0]?"وضعیت: فعال":"وضعیت: غیرفعال");});
        if(s!=null){name.setText(s.name);phone.setText(s.phone);commission.setText(raw(s.commissionPercent));notes.setText(s.notes);}
        addFields(f,name,phone,commission,notes);f.addView(active,lp(-1,dp(45),0,4,0,4));
        new AlertDialog.Builder(this).setTitle(s==null?"ویزیتور جدید":"ویرایش ویزیتور").setView(scrollWrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
            if(name.getText().toString().trim().isEmpty()){toast("نام را وارد کنید");return;}
            db.saveVisitor(new AppDb.Visitor(s==null?0:s.id,name.getText().toString().trim(),phone.getText().toString(),val(commission),notes.getText().toString(),on[0]));
            showVisitors();
        }).show();
    }
'''
)

replace_between(
    '    private void financeDialog(String type,String kind,long partyId,String partyName){',
    'private void showReports(){',
    '''    private void financeDialog(String type,String kind,long partyId,String partyName){
        LinearLayout f=form();EditText amount=num("مبلغ"),account=input("حساب (نقد / بانک / کارت)",1),desc=input("شرح",1);account.setText("نقد");addFields(f,amount,account,desc);
        new AlertDialog.Builder(this).setTitle(typeFa(type)+(partyName.isEmpty()?"":" — "+partyName)).setView(f).setNegativeButton("انصراف",null).setPositiveButton("ثبت",(d,w)->{
            if(val(amount)<=0){toast("مبلغ را وارد کنید");return;}
            db.addFinance(type,account.getText().toString(),kind,partyId,partyName,val(amount),desc.getText().toString());
            showFinance();
        }).show();
    }
'''
)

# Replace the shared styling/helper region with lightweight Fluent UI primitives.
replace_between(
    '    private void screenTitle(String title){',
    'private void confirm(String title,String msg,Runnable yes){',
    '''    private void screenTitle(String title){
        content.removeAllViews();
        LinearLayout top=row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(2),dp(2),dp(2),dp(4));
        TextView t=label(title,20,WIN_TEXT,true);
        t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        top.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));
        Button home=btn("خانه",v->showDashboard());
        home.setTextColor(WIN_ACCENT);
        top.addView(home,new LinearLayout.LayoutParams(dp(82),dp(44)));
        content.addView(top,lp(-1,-2,0,0,0,8));
        animateContent();
    }

    private TextView sectionTitle(String s){
        TextView t=label(s,15,WIN_TEXT,true);
        t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }

    private void addMenuRow(LinearLayout parent,String a,View.OnClickListener al,String b,View.OnClickListener bl){
        LinearLayout r=row();
        r.addView(menuBtn(a,al),weight());
        r.addView(menuBtn(b,bl),weightM());
        parent.addView(r,lp(-1,dp(62),0,0,0,8));
    }

    private Button menuBtn(String s,View.OnClickListener l){
        Button b=btn(s,l);
        b.setTextSize(14);
        b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        b.setPadding(dp(14),0,dp(14),0);
        b.setTextColor(WIN_TEXT);
        b.setBackground(rippleBg(WIN_CARD,WIN_BORDER,14));
        b.setElevation(dp(1));
        return b;
    }

    private LinearLayout metric(String title,String value){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(13),dp(11),dp(13),dp(11));
        c.setBackground(roundBg(WIN_CARD,WIN_BORDER,14));
        c.setElevation(dp(1));
        c.addView(label(title,11,WIN_MUTED,false));
        TextView v=label(value,18,WIN_TEXT,true);
        v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        c.addView(v,lp(-1,-2,0,3,0,0));
        return c;
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(11),dp(14),dp(11));
        c.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        c.setBackground(roundBg(WIN_CARD,WIN_BORDER,15));
        c.setElevation(dp(1));
        return c;
    }

    private LinearLayout row(){
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return r;
    }

    private LinearLayout form(){
        LinearLayout f=new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(dp(10),dp(8),dp(10),dp(8));
        f.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        f.setBackgroundColor(WIN_BG);
        return f;
    }

    private ScrollView scrollWrap(View v){
        ScrollView s=new ScrollView(this);
        s.setFillViewport(true);
        s.addView(v);
        return s;
    }

    private void addFields(LinearLayout f,EditText... es){
        for(EditText e:es)f.addView(e,lp(-1,dp(50),0,0,0,7));
    }

    private EditText input(String hint,int type){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(125,125,125));
        e.setTextColor(WIN_TEXT);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        e.setInputType(type);
        e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        e.setPadding(dp(12),0,dp(12),0);
        e.setBackground(roundBg(WIN_SURFACE,WIN_BORDER,11));
        return e;
    }

    private EditText num(String hint){return input(hint,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);}
    private EditText intNum(String hint){return input(hint,InputType.TYPE_CLASS_NUMBER);}
    private EditText searchBox(String h){
        EditText e=input("⌕  "+h,InputType.TYPE_CLASS_TEXT);
        e.setBackground(roundBg(Color.WHITE,WIN_BORDER,14));
        return e;
    }

    private TextView label(String s,float size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        if(color==Color.DKGRAY) color=WIN_TEXT;
        else if(color==Color.GRAY) color=WIN_MUTED;
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        return t;
    }

    private TextView empty(String s){
        TextView t=label(s,13,WIN_MUTED,false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0,dp(28),0,dp(28));
        return t;
    }

    private Button btn(String s,View.OnClickListener l){
        Button b=new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setTextColor(WIN_TEXT);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(12),0,dp(12),0);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setBackground(rippleBg(WIN_SURFACE,WIN_BORDER,11));
        b.setElevation(0);
        if(l!=null)b.setOnClickListener(l);
        return b;
    }

    private RippleDrawable rippleBg(int fill,int stroke,int radius){
        GradientDrawable g=roundBg(fill,stroke,radius);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(28,0,103,192)),g,null);
    }

    private GradientDrawable roundBg(int fill,int stroke,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if(stroke!=0)g.setStroke(dp(1),stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);
        p.setMargins(dp(l),dp(t),dp(r),dp(b));
        return p;
    }

    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1);}
    private LinearLayout.LayoutParams weightM(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
        p.setMargins(dp(7),0,0,0);
        return p;
    }

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}

    private TextWatcher watcher(Runnable r){
        return new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){
                if(pendingSearch!=null)uiHandler.removeCallbacks(pendingSearch);
                pendingSearch=r;
                uiHandler.postDelayed(pendingSearch,110);
            }
            public void afterTextChanged(Editable e){}
        };
    }

    private void animateContent(){
        content.setAlpha(0f);
        content.setTranslationY(dp(6));
        content.animate().alpha(1f).translationY(0f).setDuration(130).setInterpolator(new DecelerateInterpolator()).start();
    }
'''
)

p.write_text(s)
