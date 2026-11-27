package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

/** Small calendar date picker (text field + pop-up month grid) limited to a date range. */
public class DatePicker extends JPanel {

    private static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    private final LocalDate min, max;
    private LocalDate selected;
    private final JTextField field = new JTextField(12);
    private final List<Runnable> listeners = new ArrayList<>();

    public DatePicker(LocalDate min, LocalDate max){
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        this.min = min;
        this.max = max;
        this.selected = min;
        setOpaque(false);

        field.setEditable(false);
        field.setFont(new Font("Times New Roman", Font.PLAIN, 16));
        JButton pick = new JButton("Pick");
        pick.setFont(new Font("Times New Roman", Font.PLAIN, 14));

        add(field);
        add(pick);

        pick.addActionListener(e -> showPopup());
        field.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){ showPopup(); }
        });
        refresh();
    }

    public LocalDate getDate(){ return selected; }

    /** ISO format yyyy-MM-dd, ready for java.sql.Date.valueOf(). */
    public String getDateString(){ return selected.toString(); }

    public void addChangeListener(Runnable r){ listeners.add(r); }

    private void refresh(){ field.setText(selected.format(SHOW)); }

    private void showPopup(){
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog d = new JDialog(owner, "Select journey date", Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout(6, 6));

        final YearMonth[] ym = { YearMonth.from(selected) };

        JLabel monthLbl = new JLabel("", SwingConstants.CENTER);
        monthLbl.setFont(new Font("Times New Roman", Font.BOLD, 16));
        JButton prev = new JButton("<");
        JButton next = new JButton(">");

        JPanel head = new JPanel(new BorderLayout());
        head.add(prev, BorderLayout.WEST);
        head.add(monthLbl, BorderLayout.CENTER);
        head.add(next, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        grid.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        d.add(head, BorderLayout.NORTH);
        d.add(grid, BorderLayout.CENTER);

        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            grid.removeAll();
            monthLbl.setText(ym[0].getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym[0].getYear());

            for(DayOfWeek dw : DayOfWeek.values()){
                JLabel l = new JLabel(dw.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
                l.setFont(new Font("Times New Roman", Font.BOLD, 13));
                grid.add(l);
            }

            int offset = ym[0].atDay(1).getDayOfWeek().getValue() - 1;
            for(int i = 0; i < offset; i++) grid.add(new JLabel(""));

            for(int day = 1; day <= ym[0].lengthOfMonth(); day++){
                LocalDate date = ym[0].atDay(day);
                JButton b = new JButton(String.valueOf(day));
                b.setMargin(new Insets(4, 4, 4, 4));
                b.setEnabled(!date.isBefore(min) && !date.isAfter(max));
                if(date.equals(selected)){
                    b.setBackground(new Color(30, 144, 255));
                    b.setForeground(Color.WHITE);
                }
                b.addActionListener(ev -> {
                    selected = date;
                    d.dispose();
                    refresh();
                    for(Runnable r : listeners) r.run();
                });
                grid.add(b);
            }

            prev.setEnabled(ym[0].isAfter(YearMonth.from(min)));
            next.setEnabled(ym[0].isBefore(YearMonth.from(max)));
            grid.revalidate();
            grid.repaint();
            d.pack();
        };

        prev.addActionListener(e -> { ym[0] = ym[0].minusMonths(1); render[0].run(); });
        next.addActionListener(e -> { ym[0] = ym[0].plusMonths(1); render[0].run(); });

        render[0].run();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
}