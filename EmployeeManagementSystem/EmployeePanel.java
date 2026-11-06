package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.time.LocalDate;

public class EmployeePanel extends JPanel {
    private final JTextField id = Theme.field(), name = Theme.field(), contact = Theme.field(),
            address = Theme.field(), hired = Theme.field(), basic = Theme.field(), search = Theme.field();
    private final JComboBox<String> dept = Theme.combo(Theme.DEPTS);
    private final JComboBox<String> pos = Theme.combo("Barista", "Cashier", "Cook", "Pastry Cook", "Waiter",
            "Delivery Rider", "Supervisor", "Store Manager");
    private final JComboBox<String> employment = Theme.combo("Regular", "Probationary", "Contractual", "Part-time");
    private final JComboBox<String> salaryType = Theme.combo("Monthly", "Daily");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Emp ID", "Full Name", "Contact",
            "Department", "Position", "Employment", "Hire Date", "Salary Type", "Basic Salary"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private int selectedId = -1;

    public EmployeePanel() {
        setLayout(new BorderLayout(0, 16)); setBackground(Theme.BG); setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Employee Management"); title.setFont(Theme.H1); title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        id.setEditable(false); id.setText("Auto-generated");
        pos.setEditable(true);
        hired.setText(LocalDate.now().toString());

        Theme.Card form = new Theme.Card(); form.setLayout(new BorderLayout(0, 14));
        JPanel grid = new JPanel(new GridLayout(4, 5, 10, 4)); grid.setOpaque(false);
        for (String s : new String[]{"EMPLOYEE ID", "FULL NAME", "CONTACT NO.", "ADDRESS", "DEPARTMENT"}) grid.add(Theme.label(s));
        grid.add(id); grid.add(name); grid.add(contact); grid.add(address); grid.add(dept);
        for (String s : new String[]{"POSITION", "EMPLOYMENT", "HIRE DATE (YYYY-MM-DD)", "SALARY TYPE", "BASIC SALARY (₱)"}) grid.add(Theme.label(s));
        grid.add(pos); grid.add(employment); grid.add(hired); grid.add(salaryType); grid.add(basic);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); btns.setOpaque(false);
        JButton add = Theme.button("Save Employee", Theme.ACCENT), upd = Theme.button("Update", new Color(0x6D4C41)),
                del = Theme.button("Delete", Theme.DANGER), clr = Theme.button("Cancel", Theme.GRAY);
        add.addActionListener(e -> save(true)); upd.addActionListener(e -> save(false));
        del.addActionListener(e -> delete()); clr.addActionListener(e -> clear());
        btns.add(add); btns.add(upd); btns.add(del); btns.add(clr);
        form.add(grid, BorderLayout.CENTER); form.add(btns, BorderLayout.SOUTH);

        Theme.Card tc = new Theme.Card(); tc.setLayout(new BorderLayout(0, 10));
        JPanel sr = new JPanel(new BorderLayout(10, 0)); sr.setOpaque(false);
        sr.add(Theme.label("SEARCH"), BorderLayout.WEST); sr.add(search, BorderLayout.CENTER);
        search.addKeyListener(new KeyAdapter() { @Override public void keyReleased(KeyEvent e) { load(); } });
        Theme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) pick(); });
        tc.add(sr, BorderLayout.NORTH); tc.add(Theme.scroll(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16)); center.setOpaque(false);
        center.add(form, BorderLayout.NORTH); center.add(tc, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        load();
    }

    public void refresh() { load(); }

    private void msg(String s) { JOptionPane.showMessageDialog(this, s); }

    private void load() {
        model.setRowCount(0);
        String sql = "SELECT id, full_name, contact, department, position, employment, date_hired, salary_type, basic_salary "
                + "FROM employees WHERE full_name LIKE ? OR position LIKE ? OR department LIKE ? OR contact LIKE ? ORDER BY id";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + search.getText().trim() + "%";
            for (int i = 1; i <= 4; i++) ps.setString(i, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) model.addRow(new Object[]{Theme.empCode(rs.getInt(1)), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getDate(7), rs.getString(8),
                    Theme.money(rs.getDouble(9))});
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void pick() {
        int r = table.getSelectedRow(); if (r < 0) return;
        selectedId = Integer.parseInt(model.getValueAt(r, 0).toString().substring(4));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM employees WHERE id=?")) {
            ps.setInt(1, selectedId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            id.setText(Theme.empCode(selectedId));
            name.setText(rs.getString("full_name"));
            String ct = rs.getString("contact"); contact.setText(ct == null ? "" : ct);
            String ad = rs.getString("address"); address.setText(ad == null ? "" : ad);
            dept.setSelectedItem(rs.getString("department"));
            pos.setSelectedItem(rs.getString("position"));
            employment.setSelectedItem(rs.getString("employment"));
            hired.setText(rs.getDate("date_hired").toString());
            salaryType.setSelectedItem(rs.getString("salary_type"));
            basic.setText(String.valueOf(rs.getDouble("basic_salary")));
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void clear() {
        selectedId = -1; id.setText("Auto-generated"); name.setText(""); contact.setText(""); address.setText("");
        basic.setText(""); hired.setText(LocalDate.now().toString());
        pos.setSelectedIndex(0); dept.setSelectedIndex(0); employment.setSelectedIndex(0); salaryType.setSelectedIndex(0);
        table.clearSelection();
    }

    private void save(boolean insert) {
        if (!insert && selectedId < 0) { msg("Select an employee first."); return; }
        String n = name.getText().trim();
        String p = String.valueOf(pos.getEditor().getItem()).trim();
        if (n.isEmpty() || p.isEmpty()) { msg("Full name and position are required."); return; }
        double b;
        try { b = Double.parseDouble(basic.getText().trim().replace(",", "")); }
        catch (NumberFormatException ex) { msg("Basic salary must be a number."); return; }
        if (b <= 0) { msg("Basic salary must be greater than 0."); return; }
        Date d;
        try { d = Date.valueOf(hired.getText().trim()); }
        catch (IllegalArgumentException ex) { msg("Hire date must be in YYYY-MM-DD format."); return; }
        String sql = insert
                ? "INSERT INTO employees(full_name,contact,address,department,position,employment,date_hired,salary_type,basic_salary) VALUES(?,?,?,?,?,?,?,?,?)"
                : "UPDATE employees SET full_name=?,contact=?,address=?,department=?,position=?,employment=?,date_hired=?,salary_type=?,basic_salary=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n); ps.setString(2, contact.getText().trim()); ps.setString(3, address.getText().trim());
            ps.setString(4, (String) dept.getSelectedItem()); ps.setString(5, p);
            ps.setString(6, (String) employment.getSelectedItem()); ps.setDate(7, d);
            ps.setString(8, (String) salaryType.getSelectedItem()); ps.setDouble(9, b);
            if (!insert) ps.setInt(10, selectedId);
            ps.executeUpdate();
            load(); clear();
        } catch (SQLException ex) { Theme.error(this, ex); }
    }

    private void delete() {
        if (selectedId < 0) { msg("Select an employee first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this employee and all their payroll and attendance records?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM employees WHERE id=?")) {
            ps.setInt(1, selectedId); ps.executeUpdate(); load(); clear();
        } catch (SQLException ex) { Theme.error(this, ex); }
    }
}
