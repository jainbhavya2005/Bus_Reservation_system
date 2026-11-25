package auth;

import dashboard.AdminDashboard;
import dashboard.Dashboard;
import db.DBConnection;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class Login {

    public Login(){

        JFrame f = new JFrame("Bus Booking System");
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel backgroundPanel = new JPanel() {
            Image bg = new ImageIcon("Images/Bg.jpeg").getImage();

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        backgroundPanel.setLayout(new GridBagLayout());
        f.setContentPane(backgroundPanel);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(400, 350));
        panel.setBackground(new Color(255, 255, 255, 220));
        panel.setLayout(null);

        Font headingFont = new Font("Times New Roman", Font.BOLD, 24);
        Font labelFont = new Font("Times New Roman", Font.PLAIN, 18);
        Font fieldFont = new Font("Times New Roman", Font.PLAIN, 16);
        Font buttonFont = new Font("Times New Roman", Font.BOLD, 16);

        JLabel title = new JLabel("Bus Booking Login", SwingConstants.CENTER);
        title.setFont(headingFont);
        title.setBounds(0,20,400,30);

        JLabel u = new JLabel("Username");
        u.setFont(labelFont);
        u.setBounds(40,80,120,30);

        JTextField user = new JTextField();
        user.setFont(fieldFont);
        user.setBounds(160,80,180,30);

        JLabel p = new JLabel("Password");
        p.setFont(labelFont);
        p.setBounds(40,130,120,30);

        JPasswordField pass = new JPasswordField();
        pass.setFont(fieldFont);
        pass.setBounds(160,130,180,30);

        JLabel r = new JLabel("Role");
        r.setFont(labelFont);
        r.setBounds(40,180,120,30);

        String roles[]={"user","admin"};
        JComboBox<String> roleBox = new JComboBox<>(roles);
        roleBox.setFont(fieldFont);
        roleBox.setBounds(160,180,180,30);

        JButton login = new JButton("Login");
        login.setFont(buttonFont);
        login.setOpaque(true);
        login.setContentAreaFilled(true);
        login.setBackground(new Color(30,144,255));
        login.setForeground(Color.WHITE);
        login.setBorderPainted(true);
        login.setBounds(60,240,120,40);

        JButton signup = new JButton("Sign Up");
        signup.setFont(buttonFont);
        signup.setOpaque(true);
        signup.setContentAreaFilled(true);
        signup.setBackground(new Color(34,139,34));
        signup.setForeground(Color.WHITE);
        signup.setBorderPainted(true);
        signup.setBounds(210,240,120,40);

        panel.add(title);
        panel.add(u); panel.add(user);
        panel.add(p); panel.add(pass);
        panel.add(r); panel.add(roleBox);
        panel.add(login); panel.add(signup);

        backgroundPanel.add(panel);

        // Enter key triggers login
        f.getRootPane().setDefaultButton(login);

        login.addActionListener(e->{

            String username = user.getText().trim();
            String password = new String(pass.getPassword());
            String role = roleBox.getSelectedItem().toString();

            if(username.isEmpty() || password.isEmpty()){
                JOptionPane.showMessageDialog(f,"Enter username and password");
                return;
            }

            try(Connection con = DBConnection.getConnection()){

                if(con == null){
                    JOptionPane.showMessageDialog(f,"Cannot connect to database");
                    return;
                }

                try(PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM users WHERE username=? AND password=? AND role=?")){

                    ps.setString(1,username);
                    ps.setString(2,password);
                    ps.setString(3,role);

                    try(ResultSet rs = ps.executeQuery()){
                        if(rs.next()){
                            if(role.equals("admin")){
                                new AdminDashboard(username);
                            } else {
                                new Dashboard(username);
                            }
                            f.dispose();
                        } else {
                            JOptionPane.showMessageDialog(f,"Invalid Login");
                        }
                    }
                }

            }catch(Exception ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(f,"Login error: " + ex.getMessage());
            }
        });

        signup.addActionListener(e->{
            new Signup();
            f.dispose();
        });

        f.setVisible(true);
    }
}