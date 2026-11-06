package sugbomacciato;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Tiny PDF writer: monospaced text on A4 pages. No external library needed. */
public class PdfUtil {
    public static void write(File file, List<String> lines, int fontSize) throws IOException {
        int leading = fontSize + 3;
        int perPage = (842 - 80) / leading;
        int pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offs = new ArrayList<>();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pages; p++) kids.append(4 + 2 * p).append(" 0 R ");
        obj(out, offs, "<< /Type /Catalog /Pages 2 0 R >>");
        obj(out, offs, "<< /Type /Pages /Kids [ " + kids + "] /Count " + pages + " >>");
        obj(out, offs, "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");
        for (int p = 0; p < pages; p++) {
            obj(out, offs, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + (5 + 2 * p) + " 0 R >>");
            StringBuilder s = new StringBuilder("BT /F1 " + fontSize + " Tf " + leading + " TL 40 800 Td\n");
            int to = Math.min(lines.size(), (p + 1) * perPage);
            for (int i = p * perPage; i < to; i++) s.append('(').append(esc(lines.get(i))).append(") '\n");
            s.append("ET");
            String content = s.toString();
            int len = content.getBytes(StandardCharsets.ISO_8859_1).length;
            obj(out, offs, "<< /Length " + len + " >>\nstream\n" + content + "\nendstream");
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (offs.size() + 1) + "\n0000000000 65535 f \n");
        for (int o : offs) x.append(String.format("%010d 00000 n \n", o));
        x.append("trailer\n<< /Size ").append(offs.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        try (FileOutputStream fo = new FileOutputStream(file)) { out.writeTo(fo); }
    }

    private static void obj(ByteArrayOutputStream out, List<Integer> offs, String body) throws IOException {
        offs.add(out.size());
        String s = offs.size() + " 0 obj\n" + body + "\nendobj\n";
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String esc(String t) {
        StringBuilder b = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
            else if (c < 32 || c > 126) b.append('?');
            else b.append(c);
        }
        return b.toString();
    }
}
