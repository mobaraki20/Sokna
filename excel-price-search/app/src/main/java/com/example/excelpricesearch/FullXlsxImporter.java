package com.example.excelpricesearch;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

public class FullXlsxImporter {
    public static class Result {
        public int products, customers, suppliers, visitors, skipped;
        public final ArrayList<String> errors = new ArrayList<>();
        public String summary(){
            return "کالا: "+products+"\nمشتری: "+customers+"\nتأمین‌کننده: "+suppliers+"\nویزیتور: "+visitors+"\nردشده: "+skipped+(errors.isEmpty()?"":"\n\nخطاها: "+Math.min(errors.size(),20));
        }
    }

    public static Result importAll(File file, AppDb db) throws Exception {
        Result result = new Result();
        try(ZipFile zip = new ZipFile(file)){
            ArrayList<String> shared = readShared(zip);
            Map<String,String> rels = readWorkbookRelationships(zip);
            Document workbook = parse(zip.getInputStream(required(zip,"xl/workbook.xml")));
            NodeList sheets = workbook.getElementsByTagName("sheet");
            for(int i=0;i<sheets.getLength();i++){
                Element sh=(Element)sheets.item(i);
                String name=clean(sh.getAttribute("name"));
                String rid=sh.getAttribute("r:id");
                if(rid==null||rid.isEmpty()) rid=sh.getAttribute("id");
                String target=rels.get(rid);
                if(target==null) continue;
                String path=target.startsWith("/")?target.substring(1):"xl/"+target.replace("../","");
                ZipEntry e=zip.getEntry(path);
                if(e==null) continue;
                ArrayList<String[]> rows=readRows(zip.getInputStream(e),shared);
                if(rows.isEmpty()) continue;
                if(eq(name,"کالاها")||name.contains("کالا")) importProducts(rows,db,result);
                else if(eq(name,"مشتریان")||name.contains("مشتری")) importCustomers(rows,db,result);
                else if(name.contains("تامین")||name.contains("تأمین")) importSuppliers(rows,db,result);
                else if(name.contains("ویزیتور")) importVisitors(rows,db,result);
            }
        }
        return result;
    }

    private static void importProducts(ArrayList<String[]> rows,AppDb db,Result out){
        Map<String,Integer> h=headers(rows.get(0));
        boolean modern=h.containsKey("نام کالا")||h.containsKey("بارکد")||h.containsKey("قیمت فروش");
        for(int r=1;r<rows.size();r++){
            try{
                String[] row=rows.get(r);
                String name=modern?get(row,h,"نام کالا"):get(row,0);
                if(name.isEmpty()){out.skipped++;continue;}
                String barcode=modern?get(row,h,"بارکد"):"";
                double purchase=modern?num(get(row,h,"قیمت خرید")):num(get(row,3));
                double retail=modern?num(get(row,h,"قیمت فروش")):num(get(row,1));
                double wholesale=modern?(h.containsKey("قیمت عمده")?num(get(row,h,"قیمت عمده")):retail):num(get(row,2));
                int stock=(int)Math.max(0,Math.round(modern?num(get(row,h,"موجودی")):num(get(row,4))));
                String unit=modern?get(row,h,"واحد"):"عدد"; if(unit.isEmpty())unit="عدد";
                AppDb.Product existing=null;
                if(!barcode.isEmpty()) existing=db.findProductByBarcode(barcode);
                if(existing==null){for(AppDb.Product p:db.searchProducts(name))if(p.name.equalsIgnoreCase(name)){existing=p;break;}}
                long id=existing==null?0:existing.id;
                String img=existing==null?"":existing.imageHash;
                int min=existing==null?0:existing.minStock;
                db.saveProduct(new AppDb.Product(id,name,purchase,retail,wholesale,stock,unit,min,barcode,img));
                out.products++;
            }catch(Exception e){out.skipped++;out.errors.add("کالا ردیف "+(r+1)+": "+safe(e.getMessage()));}
        }
    }

    private static void importCustomers(ArrayList<String[]> rows,AppDb db,Result out){
        Map<String,Integer> h=headers(rows.get(0));
        for(int r=1;r<rows.size();r++){
            try{
                String[] row=rows.get(r);
                String first=get(row,h,"نام"), last=get(row,h,"نام خانوادگی");
                String name=(first+" "+last).trim(); if(name.isEmpty())name=get(row,h,"نام مشتری");
                if(name.isEmpty()){out.skipped++;continue;}
                String phone=get(row,h,"تلفن"), address=get(row,h,"آدرس");
                double opening=num(firstNonEmpty(get(row,h,"اعتبار"),get(row,h,"مانده اول دوره")));
                AppDb.Customer found=null;for(AppDb.Customer c:db.searchCustomers(phone.isEmpty()?name:phone))if(c.name.equalsIgnoreCase(name)||(!phone.isEmpty()&&phone.equals(c.phone))){found=c;break;}
                db.saveCustomer(new AppDb.Customer(found==null?0:found.id,name,phone,address,opening));out.customers++;
            }catch(Exception e){out.skipped++;out.errors.add("مشتری ردیف "+(r+1)+": "+safe(e.getMessage()));}
        }
    }

    private static void importSuppliers(ArrayList<String[]> rows,AppDb db,Result out){
        Map<String,Integer> h=headers(rows.get(0));
        for(int r=1;r<rows.size();r++){
            try{
                String[] row=rows.get(r);String name=firstNonEmpty(get(row,h,"نام شرکت"),get(row,h,"نام تامین کننده"),get(row,h,"نام تأمین کننده"));
                if(name.isEmpty()){out.skipped++;continue;}String phone=get(row,h,"تلفن"),address=get(row,h,"آدرس");
                AppDb.Supplier found=null;for(AppDb.Supplier s:db.searchSuppliers(phone.isEmpty()?name:phone))if(s.name.equalsIgnoreCase(name)||(!phone.isEmpty()&&phone.equals(s.phone))){found=s;break;}
                db.saveSupplier(new AppDb.Supplier(found==null?0:found.id,name,phone,address,found==null?0:found.openingBalance));out.suppliers++;
            }catch(Exception e){out.skipped++;out.errors.add("تأمین‌کننده ردیف "+(r+1)+": "+safe(e.getMessage()));}
        }
    }

    private static void importVisitors(ArrayList<String[]> rows,AppDb db,Result out){
        Map<String,Integer> h=headers(rows.get(0));
        for(int r=1;r<rows.size();r++){
            try{
                String[] row=rows.get(r);String name=get(row,h,"نام");if(name.isEmpty())name=get(row,h,"نام ویزیتور");if(name.isEmpty()){out.skipped++;continue;}
                String phone=get(row,h,"تلفن"),notes=get(row,h,"توضیحات");double commission=num(get(row,h,"درصد پورسانت"));
                AppDb.Visitor found=null;for(AppDb.Visitor v:db.searchVisitors(phone.isEmpty()?name:phone))if(v.name.equalsIgnoreCase(name)||(!phone.isEmpty()&&phone.equals(v.phone))){found=v;break;}
                db.saveVisitor(new AppDb.Visitor(found==null?0:found.id,name,phone,commission,notes,found==null||found.active));out.visitors++;
            }catch(Exception e){out.skipped++;out.errors.add("ویزیتور ردیف "+(r+1)+": "+safe(e.getMessage()));}
        }
    }

    private static Map<String,Integer> headers(String[] row){Map<String,Integer> m=new HashMap<>();for(int i=0;i<row.length;i++){String k=norm(row[i]);if(!k.isEmpty())m.put(k,i);}return m;}
    private static String get(String[] row,Map<String,Integer> h,String key){Integer i=h.get(norm(key));return i==null?"":get(row,i);}
    private static String get(String[] row,int i){return i>=0&&i<row.length?clean(row[i]):"";}
    private static String norm(String s){return clean(s).replace('ي','ی').replace('ك','ک').replace("‌"," ").replaceAll("\\s+"," ");}
    private static boolean eq(String a,String b){return norm(a).equals(norm(b));}
    private static String firstNonEmpty(String... xs){for(String x:xs)if(x!=null&&!x.trim().isEmpty())return x.trim();return "";}
    private static String safe(String s){return s==null?"خطای نامشخص":s;}

    private static ArrayList<String[]> readRows(InputStream in,ArrayList<String> shared)throws Exception{
        Document doc=parse(in);NodeList rows=doc.getElementsByTagName("row");ArrayList<String[]> out=new ArrayList<>();
        for(int i=0;i<rows.getLength();i++){
            Element row=(Element)rows.item(i);NodeList cells=row.getElementsByTagName("c");int max=0;
            for(int j=0;j<cells.getLength();j++)max=Math.max(max,columnIndex(((Element)cells.item(j)).getAttribute("r")));
            String[] vals=new String[Math.max(1,max+1)];for(int k=0;k<vals.length;k++)vals[k]="";
            for(int j=0;j<cells.getLength();j++){Element c=(Element)cells.item(j);int col=columnIndex(c.getAttribute("r"));if(col>=0&&col<vals.length)vals[col]=cellValue(c,shared);}
            out.add(vals);
        }return out;
    }

    private static Map<String,String> readWorkbookRelationships(ZipFile zip)throws Exception{
        Map<String,String> out=new HashMap<>();ZipEntry e=required(zip,"xl/_rels/workbook.xml.rels");Document d=parse(zip.getInputStream(e));NodeList rs=d.getElementsByTagName("Relationship");
        for(int i=0;i<rs.getLength();i++){Element x=(Element)rs.item(i);out.put(x.getAttribute("Id"),x.getAttribute("Target"));}return out;
    }
    private static ZipEntry required(ZipFile z,String path)throws Exception{ZipEntry e=z.getEntry(path);if(e==null)throw new Exception("بخش "+path+" در فایل Excel پیدا نشد");return e;}
    private static ArrayList<String> readShared(ZipFile zip)throws Exception{ArrayList<String> out=new ArrayList<>();ZipEntry e=zip.getEntry("xl/sharedStrings.xml");if(e==null)return out;Document d=parse(zip.getInputStream(e));NodeList sis=d.getElementsByTagName("si");for(int i=0;i<sis.getLength();i++){NodeList ts=((Element)sis.item(i)).getElementsByTagName("t");StringBuilder s=new StringBuilder();for(int j=0;j<ts.getLength();j++)s.append(ts.item(j).getTextContent());out.add(s.toString());}return out;}
    private static String cellValue(Element c,ArrayList<String> shared){String type=c.getAttribute("t");if("inlineStr".equals(type)){NodeList ts=c.getElementsByTagName("t");StringBuilder s=new StringBuilder();for(int i=0;i<ts.getLength();i++)s.append(ts.item(i).getTextContent());return s.toString();}NodeList vs=c.getElementsByTagName("v");if(vs.getLength()==0)return "";String v=vs.item(0).getTextContent();if("s".equals(type)){try{int idx=Integer.parseInt(v);return idx>=0&&idx<shared.size()?shared.get(idx):"";}catch(Exception ignored){return "";}}return v;}
    private static int columnIndex(String ref){int col=0,i=0;while(i<ref.length()&&Character.isLetter(ref.charAt(i))){col=col*26+(Character.toUpperCase(ref.charAt(i))-'A'+1);i++;}return col-1;}
    private static double num(String raw){if(raw==null)return 0;String s=raw.trim().replace(",","").replace("٬","").replace(" ","");StringBuilder a=new StringBuilder();for(int i=0;i<s.length();i++){char ch=s.charAt(i);if(ch>='۰'&&ch<='۹')a.append((char)('0'+ch-'۰'));else if(ch>='٠'&&ch<='٩')a.append((char)('0'+ch-'٠'));else a.append(ch);}try{return Double.parseDouble(a.toString());}catch(Exception e){return 0;}}
    private static String clean(String s){return s==null?"":s.replace('\n',' ').replace('\r',' ').trim();}
    private static Document parse(InputStream in)throws Exception{try(InputStream input=in){DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);f.setExpandEntityReferences(false);return f.newDocumentBuilder().parse(input);}}
}
