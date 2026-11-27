package dashboard;

import auth.Login;
import java.awt.*;
import javax.swing.*;
import util.Theme;

public class AdminDashboard {

    public AdminDashboard(String username){

        JFrame f = new JFrame("Admin Dashboard");
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
        panel.setPreferredSize(new Dimension(540,640));
        panel.setBackground(new Color(255,255,255,220));

        Font h = new Font("Times New Roman",Font.BOLD,24);
        Font b = new Font("Times New Roman",Font.BOLD,16);

        JLabel title = new JLabel("Welcome Admin " + username, SwingConstants.CENTER);
        title.setFont(h);
        title.setBounds(0,20,540,30);
        panel.add(title);

        // ===== summary cards =====
        RevenueChart.Summary s = RevenueChart.summary();
        panel.add(card("Total tickets", String.valueOf(s.tickets), 20));
        panel.add(card("Revenue (base fare)", "\u20B9 " + String.format("%,.0f", s.revenue), 190));
        panel.add(card("Most booked bus", s.topBus + " (" + s.topBusTickets + ")", 360));

        JButton viewBuses = new JButton("View Buses");
        JButton add = new JButton("Add Bus");
        JButton update = new JButton("Update Bus");
        JButton delete = new JButton("Delete Bus");
        JButton view = new JButton("View Bookings");
        JButton analytics = new JButton("Revenue Analytics");
        JButton logout = new JButton("Logout");

        JButton[] buttons={viewBuses,add,update,delete,view,analytics,logout};

        int y=130;
        for(JButton btn:buttons){
            btn.setFont(b);
            btn.setBackground(new Color(30,144,255));
            btn.setForeground(Color.WHITE);
            btn.setBounds(170,y,200,40);
            panel.add(btn);
            y+=54;
        }

        analytics.setBackground(new Color(34,139,34));
        logout.setBackground(new Color(178,34,34));

        Theme.addToggle(panel, 20, 600);
        bg.add(panel);

        viewBuses.addActionListener(e -> new ViewBuses());
        add.addActionListener(e -> new AddBus());
        update.addActionListener(e -> new UpdateBus());
        delete.addActionListener(e -> new DeleteBus());
        view.addActionListener(e -> new ViewBookings());
        analytics.addActionListener(e -> new RevenueChart());

        logout.addActionListener(e->{
            new Login();
            f.dispose();
        });

        f.setVisible(true);
    }

    private static JLabel card(String caption, String value, int x){
        JLabel l = new JLabel("<html><div style='text-align:center'><span style='font-size:10px'>" + caption +
                "</span><br><b style='font-size:14px'>" + value + "</b></div></html>", SwingConstants.CENTER);
        l.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        l.setBorder(BorderFactory.createLineBorder(new Color(30,100,200), 2, true));
        l.setBounds(x, 65, 160, 52);
        return l;
    }
}