package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.*;

public class ViewBookings {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public ViewBookings() {

        JFrame f = new JFrame("All Bookings");
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel bgPanel = new JPanel(){
            Image bg = new ImageIcon("Images/Bg.jpeg").getImage();

            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(bg,0,0,getWidth(),getHeight(),this);
            }
        };

        bgPanel.setLayout(new GridBagLayout());
        f.setContentPane(bgPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1150,560));
        panel.setBackground(new Color(255,255,255,220));

        Font headingFont = new Font("Times New Roman", Font.BOLD, 26);
        Font tableFont = new Font("Times New Roman", Font.PLAIN, 16);
        Font headerFont = new Font("Times New Roman", Font.BOLD, 18);

        JLabel title = new JLabel("All User Bookings", SwingConstants.CENTER);
        title.setFont(headingFont);
        title.setForeground(new Color(25,25,112));
        title.setBorder(BorderFactory.createEmptyBorder(20,0,5,0));

        JLabel summary = new JLabel("", SwingConstants.CENTER);
        summary.setFont(tableFont);
        summary.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(title,BorderLayout.CENTER);
        north.add(summary,BorderLayout.SOUTH);

        String cols[]={
                "Username","Passenger","Bus Name","Source","Destination",
                "Journey Date","Seats","Tickets","Total Fare"
        };

        DefaultTableModel model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){ return false; }
            public Class<?> getColumnClass(int c){
                return (c == 7) ? Integer.class : Object.class;
            }
        };
        JTable table = new JTable(model);

        table.setFont(tableFont);
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(30,144,255));
        table.setGridColor(new Color(220,220,220));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(5).setPreferredWidth(110);
        cm.getColumn(6).setPreferredWidth(140);
        cm.getColumn(7).setPreferredWidth(60);
        cm.getColumn(8).setPreferredWidth(90);

        JTableHeader header = table.getTableHeader();
        header.setFont(headerFont);
        header.setBackground(new Color(30,144,255));
        header.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10,30,30,30));

        String sql =
            "SELECT b.username, b.passenger_name, bu.bus_name, bu.source, bu.destination, bu.price, " +
            "b.journey_date, " +
            "GROUP_CONCAT(b.seat_number ORDER BY b.seat_number) AS seats, " +
            "COUNT(*) AS cnt, MIN(b.booking_id) AS first_id " +
            "FROM booking b JOIN bus bu ON b.bus_id=bu.bus_id " +
            "GROUP BY b.username, b.passenger_name, b.bus_id, bu.bus_name, bu.source, bu.destination, " +
            "bu.price, b.journey_date " +
            "ORDER BY b.journey_date DESC, first_id";

        try(Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql)) {

            int groups = 0;
            int totalTickets = 0;

            while(rs.next()) {
                Date d = rs.getDate("journey_date");
                int cnt = rs.getInt("cnt");

                groups++;
                totalTickets += cnt;

                model.addRow(new Object[]{
                        rs.getString("username"),
                        rs.getString("passenger_name"),
                        rs.getString("bus_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        d != null ? d.toLocalDate().format(DATE_FMT) : "-",
                        rs.getString("seats"),
                        cnt,
                        "₹ " + String.format("%.2f", rs.getDouble("price") * cnt)
                });
            }

            summary.setText(groups + " booking(s) • " + totalTickets + " ticket(s)");

        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Error: " + ex.getMessage());
        }

        panel.add(north, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        bgPanel.add(panel);

        f.setVisible(true);
    }
}