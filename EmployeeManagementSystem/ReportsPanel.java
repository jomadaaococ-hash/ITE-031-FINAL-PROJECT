package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportsPanel extends JPanel {
    private final JComboBox<String> typeBox = Theme.combo("Payroll Summary", "Paid Payroll", "Unpaid Payroll");
    private final JComboBox<String> deptBox = Theme.combo(Theme.deptsWith("All Departments"));
    private final JTextField from = Theme.field(), to = Theme.field();
    private final JLabel[] vals = new JLabel[5];
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Emp ID", "Employee", "Department",
            "Period", "Gross Pay", "Deductions", "Net Pay", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final List<Object[]> data = new ArrayList<>();
    private String lastType = "", lastDept = "", lastFrom = "", lastTo = "";
    private double totG, totD, totN;
    private int paidCount, unpaidCount;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 16)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Payroll Reports"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);
        LocalDate now = LocalDate.now();
        from.setText(now.withDayOfMonth(1).toString()); to.setText(now.withDayOfMonth(now.lengthOfMonth()).toString());

        Theme.Card filter = new Theme.Card(); filter.setLayout(new GridLayout(2, 5, 10, 4));
        for (String s : new String[]{"REPORT TYPE", "DEPARTMENT", "FROM DATE", "TO DATE", ""}) filter.add(Theme.label(s));
        JButton gen = Theme.button("Generate Report", Theme.ACCENT);
        gen.addActionListener(e -> generate());
        filter.add(typeBox); filter.add(deptBox); filter.add(from); filter.add(to); filter.add(gen);

        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0)); stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 84));
        String[] names = {"EMPLOYEES PAID", "TOTAL GROSS PAY", "TOTAL DEDUCTIONS", "TOTAL NET PAY", "UNPAID EMPLOYEES"};
        for (int i = 0; i < 5; i++) {
            Theme.Card c = new Theme.Card(); c.setLayout(new GridLayout(2, 1));
            vals[i] = new JLabel("0"); vals[i].setFont(new Font("Segoe UI", Font.BOLD, 20)); vals[i].setForeground(Theme.ACCENT);
            c.add(Theme.label(names[i])); c.add(vals[i]);
            stats.add(c);
        }
        JPanel top = new JPanel(new BorderLayout(0, 14)); top.setOpaque(false);
        top.add(filter, BorderLayout.NORTH); top.add(stats, BorderLayout.CENTER);

        Theme.Card tc = new Theme.Card(); tc.setLayout(new BorderLayout(0, 10));
        Theme.styleTable(table);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); btns.setOpaque(false);
        JButton pdf = Theme.button("Export PDF", Theme.DANGER), csv = Theme.button("Export CSV", new Color(0x2E7D32)),
                print = Theme.button("Print Report", new Color(0x6D4C41));
        pdf.addActionListener(e -> exportPdf()); csv.addActionListener(e -> exportCsv()); print.addActionListener(e -> printReport());
        btns.add(pdf); btns.add(csv); btns.add(print);
        tc.add(Theme.scroll(table), BorderLayout.CENTER); tc.add(btns, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 16)); center.setOpaque(false);
        center.add(top, BorderLayout.NORTH); center.add(tc, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void msg(String s) { JOptionPane.showMessageDialog(this, s); }

    private void generate() {
        LocalDate f, t;
        try { f = LocalDate.parse(from.getText().trim()); t = LocalDate.parse(to.getText().trim()); }
        catch (Exception ex) { msg("Dates must be in YYYY-MM-DD format."); return; }
        String type = (String) typeBox.getSelectedItem(), dep = (String) deptBox.getSelectedItem();
        String stat = type.startsWith("Paid") ? "PAID" : type.startsWith("Unpaid") ? "UNPAID" : "";
        String sql = "SELECT e.id, e.full_name, e.department, p.period_start, p.period_end, p.gross_pay, "
                + "p.total_deductions, p.net_pay, p.payment_status FROM payroll p JOIN employees e ON e.id=p.emp_id "
                + "WHERE p.period_end BETWEEN ? AND ? AND (?='All Departments' OR e.department=?) "
                + "AND (?='' OR p.payment_status=?) ORDER BY e.full_name, p.period_start";
        data.clear(); model.setRowCount(0);
        totG = 0; totD = 0; totN = 0;
        Set<Integer> paid = new HashSet<>(), unpaid = new HashSet<>();
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(f)); ps.setDate(2, Date.valueOf(t));
            ps.setString(3, dep); ps.setString(4, dep); ps.setString(5, stat); ps.setString(6, stat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2), dp = rs.getString(3), period = rs.getDate(4) + " to " + rs.getDate(5);
                double g = rs.getDouble(6), d = rs.getDouble(7), n = rs.getDouble(8);
                String st = rs.getString(9);
                data.add(new Object[]{id, name, dp, period, g, d, n, st});
                model.addRow(new Object[]{Theme.empCode(id), name, dp, period, Theme.money(g), Theme.money(d), Theme.money(n), st});
                totG += g; totD += d; totN += n;
                if ("PAID".equals(st)) paid.add(id); else unpaid.add(id);
            }
        } catch (SQLException ex) { Theme.error(this, ex); return; }
        paidCount = paid.size(); unpaidCount = unpaid.size();
        lastType = type; lastDept = dep; lastFrom = f.toString(); lastTo = t.toString();
        vals[0].setText(String.valueOf(paidCount)); vals[1].setText(Theme.money(totG));
        vals[2].setText(Theme.money(totD)); vals[3].setText(Theme.money(totN));
        vals[4].setText(String.valueOf(unpaidCount));
        if (data.isEmpty()) msg("No payroll records found for that filter.");
    }

    private boolean hasData() {
        if (data.isEmpty()) { msg("Generate a report first (and make sure it has records)."); return false; }
        return true;
    }

    private File choose(String defaultName, String ext) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(ext)) f = new File(f.getPath() + ext);
        return f;
    }

    private static String q(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private void exportCsv() {
        if (!hasData()) return;
        File f = choose("Payroll_Report.csv", ".csv"); if (f == null) return;
        StringBuilder sb = new StringBuilder("Emp ID,Employee,Department,Period,Gross Pay,Deductions,Net Pay,Status\n");
        for (Object[] r : data)
            sb.append(Theme.empCode((Integer) r[0])).append(',').append(q((String) r[1])).append(',').append(q((String) r[2]))
              .append(',').append(r[3]).append(',').append(String.format("%.2f,%.2f,%.2f", r[4], r[5], r[6]))
              .append(',').append(r[7]).append('\n');
        sb.append("TOTAL,,,,").append(String.format("%.2f,%.2f,%.2f", totG, totD, totN)).append(",\n");
        try {
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            msg("CSV saved:\n" + f.getAbsolutePath());
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void exportPdf() {
        if (!hasData()) return;
        File f = choose("Payroll_Report.pdf", ".pdf"); if (f == null) return;
        String fmt = "%-8s %-20.20s %-24s %12s %12s %12s %-7s";
        StringBuilder dash = new StringBuilder();
        for (int i = 0; i < 101; i++) dash.append('-');
        List<String> L = new ArrayList<>();
        L.add("SUGBOMACCIATO - PAYROLL REPORT");
        L.add("Report Type : " + lastType);
        L.add("Department  : " + lastDept);
        L.add("Period      : " + lastFrom + " to " + lastTo);
        L.add("Generated   : " + LocalDateTime.now().withNano(0).toString().replace('T', ' '));
        L.add("");
        L.add("Employees Paid    : " + paidCount);
        L.add("Total Gross Pay   : " + Theme.plain(totG));
        L.add("Total Deductions  : " + Theme.plain(totD));
        L.add("Total Net Pay     : " + Theme.plain(totN));
        L.add("Unpaid Employees  : " + unpaidCount);
        L.add("");
        L.add(String.format(fmt, "Emp ID", "Employee", "Period", "Gross (PHP)", "Deduct (PHP)", "Net (PHP)", "Status"));
        L.add(dash.toString());
        for (Object[] r : data)
            L.add(String.format(fmt, Theme.empCode((Integer) r[0]), r[1], r[3], String.format("%,.2f", r[4]),
                    String.format("%,.2f", r[5]), String.format("%,.2f", r[6]), r[7]));
        L.add(dash.toString());
        L.add(String.format(fmt, "TOTAL", "", "", String.format("%,.2f", totG), String.format("%,.2f", totD),
                String.format("%,.2f", totN), ""));
        try {
            PdfUtil.write(f, L, 8);
            msg("PDF saved:\n" + f.getAbsolutePath());
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void printReport() {
        if (!hasData()) return;
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("Sugbomacciato - Payroll Report"),
                    new MessageFormat("Page {0}"));
        } catch (PrinterException ex) { Theme.error(this, ex); }
    }
}
