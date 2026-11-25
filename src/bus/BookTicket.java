package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

public class BookTicket {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    // Old signature: asks for the date with a dialog
    public BookTicket(String username, String busId, String passenger, int ticketCount){
        this(username, busId, passenger, ticketCount, null);
    }

    public BookTicket(String username, String busId, String passenger, int ticketCount, String journeyDateStr){

        if(passenger == null || passenger.trim().isEmpty()){
            JOptionPane.showMessageDialog(null,"Passenger name required");
            return;
        }
        String defaultPassenger = passenger.trim();

        if(ticketCount <= 0){
            JOptionPane.showMessageDialog(null,"Invalid ticket count");
            return;
        }

        int id;
        try{
            id = Integer.parseInt(busId);
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null,"Invalid bus");
            return;
        }

        // ===== DATE SELECTION (only if not supplied) =====
        if(journeyDateStr == null){
            String[] dates = new String[7];
            LocalDate today = LocalDate.now();
            for(int i=0;i<7;i++) dates[i] = today.plusDays(i).toString();

            journeyDateStr = (String) JOptionPane.showInputDialog(
                    null,"Select Journey Date:","Select Date",
                    JOptionPane.QUESTION_MESSAGE,null,dates,dates[0]);

            if(journeyDateStr == null) return;
        }

        Date journeyDate = Date.valueOf(journeyDateStr);

        String busName = "Bus #" + busId;
        String source = "Source";
        String destination = "Destination";
        int totalSeats = 0;
        Time departureTime = null;
        int hours = 0;
        double price = 0;
        Set<Integer> bookedSeats;

        // ===== READ BUS + ALREADY BOOKED SEATS FOR DATE =====
        try(Connection con = DBConnection.getConnection()){

            if(con == null){
                JOptionPane.showMessageDialog(null,"Database connection failed");
                return;
            }

            try(PreparedStatement ps = con.prepareStatement(
                    "SELECT bus_name,source,destination,total_seats,departure_time,journey_hours,price FROM bus WHERE bus_id=?")){
                ps.setInt(1,id);
                try(ResultSet rs = ps.executeQuery()){
                    if(!rs.next()){
                        JOptionPane.showMessageDialog(null,"Bus not found");
                        return;
                    }
                    if(rs.getString("bus_name") != null && !rs.getString("bus_name").isBlank()){
                        busName = rs.getString("bus_name");
                    }
                    if(rs.getString("source") != null && !rs.getString("source").isBlank()){
                        source = rs.getString("source");
                    }
                    if(rs.getString("destination") != null && !rs.getString("destination").isBlank()){
                        destination = rs.getString("destination");
                    }
                    totalSeats = rs.getInt("total_seats");
                    departureTime = rs.getTime("departure_time");
                    hours = rs.getInt("journey_hours");
                    price = rs.getDouble("price");
                }
            }

            bookedSeats = bookedSeatNumbers(con, id, journeyDate);
            int available = totalSeats - bookedSeats.size();

            if(available <= 0){
                JOptionPane.showMessageDialog(null,"Bus is fully booked for " + journeyDateStr);
                return;
            }

        }catch(SQLException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"Booking Error : " + e.getMessage());
            return;
        }

        // ===== INTERACTIVE SEAT SELECTION MAP =====
      SeatSelectorDialog seatDialog = new SeatSelectorDialog(
        null,
        busName,
        totalSeats,
        ticketCount,
        price,
        bookedSeats
);
seatDialog.setVisible(true);

if(!seatDialog.isConfirmed() || seatDialog.getSelectedSeats().size() != ticketCount){
    return;
}

        List<Integer> seatList = new ArrayList<>(seatDialog.getSelectedSeats());

        // ===== COLLECT PER-PASSENGER DETAILS & CALCULATE DISCOUNTED FARES =====
        List<Passenger> passengers = TicketService.collectPassengers(seatList, price);
        if(passengers == null || passengers.isEmpty()){
            return;
        }

        for(int i = 0; i < passengers.size(); i++){
            Passenger p = passengers.get(i);
            if(p.name == null || p.name.isBlank()){
                p.name = (i == 0) ? defaultPassenger : "Passenger S" + p.seat;
            }
            p.fare = Fare.apply(price, p.age, p.student);
        }

        double totalAmount = TicketService.totalFare(passengers);

        // ===== PAYMENT =====
        String options[] = {"UPI","Credit Card","Debit Card"};

        String paymentMethod = (String) JOptionPane.showInputDialog(
                null,"Select Payment Method","Payment",
                JOptionPane.QUESTION_MESSAGE,null,options,options[0]);

        if(paymentMethod == null) return;

        if(!processPayment(paymentMethod,totalAmount)){
            JOptionPane.showMessageDialog(null,"Payment cancelled / failed");
            return;
        }

        // ===== BOOK SELECTED SEATS (single transaction, bus row locked) =====
        try(Connection con = DBConnection.getConnection()){

            if(con == null){
                JOptionPane.showMessageDialog(null,"Database connection failed");
                return;
            }

            con.setAutoCommit(false);

            try{
                try(PreparedStatement lock = con.prepareStatement(
                        "SELECT bus_id FROM bus WHERE bus_id=? FOR UPDATE")){
                    lock.setInt(1,id);
                    lock.executeQuery().close();
                }

                Set<Integer> taken = bookedSeatNumbers(con,id,journeyDate);

                for(Passenger p : passengers){
                    if(taken.contains(p.seat)){
                        con.rollback();
                        JOptionPane.showMessageDialog(null,
                                "Seat S" + p.seat + " was just booked by someone else. Please select seats again.");
                        return;
                    }
                }

                try(PreparedStatement insert = con.prepareStatement(
                        "INSERT INTO booking(username,bus_id,passenger_name,seat_number,journey_date,payment_method) VALUES(?,?,?,?,?,?)")){

                    for(Passenger p : passengers){
                        insert.setString(1,username);
                        insert.setInt(2,id);
                        insert.setString(3,p.name);
                        insert.setInt(4,p.seat);
                        insert.setDate(5,journeyDate);
                        insert.setString(6,paymentMethod);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }

                con.commit();

            }catch(SQLException ex){
                con.rollback();
                throw ex;
            }

        }catch(SQLException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"Booking Error : " + e.getMessage());
            return;
        }

        // ===== TIMES =====
        String depStr = "Not Set";
        String arrStr = "Not Set";

        if(departureTime != null){
            LocalTime d = departureTime.toLocalTime();
            int daysLater = (d.getHour() + hours) / 24;
            depStr = d.format(TIME_FMT);
            arrStr = d.plusHours(hours).format(TIME_FMT)
                    + (daysLater > 0 ? " (+" + daysLater + "d)" : "");
        }

        String passengerSummary = passengers.stream()
                .map(p -> p.name + " (S" + p.seat + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse(defaultPassenger);

        showConfirmation(busId,passengerSummary,seatList,journeyDateStr,depStr,arrStr,paymentMethod,totalAmount);

        String recipientEmail = JOptionPane.showInputDialog(
                null,
                "Enter Email to receive PDF E-Ticket (optional):",
                "Send E-Ticket",
                JOptionPane.QUESTION_MESSAGE
        );

        TicketService.generatePdfAndEmail(
                username,
                recipientEmail,
                busName,
                source,
                destination,
                journeyDateStr,
                depStr,
                arrStr,
                paymentMethod,
                passengers
        );
    }

    private Set<Integer> bookedSeatNumbers(Connection con,int busId,Date date) throws SQLException {
        Set<Integer> taken = new HashSet<>();
        try(PreparedStatement ps = con.prepareStatement(
                "SELECT seat_number FROM booking WHERE bus_id=? AND journey_date=?")){
            ps.setInt(1,busId);
            ps.setDate(2,date);
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()) taken.add(rs.getInt(1));
            }
        }
        return taken;
    }

    private void showConfirmation(String busId,String passenger,List<Integer> seats,String date,
                                  String dep,String arr,String payment,double total){

        JFrame confirm = new JFrame("Booking Confirmation");
        confirm.setSize(520,520);
        confirm.setLocationRelativeTo(null);
        confirm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(null);
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Booking Confirmation");
        title.setFont(new Font("Times New Roman",Font.BOLD,24));
        title.setBounds(140,20,300,30);

        JLabel labels[] = {
                new JLabel("Bus ID : " + busId),
                new JLabel("Passenger : " + passenger),
                new JLabel("Seats : " + seats),
                new JLabel("Journey Date : " + date),
                new JLabel("Departure : " + dep),
                new JLabel("Arrival : " + arr),
                new JLabel("Payment : " + payment),
                new JLabel("Total Amount : ₹ " + String.format("%.2f",total))
        };

        int y = 80;
        for(JLabel lbl : labels){
            lbl.setFont(new Font("Times New Roman",Font.PLAIN,18));
            lbl.setBounds(90,y,350,30);
            panel.add(lbl);
            y += 40;
        }

        JButton close = new JButton("Close");
        close.setBounds(200,y+20,120,40);
        close.setBackground(new Color(30,144,255));
        close.setForeground(Color.WHITE);
        close.addActionListener(e -> confirm.dispose());

        panel.add(title);
        panel.add(close);

        confirm.add(panel);
        confirm.setVisible(true);
    }

    // ===== PAYMENT WINDOW (simulated) =====
    private boolean processPayment(String method,double amount){

        JDialog payDialog = new JDialog((Frame)null,"Online Payment - "+method,true);
        payDialog.setSize(400,380);
        payDialog.setLocationRelativeTo(null);
        payDialog.setLayout(null);

        JLabel title = new JLabel(method+" Payment");
        title.setFont(new Font("Times New Roman",Font.BOLD,22));
        title.setBounds(120,20,250,30);

        JLabel amountLabel = new JLabel("Amount : ₹ " + String.format("%.2f",amount));
        amountLabel.setFont(new Font("Times New Roman",Font.PLAIN,18));
        amountLabel.setBounds(120,60,250,30);

        boolean upi = method.equals("UPI");

        JLabel label = new JLabel(upi ? "UPI ID" : "Card No.");
        label.setBounds(60,120,100,30);

        JTextField field1 = new JTextField();
        field1.setBounds(160,120,160,30);

        JButton payBtn = new JButton("Pay Now");
        payBtn.setBounds(140,200,120,40);
        payBtn.setBackground(new Color(30,144,255));
        payBtn.setForeground(Color.WHITE);

        final boolean success[] = {false};

        payBtn.addActionListener(e -> {
            String v = field1.getText().trim();

            if(upi){
                if(!v.matches("[\\w.\\-]{2,}@[\\w]{2,}")){
                    JOptionPane.showMessageDialog(payDialog,"Enter a valid UPI ID (e.g. name@bank)");
                    return;
                }
            }else{
                if(!v.replace(" ","").matches("\\d{16}")){
                    JOptionPane.showMessageDialog(payDialog,"Enter a valid 16-digit card number");
                    return;
                }
            }

            JOptionPane.showMessageDialog(payDialog,"Payment Successful");
            success[0] = true;
            payDialog.dispose();
        });

        payDialog.add(title);
        payDialog.add(amountLabel);
        payDialog.add(label);
        payDialog.add(field1);
        payDialog.add(payBtn);

        payDialog.setVisible(true);

        return success[0];
    }
}