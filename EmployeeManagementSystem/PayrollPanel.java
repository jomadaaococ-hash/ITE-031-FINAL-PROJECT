package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PayrollPanel extends JPanel {
    private static class Emp {
        final int id; final String name, type; final double basic;
        Emp(int id, String name, String type, double basic) { this.id = id; this.name = name; this.type = type; this.basic = basic; }
        @Override public String toString() { return Theme.empCode(id) + " - " + name; }
    }

    private static final String LINE = "-----------------------------------------\n";
    private static final String DLINE = "=========================================\n";

    private final JComboBox<Emp> empBox = new JComboBox<>();
    private final JTextField start = Theme.field(), end = Theme.field(), days = Theme.field(),
            ot = Theme.field(), allow = Theme.field(), other = Theme.field();
    private final JTextArea preview = new JTextArea();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Employee", "Period",
            "Days", "OT Hrs", "Gross", "Deductions", "Net Pay", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public PayrollPanel() {
        setLayout(new BorderLayout(0, 16)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Payroll Processing"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        empBox.setFont(Theme.BODY);
        LocalDate now = LocalDate.now();
        start.setText(now.withDayOfMonth(1).toString()); end.setText(now.withDayOfMonth(15).toString());
        days.setText("13"); ot.setText("0"); allow.setText("0"); other.setText("0");

        // Input card (two columns)
        Theme.Card in = new Theme.Card(); in.setLayout(new GridBagLayout());
        cell(in, Theme.label("EMPLOYEE"), 0, 0, 2); cell(in, empBox, 0, 1, 2);
        cell(in, Theme.label("PERIOD START (YYYY-MM-DD)"), 0, 2, 1); cell(in, Theme.label("PERIOD END"), 1, 2, 1);
        cell(in, start, 0, 3, 1); cell(in, end, 1, 3, 1);
        cell(in, Theme.label("DAYS WORKED"), 0, 4, 1); cell(in, Theme.label("OVERTIME HOURS"), 1, 4, 1);
        cell(in, days, 0, 5, 1); cell(in, ot, 1, 5, 1);
        cell(in, Theme.label("ALLOWANCES (₱)"), 0, 6, 1); cell(in, Theme.label("OTHER DEDUCTIONS (₱)"), 1, 6, 1);
        cell(in, allow, 0, 7, 1); cell(in, other, 1, 7, 1);
        JButton comp = Theme.button("Compute", new Color(0x6D4C41)), save = Theme.button("Save Payroll", Theme.ACCENT),
                att = Theme.button("Get Days from Attendance", Theme.GRAY);
        comp.addActionListener(e -> { double[] r = compute(); if (r != null) preview.setText(slipBody(r, false)); });
        save.addActionListener(e -> save());
        att.addActionListener(e -> daysFromAttendance());
        cell(in, comp, 0, 8, 1); cell(in, save, 1, 8, 1);
        cell(in, att, 0, 10, 2);
        in.setPreferredSize(new Dimension(460, 0));

        // Preview card
        Theme.Card pv = new Theme.Card(); pv.setLayout(new BorderLayout(0, 8));
        JLabel pt = new JLabel("Computation"); pt.setFont(new Font("Segoe UI", Font.BOLD, 17)); pt.setForeground(Theme.TEXT);
        preview.setEditable(false); preview.setFont(new Font("Consolas", Font.PLAIN, 14));
        preview.setForeground(Theme.TEXT); preview.setText("Choose an employee and click Compute.");
        pv.add(pt, BorderLayout.NORTH); pv.add(new JScrollPane(preview), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(16, 0)); top.setOpaque(false);
        top.add(in, BorderLayout.WEST); top.add(pv, BorderLayout.CENTER);
        top.setPreferredSize(new Dimension(0, 410));

        // History card
        Theme.Card hist = new Theme.Card(); hist.setLayout(new BorderLayout(0, 10));
        JPanel hh = new JPanel(new BorderLayout()); hh.setOpaque(false);
        JLabel ht = new JLabel("Payroll History"); ht.setFont(new Font("Segoe UI", Font.BOLD, 17)); ht.setForeground(Theme.TEXT);
        JPanel hb = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); hb.setOpaque(false);
        JButton slip = Theme.button("View Payslip", Theme.ACCENT), paid = Theme.button("Mark Paid / Unpaid", new Color(0x2E7D32)),
                del = Theme.button("Delete", Theme.DANGER);
        slip.addActionListener(e -> payslip()); paid.addActionListener(e -> togglePaid()); del.addActionListener(e -> delete());
        hb.add(slip); hb.add(paid); hb.add(del);
        hh.add(ht, BorderLayout.WEST); hh.add(hb, BorderLayout.EAST);
        Theme.styleTable(table);
        hist.add(hh, BorderLayout.NORTH); hist.add(Theme.scroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16)); center.setOpaque(false);
        center.add(top, BorderLayout.NORTH); center.add(hist, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private static void cell(JPanel p, Component c, int x, int y, int w) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y; g.gridwidth = w; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(y >= 2 && y % 2 == 0 ? 8 : 2, 3, 2, 3);
        p.add(c, g);
    }

    private void msg(String s) { JOptionPane.showMessageDialog(this, s); }

    public void refresh() {
        Emp sel = (Emp) empBox.getSelectedItem();
        int keep = sel == null ? -1 : sel.id;
        try (Connection c = DB.get(); Statement st = c.createStatement()) {
            empBox.removeAllItems();
            ResultSet rs = st.executeQuery("SELECT id, full_name, salary_type, basic_salary FROM employees ORDER BY full_name");
            while (rs.next()) {
                Emp e = new Emp(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4));
                empBox.addItem(e);
                if (e.id == keep) empBox.setSelectedItem(e);
            }
            model.setRowCount(0);
            rs = st.executeQuery("SELECT p.id, e.full_name, p.period_start, p.period_end, p.days_worked, p.ot_hours, "
                    + "p.gross_pay, p.total_deductions, p.net_pay, p.payment_status FROM payroll p "
                    + "JOIN employees e ON e.id=p.emp_id ORDER BY p.id DESC");
            while (rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                    rs.getDate(3) + " to " + rs.getDate(4), rs.getDouble(5), rs.getDouble(6),
                    Theme.money(rs.getDouble(7)), Theme.money(rs.getDouble(8)), Theme.money(rs.getDouble(9)),
                    rs.getString(10)});
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void daysFromAttendance() {
        Emp e = (Emp) empBox.getSelectedItem();
        if (e == null) { msg("No employee selected."); return; }
        String sql = "SELECT COUNT(*) FROM attendance WHERE emp_id=? AND att_date BETWEEN ? AND ? AND status IN ('Present','Late')";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, e.id);
            ps.setDate(2, Date.valueOf(start.getText().trim()));
            ps.setDate(3, Date.valueOf(end.getText().trim()));
            ResultSet rs = ps.executeQuery(); rs.next();
            days.setText(String.valueOf(rs.getInt(1)));
        } catch (IllegalArgumentException ex) { msg("Dates must be in YYYY-MM-DD format.");
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private static double num(JTextField f) {
        double v = Double.parseDouble(f.getText().trim().replace(",", ""));
        if (v < 0) throw new NumberFormatException();
        return v;
    }

    /** Returns {basic, overtime, allowances, gross, sss, philhealth, pagibig, tax, other, net}. Simplified rates per cut-off. */
    private double[] compute() {
        Emp e = (Emp) empBox.getSelectedItem();
        if (e == null) { msg("No employee selected. Add employees first."); return null; }
        try {
            double d = num(days), h = num(ot), a = num(allow), o = num(other);
            boolean monthly = e.type.equals("Monthly");
            double dailyRate = monthly ? e.basic / 26 : e.basic;
            double basic = monthly ? e.basic / 2 : e.basic * d;
            double otPay = dailyRate / 8 * 1.25 * h;
            double gross = basic + otPay + a;
            double sss = Math.min(gross * 0.045, 787.50);
            double ph = Math.min(gross * 0.025, 1250);
            double pi = Math.min(gross * 0.02, 100);
            double taxable = gross - sss - ph - pi;
            double tax = taxable > 10417 ? (taxable - 10417) * 0.15 : 0;
            double net = gross - sss - ph - pi - tax - o;
            return new double[]{basic, otPay, a, gross, sss, ph, pi, tax, o, net};
        } catch (NumberFormatException ex) {
            msg("Days, overtime, allowances and deductions must be valid non-negative numbers.");
            return null;
        }
    }

    private static String m(double v, boolean pdf) { return pdf ? Theme.plain(v) : Theme.money(v); }
    private static String row(String l, String v) { return String.format("%-22s%19s\n", l, v); }

    private static String slipBody(double[] r, boolean pdf) {
        return "EARNINGS\n"
                + row("Basic Salary", m(r[0], pdf)) + row("Overtime", m(r[1], pdf)) + row("Allowances", m(r[2], pdf))
                + row("Gross Pay", m(r[3], pdf)) + LINE + "DEDUCTIONS\n"
                + row("SSS", m(r[4], pdf)) + row("PhilHealth", m(r[5], pdf)) + row("Pag-IBIG", m(r[6], pdf))
                + row("Withholding Tax", m(r[7], pdf)) + row("Other Deductions", m(r[8], pdf)) + LINE
                + row("NET PAY", m(r[9], pdf));
    }

    private void save() {
        double[] r = compute(); if (r == null) return;
        preview.setText(slipBody(r, false));
        Emp e = (Emp) empBox.getSelectedItem();
        String sql = "INSERT INTO payroll(emp_id,period_start,period_end,days_worked,ot_hours,basic_pay,overtime_pay,allowances,"
                + "gross_pay,sss,philhealth,pagibig,tax,other_deduction,total_deductions,net_pay) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, e.id);
            ps.setDate(2, Date.valueOf(start.getText().trim())); ps.setDate(3, Date.valueOf(end.getText().trim()));
            ps.setDouble(4, num(days)); ps.setDouble(5, num(ot));
            ps.setDouble(6, r[0]); ps.setDouble(7, r[1]); ps.setDouble(8, r[2]); ps.setDouble(9, r[3]);
            ps.setDouble(10, r[4]); ps.setDouble(11, r[5]); ps.setDouble(12, r[6]); ps.setDouble(13, r[7]);
            ps.setDouble(14, r[8]); ps.setDouble(15, r[4] + r[5] + r[6] + r[7] + r[8]); ps.setDouble(16, r[9]);
            ps.executeUpdate();
            msg("Payroll saved for " + e.name + " (status: UNPAID).");
            refresh();
        } catch (IllegalArgumentException ex) { msg("Dates must be in YYYY-MM-DD format.");
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private int selectedId() {
        int r = table.getSelectedRow();
        if (r < 0) { msg("Select a payroll record first."); return -1; }
        return (Integer) model.getValueAt(r, 0);
    }

    private void delete() {
        int id = selectedId(); if (id < 0) return;
        if (JOptionPane.showConfirmDialog(this, "Delete this payroll record?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM payroll WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate(); refresh();
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void togglePaid() {
        int id = selectedId(); if (id < 0) return;
        try (Connection c = DB.get()) {
            String cur;
            try (PreparedStatement ps = c.prepareStatement("SELECT payment_status FROM payroll WHERE id=?")) {
                ps.setInt(1, id); ResultSet rs = ps.executeQuery();
                if (!rs.next()) return;
                cur = rs.getString(1);
            }
            boolean toPaid = !"PAID".equals(cur);
            try (PreparedStatement ps = c.prepareStatement("UPDATE payroll SET payment_status=?, payment_date=? WHERE id=?")) {
                ps.setString(1, toPaid ? "PAID" : "UNPAID");
                if (toPaid) ps.setDate(2, Date.valueOf(LocalDate.now())); else ps.setNull(2, Types.DATE);
                ps.setInt(3, id); ps.executeUpdate();
            }
            refresh();
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private static String center(String s, int w) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < (w - s.length()) / 2; i++) b.append(' ');
        return b.append(s).append('\n').toString();
    }

    static String payslipText(int id, boolean pdf, int onlyEmp) throws SQLException {
        String sql = "SELECT e.id AS eid, e.full_name, e.position, p.* FROM payroll p JOIN employees e ON e.id=p.emp_id WHERE p.id=? AND (?<=0 OR p.emp_id=?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, onlyEmp); ps.setInt(3, onlyEmp);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            LocalDate s = rs.getDate("period_start").toLocalDate(), t = rs.getDate("period_end").toLocalDate();
            String period = (s.getMonth() == t.getMonth() && s.getYear() == t.getYear())
                    ? s.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + s.getDayOfMonth() + "-"
                    + t.getDayOfMonth() + ", " + s.getYear()
                    : s + " to " + t;
            double[] r = {rs.getDouble("basic_pay"), rs.getDouble("overtime_pay"), rs.getDouble("allowances"),
                    rs.getDouble("gross_pay"), rs.getDouble("sss"), rs.getDouble("philhealth"), rs.getDouble("pagibig"),
                    rs.getDouble("tax"), rs.getDouble("other_deduction"), rs.getDouble("net_pay")};
            Date pd = rs.getDate("payment_date");
            return DLINE + center("SUGBOMACCIATO", 41) + center("PAYSLIP", 41) + DLINE
                    + "Employee ID    : " + Theme.empCode(rs.getInt("eid")) + "\n"
                    + "Employee Name  : " + rs.getString("full_name") + "\n"
                    + "Position       : " + rs.getString("position") + "\n"
                    + "Payroll Period : " + period + "\n" + LINE
                    + slipBody(r, pdf) + DLINE
                    + "Payment Status : " + rs.getString("payment_status") + "\n"
                    + "Payment Date   : " + (pd == null ? "______________" : pd.toString()) + "\n" + DLINE;
        }
    }

    private void payslip() {
        int id = selectedId(); if (id < 0) return;
        showPayslip(this, id, -1);
    }

    /** Payslip dialog with print / PDF. onlyEmp > 0 restricts it to that employee's own payroll. */
    static void showPayslip(Component parent, int id, int onlyEmp) {
        try {
            String s = payslipText(id, false, onlyEmp); if (s == null) return;
            JTextArea ta = new JTextArea(s); ta.setEditable(false); ta.setFont(new Font("Consolas", Font.PLAIN, 14));
            int ch = JOptionPane.showOptionDialog(parent, new JScrollPane(ta), "Payslip", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, new String[]{"Print Payslip", "Download PDF", "Close"}, "Close");
            if (ch == 0) {
                ta.print();
            } else if (ch == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File("Payslip_" + id + ".pdf"));
                if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (!f.getName().toLowerCase().endsWith(".pdf")) f = new File(f.getPath() + ".pdf");
                    List<String> lines = Arrays.asList(payslipText(id, true, onlyEmp).split("\r?\n"));
                    PdfUtil.write(f, lines, 11);
                    JOptionPane.showMessageDialog(parent, "PDF saved:\n" + f.getAbsolutePath());
                }
            }
        } catch (Exception ex) { Theme.error(parent, ex); }
    }
}
