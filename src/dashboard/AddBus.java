package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import util.RunDays;

public class AddBus {

    public AddBus(){

        JFrame f = new JFrame("Add Bus");
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

        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(540,700));
        panel.setBackground(new Color(255,255,255,220));

        Font h = new Font("Times New Roman",Font.BOLD,24);
        Font l = new Font("Times New Roman",Font.PLAIN,18);
        Font fnt = new Font("Times New Roman",Font.PLAIN,16);
        Font b = new Font("Times New Roman",Font.BOLD,16);

        JLabel title = new JLabel("Add New Bus", SwingConstants.CENTER);
        title.setFont(h);
        title.setBounds(140,20,260,30);

        String labels[]={
                "Bus Name",
                "Bus Type",
                "Source",
                "Destination",
                "Departure (HH:MM)",
                "Journey Hours",
                "Total Seats",
                "Price (₹)",
                "Runs On (e.g. DAILY)"
        };

        JTextField nameField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{
                "Ordinary", "Express", "Deluxe", "Volvo AC",
                "AC Seater", "AC Sleeper", "Semi-Sleeper", "Super Luxury"
        });
        JTextField srcField = new JTextField();
        JTextField destField = new JTextField();
        JTextField timeField = new JTextField();
        JTextField hoursField = new JTextField();
        JTextField seatsField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField runDaysField = new JTextField("DAILY");

        JComponent components[] = {
                nameField, typeBox, srcField, destField,
                timeField, hoursField, seatsField, priceField, runDaysField
        };

        int y = 68;
        for(int i = 0; i < labels.length; i++){
            JLabel lab = new JLabel(labels[i]);
            lab.setFont(l);
            lab.setBounds(40, y, 220, 30);

            components[i].setFont(fnt);
            components[i].setBounds(270, y, 220, 30);

            panel.add(lab);
            panel.add(components[i]);
            y += 56;
        }

        JButton add = new JButton("Add Bus");
        add.setFont(b);
        add.setBackground(new Color(30,144,255));
        add.setForeground(Color.WHITE);
        add.setBounds(190, y + 14, 160, 40);

        panel.add(title);
        panel.add(add);
        bg.add(panel);

        add.addActionListener(e->{

            String name = nameField.getText().trim();
            String type = (String) typeBox.getSelectedItem();
            String source = srcField.getText().trim();
            String dest = destField.getText().trim();
            String time = timeField.getText().trim();
            String runDays = RunDays.normalize(runDaysField.getText().trim());

            if(name.isEmpty() || source.isEmpty() || dest.isEmpty()){
                JOptionPane.showMessageDialog(f,"Bus name, source and destination are required");
                return;
            }

            if(source.equalsIgnoreCase(dest)){
                JOptionPane.showMessageDialog(f,"Source and destination cannot be the same");
                return;
            }

            // accept HH:MM or HH:MM:SS
            if(time.matches("\\d{1,2}:\\d{2}")) time = time + ":00";

            Time dep;
            int hours, seats;
            double price;

            try{
                dep = Time.valueOf(time);
            }catch(IllegalArgumentException ex){
                JOptionPane.showMessageDialog(f,"Invalid time. Use HH:MM (24-hour), e.g. 14:30");
                return;
            }

            try{
                hours = Integer.parseInt(hoursField.getText().trim());
                seats = Integer.parseInt(seatsField.getText().trim());
                price = Double.parseDouble(priceField.getText().trim());
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(f,"Journey hours, seats and price must be numbers");
                return;
            }

            if(hours <= 0 || seats <= 0 || price <= 0){
                JOptionPane.showMessageDialog(f,"Hours, seats and price must be greater than 0");
                return;
            }

            try(Connection con=DBConnection.getConnection();
                PreparedStatement ps=con.prepareStatement(
                    "INSERT INTO bus(bus_name,source,destination,departure_time,journey_hours,total_seats,booked_seats,price,bus_type,run_days) " +
                    "VALUES(?,?,?,?,?,?,0,?,?,?)")){

                ps.setString(1,name);
                ps.setString(2,source);
                ps.setString(3,dest);
                ps.setTime(4,dep);
                ps.setInt(5,hours);
                ps.setInt(6,seats);
                ps.setDouble(7,price);
                ps.setString(8,type != null ? type : "Ordinary");
                ps.setString(9,runDays);

                ps.executeUpdate();

                JOptionPane.showMessageDialog(f,"Bus Added Successfully");
                f.dispose();

            }catch(Exception ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(f,"Database error: " + ex.getMessage());
            }
        });

        f.setVisible(true);
    }
}
