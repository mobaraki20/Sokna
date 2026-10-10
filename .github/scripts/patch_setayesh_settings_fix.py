from pathlib import Path

p=Path('app/src/main/java/com/example/excelpricesearch/MainActivity.java')
s=p.read_text()
if 'private View buildRoot()' not in s:
    marker='    private void loadAppearanceSettings(){'
    if marker not in s:
        raise SystemExit('loadAppearanceSettings marker missing')
    method='''    private View buildRoot(){
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
        title.setTypeface(resolveTypeface(true));
        TextView sub=label("اکسسوری مادر • مدیریت بازرگانی و حسابداری",11,WIN_MUTED,false);
        names.addView(title);
        names.addView(sub,lp(-1,-2,0,1,0,0));
        brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));

        TextView badge=label(currentThemeName(),10,WIN_ACCENT,true);
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
    s=s.replace(marker,method+marker,1)
p.write_text(s)
