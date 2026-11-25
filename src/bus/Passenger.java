package bus;

public class Passenger {

    public int seat;
    public String name;
    public int age;
    public String gender;
    public boolean student;
    public double fare;      // fare after discount

    public Passenger(int seat, String name, int age, String gender, boolean student){
        this.seat = seat;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.student = student;
    }
}
