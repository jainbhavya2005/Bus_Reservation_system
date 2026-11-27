package bus;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import util.DatePicker;

public class SearchBusFinal {

    private JComboBox<String> sourceBox;
    private JComboBox<String> destBox;
    private DatePicker datePicker;
    private DefaultTableModel model;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private static final String[] SORT_OPTIONS = {
            "Bus ID", "Cheapest first", "Costliest first",
            "Earliest departure", "Latest departure", "Most seats left"
    };

    public SearchBusFinal(String username){
        this(username, null, null, null);
    }

    /** presetSource/presetDest/presetNames are used by "Book Again" and "Return Trip" in My Bookings. */
    public SearchBusFinal(String username, String presetSource, String presetDest, List<String> presetNames){

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
        panel.setPreferredSize(new Dimension(1150,620));
        panel.setBackground(new Color(255,255,255,220));

        Font heading = new Font("Times New Roman",Font.BOLD,26);
        Font text = new Font("Times New Roman",Font.PLAIN,16);

        JLabel title = new JLabel("Search Available Buses",SwingConstants.CENTER);
        title.setFont(heading);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));

        sourceBox = new JComboBox<>();
        destBox = new JComboBox<>();
        sourceBox.setFont(text);
        destBox.setFont(text);

        LocalDate today = LocalDate.now();
        datePicker = new DatePicker(today, today.plusDays(30));

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
        top.add(datePicker);
        top.add(searchBtn);
        top.add(showAllBtn);

        // ===== sort + filter row =====
        JComboBox<String> sortBox = new JComboBox<>(SORT_OPTIONS);
        sortBox.setFont(text);
        JComboBox<String> typeFilter = new JComboBox<>(new String[]{
                "All Types", "Ordinary", "Express", "Deluxe", "Volvo AC",
                "AC Seater", "AC Sleeper", "Semi-Sleeper", "Super Luxury"
        });
        typeFilter.setFont(text);
        JTextField maxFare = new JTextField(5);
        maxFare.setFont(text);
        JCheckBox availableOnly = new JCheckBox("Only buses with seats left");
        availableOnly.setFont(text);
        availableOnly.setOpaque(false);
        JButton resetBtn = new JButton("Reset filters");
        resetBtn.setFont(text);

        JLabel sortLbl = new JLabel("Sort by:");
        JLabel typeLbl = new JLabel("Type:");
        JLabel fareLbl = new JLabel("Max fare (Rs.):");
        sortLbl.setFont(text);
        typeLbl.setFont(text);
        fareLbl.setFont(text);

        JPanel filters = new JPanel();
        filters.add(sortLbl);
        filters.add(sortBox);
        filters.add(typeLbl);
        filters.add(typeFilter);
        filters.add(fareLbl);
        filters.add(maxFare);
        filters.add(availableOnly);
        filters.add(resetBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north,BoxLayout.Y_AXIS));
        north.setOpaque(false);
        north.add(title);
        north.add(top);
        north.add(filters);

        String cols[]={
                "Bus ID","Bus Name","Type","Source","Destination",
                "Departure","Arrival","Seats Available","Fare"
        };

        model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){
                return false;
            }
            public Class<?> getColumnClass(int c){
                if(c == 0 || c == 7) return Integer.class;
                if(c == 8) return Double.class;
                return String.class;
            }
        };

        JTable table = new JTable(model);
        table.setFont(text);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ===== sorting (click a header, or use the "Sort by" box) =====
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(5, (a, b) -> Long.compare(timeKey(a), timeKey(b)));
        sorter.setComparator(6, (a, b) -> Long.compare(timeKey(a), timeKey(b)));
        table.setRowSorter(sorter);

        sortBox.addActionListener(e -> {
            List<RowSorter.SortKey> keys = new ArrayList<>();
            switch(sortBox.getSelectedIndex()){
                case 1: keys.add(new RowSorter.SortKey(8, SortOrder.ASCENDING)); break;
                case 2: keys.add(new RowSorter.SortKey(8, SortOrder.DESCENDING)); break;
                case 3: keys.add(new RowSorter.SortKey(5, SortOrder.ASCENDING)); break;
                case 4: keys.add(new RowSorter.SortKey(5, SortOrder.DESCENDING)); break;
                case 5: keys.add(new RowSorter.SortKey(7, SortOrder.DESCENDING)); break;
                default: keys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            }
            sorter.setSortKeys(keys);
        });

        // ===== filtering =====
        Runnable applyFilters = () -> {
            List<RowFilter<Object,Object>> list = new ArrayList<>();

            String selectedType = (String) typeFilter.getSelectedItem();
            if(selectedType != null && !selectedType.equals("All Types")){
                list.add(new RowFilter<Object, Object>(){
                    public boolean include(Entry<?, ?> entry){
                        Object val = entry.getValue(2);
                        return val != null && selectedType.equalsIgnoreCase(val.toString().trim());
                    }
                });
            }

            String fareText = maxFare.getText().trim();
            if(!fareText.isEmpty()){
                try{
                    double max = Double.parseDouble(fareText);
                    list.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, max + 0.01, 8));
                }catch(NumberFormatException ignored){
                    // ignore invalid text until it becomes a number
                }
            }
            if(availableOnly.isSelected()){
                list.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 0, 7));
            }
            sorter.setRowFilter(list.isEmpty() ? null : RowFilter.andFilter(list));
        };

        typeFilter.addActionListener(e -> applyFilters.run());
        maxFare.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){ applyFilters.run(); }
            public void removeUpdate(DocumentEvent e){ applyFilters.run(); }
            public void changedUpdate(DocumentEvent e){ applyFilters.run(); }
        });
        availableOnly.addActionListener(e -> applyFilters.run());

        resetBtn.addActionListener(e -> {
            typeFilter.setSelectedIndex(0);
            maxFare.setText("");
            availableOnly.setSelected(false);
            sortBox.setSelectedIndex(0);
            sorter.setRowFilter(null);
        });

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
        select(sourceBox, presetSource);
        select(destBox, presetDest);
        loadBuses();

        searchBtn.addActionListener(e -> loadBuses());
        datePicker.addChangeListener(() -> loadBuses());

        showAllBtn.addActionListener(e -> {
            sourceBox.setSelectedItem("All");
            destBox.setSelectedItem("All");
            loadBuses();
        });

        bookBtn.addActionListener(e -> {

            int row = table.getSelectedRow();   // view row (works with sorting/filtering)

            if(row==-1){
                JOptionPane.showMessageDialog(f,"Select bus first");
                return;
            }

            String busId = table.getValueAt(row,0).toString();
            int available = Integer.parseInt(table.getValueAt(row,7).toString());

            if(available <= 0){
                JOptionPane.showMessageDialog(f,"Bus is fully booked for this date");
                return;
            }

            String defaultName = (presetNames != null && !presetNames.isEmpty()) ? presetNames.get(0) : "";
            String defaultCount = (presetNames != null && !presetNames.isEmpty())
                    ? String.valueOf(Math.min(10, presetNames.size())) : "1";

            String passenger = JOptionPane.showInputDialog(f,"Enter Passenger Name", defaultName);
            if(passenger==null || passenger.isBlank()) return;

            String ticketStr = JOptionPane.showInputDialog(f,"Enter Ticket Count (1-10)", defaultCount);
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

            TicketService.presetNames = presetNames != null ? new ArrayList<>(presetNames) : new ArrayList<>();
            try{
                new BookTicket(username,busId,passenger,tickets,datePicker.getDateString());
            }finally{
                TicketService.presetNames = new ArrayList<>();
            }

            loadBuses(); // refresh availability
        });

        f.setVisible(true);
    }

    /** Sort key for a "hh:mm a" cell; "Not Set" and unparsable values sort last. */
    private static long timeKey(Object cell){
        if(cell == null) return Long.MAX_VALUE;
        String s = cell.toString();
        int plus = s.indexOf(" (+");
        if(plus > 0) s = s.substring(0, plus);
        try{
            long extraDays = cell.toString().contains("(+") ? 1 : 0;
            return LocalTime.parse(s.trim(), TIME_FMT).toSecondOfDay() + extraDays * 86400L;
        }catch(Exception e){
            return Long.MAX_VALUE;
        }
    }

    private void select(JComboBox<String> box, String value){
        if(value == null) return;
        for(int i = 0; i < box.getItemCount(); i++){
            if(box.getItemAt(i).equalsIgnoreCase(value.trim())){
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadCities(){
        try(Connection con = DBConnection.getConnection()){

            sourceBox.removeAllItems();
            destBox.removeAllItems();
            sourceBox.addItem("All");
            destBox.addItem("All");

            if(con == null) return;

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
        String dateStr = datePicker.getDateString();

        StringBuilder sql = new StringBuilder(
            "SELECT b.*, (SELECT COUNT(*) FROM booking k WHERE k.bus_id=b.bus_id AND k.journey_date=?) AS booked " +
            "FROM bus b WHERE 1=1"
        );
        if(src != null) sql.append(" AND LOWER(TRIM(b.source))=?");
        if(dst != null) sql.append(" AND LOWER(TRIM(b.destination))=?");
        sql.append(" ORDER BY b.bus_id");

        try(Connection con = DBConnection.getConnection()){

            if(con == null) return;   // DBConnection already showed an explanatory dialog

            try(PreparedStatement ps = con.prepareStatement(sql.toString())){

                int i = 1;
                ps.setDate(i++, Date.valueOf(dateStr));
                if(src != null) ps.setString(i++, src.toLowerCase());
                if(dst != null) ps.setString(i++, dst.toLowerCase());

                try(ResultSet rs = ps.executeQuery()){
                    while(rs.next()){

                        String runDays = rs.getString("run_days");
                        if(!util.RunDays.runsOn(runDays, datePicker.getDate())){
                            continue; // Skip buses that do not operate on the chosen date
                        }

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

                        String busType = rs.getString("bus_type");
                        if(busType == null || busType.isBlank()) busType = "Ordinary";

                        model.addRow(new Object[]{
                                rs.getInt("bus_id"),
                                rs.getString("bus_name"),
                                busType,
                                rs.getString("source"),
                                rs.getString("destination"),
                                depStr,
                                arrStr,
                                available,
                                rs.getDouble("price")
                        });
                    }
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,"Could not load buses: " + ex.getMessage());
        }
    }
}