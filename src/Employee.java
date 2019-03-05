import java.util.ArrayList;
import java.util.List;

public class Employee {
  private String firstName;
  private String lastName;
  private long id;
  private List<Shift> shifts;
  private List<Availability> availabilities;
  private List<Unavailability> unavailabilities;
  private List<TimeOff> timeOffs;
  List<String> positions;
  int minutesWorked;
  int maxHours;
  int softMinHours;

  public Employee(String firstName, String lastName, long id, List<Shift> shifts,
      List<Availability> availabilities, List<Unavailability> unavailabilities,
      List<TimeOff> timeOffs, List<String> positions, int minutesWorked, int maxHours) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.id = id;
    this.shifts = shifts;
    this.availabilities = availabilities;
    this.unavailabilities = unavailabilities;
    this.timeOffs = timeOffs;
    this.positions = positions;
    this.minutesWorked = minutesWorked;
    this.maxHours = maxHours;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public List<Availability> getAvailabilities() {
    return availabilities;
  }

  public List<Unavailability> getUnavailabilities() {
    return unavailabilities;
  }

  public void addAvailability(Availability a) {
    availabilities.add(a);
  }

  public void addAvailabilities(List<Availability> a) {
    availabilities.addAll(a);
  }

  public void addUnavailability(Unavailability u) {
    unavailabilities.add(u);
  }
}
