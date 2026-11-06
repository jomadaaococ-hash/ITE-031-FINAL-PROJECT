package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendancePanel extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final String[] STATUSES = {"Present", "Late", "Absent", "Leave"};
    private static final String UPSERT = "INSERT INTO attendance(emp_id,att_date,time_in,time_out,status) VALUES(?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE time_in=VALUES(time_in), time_out=VALUES(time_out), status=VALUES(status)";

    private static class Emp {
        final int id; final String name;
        Emp(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return Theme.empCode(id) + " - " + name; }
    }

    private final JTextField dateField = Theme.field(), search = Theme.field();
    private final JComboBox<String> deptBox = Theme.combo(Theme.deptsWith("All"));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Emp ID", "Employee", "Department", "Time In", "Time Out", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public AttendancePanel() {
        setLayout(new BorderLayout(0, 16)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Attendance Management"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);
        dateField.setText(LocalDate.now().toString());

        Theme.Card top = new Theme.Card(); top.setLayout(new BorderLayout(0, 14));
        JPanel grid = new JPanel(new GridLayout(2, 4, 10, 4)); grid.setOpaque(false);
        for (String s : new String[]{"DATE (YYYY-MM-DD)", "DEPARTMENT", "SEARCH EMPLOYEE", ""}) grid.add(Theme.label(s));
        JButton go = Theme.button("Load", new Color(0x6D4C41));
        grid.add(dateField); grid.add(deptBox); grid.add(search); grid.add(go);
        go.addActionListener(e -> load());
        dateField.addActionListener(e -> load());
        deptBox.addActionListener(e -> load());
        search.addKeyListener(new KeyAdapter() { @Override public void keyReleased(KeyEvent e) { load(); } });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); btns.setOpaque(false);
        JButton rec = Theme.button("+ Record Attendance", Theme.ACCENT), edit = Theme.button("Edit Record", new Color(0x6D4C41)),
                sum = Theme.button("View Summary", new Color(0x1565C0)), imp = Theme.button("Import Attendance", Theme.GRAY);
        rec.addActionListener(e -> recordDialog(-1));
        edit.addActionListener(e -> editSelected());
        sum.addActionListener(e -> summary());
        imp.addActionListener(e -> importCsv());
        btns.add(rec); btns.add(edit); btns.add(sum); btns.add(imp);
        top.add(grid, BorderLayout.CENTER); top.add(btns, BorderLayout.SOUTH);

        Theme.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, f, r, c);
                if (!s) setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xFBF7F2));
                String t = String.valueOf(v);
                setForeground(t.equals("Present") ? new Color(0x2E7D32) : t.equals("Late") ? new Color(0xE65100)
                        : t.equals("Absent") ? Theme.DANGER : t.equals("Leave") ? new Color(0x1565C0) : Theme.GRAY);
                setFont(Theme.BOLD); setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        Theme.Card tc = new Theme.Card(); tc.setLayout(new BorderLayout());
        tc.add(Theme.scroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16)); center.setOpaque(false);
        center.add(top, BorderLayout.NORTH); center.add(tc, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        load();
    }

    private void msg(String s) { JOptionPane.showMessageDialog(this, s); }

    static LocalTime parseTime(String s) {
        s = s.trim();
        if (s.isEmpty() || s.equals("--")) return null;
        if (s.matches("\\d:\\d\\d")) s = "0" + s;
        return LocalTime.parse(s);
    }

    static String resolve(String chosen, LocalTime in) {
        if (!chosen.equals("Auto")) return chosen;
        if (in == null) return "Absent";
        return in.isAfter(LocalTime.of(8, 0)) ? "Late" : "Present";
    }

    private static void upsert(PreparedStatement ps, int id, LocalDate d, LocalTime in, LocalTime out, String st) throws SQLException {
        ps.setInt(1, id); ps.setDate(2, Date.valueOf(d));
        if (in == null) ps.setNull(3, Types.TIME); else ps.setTime(3, Time.valueOf(in));
        if (out == null) ps.setNull(4, Types.TIME); else ps.setTime(4, Time.valueOf(out));
        ps.setString(5, st);
        ps.executeUpdate();
    }

    public void load() {
        LocalDate d;
        try { d = LocalDate.parse(dateField.getText().trim()); }
        catch (Exception ex) { msg("Date must be in YYYY-MM-DD format."); return; }
        model.setRowCount(0);
        String dep = (String) deptBox.getSelectedItem();
        String sql = "SELECT e.id, e.full_name, e.department, a.time_in, a.time_out, a.status FROM employees e "
                + "LEFT JOIN attendance a ON a.emp_id=e.id AND a.att_date=? "
                + "WHERE (?='All' OR e.department=?) AND e.full_name LIKE ? ORDER BY e.full_name";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(d)); ps.setString(2, dep); ps.setString(3, dep);
            ps.setString(4, "%" + search.getText().trim() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Time ti = rs.getTime(4), tout = rs.getTime(5);
                String st = rs.getString(6);
                model.addRow(new Object[]{Theme.empCode(rs.getInt(1)), rs.getString(2), rs.getString(3),
                        ti == null ? "--" : ti.toLocalTime().format(FMT),
                        tout == null ? "--" : tout.toLocalTime().format(FMT),
                        st == null ? "No record" : st});
            }
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void editSelected() {
        int r = table.getSelectedRow();
        if (r < 0) { msg("Select an employee in the table first."); return; }
        recordDialog(Integer.parseInt(model.getValueAt(r, 0).toString().substring(4)));
    }

    private void recordDialog(int presetId) {
        LocalDate d;
        try { d = LocalDate.parse(dateField.getText().trim()); }
        catch (Exception ex) { msg("Date must be in YYYY-MM-DD format."); return; }
        List<Emp> emps = new ArrayList<>();
        try (Connection c = DB.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, full_name FROM employees ORDER BY full_name")) {
            while (rs.next()) emps.add(new Emp(rs.getInt(1), rs.getString(2)));
        } catch (SQLException ex) { Theme.error(this, ex); return; }
        if (emps.isEmpty()) { msg("Add employees first."); return; }

        JComboBox<Emp> eb = new JComboBox<>(emps.toArray(new Emp[0])); eb.setFont(Theme.BODY);
        JTextField tdate = Theme.field(), tin = Theme.field(), tout = Theme.field();
        tdate.setText(d.toString());
        JComboBox<String> sb = Theme.combo("Auto", "Present", "Late", "Absent", "Leave");
        if (presetId > 0) {
            for (Emp e : emps) if (e.id == presetId) eb.setSelectedItem(e);
            try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                    "SELECT time_in, time_out, status FROM attendance WHERE emp_id=? AND att_date=?")) {
                ps.setInt(1, presetId); ps.setDate(2, Date.valueOf(d));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getTime(1) != null) tin.setText(rs.getTime(1).toLocalTime().toString());
                    if (rs.getTime(2) != null) tout.setText(rs.getTime(2).toLocalTime().toString());
                    sb.setSelectedItem(rs.getString(3));
                }
            } catch (SQLException ex) { Theme.error(this, ex); return; }
        }
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(Theme.label("EMPLOYEE")); p.add(eb);
        p.add(Theme.label("DATE (YYYY-MM-DD)")); p.add(tdate);
        p.add(Theme.label("TIME IN (HH:mm, e.g. 08:00)")); p.add(tin);
        p.add(Theme.label("TIME OUT (HH:mm, e.g. 17:00)")); p.add(tout);
        p.add(Theme.label("STATUS (Auto = by time in)")); p.add(sb);
        if (JOptionPane.showConfirmDialog(this, p, presetId > 0 ? "Edit Attendance" : "Record Attendance",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(UPSERT)) {
            Emp e = (Emp) eb.getSelectedItem();
            LocalDate dd = LocalDate.parse(tdate.getText().trim());
            LocalTime in = parseTime(tin.getText()), out = parseTime(tout.getText());
            if (in != null && out != null && out.isBefore(in)) throw new IllegalArgumentException("Time out is before time in.");
            upsert(ps, e.id, dd, in, out, resolve((String) sb.getSelectedItem(), in));
            dateField.setText(dd.toString());
            load();
        } catch (DateTimeParseException ex) {
            msg("Use YYYY-MM-DD for the date and HH:mm (for example 08:00) for the time.");
        } catch (IllegalArgumentException ex) { msg(ex.getMessage());
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void summary() {
        LocalDate d;
        try { d = LocalDate.parse(dateField.getText().trim()); }
        catch (Exception ex) { msg("Date must be in YYYY-MM-DD format."); return; }
        DefaultTableModel sm = new DefaultTableModel(new String[]{"Employee", "Present", "Late", "Absent", "Leave"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = "SELECT e.full_name, COALESCE(SUM(a.status='Present'),0), COALESCE(SUM(a.status='Late'),0), "
                + "COALESCE(SUM(a.status='Absent'),0), COALESCE(SUM(a.status='Leave'),0) FROM employees e "
                + "LEFT JOIN attendance a ON a.emp_id=e.id AND a.att_date BETWEEN ? AND ? GROUP BY e.id, e.full_name ORDER BY e.full_name";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(d.withDayOfMonth(1))); ps.setDate(2, Date.valueOf(d.withDayOfMonth(d.lengthOfMonth())));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) sm.addRow(new Object[]{rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)});
        } catch (SQLException ex) { Theme.error(this, ex); return; }
        JTable t = new JTable(sm); Theme.styleTable(t);
        JScrollPane sp = Theme.scroll(t); sp.setPreferredSize(new Dimension(560, 260));
        JOptionPane.showMessageDialog(this, sp, "Attendance Summary - "
                + d.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + d.getYear(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private void importCsv() {
        JOptionPane.showMessageDialog(this, "CSV format (one record per line):\n"
                + "employee_id,date,time_in,time_out,status\n\nExample:\n1,2026-10-14,08:00,17:00,Present\n\n"
                + "Status is optional (Present, Late, Absent, Leave). If blank, it is set from the time in.",
                "Import Attendance", JOptionPane.INFORMATION_MESSAGE);
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int ok = 0, bad = 0;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(UPSERT)) {
            boolean first = true;
            for (String line : Files.readAllLines(fc.getSelectedFile().toPath())) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (first) {
                    first = false;
                    if (!p[0].trim().matches("(?i)(EMP-)?\\d+")) continue;   // header row
                }
                try {
                    int id = Integer.parseInt(p[0].trim().toUpperCase().replace("EMP-", ""));
                    LocalDate d = LocalDate.parse(p[1].trim());
                    LocalTime in = parseTime(p[2]), out = parseTime(p[3]);
                    String st = "Auto";
                    if (p.length > 4) for (String s : STATUSES) if (s.equalsIgnoreCase(p[4].trim())) st = s;
                    upsert(ps, id, d, in, out, resolve(st, in));
                    ok++;
                } catch (Exception ex) { bad++; }
            }
        } catch (Exception ex) { Theme.error(this, ex); return; }
        msg("Imported: " + ok + " record(s)\nSkipped: " + bad + " invalid line(s)");
        load();
    }
}
