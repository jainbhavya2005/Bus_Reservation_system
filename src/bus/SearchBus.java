package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class SearchBus {

    private JComboBox<String> fromBox;
    private JComboBox<String> toBox;
    private JComboBox<String> busResult;

    public SearchBus(String username){

        JFrame f = new JFrame("Search Bus");
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);

        ImageIcon bg = new ImageIcon("images/busbg.jpg");
        JLabel background = new JLabel(bg);
        background.setLayout(null);
        f.setContentPane(background);

        JPanel overlay = new JPanel();
        overlay.setLayout(null);
        overlay.setBackground(new Color(0,0,0,170));
        overlay.setBounds(350,120,750,600);
        background.add(overlay);

        JLabel title = new JLabel("Search Buses");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI",Font.BOLD,30));
        title.setBounds(250,40,300,40);
        overlay.add(title);

        // FROM
        JLabel fromLabel = new JLabel("From");
        fromLabel.setForeground(Color.WHITE);
        fromLabel.setBounds(150,140,100,30);
        overlay.add(fromLabel);

        fromBox = new JComboBox<>();
        fromBox.setBounds(150,180,200,40);
        overlay.add(fromBox);

        // TO
        JLabel toLabel = new JLabel("To");
        toLabel.setForeground(Color.WHITE);
        toLabel.setBounds(400,140,100,30);
        overlay.add(toLabel);

        toBox = new JComboBox<>();
        toBox.setBounds(400,180,200,40);
        overlay.add(toBox);

        busResult = new JComboBox<>();
        busResult.setBounds(150,320,450,40);
        overlay.add(busResult);

        JButton searchBtn = new JButton("Search Bus");
        searchBtn.setBounds(190,240,170,45);
        searchBtn.setBackground(new Color(0,120,215));
        searchBtn.setForeground(Color.WHITE);
        overlay.add(searchBtn);

        JButton showAllBtn = new JButton("Show All");
        showAllBtn.setBounds(380,240,170,45);
        showAllBtn.setBackground(new Color(100,116,139));
        showAllBtn.setForeground(Color.WHITE);
        overlay.add(showAllBtn);

        JButton bookBtn = new JButton("Book Bus");
        bookBtn.setBounds(280,380,200,45);
        bookBtn.setBackground(new Color(0,180,100));
        bookBtn.setForeground(Color.WHITE);
        overlay.add(bookBtn);

        // ===== LOAD CITIES & ALL AVAILABLE BUSES IMMEDIATELY =====
        loadCities();
        loadBuses(null, null);

        f.setVisible(true);

        // ===== FILTERED SEARCH =====
        searchBtn.addActionListener(e -> {
            Object fromItem = fromBox.getSelectedItem();
            Object toItem = toBox.getSelectedItem();
            loadBuses(
                fromItem != null ? fromItem.toString() : null,
                toItem != null ? toItem.toString() : null
            );
        });

        // ===== SHOW ALL BUSES =====
        showAllBtn.addActionListener(e -> loadBuses(null, null));

        // ===== BOOK =====
        bookBtn.addActionListener(e -> {
            if(busResult.getSelectedItem() == null) return;

            String selected = busResult.getSelectedItem().toString();
            String busId = selected.split("\\|")[0].trim();

            new BookTicket(username, busId, username, 1);
            f.dispose();
        });
    }

    private void loadCities(){
        try{
            Connection con = DBConnection.getConnection();

            ResultSet rs1 = con.createStatement()
                .executeQuery("SELECT DISTINCT source FROM bus");
            while(rs1.next()){
                fromBox.addItem(rs1.getString(1));
            }

            ResultSet rs2 = con.createStatement()
                .executeQuery("SELECT DISTINCT destination FROM bus");
            while(rs2.next()){
                toBox.addItem(rs2.getString(1));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadBuses(String source, String destination){
        try{
            busResult.removeAllItems();
            Connection con = DBConnection.getConnection();
            PreparedStatement ps;

            if(source == null || destination == null || source.isBlank() || destination.isBlank()){
                ps = con.prepareStatement("SELECT * FROM bus ORDER BY bus_id");
            } else {
                ps = con.prepareStatement(
                    "SELECT * FROM bus WHERE source=? AND destination=? ORDER BY bus_id"
                );
                ps.setString(1, source);
                ps.setString(2, destination);
            }

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                int totalSeats = rs.getInt("total_seats");
                int bookedSeats = rs.getInt("booked_seats");
                int availableSeats = Math.max(0, totalSeats - bookedSeats);

                busResult.addItem(
                    rs.getInt("bus_id") + " | " +
                    rs.getString("bus_name") + " | " +
                    rs.getString("source") + " -> " +
                    rs.getString("destination") + " | " +
                    rs.getInt("journey_hours") + " hrs | " +
                    availableSeats + " seats"
                );
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}