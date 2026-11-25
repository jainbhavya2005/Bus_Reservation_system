package bus;

/** Discount rules: child (12 and under) 50%, senior (60+) 20%, student (13-59) 10%. */
public class Fare {

    public static int discountPercent(int age, boolean student){
        if(age <= 12) return 50;
        if(age >= 60) return 20;
        if(student) return 10;
        return 0;
    }

    public static String label(int age, boolean student){
        if(age <= 12) return "Child 50% off";
        if(age >= 60) return "Senior 20% off";
        if(student)   return "Student 10% off";
        return "";
    }

    public static double apply(double base, int age, boolean student){
        double v = base * (100 - discountPercent(age, student)) / 100.0;
        return Math.round(v * 100.0) / 100.0;
    }
}
