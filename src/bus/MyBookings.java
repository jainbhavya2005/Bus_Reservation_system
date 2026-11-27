package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class MyBookings extends JFrame {

    private static final DateTimeFormatter BOOKED_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static class GroupedBooking {
        final String busName;
        final String route;
        final String journeyDate;
        final String bookedAtDisplay;
        final String paymentMethod;
        final List<Integer> seats = new ArrayList<>();
        final List<Integer> bookingIds = new ArrayList<>();
        final java.util.LinkedHashSet<String> passengerNames = new java.util.LinkedHashSet<>();

        GroupedBooking(
                String busName,
                String route,
                String journeyDate,
                String bookedAtDisplay,
                String paymentMethod
        ) {
            this.busName = busName;
            this.route = route;
            this.journeyDate = journeyDate;
            this.bookedAtDisplay = bookedAtDisplay;
            this.paymentMethod = paymentMethod;
        }
    }

    private final List<GroupedBooking> rowGroups = new ArrayList<>();

    public MyBookings(String username) {
        setTitle("My Bookings - " + username);
        setSize(920, 540);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 6, 18));
        topPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("My Bookings — " + username, SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(20, 33, 61));

        JLabel subtitle = new JLabel("Multi-seat bookings are grouped by bus, journey date, and booking time");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 116, 139));

        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(subtitle, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {
                "Booking IDs",
                "Bus",
                "Route",
                "Seats",
                "Journey Date",
                "Booked At",
                "Payment"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setPreferredWidth(165);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        loadGroupedBookings(username, model);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottomPanel.setBackground(Color.WHITE);

        JButton againBtn = new JButton("Book Again");
        JButton returnBtn = new JButton("Return Trip");
        againBtn.addActionListener(e -> repeatBooking(username, table, false));
        returnBtn.addActionListener(e -> repeatBooking(username, table, true));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            model.setRowCount(0);
            loadGroupedBookings(username, model);
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        bottomPanel.add(againBtn);
        bottomPanel.add(returnBtn);
        bottomPanel.add(refreshBtn);
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadGroupedBookings(String username, DefaultTableModel model) {
        rowGroups.clear();
        boolean hasBookedAt = columnExists("booking", "booked_at");
        boolean hasPayment = columnExists("booking", "payment_method");

        String bookedExpr = hasBookedAt ? "bk.booked_at" : "bk.booking_date";
        String paymentExpr = hasPayment ? "COALESCE(bk.payment_method, '-')" : "'-'";

        String sql =
                "SELECT bk.booking_id, bk.bus_id, bk.seat_number, bk.journey_date, bk.passenger_name, "
                        + bookedExpr + " AS booked_ts, " + paymentExpr + " AS pay_method, "
                        + "b.bus_name, b.source, b.destination "
                        + "FROM booking bk "
                        + "LEFT JOIN bus b ON bk.bus_id = b.bus_id "
                        + "WHERE bk.username = ? "
                        + "ORDER BY booked_ts DESC, bk.bus_id ASC, bk.journey_date DESC, bk.seat_number ASC";

        Map<String, GroupedBooking> grouped = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "Database connection failed.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int bookingId = rs.getInt("booking_id");
                        int busId = rs.getInt("bus_id");
                        int seatNumber = rs.getInt("seat_number");

                        String rawBusName = rs.getString("bus_name");
                        final String displayBusName = (rawBusName == null || rawBusName.isBlank())
                                ? "Bus #" + busId
                                : rawBusName + " (#" + busId + ")";

                        String src = rs.getString("source");
                        String dst = rs.getString("destination");
                        final String displayRoute = (src != null && dst != null && !src.isBlank() && !dst.isBlank())
                                ? src + " → " + dst
                                : "-";

                        java.sql.Date jDate = rs.getDate("journey_date");
                        final String journeyDateStr = (jDate != null) ? jDate.toString() : "-";

                        Timestamp ts = rs.getTimestamp("booked_ts");
                        final String bookedDisplay = formatTimestamp(ts, hasBookedAt);

                        String rawPayment = rs.getString("pay_method");
                        final String displayPayment = (rawPayment == null || rawPayment.isBlank())
                                ? "-"
                                : rawPayment;

                        String bucket = (ts != null) ? String.valueOf(ts.getTime() / 1000) : bookedDisplay;
                        String key = busId + "|" + journeyDateStr + "|" + bucket + "|" + displayPayment;

                        GroupedBooking gb = grouped.computeIfAbsent(
                                key,
                                k -> new GroupedBooking(
                                        displayBusName,
                                        displayRoute,
                                        journeyDateStr,
                                        bookedDisplay,
                                        displayPayment
                                )
                        );
                        gb.bookingIds.add(bookingId);
                        gb.seats.add(seatNumber);
                        String pName = rs.getString("passenger_name");
                        if (pName != null && !pName.isBlank()) gb.passengerNames.add(pName);
                    }
                }
            }

            for (GroupedBooking gb : grouped.values()) {
                String idsText = gb.bookingIds.size() == 1
                        ? String.valueOf(gb.bookingIds.get(0))
                        : gb.bookingIds.get(0) + "–" + gb.bookingIds.get(gb.bookingIds.size() - 1);

                String seatsText = gb.seats.stream()
                        .map(s -> "S" + s)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-") + " (" + gb.seats.size() + ")";

                rowGroups.add(gb);
                model.addRow(new Object[]{
                        idsText,
                        gb.busName,
                        gb.route,
                        seatsText,
                        gb.journeyDate,
                        gb.bookedAtDisplay,
                        gb.paymentMethod
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage());
        }
    }

    /** Opens the search screen with the route (and passenger names) of the selected booking pre-filled. */
    private void repeatBooking(String username, JTable table, boolean reverse) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rowGroups.size()) {
            JOptionPane.showMessageDialog(this, "Select a booking first");
            return;
        }
        GroupedBooking gb = rowGroups.get(row);
        String[] parts = gb.route.split(" \u2192 ");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(this, "Route details are not available for this booking");
            return;
        }
        String from = reverse ? parts[1].trim() : parts[0].trim();
        String to = reverse ? parts[0].trim() : parts[1].trim();
        new SearchBusFinal(username, from, to, new ArrayList<>(gb.passengerNames));
    }

    private String formatTimestamp(Timestamp ts, boolean hasExactTime) {
        if (ts == null) {
            return "-";
        }
        LocalDateTime ldt = ts.toLocalDateTime();
        if (!hasExactTime) {
            return ldt.toLocalDate().toString() + " (Date only)";
        }
        return ldt.format(BOOKED_FMT);
    }

    private boolean columnExists(String tableName, String columnName) {
    String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, tableName);
        ps.setString(2, columnName);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    } catch (Exception ignored) {   // was SQLException; also covers a null connection
        return false;
    }
}
}