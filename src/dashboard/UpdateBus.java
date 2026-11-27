package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;
import util.RunDays;

public class UpdateBus {

    private final JFrame f;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField[] fields = new JTextField[9];
    private int selectedBusId = -1;

    public UpdateBus(){

        f = new JFrame("Update Bus");
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
        panel.setPreferredSize(new Dimension(1150,620));
        panel.setBackground(new Color(255,255,255,220));

        Font h = new Font("Times New Roman",Font.BOLD,26);
        Font text = new Font("Times New Roman",Font.PLAIN,15);
        Font b = new Font("Times New Roman",Font.BOLD,16);

        JLabel title = new JLabel("Update Bus - select a bus, edit, then save",SwingConstants.CENTER);
        title.setFont(h);
        title.setBorder(BorderFactory.createEmptyBorder(15,0,10,0));

        String cols[] = {"Bus ID","Bus Name","Type","Source","Destination","Departure","Hours","Total Seats","Price","Runs On"};
        model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){ return false; }
        };

        table = new JTable(model);
        table.setFont(text);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        // ===== EDIT FORM =====
        String labels[] = {"Bus Name","Type","Source","Destination","Departure (HH:MM)","Hours","Total Seats","Price (₹)","Runs On"};

        JPanel form = new JPanel(new GridLayout(2,9,6,4));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(10,15,5,15));

        for(String l : labels){
            JLabel lab = new JLabel(l, SwingConstants.CENTER);
            lab.setFont(new Font("Times New Roman", Font.BOLD, 13));
            form.add(lab);
        }
        for(int i=0;i<9;i++){
            fields[i] = new JTextField();
            fields[i].setFont(text);
            form.add(fields[i]);
        }

        JButton update = new JButton("Save Changes");
        update.setFont(b);
        update.setBackground(new Color(30,144,255));
        update.setForeground(Color.WHITE);
        update.setOpaque(true);
        update.setBorderPainted(false);

        JPanel btnRow = new JPanel();
        btnRow.setOpaque(false);
        btnRow.add(update);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south,BoxLayout.Y_AXIS));
        south.setOpaque(false);
        south.add(form);
        south.add(btnRow);

        panel.add(title,BorderLayout.NORTH);
        panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.add(south,BorderLayout.SOUTH);
        bg.add(panel);

        loadBuses();

        table.getSelectionModel().addListSelectionListener(e -> {
            if(e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if(row < 0) return;

            selectedBusId = Integer.parseInt(model.getValueAt(row,0).toString());
            for(int i=0;i<9;i++){
                fields[i].setText(String.valueOf(model.getValueAt(row,i+1)));
            }
        });

        update.addActionListener(e -> saveChanges());

        f.setVisible(true);
    }

    private void loadBuses(){
        model.setRowCount(0);

        try(Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM bus ORDER BY bus_id")){

            while(rs.next()){
                Time dep = rs.getTime("departure_time");

                model.addRow(new Object[]{
                        rs.getInt("bus_id"),
                        rs.getString("bus_name"),
                        rs.getString("bus_type") != null ? rs.getString("bus_type") : "Ordinary",
                        rs.getString("source"),
                        rs.getString("destination"),
                        dep != null ? dep.toLocalTime().toString() : "",   // HH:MM
                        rs.getInt("journey_hours"),
                        rs.getInt("total_seats"),
                        rs.getDouble("price"),
                        rs.getString("run_days") != null ? rs.getString("run_days") : "DAILY"
                });
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Could not load buses: " + ex.getMessage());
        }
    }

    private void saveChanges(){

        if(selectedBusId < 0){
            JOptionPane.showMessageDialog(f,"Select a bus from the table first");
            return;
        }

        String name = fields[0].getText().trim();
        String type = fields[1].getText().trim();
        String source = fields[2].getText().trim();
        String dest = fields[3].getText().trim();
        String time = fields[4].getText().trim();

        if(name.isEmpty() || source.isEmpty() || dest.isEmpty()){
            JOptionPane.showMessageDialog(f,"Bus name, source and destination are required");
            return;
        }

        if(type.isEmpty()) type = "Ordinary";

        if(source.equalsIgnoreCase(dest)){
            JOptionPane.showMessageDialog(f,"Source and destination cannot be the same");
            return;
        }

        if(time.matches("\\d{1,2}:\\d{2}")) time = time + ":00";

        Time dep;
        try{
            dep = Time.valueOf(time);
        }catch(IllegalArgumentException ex){
            JOptionPane.showMessageDialog(f,"Invalid time. Use HH:MM (24-hour), e.g. 14:30");
            return;
        }

        int hours, seats;
        double price;

        try{
            hours = Integer.parseInt(fields[5].getText().trim());
            seats = Integer.parseInt(fields[6].getText().trim());
            price = Double.parseDouble(fields[7].getText().trim());
        }catch(NumberFormatException ex){
            JOptionPane.showMessageDialog(f,"Hours, seats and price must be numbers");
            return;
        }

        if(hours <= 0 || seats <= 0 || price <= 0){
            JOptionPane.showMessageDialog(f,"Hours, seats and price must be greater than 0");
            return;
        }

        String runDays = RunDays.normalize(fields[8].getText().trim());

        try(Connection con = DBConnection.getConnection()){

            // do not allow shrinking below a seat that is already booked on a future date
            try(PreparedStatement chk = con.prepareStatement(
                    "SELECT COALESCE(MAX(seat_number),0) FROM booking WHERE bus_id=? AND journey_date>=CURDATE()")){
                chk.setInt(1,selectedBusId);
                try(ResultSet rs = chk.executeQuery()){
                    if(rs.next() && seats < rs.getInt(1)){
                        JOptionPane.showMessageDialog(f,
                            "Cannot set total seats to " + seats + ": seat number " + rs.getInt(1) +
                            " is already booked on an upcoming date.\nMinimum allowed: " + rs.getInt(1));
                        return;
                    }
                }
            }

            try(PreparedStatement ps = con.prepareStatement(
                    "UPDATE bus SET bus_name=?,bus_type=?,source=?,destination=?,departure_time=?," +
                    "journey_hours=?,total_seats=?,price=?,run_days=? WHERE bus_id=?")){

                ps.setString(1,name);
                ps.setString(2,type);
                ps.setString(3,source);
                ps.setString(4,dest);
                ps.setTime(5,dep);
                ps.setInt(6,hours);
                ps.setInt(7,seats);
                ps.setDouble(8,price);
                ps.setString(9,runDays);
                ps.setInt(10,selectedBusId);

                if(ps.executeUpdate() == 0){
                    JOptionPane.showMessageDialog(f,"Bus no longer exists");
                    return;
                }
            }

            JOptionPane.showMessageDialog(f,"Bus " + selectedBusId + " updated successfully");

            int keep = selectedBusId;
            loadBuses();
            for(int i=0;i<model.getRowCount();i++){
                if(Integer.parseInt(model.getValueAt(i,0).toString()) == keep){
                    table.setRowSelectionInterval(i,i);
                    break;
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(f,"Update failed: " + ex.getMessage());
        }
    }
}
