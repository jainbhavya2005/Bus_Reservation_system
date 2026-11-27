package util;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

/**
 * Global dark-mode support. Every window that opens is re-coloured automatically, so
 * individual screens need no changes. The choice is remembered between runs.
 */
public class Theme {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static boolean dark = PREFS.getBoolean("dark", false);
    private static boolean installed = false;

    private static final String BG = "theme.bg";
    private static final String FG = "theme.fg";
    private static final String GRID = "theme.grid";

    public static boolean isDark(){
        return dark;
    }

    /** Call once at start-up (before the first window). */
    public static void install(){
        if(installed) return;
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(ev -> {
            if(dark && ev.getID() == WindowEvent.WINDOW_OPENED){
                applyTo(((WindowEvent) ev).getWindow());
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    public static void setDark(boolean value){
        dark = value;
        PREFS.putBoolean("dark", value);
        for(Window w : Window.getWindows()) applyTo(w);
    }

    /** Adds a "Dark mode" check box to a panel that uses a null layout. */
    public static JCheckBox addToggle(JPanel panel, int x, int y){
        JCheckBox box = new JCheckBox("Dark mode", dark);
        box.setOpaque(false);
        box.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        box.setBounds(x, y, 120, 24);
        box.addActionListener(e -> setDark(box.isSelected()));
        panel.add(box);
        return box;
    }

    // ================= colouring =================

    public static void applyTo(Component c){
        style(c);
        if(c instanceof Container){
            for(Component child : ((Container) c).getComponents()) applyTo(child);
        }
        c.repaint();
    }

    private static void style(Component c){
        if(!(c instanceof JComponent)) return;
        JComponent jc = (JComponent) c;

        Color obg = original(jc, BG, jc.getBackground());
        Color ofg = original(jc, FG, jc.getForeground());

        jc.setBackground(dark ? darkBg(obg) : obg);
        jc.setForeground(dark ? darkFg(ofg) : ofg);

        if(jc instanceof JTextComponent){
            ((JTextComponent) jc).setCaretColor(dark ? new Color(232, 234, 240) : Color.BLACK);
        }
        if(jc instanceof JTable){
            JTable t = (JTable) jc;
            Color og = original(t, GRID, t.getGridColor());
            t.setGridColor(dark ? new Color(80, 86, 104) : og);
        }
        if(jc instanceof JTableHeader){
            // keep explicitly coloured (blue) headers; default grey ones follow the theme
            jc.setOpaque(true);
        }
    }

    private static Color original(JComponent c, String key, Color current){
        Object o = c.getClientProperty(key);
        if(o instanceof Color) return (Color) o;
        if(current == null) return null;
        c.putClientProperty(key, current);
        return current;
    }

    private static float lum(Color c){
        return (0.299f * c.getRed() + 0.587f * c.getGreen() + 0.114f * c.getBlue()) / 255f;
    }

    private static Color darkBg(Color o){
        if(o == null) return null;
        if(o.getAlpha() < 255) return new Color(26, 30, 40, o.getAlpha());
        float l = lum(o);
        if(l > 0.80f) return new Color(43, 47, 58);
        if(l > 0.60f) return new Color(60, 66, 82);
        return o;
    }

    private static Color darkFg(Color o){
        if(o == null) return null;
        return lum(o) < 0.45f ? new Color(232, 234, 240) : o;
    }
}