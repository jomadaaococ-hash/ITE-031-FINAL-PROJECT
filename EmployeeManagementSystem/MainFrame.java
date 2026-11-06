package sugbomacciato;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final DashboardPanel dash = new DashboardPanel();
    private final EmployeePanel emp = new EmployeePanel();
    private final AttendancePanel att = new AttendancePanel();
    private final PayrollPanel pay = new PayrollPanel();
    private final ReportsPanel rep = new ReportsPanel();
    private final List<JButton> navButtons = new ArrayList<>();

    public MainFrame(String fullName) {
        super("Sugbomacciato - Payroll System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Logo.image(64)); setSize(1240, 740); setMinimumSize(new Dimension(1100, 650)); setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);   // full screen

        JPanel side = new JPanel();
        side.setBackground(Theme.SIDEBAR); side.setPreferredSize(new Dimension(270, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("Sugbomacciato");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22)); brand.setForeground(Color.WHITE);
        brand.setBorder(new EmptyBorder(10, 24, 4, 10));
        JLabel sub = new JLabel("Payroll System");
        sub.setFont(Theme.BODY); sub.setForeground(new Color(0xE8C9A0)); sub.setBorder(new EmptyBorder(0, 24, 28, 10));
        JLabel logo = new JLabel(Logo.icon(64));
        logo.setBorder(new EmptyBorder(26, 24, 0, 10));
        side.add(logo); side.add(brand); side.add(sub);
        side.add(nav("Dashboard", "dash"));
        side.add(nav("Employees", "emp"));
        side.add(nav("Attendance", "att"));
        side.add(nav("Payroll", "pay"));
        side.add(nav("Reports", "rep"));
        side.add(Box.createVerticalGlue());
        JLabel who = new JLabel("Signed in: " + fullName);
        who.setFont(Theme.BODY); who.setForeground(new Color(0xD7CCC8)); who.setBorder(new EmptyBorder(0, 24, 8, 10));
        JButton out = Theme.button("Logout", Theme.DANGER);
        out.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        JPanel outWrap = new JPanel(new BorderLayout()); outWrap.setOpaque(false);
        outWrap.setBorder(new EmptyBorder(0, 20, 24, 20)); outWrap.add(out);
        outWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        side.add(who); side.add(outWrap);

        content.add(dash, "dash"); content.add(emp, "emp"); content.add(att, "att");
        content.add(pay, "pay"); content.add(rep, "rep");
        add(side, BorderLayout.WEST); add(content, BorderLayout.CENTER);
        show("dash", navButtons.get(0));
    }

    private JButton nav(String text, String card) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                boolean act = Boolean.TRUE.equals(getClientProperty("active"));
                g.setColor(act ? Theme.ACCENT : getModel().isRollover() ? new Color(0x5D4037) : Theme.SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.BOLD); b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setRolloverEnabled(true); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(14, 24, 14, 24));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> show(card, b));
        navButtons.add(b);
        return b;
    }

    private void show(String card, JButton active) {
        for (JButton b : navButtons) { b.putClientProperty("active", b == active); b.repaint(); }
        if (card.equals("dash")) dash.refresh();
        if (card.equals("emp")) emp.refresh();
        if (card.equals("att")) att.load();
        if (card.equals("pay")) pay.refresh();
        cards.show(content, card);
    }
}
