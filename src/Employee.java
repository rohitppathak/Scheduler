import java.util.ArrayList;

public class Employee {
  private String firstName;
  private String lastName;
  private long id;
  private ArrayList<Shift> shifts;
  private ArrayList<Availability> availabilities;
  private ArrayList<Unavailability> unavailabilities;
  private ArrayList<TimeOff> timeOffs;
  ArrayList<String> positions;
  int minutesWorked;
  int maxHours;
  int softMinHours;

  public Employee(String firstName, String lastName, long id, ArrayList<Shift> shifts,
      ArrayList<Availability> availabilities, ArrayList<Unavailability> unavailabilities,
      ArrayList<TimeOff> timeOffs, ArrayList<String> positions, int minutesWorked, int maxHours) {
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

  public ArrayList<Availability> getAvailabilities() {
    return availabilities;
  }

  public ArrayList<Unavailability> getUnavailabilities() {
    return unavailabilities;
  }

  public void addAvailability(Availability a) {
    availabilities.add(a);
  }

  public void addUnavailability(Unavailability u) {
    unavailabilities.add(u);
  }
}
