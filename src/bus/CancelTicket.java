package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class CancelTicket {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final String username;
    private final DefaultTableModel model;
    private final List<List<Integer>> groupIds = new ArrayList<>(); // booking_ids per table row
    private final List<List<Integer>> groupSeats = new ArrayList<>(); // seat numbers per row (same order as ids)
    private final List<Double> groupPrice = new ArrayList<>();       // fare per ticket per row
    private final List<String> groupPayment = new ArrayList<>();
    private final List<LocalDate> groupDate = new ArrayList<>();
    private final JFrame f;

    public CancelTicket(String username){

        this.username = username;

        f = new JFrame("Cancel Ticket");
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
        panel.setPreferredSize(new Dimension(950,520));
        panel.setBackground(new Color(255,255,255,220));

        Font heading = new Font("Times New Roman",Font.BOLD,24);
        Font text = new Font("Times New Roman",Font.PLAIN,16);

        JLabel title = new JLabel("Cancel Your Bookings", SwingConstants.CENTER);
        title.setFont(heading);
        title.setBorder(BorderFactory.createEmptyBorder(15,0,5,0));

        JLabel hint = new JLabel("Each row is one booking (all seats booked together). Ctrl/Shift-click to select several.",
                SwingConstants.CENTER);
        hint.setFont(text);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north,BoxLayout.Y_AXIS));
        north.setOpaque(false);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        north.add(title);
        north.add(hint);
        north.add(Box.createVerticalStrut(10));

        String cols[] = {"Bus","Route","Passenger","Journey Date","Seats","Tickets","Refund (90%)"};
        model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){ return false; }
        };

        JTable table = new JTable(model);
        table.setFont(text);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);

        JButton cancelBtn = new JButton("Cancel Selected Booking(s)");
        cancelBtn.setFont(text);
        cancelBtn.setBackground(new Color(178,34,34));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setOpaque(true);
        cancelBtn.setBorderPainted(false);

        JButton cancelSeatBtn = new JButton("Cancel Specific Seats");
        cancelSeatBtn.setFont(text);
        cancelSeatBtn.setBackground(new Color(205,92,20));
        cancelSeatBtn.setForeground(Color.WHITE);
        cancelSeatBtn.setOpaque(true);
        cancelSeatBtn.setBorderPainted(false);

        JButton cancelAllBtn = new JButton("Cancel ALL My Upcoming Tickets");
        cancelAllBtn.setFont(text);
        cancelAllBtn.setBackground(new Color(120,20,20));
        cancelAllBtn.setForeground(Color.WHITE);
        cancelAllBtn.setOpaque(true);
        cancelAllBtn.setBorderPainted(false);

        JPanel bottom = new JPanel();
        bottom.add(cancelBtn);
        bottom.add(cancelSeatBtn);
        bottom.add(cancelAllBtn);

        panel.add(north,BorderLayout.NORTH);
        panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.add(bottom,BorderLayout.SOUTH);
        bg.add(panel);

        loadBookings();

        cancelBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if(rows.length == 0){
                JOptionPane.showMessageDialog(f,"Select at least one booking");
                return;
            }
            List<Integer> list = new ArrayList<>();
            for(int r : rows) list.add(r);
            cancelRows(list);
        });

        cancelSeatBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if(rows.length != 1){
                JOptionPane.showMessageDialog(f,"Select exactly one booking to pick seats from");
                return;
            }
            cancelSpecificSeats(rows[0]);
        });

        cancelAllBtn.addActionListener(e -> {
            List<Integer> list = new ArrayList<>();
            for(int i=0;i<model.getRowCount();i++) list.add(i);
            if(list.isEmpty()){
                JOptionPane.showMessageDialog(f,"No bookings to cancel");
                return;
            }
            cancelRows(list);
        });

        f.setVisible(true);
    }

    private void loadBookings(){
        model.setRowCount(0);
        groupIds.clear();
        groupSeats.clear();
        groupPrice.clear();
        groupPayment.clear();
        groupDate.clear();

        String sql =
            "SELECT bu.bus_name, bu.source, bu.destination, bu.price, k.passenger_name, k.journey_date, " +
            "GROUP_CONCAT(k.seat_number ORDER BY k.seat_number) AS seats, " +
            "GROUP_CONCAT(k.booking_id ORDER BY k.seat_number) AS ids, " +
            "COUNT(*) AS cnt, MAX(k.payment_method) AS pm " +
            "FROM booking k JOIN bus bu ON k.bus_id=bu.bus_id " +
            "WHERE k.username=? " +
            "GROUP BY k.bus_id, bu.bus_name, bu.source, bu.destination, bu.price, k.passenger_name, k.journey_date " +
            "ORDER BY k.journey_date, k.bus_id";

        try(Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1,username);

            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){

                    List<Integer> ids = new ArrayList<>();
                    for(String s : rs.getString("ids").split(",")) ids.add(Integer.parseInt(s.trim()));

                    List<Integer> seatNums = new ArrayList<>();
                    for(String s : rs.getString("seats").split(",")) seatNums.add(Integer.parseInt(s.trim()));

                    int cnt = rs.getInt("cnt");
                    double price = rs.getDouble("price");
                    Date d = rs.getDate("journey_date");
                    LocalDate ld = d != null ? d.toLocalDate() : null;
                    String pm = rs.getString("pm");

                    groupIds.add(ids);
                    groupSeats.add(seatNums);
                    groupPrice.add(price);
                    groupPayment.add(pm == null ? "Online" : pm);
                    groupDate.add(ld);

                    model.addRow(new Object[]{
                        rs.getString("bus_name"),
                        rs.getString("source") + " → " + rs.getString("destination"),
                        rs.getString("passenger_name"),
                        ld != null ? ld.format(DATE_FMT) : "-",
                        rs.getString("seats"),
                        cnt,
                        "₹ " + String.format("%.2f", price * cnt * 0.90)
                    });
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Could not load bookings: " + ex.getMessage());
        }
    }

    private void cancelRows(List<Integer> rows){

        LocalDate today = LocalDate.now();

        List<Integer> allIds = new ArrayList<>();
        double totalFare = 0;
        int tickets = 0;
        int skippedPast = 0;
        String payment = "Online";

        for(int r : rows){
            LocalDate d = groupDate.get(r);
            if(d != null && d.isBefore(today)){
                skippedPast++;
                continue;
            }
            List<Integer> ids = groupIds.get(r);
            allIds.addAll(ids);
            totalFare += groupPrice.get(r) * ids.size();
            tickets += ids.size();
            payment = groupPayment.get(r);
        }

        if(allIds.isEmpty()){
            JOptionPane.showMessageDialog(f,"Past journeys cannot be cancelled");
            return;
        }

        confirmAndCancel(allIds,totalFare,tickets,payment,skippedPast);
    }

    // Pick individual seats from one booking row
    private void cancelSpecificSeats(int row){

        LocalDate d = groupDate.get(row);
        if(d != null && d.isBefore(LocalDate.now())){
            JOptionPane.showMessageDialog(f,"Past journeys cannot be cancelled");
            return;
        }

        List<Integer> ids = groupIds.get(row);
        List<Integer> seats = groupSeats.get(row);

        JPanel box = new JPanel(new GridLayout(0,5,8,4));
        List<JCheckBox> checks = new ArrayList<>();
        for(int seat : seats){
            JCheckBox cb = new JCheckBox("Seat " + seat);
            checks.add(cb);
            box.add(cb);
        }

        JPanel wrap = new JPanel(new BorderLayout(0,8));
        wrap.add(new JLabel("Select the seats to cancel:"),BorderLayout.NORTH);
        wrap.add(box,BorderLayout.CENTER);

        if(JOptionPane.showConfirmDialog(f,wrap,"Cancel Specific Seats",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        List<Integer> chosen = new ArrayList<>();
        for(int i=0;i<checks.size();i++){
            if(checks.get(i).isSelected()) chosen.add(ids.get(i));
        }

        if(chosen.isEmpty()){
            JOptionPane.showMessageDialog(f,"No seats selected");
            return;
        }

        double totalFare = groupPrice.get(row) * chosen.size();
        confirmAndCancel(chosen,totalFare,chosen.size(),groupPayment.get(row),0);
    }

    private void confirmAndCancel(List<Integer> allIds,double totalFare,int tickets,
                                  String payment,int skippedPast){

        double charge = totalFare * 0.10;
        double refund = totalFare - charge;

        String msg = "Cancel " + tickets + " ticket(s)?\n" +
                     "Cancellation charge (10%): ₹ " + String.format("%.2f",charge) + "\n" +
                     "Refund: ₹ " + String.format("%.2f",refund) +
                     (skippedPast > 0 ? "\n(" + skippedPast + " past booking(s) skipped)" : "");

        if(JOptionPane.showConfirmDialog(f,msg,"Confirm Cancellation",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        StringBuilder in = new StringBuilder();
        for(int i=0;i<allIds.size();i++) in.append(i==0 ? "?" : ",?");

        try(Connection con = DBConnection.getConnection()){

            if(con == null){
                JOptionPane.showMessageDialog(f,"Cannot connect to database");
                return;
            }

            con.setAutoCommit(false);

            try(PreparedStatement del = con.prepareStatement(
                    "DELETE FROM booking WHERE username=? AND booking_id IN (" + in + ")")){

                del.setString(1,username);
                for(int i=0;i<allIds.size();i++) del.setInt(i+2,allIds.get(i));

                int deleted = del.executeUpdate();
                con.commit();

                if(deleted != allIds.size()){
                    JOptionPane.showMessageDialog(f,"Only " + deleted + " of " + allIds.size() +
                            " tickets were cancelled.");
                }

            }catch(SQLException ex){
                con.rollback();
                throw ex;
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Cancellation failed: " + ex.getMessage());
            return;
        }

        processRefund(payment,refund,charge,tickets);
        loadBookings();
    }

    private void processRefund(String paymentMethod,double refundAmount,double charge,int tickets){

        JDialog d = new JDialog(f,"Refund Processing",true);
        d.setSize(450,350);
        d.setLocationRelativeTo(f);
        d.setLayout(null);

        JLabel title = new JLabel("Refund Details");
        title.setFont(new Font("Times New Roman",Font.BOLD,22));
        title.setBounds(140,30,200,30);

        JLabel l1 = new JLabel("Tickets cancelled: " + tickets);
        l1.setFont(new Font("Times New Roman",Font.PLAIN,18));
        l1.setBounds(80,80,320,30);

        JLabel l2 = new JLabel("Refund To: " + username + " (" + paymentMethod + ")");
        l2.setFont(new Font("Times New Roman",Font.PLAIN,18));
        l2.setBounds(80,110,340,30);

        JLabel l3 = new JLabel("Cancellation Charge (10%): ₹ " + String.format("%.2f",charge));
        l3.setFont(new Font("Times New Roman",Font.PLAIN,16));
        l3.setBounds(80,140,340,30);

        JLabel l4 = new JLabel("Refund Amount: ₹ " + String.format("%.2f",refundAmount));
        l4.setFont(new Font("Times New Roman",Font.BOLD,18));
        l4.setBounds(80,175,340,30);

        JButton ok = new JButton("OK");
        ok.setBounds(150,230,150,40);
        ok.setBackground(new Color(34,139,34));
        ok.setForeground(Color.WHITE);
        ok.addActionListener(e -> {
            JOptionPane.showMessageDialog(d,"Refund Successful\nAmount will reflect in 2-3 working days.");
            d.dispose();
        });

        d.add(title); d.add(l1); d.add(l2); d.add(l3); d.add(l4); d.add(ok);
        d.setVisible(true);
    }
}
