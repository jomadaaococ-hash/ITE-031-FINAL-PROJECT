package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DashboardPanel extends JPanel {
    private final JLabel[] vals = new JLabel[4];
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Employee", "Period", "Gross Pay", "Deductions", "Net Pay"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 18)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Dashboard"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0)); stats.setOpaque(false);
        String[] names = {"TOTAL EMPLOYEES", "DEPARTMENTS", "PAYROLL RECORDS", "NET PAID THIS MONTH"};
        for (int i = 0; i < 4; i++) stats.add(stat(names[i], i));

        Theme.Card recent = new Theme.Card(); recent.setLayout(new BorderLayout(0, 10));
        JLabel rt = new JLabel("Recent Payroll"); rt.setFont(new Font("Segoe UI", Font.BOLD, 17)); rt.setForeground(Theme.TEXT);
        JTable table = new JTable(model); Theme.styleTable(table);
        recent.add(rt, BorderLayout.NORTH); recent.add(Theme.scroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 18)); center.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 110));
        center.add(stats, BorderLayout.NORTH); center.add(recent, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private Theme.Card stat(String title, int i) {
        Theme.Card c = new Theme.Card(); c.setLayout(new GridLayout(2, 1));
        JLabel t = Theme.label(title);
        vals[i] = new JLabel("0"); vals[i].setFont(new Font("Segoe UI", Font.BOLD, 28)); vals[i].setForeground(Theme.ACCENT);
        c.add(t); c.add(vals[i]);
        return c;
    }

    private double scalar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public void refresh() {
        try (Connection c = DB.get()) {
            vals[0].setText(String.valueOf((int) scalar(c, "SELECT COUNT(*) FROM employees")));
            vals[1].setText(String.valueOf((int) scalar(c, "SELECT COUNT(DISTINCT department) FROM employees")));
            vals[2].setText(String.valueOf((int) scalar(c, "SELECT COUNT(*) FROM payroll")));
            vals[3].setText(Theme.money(scalar(c, "SELECT COALESCE(SUM(net_pay),0) FROM payroll "
                    + "WHERE MONTH(date_generated)=MONTH(CURDATE()) AND YEAR(date_generated)=YEAR(CURDATE())")));
            model.setRowCount(0);
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT e.full_name, p.period_start, p.period_end, p.gross_pay, p.total_deductions, p.net_pay "
                    + "FROM payroll p JOIN employees e ON e.id=p.emp_id ORDER BY p.id DESC LIMIT 10")) {
                while (rs.next()) model.addRow(new Object[]{rs.getString(1),
                        rs.getDate(2) + " to " + rs.getDate(3), Theme.money(rs.getDouble(4)),
                        Theme.money(rs.getDouble(5)), Theme.money(rs.getDouble(6))});
            }
        } catch (SQLException ex) { Theme.error(this, ex); }
    }
}
