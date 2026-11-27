package bus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.*;
import util.Mailer;
import util.TicketPdf;

public class TicketService {

    /** Names pre-filled in the passenger forms (used by "Book Again" / "Return Trip"). */
    public static List<String> presetNames = new ArrayList<>();

    public static List<Passenger> collectPassengers(List<Integer> selectedSeats, double baseFare) {
        List<Passenger> list = new ArrayList<>();

        for (int seat : selectedSeats) {
            String presetName = list.size() < presetNames.size() ? presetNames.get(list.size()) : "";
            JTextField nameField = new JTextField(presetName, 16);
            JTextField ageField = new JTextField("20", 5);
            String[] genders = {"Male", "Female", "Other"};
            JComboBox<String> genderBox = new JComboBox<>(genders);
            JCheckBox studentCheck = new JCheckBox("Student (13-59 yrs: 10% off)");

            JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
            panel.add(new JLabel("Seat Number:"));
            panel.add(new JLabel("S" + seat));
            panel.add(new JLabel("Passenger Name:"));
            panel.add(nameField);
            panel.add(new JLabel("Age:"));
            panel.add(ageField);
            panel.add(new JLabel("Gender:"));
            panel.add(genderBox);
            panel.add(new JLabel("Discount:"));
            panel.add(studentCheck);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Passenger Details - Seat S" + seat,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                name = "Passenger S" + seat;
            }

            int age = 20;
            try {
                age = Math.max(0, Integer.parseInt(ageField.getText().trim()));
            } catch (NumberFormatException ignored) {
            }

            boolean isStudent = studentCheck.isSelected();
            Passenger p = new Passenger(seat, name, age, (String) genderBox.getSelectedItem(), isStudent);
            p.fare = Fare.apply(baseFare, age, isStudent);
            list.add(p);
        }

        return list;
    }

    public static double totalFare(List<Passenger> passengers) {
        double sum = 0.0;
        for (Passenger p : passengers) {
            sum += p.fare;
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    public static void generatePdfAndEmail(
            String username,
            String recipientEmail,
            String busName,
            String source,
            String destination,
            String journeyDate,
            String departure,
            String arrival,
            String paymentMethod,
            List<Passenger> passengers
    ) {
        String pnr = "PNR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TicketPdf.Ticket ticket = new TicketPdf.Ticket();
        ticket.pnr = pnr;
        ticket.username = username;
        ticket.email = recipientEmail;
        ticket.busName = busName;
        ticket.source = source;
        ticket.destination = destination;
        ticket.journeyDate = journeyDate;
        ticket.departure = departure;
        ticket.arrival = arrival;
        ticket.payment = paymentMethod;

        for (Passenger p : passengers) {
            TicketPdf.Pax pax = new TicketPdf.Pax();
            pax.seat = p.seat;
            pax.name = p.name;
            pax.age = p.age;
            pax.gender = p.gender;
            pax.fare = p.fare;
            ticket.passengers.add(pax);
        }

        byte[] pdfBytes = TicketPdf.build(ticket);
        String fileName = "Ticket_" + pnr + ".pdf";

        try {
            Files.write(Path.of(fileName), pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not save PDF: " + e.getMessage());
        }

        if (recipientEmail != null && !recipientEmail.isBlank() && Mailer.isConfigured()) {
            String subject = "Bus E-Ticket (" + pnr + ") - " + source + " to " + destination;
            String body = "Hello " + username + ",\n\n"
                    + "Your booking is confirmed.\n"
                    + "PNR: " + pnr + "\n"
                    + "Bus: " + busName + " (" + source + " -> " + destination + ")\n"
                    + "Journey Date: " + journeyDate + "\n"
                    + "Departure: " + departure + " | Arrival: " + arrival + "\n"
                    + "Total Paid: Rs. " + String.format("%.2f", ticket.total()) + "\n\n"
                    + "Your PDF ticket with QR code is attached.";

            Mailer.sendAsync(recipientEmail, subject, body, fileName, pdfBytes, err -> {
                if (err == null || err.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "PDF saved as " + fileName + " and sent to " + recipientEmail);
                } else {
                    JOptionPane.showMessageDialog(null, "PDF saved as " + fileName + ", but email failed:\n" + err);
                }
            });
        } else {
            String reason = (recipientEmail == null || recipientEmail.isBlank())
                    ? "No email address entered."
                    : "Email not configured (check mail.properties or SMTP_USER/SMTP_PASS).";
            JOptionPane.showMessageDialog(null, "PDF e-ticket saved as " + fileName + "\n" + reason);
        }
    }
}