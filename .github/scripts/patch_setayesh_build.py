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
''')

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
''')

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
''')

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
''')

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
''')

p.write_text(s)
