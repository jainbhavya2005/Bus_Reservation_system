package util;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Sends e-mail through SMTP (Gmail by default).
 * Credentials come from environment variables SMTP_USER / SMTP_PASS
 * or from a mail.properties file in the project root (see mail.properties.example).
 */
public class Mailer {

    private static String user, pass, host = "smtp.gmail.com", port = "587";
    private static boolean loaded = false;

    private static synchronized void load(){
        if(loaded) return;
        loaded = true;

        File f = new File("mail.properties");
        if(f.exists()){
            try(FileInputStream in = new FileInputStream(f)){
                Properties p = new Properties();
                p.load(in);
                user = p.getProperty("smtp.user");
                pass = p.getProperty("smtp.password");
                host = p.getProperty("smtp.host", host);
                port = p.getProperty("smtp.port", port);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        if(user == null || user.isBlank()) user = System.getenv("SMTP_USER");
        if(pass == null || pass.isBlank()) pass = System.getenv("SMTP_PASS");
        if(System.getenv("SMTP_HOST") != null) host = System.getenv("SMTP_HOST");
        if(System.getenv("SMTP_PORT") != null) port = System.getenv("SMTP_PORT");
    }

    public static boolean isConfigured(){
        load();
        return user != null && !user.isBlank() && pass != null && !pass.isBlank();
    }

    public static void send(String to, String subject, String body,
                            String attachName, byte[] attachment) throws Exception {

        if(!isConfigured()) throw new IllegalStateException("Email is not configured (mail.properties missing)");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        Session session = Session.getInstance(props, new Authenticator(){
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication(user, pass);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(user, "Bus Booking System"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject, "UTF-8");

        Multipart mp = new MimeMultipart();

        MimeBodyPart text = new MimeBodyPart();
        text.setText(body, "UTF-8");
        mp.addBodyPart(text);

        if(attachment != null){
            MimeBodyPart att = new MimeBodyPart();
            att.setDataHandler(new DataHandler(new ByteArrayDataSource(attachment, "application/pdf")));
            att.setFileName(attachName);
            mp.addBodyPart(att);
        }

        msg.setContent(mp);
        Transport.send(msg);
    }

    /** Sends on a background thread; callback gets "" on success or an error message (runs on the Swing thread). */
    public static void sendAsync(String to, String subject, String body,
                                 String attachName, byte[] attachment, Consumer<String> done){
        new Thread(() -> {
            String result = "";
            try{
                send(to, subject, body, attachName, attachment);
            }catch(Exception e){
                e.printStackTrace();
                result = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final String r = result;
            if(done != null) SwingUtilities.invokeLater(() -> done.accept(r));
        }, "mailer").start();
    }
}
