package sugbomacciato;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Sugbomacciato logo: a steaming coffee cup in a gold badge, drawn with Java2D. */
public class Logo {
    public static Icon icon(int size) { return new ImageIcon(image(size)); }

    public static BufferedImage image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 100.0, size / 100.0);

        g.setPaint(new GradientPaint(0, 0, new Color(0xE0A050), 100, 100, new Color(0x8D5A22)));
        g.fill(new Ellipse2D.Double(2, 2, 96, 96));
        g.setColor(new Color(0x3E2723)); g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(5, 5, 90, 90));

        // steam
        g.setColor(new Color(0xFFF3E0));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            double x = 38 + i * 12;
            Path2D p = new Path2D.Double();
            p.moveTo(x, 40); p.curveTo(x - 6, 34, x + 6, 28, x, 22);
            g.draw(p);
        }
        // handle
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke(4.5f));
        g.draw(new Arc2D.Double(62, 50, 16, 16, 90, -180, Arc2D.OPEN));
        // cup body
        Path2D cup = new Path2D.Double();
        cup.moveTo(28, 46); cup.lineTo(72, 46); cup.lineTo(66, 72);
        cup.quadTo(65, 76, 60, 76); cup.lineTo(40, 76); cup.quadTo(35, 76, 34, 72);
        cup.closePath();
        g.setColor(Color.WHITE); g.fill(cup);
        // coffee surface and saucer
        g.setColor(new Color(0x6D4C41)); g.fill(new Ellipse2D.Double(30, 43.5, 40, 6));
        g.setColor(new Color(0xFFF3E0)); g.fill(new RoundRectangle2D.Double(24, 76, 52, 5, 5, 5));
        g.dispose();
        return img;
    }
}
