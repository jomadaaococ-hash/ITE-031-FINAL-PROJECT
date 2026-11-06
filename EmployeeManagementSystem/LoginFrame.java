package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private final JTextField user = Theme.field();
    private final JPasswordField pass = new JPasswordField();

    public LoginFrame() {
        super("Sugbomacciato - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Logo.image(64));
        setSize(860, 580); setResizable(false); setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        JPanel left = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x3E2723), 0, getHeight(), new Color(0x6D4C41)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        JPanel brand = new JPanel(); brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel(Logo.icon(120));
        JLabel name = new JLabel("Sugbomacciato");
        name.setFont(new Font("Segoe UI", Font.BOLD, 32)); name.setForeground(Color.WHITE);
        JLabel tag = new JLabel("<html><div style='text-align:center'>Employee Salary &amp;<br>Payroll Management System</div></html>");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 16)); tag.setForeground(new Color(0xE8C9A0));
        tag.setHorizontalAlignment(SwingConstants.CENTER);
        for (JComponent c : new JComponent[]{logo, name, tag}) c.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(logo); brand.add(Box.createVerticalStrut(14)); brand.add(name);
        brand.add(Box.createVerticalStrut(8)); brand.add(tag);
        left.add(brand);

        JPanel right = new JPanel(); right.setBackground(Theme.BG);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(70, 50, 40, 50));
        JLabel h = new JLabel("Welcome back"); h.setFont(Theme.H1); h.setForeground(Theme.TEXT);
        JLabel sub = new JLabel("Sign in to continue"); sub.setFont(Theme.BODY); sub.setForeground(Theme.GRAY);
        pass.setFont(Theme.BODY); pass.setBorder(user.getBorder());
        JButton login = Theme.button("Login", Theme.ACCENT);
        login.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(login);
        JLabel hint = new JLabel("Employee? Create an account to view your salary.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12)); hint.setForeground(Theme.GRAY);
        JButton signup = Theme.button("Employee Sign Up", new Color(0x6D4C41));
        signup.addActionListener(e -> signUp());

        JLabel ul = Theme.label("USERNAME"), pl = Theme.label("PASSWORD");
        for (JComponent c : new JComponent[]{h, sub, ul, user, pl, pass, login, hint, signup}) c.setAlignmentX(LEFT_ALIGNMENT);
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        pass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        signup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        right.add(h); right.add(sub); right.add(Box.createVerticalStrut(24));
        right.add(ul); right.add(Box.createVerticalStrut(4)); right.add(user);
        right.add(Box.createVerticalStrut(14));
        right.add(pl); right.add(Box.createVerticalStrut(4)); right.add(pass);
        right.add(Box.createVerticalStrut(22)); right.add(login);
        right.add(Box.createVerticalStrut(26)); right.add(hint);
        right.add(Box.createVerticalStrut(6)); right.add(signup);

        add(left); add(right);
    }

    private void warn(String s) { JOptionPane.showMessageDialog(this, s, "Sign Up", JOptionPane.WARNING_MESSAGE); }

    private void doLogin() {
        String sql = "SELECT full_name, role, emp_id FROM users WHERE username=? AND password=SHA2(?,256)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getText().trim());
            ps.setString(2, new String(pass.getPassword()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fn = rs.getString(1), role = rs.getString(2);
                int emp = rs.getInt(3);
                boolean noEmp = rs.wasNull();
                if ("EMPLOYEE".equals(role) && !noEmp) new EmployeeFrame(fn, emp).setVisible(true);
                else new MainFrame(fn).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect to database.\nIs XAMPP MySQL running?\n\n" + ex.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Employees create their own account. They must know their Employee ID and the contact number on file. */
    private void signUp() {
        JTextField fid = Theme.field(), fct = Theme.field(), fu = Theme.field();
        JPasswordField p1 = new JPasswordField(), p2 = new JPasswordField();
        p1.setBorder(fid.getBorder()); p2.setBorder(fid.getBorder());
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(Theme.label("EMPLOYEE ID (e.g. EMP-001)")); p.add(fid);
        p.add(Theme.label("CONTACT NO. (as given to HR)")); p.add(fct);
        p.add(Theme.label("CHOOSE A USERNAME")); p.add(fu);
        p.add(Theme.label("PASSWORD (min. 6 characters)")); p.add(p1);
        p.add(Theme.label("CONFIRM PASSWORD")); p.add(p2);
        if (JOptionPane.showConfirmDialog(this, p, "Employee Sign Up", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String un = fu.getText().trim(), pw = new String(p1.getPassword());
        int id;
        try { id = Integer.parseInt(fid.getText().trim().toUpperCase().replace("EMP-", "")); }
        catch (NumberFormatException ex) { warn("Invalid Employee ID."); return; }
        if (un.length() < 4 || un.contains(" ")) { warn("Username must be at least 4 characters, no spaces."); return; }
        if (pw.length() < 6) { warn("Password must be at least 6 characters."); return; }
        if (!pw.equals(new String(p2.getPassword()))) { warn("Passwords do not match."); return; }
        String digits = fct.getText().replaceAll("\\D", "");

        try (Connection c = DB.get()) {
            String name, ct;
            try (PreparedStatement ps = c.prepareStatement("SELECT full_name, contact FROM employees WHERE id=?")) {
                ps.setInt(1, id); ResultSet rs = ps.executeQuery();
                if (!rs.next()) { warn("Employee ID not found."); return; }
                name = rs.getString(1); ct = rs.getString(2) == null ? "" : rs.getString(2);
            }
            String cd = ct.replaceAll("\\D", "");
            if (cd.isEmpty() || !cd.equals(digits)) {
                warn("Contact number does not match our records.\nPlease ask the admin to check your details.");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE emp_id=?")) {
                ps.setInt(1, id);
                if (ps.executeQuery().next()) { warn("This employee already has an account."); return; }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(username,password,full_name,role,emp_id) VALUES(?,SHA2(?,256),?,'EMPLOYEE',?)")) {
                ps.setString(1, un); ps.setString(2, pw); ps.setString(3, name); ps.setInt(4, id);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Account created for " + name + ".\nYou can now log in.");
            user.setText(un); pass.setText("");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) warn("That username is already taken.");
            else JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
