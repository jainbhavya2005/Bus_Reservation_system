package bus;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class SeatSelectorDialog extends JDialog {

    private final List<Integer> selectedSeats = new ArrayList<>();
    private final int requiredSeats;
    private boolean confirmed = false;

    public SeatSelectorDialog(
            JFrame parent,
            String busName,
            int totalSeats,
            int requiredSeats,
            double farePerSeat,
            Set<Integer> bookedSeatNumbers
    ) {
        super(parent, "Select " + requiredSeats + " Seat(s) - " + busName, true);
        this.requiredSeats = Math.max(1, requiredSeats);
        setSize(540, 460);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JLabel header = new JLabel(
                "Select Exactly " + this.requiredSeats + " Seat(s) - " + busName,
                SwingConstants.CENTER
        );
        header.setFont(new Font("Times New Roman", Font.BOLD, 20));
        add(header, BorderLayout.NORTH);

        int cols = 5;
        int rows = (int) Math.ceil(totalSeats / 4.0);
        JPanel grid = new JPanel(new GridLayout(rows, cols, 8, 8));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel statusLabel = new JLabel(
                "Selected (0/" + this.requiredSeats + "): None | Total Fare: ₹0.0",
                SwingConstants.CENTER
        );
        statusLabel.setFont(new Font("Times New Roman", Font.PLAIN, 16));

        int seatNo = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c == 2) {
                    grid.add(new JLabel("", SwingConstants.CENTER));
                    continue;
                }
                if (seatNo > totalSeats) {
                    grid.add(new JLabel(""));
                    continue;
                }

                int currentSeat = seatNo++;
                JToggleButton btn = new JToggleButton("S" + currentSeat);
                btn.setFont(new Font("Times New Roman", Font.BOLD, 13));
                btn.setFocusPainted(false);

                if (bookedSeatNumbers.contains(currentSeat)) {
                    btn.setEnabled(false);
                    btn.setBackground(new Color(220, 80, 80));
                    btn.setForeground(Color.WHITE);
                    btn.setToolTipText("Already Booked");
                } else {
                    btn.setBackground(new Color(220, 252, 231));
                    btn.addActionListener(e -> {
                        if (btn.isSelected()) {
                            if (selectedSeats.size() >= this.requiredSeats) {
                                btn.setSelected(false);
                                JOptionPane.showMessageDialog(
                                        this,
                                        "You can only select " + this.requiredSeats + " seat(s)."
                                );
                                return;
                            }
                            selectedSeats.add(currentSeat);
                            btn.setBackground(new Color(37, 99, 235));
                            btn.setForeground(Color.WHITE);
                        } else {
                            selectedSeats.remove(Integer.valueOf(currentSeat));
                            btn.setBackground(new Color(220, 252, 231));
                            btn.setForeground(Color.BLACK);
                        }
                        Collections.sort(selectedSeats);
                        double totalFare = selectedSeats.size() * farePerSeat;
                        String seatsText = selectedSeats.isEmpty()
                                ? "None"
                                : selectedSeats.stream().map(n -> "S" + n).reduce((a, b) -> a + ", " + b).orElse("None");
                        statusLabel.setText(
                                "Selected (" + selectedSeats.size() + "/" + this.requiredSeats + "): "
                                        + seatsText + " | Total Fare: ₹" + totalFare
                        );
                    });
                }

                grid.add(btn);
            }
        }

        add(new JScrollPane(grid), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 15, 12, 15));
        bottom.add(statusLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        JButton confirmBtn = new JButton("Confirm Seats");
        JButton cancelBtn = new JButton("Cancel");

        confirmBtn.addActionListener(e -> {
            if (selectedSeats.size() != this.requiredSeats) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select exactly " + this.requiredSeats + " seat(s). Currently selected: " + selectedSeats.size()
                );
                return;
            }
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            selectedSeats.clear();
            confirmed = false;
            dispose();
        });

        buttons.add(confirmBtn);
        buttons.add(cancelBtn);
        bottom.add(buttons, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<Integer> getSelectedSeats() {
        return Collections.unmodifiableList(selectedSeats);
    }
}