import java.util.ArrayList;
import java.util.List;

public class Availability {

  long id;
  int day;
  int startMinute;
  int endMinute;
  Employee employee;
  boolean inSchedule;
  List<Shift> scheduledShifts;

  public Availability(long id, int day, int startMinute, int endMinute, Employee employee) {
    this.id = id;
    this.day = day;
    this.startMinute = startMinute;
    this.endMinute = endMinute;
    this.employee = employee;
    this.scheduledShifts = new ArrayList<>();
  }

  public long getID() {
    return id;
  }

  public boolean doesStartWithTime(int day, int startMinute) {
    return this.day == day && this.startMinute <= startMinute && this.endMinute > startMinute;
  }
}
