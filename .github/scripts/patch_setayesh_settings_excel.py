from pathlib import Path
import re

p=Path('app/src/main/java/com/example/excelpricesearch/MainActivity.java')
s=p.read_text()

def once(old,new,name):
    global s
    if old not in s: raise SystemExit('missing '+name)
    s=s.replace(old,new,1)

def between(start,next_sig,body,name):
    global s
    pat=re.escape(start)+r'.*?(?=\n\s*'+re.escape(next_sig)+r')'
    s,n=re.subn(pat,body.rstrip(),s,count=1,flags=re.S)
    if n!=1: raise SystemExit('missing range '+name)

# imports
once('import android.content.Intent;\n','import android.content.Intent;\nimport android.content.SharedPreferences;\n','prefs import')

# request code for custom font
once('PICK_IMAGE_REFERENCE=4003, CAPTURE_IMAGE_REFERENCE=4004;','PICK_IMAGE_REFERENCE=4003, CAPTURE_IMAGE_REFERENCE=4004, PICK_CUSTOM_FONT=5001;','font request')

# turn fixed Windows palette into user-selectable runtime palette
old='''    private static final int WIN_BG = Color.rgb(243,243,243);\n    private static final int WIN_SURFACE = Color.rgb(251,251,251);\n    private static final int WIN_CARD = Color.WHITE;\n    private static final int WIN_TEXT = Color.rgb(28,28,28);\n    private static final int WIN_MUTED = Color.rgb(96,96,96);\n    private static final int WIN_BORDER = Color.rgb(225,225,225);\n    private static final int WIN_ACCENT = Color.rgb(0,103,192);\n    private static final int WIN_ACCENT_SOFT = Color.rgb(230,242,252);\n    private final Handler uiHandler = new Handler(Looper.getMainLooper());\n    private Runnable pendingSearch;\n'''
new='''    private int WIN_BG = Color.rgb(243,243,243);\n    private int WIN_SURFACE = Color.rgb(251,251,251);\n    private int WIN_CARD = Color.WHITE;\n    private int WIN_TEXT = Color.rgb(28,28,28);\n    private int WIN_MUTED = Color.rgb(96,96,96);\n    private int WIN_BORDER = Color.rgb(225,225,225);\n    private int WIN_ACCENT = Color.rgb(0,103,192);\n    private int WIN_ACCENT_SOFT = Color.rgb(230,242,252);\n    private final Handler uiHandler = new Handler(Looper.getMainLooper());\n    private Runnable pendingSearch;\n    private SharedPreferences appearancePrefs;\n    private int themeIndex=1;\n    private float appFontScale=1f;\n    private String appFontFamily=\"sans-serif\";\n    private boolean appForceBold=false;\n    private Typeface customTypeface;\n'''
once(old,new,'palette')

# load saved appearance before splash/main UI
once('''        super.onCreate(b);\n        db=new AppDb(this);''','''        super.onCreate(b);\n        loadAppearanceSettings();\n        db=new AppDb(this);''','load appearance')

# dynamic system bars and theme badge
between('    private void launchMainUi(){','private void showProducts(){', '''    private void launchMainUi(){
        applySystemBars();
        setContentView(buildRoot());
        showDashboard();
    }

    private void loadAppearanceSettings(){
        appearancePrefs=getSharedPreferences("appearance",MODE_PRIVATE);
        themeIndex=appearancePrefs.getInt("theme",1);
        appFontScale=appearancePrefs.getFloat("font_scale",1f);
        appFontFamily=appearancePrefs.getString("font_family","sans-serif");
        appForceBold=appearancePrefs.getBoolean("font_bold",false);
        File custom=new File(getFilesDir(),"setayesh_custom_font.ttf");
        customTypeface=null;
        if("custom".equals(appFontFamily)&&custom.exists())try{customTypeface=Typeface.createFromFile(custom);}catch(Exception ignored){}
        applyTheme(themeIndex);
    }

    private void applyTheme(int idx){
        themeIndex=Math.max(0,Math.min(8,idx));
        switch(themeIndex){
            case 0: // Classic Gold
                WIN_BG=Color.rgb(20,20,20);WIN_SURFACE=Color.rgb(29,29,29);WIN_CARD=Color.rgb(35,35,35);WIN_TEXT=Color.rgb(244,235,210);WIN_MUTED=Color.rgb(183,170,140);WIN_BORDER=Color.rgb(78,67,43);WIN_ACCENT=Color.rgb(205,164,52);WIN_ACCENT_SOFT=Color.rgb(58,49,27);break;
            case 1: // Windows 11 Light
                WIN_BG=Color.rgb(243,243,243);WIN_SURFACE=Color.rgb(251,251,251);WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(28,28,28);WIN_MUTED=Color.rgb(96,96,96);WIN_BORDER=Color.rgb(225,225,225);WIN_ACCENT=Color.rgb(0,103,192);WIN_ACCENT_SOFT=Color.rgb(230,242,252);break;
            case 2: // Windows 11 Dark
                WIN_BG=Color.rgb(31,31,31);WIN_SURFACE=Color.rgb(38,38,38);WIN_CARD=Color.rgb(45,45,45);WIN_TEXT=Color.rgb(245,245,245);WIN_MUTED=Color.rgb(178,178,178);WIN_BORDER=Color.rgb(62,62,62);WIN_ACCENT=Color.rgb(96,205,255);WIN_ACCENT_SOFT=Color.rgb(31,66,83);break;
            case 3: // Luxury Black
                WIN_BG=Color.rgb(9,9,10);WIN_SURFACE=Color.rgb(18,18,19);WIN_CARD=Color.rgb(24,24,26);WIN_TEXT=Color.rgb(250,250,250);WIN_MUTED=Color.rgb(170,170,170);WIN_BORDER=Color.rgb(55,55,58);WIN_ACCENT=Color.rgb(232,196,102);WIN_ACCENT_SOFT=Color.rgb(63,53,29);break;
            case 4: // Blue Professional
                WIN_BG=Color.rgb(237,244,251);WIN_SURFACE=Color.rgb(249,252,255);WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(25,42,62);WIN_MUTED=Color.rgb(89,111,135);WIN_BORDER=Color.rgb(203,220,237);WIN_ACCENT=Color.rgb(26,92,164);WIN_ACCENT_SOFT=Color.rgb(221,235,250);break;
            case 5: // Emerald Business
                WIN_BG=Color.rgb(239,247,244);WIN_SURFACE=Color.rgb(250,253,252);WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(25,55,47);WIN_MUTED=Color.rgb(84,115,105);WIN_BORDER=Color.rgb(204,225,217);WIN_ACCENT=Color.rgb(20,126,96);WIN_ACCENT_SOFT=Color.rgb(218,241,233);break;
            case 6: // Purple Modern
                WIN_BG=Color.rgb(246,242,250);WIN_SURFACE=Color.rgb(252,250,254);WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(54,36,67);WIN_MUTED=Color.rgb(113,93,126);WIN_BORDER=Color.rgb(226,214,234);WIN_ACCENT=Color.rgb(119,67,156);WIN_ACCENT_SOFT=Color.rgb(239,228,247);break;
            case 7: // Minimal White
                WIN_BG=Color.rgb(250,250,250);WIN_SURFACE=Color.WHITE;WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(20,20,20);WIN_MUTED=Color.rgb(110,110,110);WIN_BORDER=Color.rgb(232,232,232);WIN_ACCENT=Color.rgb(35,35,35);WIN_ACCENT_SOFT=Color.rgb(239,239,239);break;
            default: // Mother Accessories brand
                WIN_BG=Color.rgb(248,243,239);WIN_SURFACE=Color.rgb(255,252,249);WIN_CARD=Color.WHITE;WIN_TEXT=Color.rgb(64,44,40);WIN_MUTED=Color.rgb(126,99,91);WIN_BORDER=Color.rgb(232,215,206);WIN_ACCENT=Color.rgb(174,116,87);WIN_ACCENT_SOFT=Color.rgb(245,229,220);break;
        }
    }

    private String currentThemeName(){String[] n={"Classic Gold","Windows 11 Light","Windows 11 Dark","Luxury Black","Blue Professional","Emerald Business","Purple Modern","Minimal White","Mother Accessories"};return n[themeIndex];}
    private void applySystemBars(){getWindow().setStatusBarColor(WIN_SURFACE);getWindow().setNavigationBarColor(WIN_SURFACE);int flags=0;if(isLight(WIN_SURFACE)){flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(android.os.Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;}getWindow().getDecorView().setSystemUiVisibility(flags);}
    private boolean isLight(int c){double y=.299*Color.red(c)+.587*Color.green(c)+.114*Color.blue(c);return y>150;}
    private Typeface resolveTypeface(boolean bold){if(customTypeface!=null&&"custom".equals(appFontFamily))return Typeface.create(customTypeface,(bold||appForceBold)?Typeface.BOLD:Typeface.NORMAL);String fam=appFontFamily;if(fam==null||fam.isEmpty()||"system".equals(fam))fam="sans-serif";return Typeface.create(fam,(bold||appForceBold)?Typeface.BOLD:Typeface.NORMAL);}

    private void showDashboard(){
        content.removeAllViews();
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);box.setPadding(0,0,0,dp(10));sc.addView(box,new ScrollView.LayoutParams(-1,-2));content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        AppDb.Summary s=db.summary();
        LinearLayout hero=card();hero.setBackground(roundBg(WIN_ACCENT,0,18));hero.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView h=label("داشبورد مدیریتی",22,contrastText(WIN_ACCENT),true);hero.addView(h);TextView hs=label("اکسسوری مادر • نمای سریع وضعیت کسب‌وکار",12,contrastMuted(WIN_ACCENT),false);hero.addView(hs,lp(-1,-2,0,2,0,8));hero.addView(label("مانده نقد و بانک   "+money(s.cash),17,contrastText(WIN_ACCENT),true));box.addView(hero,lp(-1,-2,0,0,0,10));
        LinearLayout r1=row();r1.addView(metric("فروش کل",money(s.sales)),weight());r1.addView(metric("خرید کل",money(s.purchases)),weightM());box.addView(r1,lp(-1,-2,0,0,0,8));
        LinearLayout r2=row();r2.addView(metric("مطالبات",money(s.receivables)),weight());r2.addView(metric("بدهی‌ها",money(s.payables)),weightM());box.addView(r2,lp(-1,-2,0,0,0,8));
        LinearLayout r3=row();r3.addView(metric("ارزش موجودی",money(s.stockValue)),weight());r3.addView(metric("کمبود موجودی",String.valueOf(s.lowStock)),weightM());box.addView(r3,lp(-1,-2,0,0,0,14));
        box.addView(sectionTitle("عملیات اصلی"),lp(-1,-2,0,0,0,7));
        addMenuRow(box,"کالا و انبار",v->showProducts(),"فروش",v->showSale());addMenuRow(box,"خرید",v->showPurchase(),"مشتریان",v->showCustomers());addMenuRow(box,"تأمین‌کنندگان",v->showSuppliers(),"ویزیتورها",v->showVisitors());addMenuRow(box,"دریافت و پرداخت",v->showFinance(),"گزارش‌ها",v->showReports());addMenuRow(box,"بکاپ و بازیابی",v->showBackup(),"ورود اطلاعات Excel",v->pickExcel());addMenuRow(box,"تنظیمات",v->showSettings(),"راهنمای قالب Excel",v->showExcelGuide());
        TextView counts=label("کالا "+s.productCount+"   •   مشتری "+s.customerCount+"   •   تأمین‌کننده "+s.supplierCount,12,WIN_MUTED,false);counts.setGravity(Gravity.CENTER);box.addView(counts,lp(-1,-2,0,10,0,6));animateContent();
    }

    private int contrastText(int bg){return isLight(bg)?Color.rgb(20,20,20):Color.WHITE;}
    private int contrastMuted(int bg){return isLight(bg)?Color.rgb(70,70,70):Color.rgb(225,225,225);}
''','launch/dashboard')

# badge should reflect selected theme
s=s.replace('label("Windows 11 UI",10,WIN_ACCENT,true)','label(currentThemeName(),10,WIN_ACCENT,true)')

# replace hardcoded typefaces throughout already-patched source
s=s.replace('Typeface.create("sans-serif-medium",Typeface.NORMAL)','resolveTypeface(true)')
s=s.replace('Typeface.create("sans-serif",Typeface.NORMAL)','resolveTypeface(false)')

# settings UI inserted before backup
marker='    private void showBackup(){'
if marker not in s: raise SystemExit('missing backup marker')
settings=r'''    private void showSettings(){
        screenTitle("تنظیمات ظاهر و فونت");
        ScrollView sc=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);sc.addView(box);content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout preview=card();preview.addView(label("اکسسوری مادر",22,WIN_TEXT,true));preview.addView(label("پیش‌نمایش تم و فونت انتخاب‌شده",13,WIN_MUTED,false),lp(-1,-2,0,3,0,5));TextView chip=label(currentThemeName(),12,contrastText(WIN_ACCENT),true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(10),dp(6),dp(10),dp(6));chip.setBackground(roundBg(WIN_ACCENT,0,16));preview.addView(chip);box.addView(preview,lp(-1,-2,0,0,0,10));
        box.addView(btn("تم برنامه: "+currentThemeName(),v->chooseTheme()),lp(-1,dp(52),0,0,0,7));
        box.addView(btn("فونت: "+fontDisplayName(),v->chooseFont()),lp(-1,dp(52),0,0,0,7));
        box.addView(btn("اندازه فونت: "+fontSizeName(),v->chooseFontSize()),lp(-1,dp(52),0,0,0,7));
        box.addView(btn("ضخامت متن: "+(appForceBold?"Bold":"معمولی"),v->{appForceBold=!appForceBold;appearancePrefs.edit().putBoolean("font_bold",appForceBold).apply();reloadSettingsScreen();}),lp(-1,dp(52),0,0,0,7));
        box.addView(btn("انتخاب فونت سفارشی TTF / OTF",v->pickCustomFont()),lp(-1,dp(52),0,0,0,7));
        box.addView(btn("بازنشانی ظاهر به Windows 11 Light",v->{appearancePrefs.edit().clear().apply();loadAppearanceSettings();reloadSettingsScreen();}),lp(-1,dp(52),0,4,0,10));
        box.addView(label("تنظیمات تم، فونت، اندازه و ضخامت روی دستگاه ذخیره می‌شوند و در اجرای بعدی نیز باقی می‌مانند.",12,WIN_MUTED,false));animateContent();
    }

    private void chooseTheme(){String[] names={"Classic Gold","Windows 11 Light","Windows 11 Dark","Luxury Black","Blue Professional","Emerald Business","Purple Modern","Minimal White","Mother Accessories"};new AlertDialog.Builder(this).setTitle("انتخاب تم").setSingleChoiceItems(names,themeIndex,(d,w)->{appearancePrefs.edit().putInt("theme",w).apply();loadAppearanceSettings();d.dismiss();reloadSettingsScreen();}).setNegativeButton("انصراف",null).show();}
    private void chooseFont(){String[] labels={"پیش‌فرض سیستم","ساده و خوانا","متوسط","Serif","Monospace","فشرده","فونت سفارشی"};String[] vals={"system","sans-serif","sans-serif-medium","serif","monospace","sans-serif-condensed","custom"};int selected=0;for(int i=0;i<vals.length;i++)if(vals[i].equals(appFontFamily))selected=i;new AlertDialog.Builder(this).setTitle("انتخاب فونت").setSingleChoiceItems(labels,selected,(d,w)->{if("custom".equals(vals[w])&&!new File(getFilesDir(),"setayesh_custom_font.ttf").exists()){d.dismiss();pickCustomFont();return;}appearancePrefs.edit().putString("font_family",vals[w]).apply();loadAppearanceSettings();d.dismiss();reloadSettingsScreen();}).setNegativeButton("انصراف",null).show();}
    private void chooseFontSize(){String[] n={"کوچک","متوسط","بزرگ","خیلی بزرگ"};float[] v={.88f,1f,1.14f,1.28f};int sel=1;float cur=appFontScale;for(int i=0;i<v.length;i++)if(Math.abs(cur-v[i])<.03)sel=i;new AlertDialog.Builder(this).setTitle("اندازه فونت").setSingleChoiceItems(n,sel,(d,w)->{appearancePrefs.edit().putFloat("font_scale",v[w]).apply();loadAppearanceSettings();d.dismiss();reloadSettingsScreen();}).setNegativeButton("انصراف",null).show();}
    private String fontDisplayName(){if("custom".equals(appFontFamily))return "سفارشی";if("serif".equals(appFontFamily))return "Serif";if("monospace".equals(appFontFamily))return "Monospace";if("sans-serif-condensed".equals(appFontFamily))return "فشرده";if("sans-serif-medium".equals(appFontFamily))return "متوسط";if("system".equals(appFontFamily))return "پیش‌فرض سیستم";return "ساده و خوانا";}
    private String fontSizeName(){if(appFontScale<.94f)return "کوچک";if(appFontScale<1.07f)return "متوسط";if(appFontScale<1.21f)return "بزرگ";return "خیلی بزرگ";}
    private void reloadSettingsScreen(){applySystemBars();setContentView(buildRoot());showSettings();}
    private void pickCustomFont(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_CUSTOM_FONT);}

    private void showExcelGuide(){
        screenTitle("راهنمای قالب Excel");ScrollView sc=new ScrollView(this);LinearLayout b=form();
        b.addView(label("فایل نمونه چندشیتی",19,WIN_TEXT,true));b.addView(label("برای ورود گروهی اطلاعات، نام شیت‌ها و عنوان ستون‌ها را مانند الگوی زیر نگه دارید. ردیف اول عنوان ستون‌هاست.",13,WIN_MUTED,false),lp(-1,-2,0,4,0,12));
        String[] guides={"کالاها\nکد کالا | نام کالا | دسته | زیر دسته | برند | شرکت | بارکد | قیمت خرید | قیمت فروش | موجودی | واحد | تصویر","مشتریان\nکد مشتری | نام | نام خانوادگی | تلفن | آدرس | اعتبار | توضیحات","تامین کنندگان\nکد تامین کننده | نام شرکت | نام مسئول | تلفن | آدرس | توضیحات","ویزیتورها\nکد ویزیتور | نام | تلفن | درصد پورسانت | توضیحات"};
        for(String g:guides){LinearLayout c=card();c.addView(label(g,13,WIN_TEXT,false));b.addView(c,lp(-1,-2,0,0,0,7));}
        b.addView(label("ورود اطلاعات، رکوردهای دارای نام/تلفن یا بارکد مشابه را تا حد ممکن بروزرسانی می‌کند و از ایجاد تکراری‌های واضح جلوگیری می‌کند.",12,WIN_MUTED,false),lp(-1,-2,0,5,0,5));sc.addView(b);content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));animateContent();
    }

'''
s=s.replace(marker,settings+marker,1)

# font-aware core widgets
between('    private EditText input(String hint,int type){','private EditText num(String hint){', '''    private EditText input(String hint,int type){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(WIN_MUTED);e.setTextColor(WIN_TEXT);e.setSingleLine(true);e.setTextSize(14*appFontScale);e.setTypeface(resolveTypeface(false));e.setInputType(type);e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);e.setPadding(dp(12),0,dp(12),0);e.setBackground(roundBg(WIN_SURFACE,WIN_BORDER,11));return e;
    }
''','input')
between('    private TextView label(String s,float size,int color,boolean bold){','private TextView empty(String s){', '''    private TextView label(String s,float size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size*appFontScale);if(color==Color.DKGRAY)color=WIN_TEXT;else if(color==Color.GRAY)color=WIN_MUTED;t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setTypeface(resolveTypeface(bold));return t;
    }
''','label')
between('    private Button btn(String s,View.OnClickListener l){','private RippleDrawable rippleBg(int fill,int stroke,int radius){', '''    private Button btn(String s,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextSize(13*appFontScale);b.setTextColor(WIN_TEXT);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(12),0,dp(12),0);b.setTypeface(resolveTypeface(true));b.setBackground(rippleBg(WIN_SURFACE,WIN_BORDER,11));b.setElevation(0);if(l!=null)b.setOnClickListener(l);return b;
    }
''','button')

# full multi-sheet Excel importer
between('    private void importExcel(Uri uri){','private void writePdf(Uri uri){', '''    private void importExcel(Uri uri){
        try{File tmp=new File(getCacheDir(),"setayesh_import.xlsx");copy(getContentResolver().openInputStream(uri),new FileOutputStream(tmp));FullXlsxImporter.Result result=FullXlsxImporter.importAll(tmp,db);tmp.delete();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("نتیجه ورود Excel").setMessage(result.summary()).setPositiveButton("باشه",(d,w)->showDashboard()).show());}catch(Exception e){runOnUiThread(()->toast("خطا در Excel: "+e.getMessage()));}
    }
''','excel import')

# custom font result handling inside existing activity-result method
needle='''            if(data==null||data.getData()==null)return;Uri uri=data.getData();\n            if(req==PICK_IMAGE_REFERENCE||req==PICK_IMAGE_IDENTIFY){'''
repl='''            if(data==null||data.getData()==null)return;Uri uri=data.getData();\n            if(req==PICK_CUSTOM_FONT){try{File dst=new File(getFilesDir(),"setayesh_custom_font.ttf");copy(getContentResolver().openInputStream(uri),new FileOutputStream(dst));Typeface.createFromFile(dst);appearancePrefs.edit().putString("font_family","custom").apply();loadAppearanceSettings();reloadSettingsScreen();toast("فونت سفارشی اعمال شد");}catch(Exception ex){toast("فایل فونت معتبر نیست");}return;}\n            if(req==PICK_IMAGE_REFERENCE||req==PICK_IMAGE_IDENTIFY){'''
once(needle,repl,'font result')

p.write_text(s)
