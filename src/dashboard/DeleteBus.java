package dashboard;

import db.DBConnection;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class DeleteBus {

    public DeleteBus(){

        JFrame f=new JFrame("Delete Bus");
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel bg=new JPanel(){
            Image img=new ImageIcon("Images/Bg.jpeg").getImage();
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(img,0,0,getWidth(),getHeight(),this);
            }
        };

        bg.setLayout(new GridBagLayout());
        f.setContentPane(bg);

        JPanel panel=new JPanel(null);
        panel.setPreferredSize(new Dimension(400,250));
        panel.setBackground(new Color(255,255,255,220));

        Font h=new Font("Times New Roman",Font.BOLD,24);
        Font l=new Font("Times New Roman",Font.PLAIN,18);
        Font b=new Font("Times New Roman",Font.BOLD,16);

        JLabel title=new JLabel("Delete Bus");
        title.setFont(h);
        title.setBounds(120,20,200,30);

        JLabel idL=new JLabel("Bus ID");
        idL.setFont(l);
        idL.setBounds(50,80,100,30);

        JTextField id=new JTextField();
        id.setBounds(150,80,180,30);

        JButton del=new JButton("Delete");
        del.setFont(b);
        del.setBackground(new Color(178,34,34));
        del.setForeground(Color.WHITE);
        del.setBounds(120,140,150,40);

        panel.add(title);
        panel.add(idL);
        panel.add(id);
        panel.add(del);

        bg.add(panel);

        del.addActionListener(e->{

            int busId;
            try{
                busId = Integer.parseInt(id.getText().trim());
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(f,"Enter a valid numeric Bus ID");
                return;
            }

            try(Connection con=DBConnection.getConnection()){

                // block deletion if upcoming bookings exist
                try(PreparedStatement chk=con.prepareStatement(
                        "SELECT COUNT(*) FROM booking WHERE bus_id=? AND journey_date>=CURDATE()")){
                    chk.setInt(1,busId);
                    try(ResultSet rs=chk.executeQuery()){
                        if(rs.next() && rs.getInt(1)>0){
                            JOptionPane.showMessageDialog(f,
                                "Cannot delete: this bus has " + rs.getInt(1) + " upcoming booking(s).");
                            return;
                        }
                    }
                }

                int confirm = JOptionPane.showConfirmDialog(f,
                        "Delete bus " + busId + "? This cannot be undone.",
                        "Confirm",JOptionPane.YES_NO_OPTION);
                if(confirm != JOptionPane.YES_OPTION) return;

                try(PreparedStatement ps=con.prepareStatement("DELETE FROM bus WHERE bus_id=?")){
                    ps.setInt(1,busId);
                    int rows = ps.executeUpdate();

                    if(rows==0){
                        JOptionPane.showMessageDialog(f,"No bus found with ID " + busId);
                        return;
                    }
                }

                JOptionPane.showMessageDialog(f,"Deleted Successfully");
                f.dispose();

            }catch(SQLException ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(f,
                    "Could not delete (past bookings may reference this bus): " + ex.getMessage());
            }
        });

        f.setVisible(true);
    }
}