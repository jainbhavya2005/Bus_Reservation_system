package util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

/** Builds a one-page PDF e-ticket (with QR code) without any external library. */
public class TicketPdf {

    public static class Pax {
        public int seat;
        public String name;
        public int age;          // 0 = unknown
        public String gender;
        public double fare;
    }

    public static class Ticket {
        public String pnr, username, email;
        public String busName, source, destination;
        public String journeyDate, departure, arrival, payment;
        public List<Pax> passengers = new ArrayList<>();

        public double total(){
            double t = 0;
            for(Pax p : passengers) t += p.fare;
            return t;
        }

        public String seatList(){
            StringBuilder sb = new StringBuilder();
            for(Pax p : passengers){
                if(sb.length() > 0) sb.append(",");
                sb.append(p.seat);
            }
            return sb.toString();
        }
    }

    public static String qrPayload(Ticket t){
        return "PNR:" + t.pnr + "|BUS:" + t.busName + "|" + t.source + "-" + t.destination +
               "|DATE:" + t.journeyDate + "|SEATS:" + t.seatList();
    }

    // ================= PDF building =================

    public static byte[] build(Ticket t){

        // ----- QR image -----
        QrCode qr = QrCode.encode(qrPayload(t));
        int scale = 6, quiet = 4;
        int px = (qr.size + quiet * 2) * scale;
        byte[] gray = new byte[px * px];
        for(int y = 0; y < px; y++){
            for(int x = 0; x < px; x++){
                boolean dark = qr.get(x / scale - quiet, y / scale - quiet);
                gray[y * px + x] = (byte) (dark ? 0x00 : 0xFF);
            }
        }
        byte[] qrData = deflate(gray);

        // ----- page content -----
        StringBuilder c = new StringBuilder();

        // header band
        c.append("0.12 0.56 1 rg 0 742 595 100 re f\n");
        text(c, "F2", 26, 40, 790, "BUS E-TICKET", true);
        text(c, "F2", 14, 380, 795, "PNR: " + t.pnr, true);
        text(c, "F1", 11, 380, 775, "Booked by: " + t.username, true);

        // route
        c.append("0 g\n");
        text(c, "F2", 22, 40, 700, t.source + "  ->  " + t.destination, false);

        // trip details
        int y = 668;
        y = line(c, y, "Bus", t.busName);
        y = line(c, y, "Journey Date", t.journeyDate);
        y = line(c, y, "Departure", t.departure);
        y = line(c, y, "Arrival", t.arrival);
        y = line(c, y, "Payment", t.payment);

        // passenger table
        y -= 12;
        c.append("0.9 0.93 1 rg 40 " + (y - 6) + " 515 22 re f\n0 g\n");
        text(c, "F2", 11, 48, y, "Seat", false);
        text(c, "F2", 11, 100, y, "Passenger", false);
        text(c, "F2", 11, 330, y, "Age", false);
        text(c, "F2", 11, 380, y, "Gender", false);
        text(c, "F2", 11, 470, y, "Fare (Rs.)", false);
        y -= 22;

        for(Pax p : t.passengers){
            text(c, "F1", 11, 48, y, String.valueOf(p.seat), false);
            text(c, "F1", 11, 100, y, p.name, false);
            text(c, "F1", 11, 330, y, p.age > 0 ? String.valueOf(p.age) : "-", false);
            text(c, "F1", 11, 380, y, p.gender == null ? "-" : p.gender, false);
            text(c, "F1", 11, 470, y, String.format("%.2f", p.fare), false);
            c.append("0.8 w 0.8 g 40 " + (y - 6) + " m 555 " + (y - 6) + " l S 0 g\n");
            y -= 20;
        }

        text(c, "F2", 13, 380, y - 4, "Total Paid: Rs. " + String.format("%.2f", t.total()), false);

        // QR + footer
        c.append("q 150 0 0 150 40 70 cm /Im1 Do Q\n");
        text(c, "F2", 12, 210, 190, "Scan this code at boarding", false);
        text(c, "F1", 10, 210, 172, "Carry a valid photo ID. Reach the stop 15 minutes before departure.", false);
        text(c, "F1", 10, 210, 158, "Cancellation charge: 10% of fare. Refund in 2-3 working days.", false);
        text(c, "F1", 9, 40, 40, "This is a computer-generated ticket and does not require a signature.", false);

        byte[] content = c.toString().getBytes(StandardCharsets.ISO_8859_1);

        // ----- assemble file -----
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        write(out, "%PDF-1.4\n");

        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                   "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> /XObject << /Im1 7 0 R >> >> " +
                   "/Contents 4 0 R >>\nendobj\n");

        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Length " + content.length + " >>\nstream\n");
        out.write(content, 0, content.length);
        write(out, "\nendstream\nendobj\n");

        offsets.add(out.size());
        write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        offsets.add(out.size());
        write(out, "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        offsets.add(out.size());
        write(out, "7 0 obj\n<< /Type /XObject /Subtype /Image /Width " + px + " /Height " + px +
                   " /ColorSpace /DeviceGray /BitsPerComponent 8 /Filter /FlateDecode /Length " +
                   qrData.length + " >>\nstream\n");
        out.write(qrData, 0, qrData.length);
        write(out, "\nendstream\nendobj\n");

        int xref = out.size();
        write(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for(int off : offsets) write(out, String.format("%010d 00000 n \n", off));
        write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");

        return out.toByteArray();
    }

    // ================= helpers =================

    private static int line(StringBuilder c, int y, String label, String value){
        text(c, "F2", 12, 40, y, label + ":", false);
        text(c, "F1", 12, 160, y, value == null ? "-" : value, false);
        return y - 22;
    }

    private static void text(StringBuilder c, String font, int size, int x, int y, String s, boolean white){
        if(white) c.append("1 g\n");
        c.append("BT /").append(font).append(' ').append(size).append(" Tf ")
         .append(x).append(' ').append(y).append(" Td (").append(escape(s)).append(") Tj ET\n");
        if(white) c.append("0 g\n");
    }

    private static String escape(String s){
        if(s == null) return "";
        StringBuilder sb = new StringBuilder();
        for(char ch : s.toCharArray()){
            if(ch == '\u20B9') sb.append("Rs.");
            else if(ch == '\\' || ch == '(' || ch == ')') sb.append('\\').append(ch);
            else if(ch < 32 || ch > 255) sb.append('?');
            else sb.append(ch);
        }
        return sb.toString();
    }

    private static void write(ByteArrayOutputStream out, String s){
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(b, 0, b.length);
    }

    private static byte[] deflate(byte[] data){
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try{
            d.setInput(data);
            d.finish();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while(!d.finished()){
                int n = d.deflate(buf);
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            d.end();
        }
    }
}
