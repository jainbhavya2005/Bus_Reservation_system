package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.*;

public class ViewBuses {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public ViewBuses(){

        JFrame f = new JFrame("All Buses");
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel bg = new JPanel(){
            Image img = new ImageIcon("Images/Bg.jpeg").getImage();
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(img,0,0,getWidth(),getHeight(),this);
            }
        };

        bg.setLayout(new GridBagLayout());
        f.setContentPane(bg);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(1100,550));
        panel.setBackground(new Color(255,255,255,220));

        Font headingFont = new Font("Times New Roman",Font.BOLD,26);
        Font tableFont = new Font("Times New Roman",Font.PLAIN,16);
        Font headerFont = new Font("Times New Roman",Font.BOLD,18);

        JLabel title = new JLabel("All Buses",SwingConstants.CENTER);
        title.setFont(headingFont);
        title.setForeground(new Color(25,25,112));
        title.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));

        String cols[]={
                "Bus ID","Bus Name","Source","Destination","Departure",
                "Arrival","Total Seats","Fare","Upcoming Bookings"
        };

        DefaultTableModel model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){ return false; }
        };

        JTable table = new JTable(model);
        table.setFont(tableFont);
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(30,144,255));
        table.setAutoCreateRowSorter(true);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(1).setPreferredWidth(170);
        cm.getColumn(8).setPreferredWidth(130);

        JTableHeader header = table.getTableHeader();
        header.setFont(headerFont);
        header.setBackground(new Color(30,144,255));
        header.setForeground(Color.WHITE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(10,30,30,30));

        panel.add(title,BorderLayout.NORTH);
        panel.add(sp,BorderLayout.CENTER);
        bg.add(panel);

        try(Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                "SELECT b.*, " +
                "(SELECT COUNT(*) FROM booking k WHERE k.bus_id=b.bus_id AND k.journey_date>=CURDATE()) AS upcoming " +
                "FROM bus b ORDER BY b.bus_id")){

            while(rs.next()){

                Time dep = rs.getTime("departure_time");
                int hours = rs.getInt("journey_hours");

                String depStr = "Not Set";
                String arrStr = "Not Set";

                if(dep != null){
                    LocalTime d = dep.toLocalTime();
                    int daysLater = (d.getHour() + hours) / 24;
                    depStr = d.format(TIME_FMT);
                    arrStr = d.plusHours(hours).format(TIME_FMT)
                            + (daysLater > 0 ? " (+" + daysLater + "d)" : "");
                }

                model.addRow(new Object[]{
                        rs.getInt("bus_id"),
                        rs.getString("bus_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        depStr,
                        arrStr,
                        rs.getInt("total_seats"),
                        rs.getDouble("price"),
                        rs.getInt("upcoming")
                });
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Error: " + ex.getMessage());
        }

        f.setVisible(true);
    }
}
