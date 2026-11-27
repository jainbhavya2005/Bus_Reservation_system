package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

/** Admin analytics: headline numbers plus bar charts of tickets and revenue. Revenue is at base bus fare. */
public class RevenueChart {

    public static class Summary {
        public long tickets;
        public double revenue;
        public String topBus = "-";
        public long topBusTickets;
    }

    // ================= headline numbers =================

    public static Summary summary(){
        Summary s = new Summary();
        try(Connection con = DBConnection.getConnection()){
            if(con == null) return s;

            try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*), COALESCE(SUM(b.price),0) FROM booking k JOIN bus b ON b.bus_id=k.bus_id")){
                if(rs.next()){
                    s.tickets = rs.getLong(1);
                    s.revenue = rs.getDouble(2);
                }
            }

            try(Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(
                    "SELECT b.bus_name, COUNT(*) AS c FROM booking k JOIN bus b ON b.bus_id=k.bus_id " +
                    "GROUP BY b.bus_id, b.bus_name ORDER BY c DESC LIMIT 1")){
                if(rs.next()){
                    s.topBus = rs.getString(1);
                    s.topBusTickets = rs.getLong(2);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return s;
    }

    // ================= chart window =================

    public RevenueChart(){

        JFrame f = new JFrame("Revenue Analytics");
        f.setSize(940, 600);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        List<String> dayLabels = new ArrayList<>();
        List<Double> dayTickets = new ArrayList<>();
        List<Double> dayRevenue = new ArrayList<>();
        List<String> routeLabels = new ArrayList<>();
        List<Double> routeRevenue = new ArrayList<>();

        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd MMM");

        try(Connection con = DBConnection.getConnection()){
            if(con != null){

                try(Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                        "SELECT k.journey_date, COUNT(*), COALESCE(SUM(b.price),0) " +
                        "FROM booking k JOIN bus b ON b.bus_id=k.bus_id " +
                        "GROUP BY k.journey_date ORDER BY k.journey_date DESC LIMIT 14")){
                    while(rs.next()){
                        dayLabels.add(rs.getDate(1).toLocalDate().format(dm));
                        dayTickets.add(rs.getDouble(2));
                        dayRevenue.add(rs.getDouble(3));
                    }
                }
                Collections.reverse(dayLabels);
                Collections.reverse(dayTickets);
                Collections.reverse(dayRevenue);

                try(Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                        "SELECT CONCAT(b.source,' - ',b.destination), COALESCE(SUM(b.price),0) " +
                        "FROM booking k JOIN bus b ON b.bus_id=k.bus_id " +
                        "GROUP BY b.source, b.destination ORDER BY SUM(b.price) DESC LIMIT 8")){
                    while(rs.next()){
                        routeLabels.add(rs.getString(1));
                        routeRevenue.add(rs.getDouble(2));
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(f, "Could not load analytics: " + e.getMessage());
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Times New Roman", Font.BOLD, 15));
        tabs.addTab("Tickets per journey date", new BarChart("Tickets booked per journey date (latest 14 dates)", dayLabels, dayTickets, ""));
        tabs.addTab("Revenue per journey date", new BarChart("Revenue per journey date (latest 14 dates)", dayLabels, dayRevenue, "Rs. "));
        tabs.addTab("Revenue per route", new BarChart("Top routes by revenue", routeLabels, routeRevenue, "Rs. "));

        JLabel note = new JLabel("  Revenue is calculated at the base bus fare (before passenger discounts).");
        note.setFont(new Font("Times New Roman", Font.ITALIC, 13));

        f.add(tabs, BorderLayout.CENTER);
        f.add(note, BorderLayout.SOUTH);
        f.setVisible(true);
    }

    // ================= simple bar chart (Java2D, no library) =================

    public static class BarChart extends JPanel {

        private final String title;
        private final List<String> labels;
        private final List<Double> values;
        private final String prefix;

        public BarChart(String title, List<String> labels, List<Double> values, String prefix){
            this.title = title;
            this.labels = labels;
            this.values = values;
            this.prefix = prefix;
            setBackground(Color.WHITE);
            setForeground(new Color(40, 40, 40));
        }

        private static double niceMax(double v){
            if(v <= 0) return 1;
            double exp = Math.pow(10, Math.floor(Math.log10(v)));
            double f = v / exp;
            double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
            return nice * exp;
        }

        private String fmt(double v){
            return prefix + (v == Math.floor(v) ? String.format("%,.0f", v) : String.format("%,.1f", v));
        }

        @Override
        protected void paintComponent(Graphics g0){
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fg = getForeground();
            int w = getWidth(), h = getHeight();

            g.setColor(fg);
            g.setFont(new Font("Times New Roman", Font.BOLD, 18));
            FontMetrics tm = g.getFontMetrics();
            g.drawString(title, (w - tm.stringWidth(title)) / 2, 28);

            if(values.isEmpty()){
                g.setFont(new Font("Times New Roman", Font.ITALIC, 16));
                String msg = "No bookings yet";
                g.drawString(msg, (w - g.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                g.dispose();
                return;
            }

            int left = 90, right = 30, top = 55, bottom = 95;
            int cw = w - left - right, ch = h - top - bottom;

            double max = 0;
            for(double v : values) max = Math.max(max, v);
            double top5 = niceMax(max);

            g.setFont(new Font("Times New Roman", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();

            for(int i = 0; i <= 5; i++){
                int y = top + ch - (int) (ch * i / 5.0);
                g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 45));
                g.drawLine(left, y, left + cw, y);
                g.setColor(fg);
                String s = fmt(top5 * i / 5.0);
                g.drawString(s, left - 8 - fm.stringWidth(s), y + 4);
            }

            g.setColor(fg);
            g.drawLine(left, top, left, top + ch);
            g.drawLine(left, top + ch, left + cw, top + ch);

            int n = values.size();
            double slot = cw / (double) n;
            int barW = (int) Math.max(8, Math.min(70, slot * 0.6));

            for(int i = 0; i < n; i++){
                double v = values.get(i);
                int bh = (int) (ch * v / top5);
                int x = left + (int) (slot * i + (slot - barW) / 2);
                int y = top + ch - bh;

                g.setPaint(new GradientPaint(0, y, new Color(60, 150, 255), 0, y + Math.max(bh, 1), new Color(20, 80, 190)));
                g.fillRoundRect(x, y, barW, bh, 6, 6);

                g.setColor(fg);
                String vs = fmt(v);
                g.drawString(vs, x + (barW - fm.stringWidth(vs)) / 2, y - 5);

                // rotated x label
                Graphics2D gl = (Graphics2D) g.create();
                gl.translate(x + barW / 2, top + ch + 14);
                gl.rotate(Math.toRadians(35));
                String lbl = labels.get(i);
                if(lbl.length() > 18) lbl = lbl.substring(0, 17) + "...";
                gl.drawString(lbl, 0, 0);
                gl.dispose();
            }
            g.dispose();
        }
    }
}
