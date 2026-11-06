package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** Employee portal: an employee can only see their own salary and payslips. */
public class EmployeeFrame extends JFrame {
    private final int empId;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Period", "Basic", "Overtime",
            "Allowances", "Gross Pay", "Deductions", "Net Pay", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel[] info = new JLabel[4], vals = new JLabel[3];

    public EmployeeFrame(String fullName, int empId) {
        super("Sugbomacciato - My Salary");
        this.empId = empId;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Logo.image(64));
        setSize(1100, 700); setLocationRelativeTo(null); setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel header = new JPanel(new BorderLayout()); header.setBackground(Theme.SIDEBAR);
        header.setBorder(new EmptyBorder(12, 24, 12, 24));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0)); left.setOpaque(false);
        left.add(new JLabel(Logo.icon(48)));
        JPanel tt = new JPanel(); tt.setOpaque(false); tt.setLayout(new BoxLayout(tt, BoxLayout.Y_AXIS));
        JLabel b = new JLabel("Sugbomacciato"); b.setFont(new Font("Segoe UI", Font.BOLD, 20)); b.setForeground(Color.WHITE);
        JLabel s = new JLabel("Employee Portal"); s.setFont(Theme.BODY); s.setForeground(new Color(0xE8C9A0));
        tt.add(b); tt.add(s); left.add(tt);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8)); right.setOpaque(false);
        JLabel who = new JLabel("Welcome, " + fullName); who.setFont(Theme.BOLD); who.setForeground(Color.WHITE);
        JButton out = Theme.button("Logout", Theme.DANGER);
        out.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        right.add(who); right.add(out);
        header.add(left, BorderLayout.WEST); header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16)); body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("My Salary"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);

        Theme.Card infoCard = new Theme.Card(); infoCard.setLayout(new GridLayout(2, 4, 10, 4));
        for (String l : new String[]{"EMPLOYEE ID", "POSITION", "DEPARTMENT", "BASIC SALARY"}) infoCard.add(Theme.label(l));
        for (int i = 0; i < 4; i++) {
            info[i] = new JLabel("-"); info[i].setFont(new Font("Segoe UI", Font.BOLD, 17)); info[i].setForeground(Theme.TEXT);
            infoCard.add(info[i]);
        }
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0)); stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 84));
        String[] names = {"LATEST NET PAY", "TOTAL NET PAID", "PAYSLIPS"};
        for (int i = 0; i < 3; i++) {
            Theme.Card c = new Theme.Card(); c.setLayout(new GridLayout(2, 1));
            vals[i] = new JLabel("0"); vals[i].setFont(new Font("Segoe UI", Font.BOLD, 24)); vals[i].setForeground(Theme.ACCENT);
            c.add(Theme.label(names[i])); c.add(vals[i]);
            stats.add(c);
        }
        JPanel two = new JPanel(new BorderLayout(0, 14)); two.setOpaque(false);
        two.add(infoCard, BorderLayout.NORTH); two.add(stats, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, 14)); top.setOpaque(false);
        top.add(title, BorderLayout.NORTH); top.add(two, BorderLayout.CENTER);

        Theme.Card tc = new Theme.Card(); tc.setLayout(new BorderLayout(0, 10));
        JPanel hb = new JPanel(new BorderLayout()); hb.setOpaque(false);
        JLabel ht = new JLabel("My Payslips"); ht.setFont(new Font("Segoe UI", Font.BOLD, 17)); ht.setForeground(Theme.TEXT);
        JButton slip = Theme.button("View Payslip", Theme.ACCENT);
        slip.addActionListener(e -> viewSlip());
        hb.add(ht, BorderLayout.WEST); hb.add(slip, BorderLayout.EAST);
        Theme.styleTable(table);
        tc.add(hb, BorderLayout.NORTH); tc.add(Theme.scroll(table), BorderLayout.CENTER);

        body.add(top, BorderLayout.NORTH); body.add(tc, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        load();
    }

    private void load() {
        try (Connection c = DB.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT position, department, salary_type, basic_salary FROM employees WHERE id=?")) {
                ps.setInt(1, empId); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    info[0].setText(Theme.empCode(empId)); info[1].setText(rs.getString(1)); info[2].setText(rs.getString(2));
                    info[3].setText(Theme.money(rs.getDouble(4)) + " (" + rs.getString(3) + ")");
                }
            }
            model.setRowCount(0);
            double paidTotal = 0, latest = 0; int n = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT id, period_start, period_end, basic_pay, overtime_pay, "
                    + "allowances, gross_pay, total_deductions, net_pay, payment_status FROM payroll WHERE emp_id=? ORDER BY id DESC")) {
                ps.setInt(1, empId); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (n == 0) latest = rs.getDouble(9);
                    n++;
                    if ("PAID".equals(rs.getString(10))) paidTotal += rs.getDouble(9);
                    model.addRow(new Object[]{rs.getInt(1), rs.getDate(2) + " to " + rs.getDate(3),
                            Theme.money(rs.getDouble(4)), Theme.money(rs.getDouble(5)), Theme.money(rs.getDouble(6)),
                            Theme.money(rs.getDouble(7)), Theme.money(rs.getDouble(8)), Theme.money(rs.getDouble(9)),
                            rs.getString(10)});
                }
            }
            vals[0].setText(Theme.money(latest)); vals[1].setText(Theme.money(paidTotal)); vals[2].setText(String.valueOf(n));
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void viewSlip() {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a payslip first."); return; }
        PayrollPanel.showPayslip(this, (Integer) model.getValueAt(r, 0), empId);
    }
}
