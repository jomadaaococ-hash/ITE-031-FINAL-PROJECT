package sugbomacciato;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

/** Shared look & feel: coffee-inspired palette and styled components. */
public class Theme {
    public static final Color BG = new Color(0xF7F1EA);
    public static final Color SIDEBAR = new Color(0x3E2723);
    public static final Color ACCENT = new Color(0xC8863E);
    public static final Color DANGER = new Color(0xB5473A);
    public static final Color GRAY = new Color(0x8D7B72);
    public static final Color TEXT = new Color(0x3E2723);
    public static final Font H1 = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 14);

    public static String money(double v) { return String.format("₱%,.2f", v); }
    public static String plain(double v) { return String.format("PHP %,.2f", v); }
    public static String empCode(int id) { return String.format("EMP-%03d", id); }
    public static final String[] DEPTS = {"Barista", "Kitchen", "Cashier", "Management", "Delivery"};

    public static String[] deptsWith(String first) {
        String[] a = new String[DEPTS.length + 1];
        a[0] = first;
        System.arraycopy(DEPTS, 0, a, 1, DEPTS.length);
        return a;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(BODY);
        return c;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(BOLD); b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setRolloverEnabled(true); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(9, 20, 9, 20));
        return b;
    }

    public static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(BODY);
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0xD7CCC8), 1, true), new EmptyBorder(6, 8, 6, 8)));
        return f;
    }

    public static JLabel label(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(GRAY);
        return l;
    }

    public static void error(Component c, Exception ex) {
        JOptionPane.showMessageDialog(c, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(32); t.setFont(BODY); t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xF3DFC1)); t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        DefaultTableCellRenderer head = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, f, r, c);
                setBackground(SIDEBAR); setForeground(Color.WHITE); setFont(BOLD);
                setBorder(new EmptyBorder(0, 10, 0, 10)); return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(head);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, f, r, c);
                if (!s) setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xFBF7F2));
                setForeground(TEXT); setBorder(new EmptyBorder(0, 10, 0, 10)); return this;
            }
        });
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(new Color(0xE7DDD3)));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Rounded white card panel. */
    public static class Card extends JPanel {
        public Card() { setOpaque(false); setBorder(new EmptyBorder(16, 18, 16, 18)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(new Color(0xE7DDD3)); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose(); super.paintComponent(g);
        }
    }
}
