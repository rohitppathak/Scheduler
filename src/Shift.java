import java.util.ArrayList;

public class Shift {

  int id;
  int day;
  int startMinute;
  int endMinute;
  Employee employee;
  String position;
  ArrayList<Availability> conflicts;
  int maxEnding;
  Availability associatedAvailability;

  public Shift(int id, int day, int startMinute, int endMinute, Employee employee,
      String position) {
    this.id = id;
    this.day = day;
    this.startMinute = startMinute;
    this.endMinute = endMinute;
    this.employee = employee;
    this.position = position;
    this.conflicts = new ArrayList<>();
  }

  int getDuration() {
    return endMinute - startMinute;
  }
}
