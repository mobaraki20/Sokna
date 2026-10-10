package com.example.excelpricesearch;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

public class XlsxReader {
    public static List<AppDb.Product> read(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            ArrayList<String> shared = readShared(zip);
            ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheet == null) {
                for (java.util.Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                    ZipEntry z = e.nextElement();
                    if (z.getName().startsWith("xl/worksheets/") && z.getName().endsWith(".xml")) {
                        sheet = z;
                        break;
                    }
                }
            }
            if (sheet == null) throw new Exception("Worksheet پیدا نشد");

            Document doc = parse(zip.getInputStream(sheet));
            NodeList rows = doc.getElementsByTagName("row");
            ArrayList<AppDb.Product> result = new ArrayList<>();
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                String[] v = new String[]{"", "", "", "", ""};
                NodeList cells = row.getElementsByTagName("c");
                for (int j = 0; j < cells.getLength(); j++) {
                    Element c = (Element) cells.item(j);
                    int col = columnIndex(c.getAttribute("r"));
                    if (col >= 0 && col < v.length) v[col] = cellValue(c, shared);
                }
                String name = clean(v[0]);
                if (name.isEmpty()) continue;
                if (i == 0 && isHeader(v)) continue;
                double retail = parseNumber(v[1]);
                double wholesale = parseNumber(v[2]);
                double purchase = parseNumber(v[3]);
                int stock = (int) Math.max(0, Math.round(parseNumber(v[4])));
                result.add(new AppDb.Product(0, name, purchase, retail, wholesale, stock));
            }
            return result;
        }
    }

    private static boolean isHeader(String[] v) {
        String a = clean(v[0]);
        String b = clean(v[1]);
        String c = clean(v[2]);
        String d = clean(v[3]);
        String e = clean(v[4]);
        return (a.contains("نام") && a.contains("کالا")) || b.contains("خرده") || c.contains("عمده") || d.contains("خرید") || e.contains("موجودی");
    }

    private static double parseNumber(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().replace(",", "").replace("٬", "").replace(" ", "");
        if (s.isEmpty()) return 0;
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '۰' && ch <= '۹') ascii.append((char) ('0' + ch - '۰'));
            else if (ch >= '٠' && ch <= '٩') ascii.append((char) ('0' + ch - '٠'));
            else ascii.append(ch);
        }
        try { return Double.parseDouble(ascii.toString()); }
        catch (Exception ignored) { return 0; }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static ArrayList<String> readShared(ZipFile zip) throws Exception {
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

    private static String cellValue(Element c, ArrayList<String> shared) {
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

    private static int columnIndex(String ref) {
        int col = 0;
        int i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
            col = col * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
            i++;
        }
        return col - 1;
    }

    private static Document parse(InputStream in) throws Exception {
        try (InputStream input = in) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(input);
        }
    }
}
