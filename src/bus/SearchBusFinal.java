package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.*;

public class SearchBusFinal {

    private JComboBox<String> sourceBox;
    private JComboBox<String> destBox;
    private JComboBox<String> dateBox;
    private DefaultTableModel model;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public SearchBusFinal(String username){

        JFrame f = new JFrame("Search Bus");
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
        panel.setPreferredSize(new Dimension(1100,600));
        panel.setBackground(new Color(255,255,255,220));

        Font heading = new Font("Times New Roman",Font.BOLD,26);
        Font text = new Font("Times New Roman",Font.PLAIN,16);

        JLabel title = new JLabel("Search Available Buses",SwingConstants.CENTER);
        title.setFont(heading);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));

        sourceBox = new JComboBox<>();
        destBox = new JComboBox<>();
        dateBox = new JComboBox<>();
        sourceBox.setFont(text);
        destBox.setFont(text);
        dateBox.setFont(text);

        LocalDate today = LocalDate.now();
        for(int i=0;i<7;i++){
            dateBox.addItem(today.plusDays(i).toString());
        }

        JButton searchBtn = new JButton("Search");
        JButton showAllBtn = new JButton("Show All");
        searchBtn.setFont(text);
        showAllBtn.setFont(text);

        JLabel srcLabel = new JLabel("Source:");
        JLabel dstLabel = new JLabel("Destination:");
        JLabel dateLabel = new JLabel("Journey Date:");
        srcLabel.setFont(text);
        dstLabel.setFont(text);
        dateLabel.setFont(text);

        JPanel top = new JPanel();
        top.add(srcLabel);
        top.add(sourceBox);
        top.add(dstLabel);
        top.add(destBox);
        top.add(dateLabel);
        top.add(dateBox);
        top.add(searchBtn);
        top.add(showAllBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north,BoxLayout.Y_AXIS));
        north.setOpaque(false);
        north.add(title);
        north.add(top);

        String cols[]={
                "Bus ID","Bus Name","Source","Destination",
                "Departure","Arrival","Seats Available","Fare"
        };

        model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFont(text);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane sp = new JScrollPane(table);

        JButton bookBtn = new JButton("Book Selected Bus");
        bookBtn.setFont(text);

        JPanel bottom = new JPanel();
        bottom.add(bookBtn);

        panel.add(north,BorderLayout.NORTH);
        panel.add(sp,BorderLayout.CENTER);
        panel.add(bottom,BorderLayout.SOUTH);

        bg.add(panel);

        loadCities();
        loadBuses();

        searchBtn.addActionListener(e -> loadBuses());
        dateBox.addActionListener(e -> loadBuses());

        showAllBtn.addActionListener(e -> {
            sourceBox.setSelectedItem("All");
            destBox.setSelectedItem("All");
            loadBuses();
        });

        bookBtn.addActionListener(e -> {

            int row = table.getSelectedRow();

            if(row==-1){
                JOptionPane.showMessageDialog(f,"Select bus first");
                return;
            }

            String busId = model.getValueAt(row,0).toString();
            int available = Integer.parseInt(model.getValueAt(row,6).toString());

            if(available <= 0){
                JOptionPane.showMessageDialog(f,"Bus is fully booked for this date");
                return;
            }

            String passenger = JOptionPane.showInputDialog(f,"Enter Passenger Name");
            if(passenger==null || passenger.isBlank()) return;

            String ticketStr = JOptionPane.showInputDialog(f,"Enter Ticket Count (1-10)");
            if(ticketStr==null || ticketStr.isBlank()) return;

            int tickets;
            try{
                tickets = Integer.parseInt(ticketStr.trim());
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(f,"Ticket count must be a number");
                return;
            }

            if(tickets < 1 || tickets > 10){
                JOptionPane.showMessageDialog(f,"You can book 1 to 10 tickets at a time");
                return;
            }

            String date = (String) dateBox.getSelectedItem();

            new BookTicket(username,busId,passenger,tickets,date);

            loadBuses(); // refresh availability
        });

        f.setVisible(true);
    }

    private void loadCities(){
        try(Connection con = DBConnection.getConnection()){

            sourceBox.removeAllItems();
            destBox.removeAllItems();
            sourceBox.addItem("All");
            destBox.addItem("All");

            try(ResultSet rs1 = con.createStatement().executeQuery(
                    "SELECT DISTINCT TRIM(source) FROM bus WHERE source IS NOT NULL AND TRIM(source) <> '' ORDER BY 1")){
                while(rs1.next()) sourceBox.addItem(rs1.getString(1));
            }

            try(ResultSet rs2 = con.createStatement().executeQuery(
                    "SELECT DISTINCT TRIM(destination) FROM bus WHERE destination IS NOT NULL AND TRIM(destination) <> '' ORDER BY 1")){
                while(rs2.next()) destBox.addItem(rs2.getString(1));
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String selected(JComboBox<String> box){
        Object o = box.getSelectedItem();
        if(o == null) return null;
        String s = o.toString().trim();
        return (s.isEmpty() || s.equalsIgnoreCase("All")) ? null : s;
    }

    private void loadBuses(){
        model.setRowCount(0);

        String src = selected(sourceBox);
        String dst = selected(destBox);
        Object dateObj = dateBox.getSelectedItem();
        if(dateObj == null) return;

        StringBuilder sql = new StringBuilder(
            "SELECT b.*, (SELECT COUNT(*) FROM booking k WHERE k.bus_id=b.bus_id AND k.journey_date=?) AS booked " +
            "FROM bus b WHERE 1=1"
        );
        if(src != null) sql.append(" AND LOWER(TRIM(b.source))=?");
        if(dst != null) sql.append(" AND LOWER(TRIM(b.destination))=?");
        sql.append(" ORDER BY b.bus_id");

        try(Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql.toString())){

            int i = 1;
            ps.setDate(i++, Date.valueOf(dateObj.toString()));
            if(src != null) ps.setString(i++, src.toLowerCase());
            if(dst != null) ps.setString(i++, dst.toLowerCase());

            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){

                    int total = rs.getInt("total_seats");
                    int booked = rs.getInt("booked");
                    int available = Math.max(0, total - booked);

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
                            available,
                            rs.getDouble("price")
                    });
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,"Could not load buses: " + ex.getMessage());
        }
    }
}
