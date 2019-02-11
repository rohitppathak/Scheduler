public class Unavailability {
  private long id;
  private int day;
  private int startMinute;
  private int endMinute;
  private Employee employee;

  public Unavailability(long id, int day, int startMinute, int endMinute, Employee employee) {
    this.id = id;
    this.day = day;
    this.startMinute = startMinute;
    this.endMinute = endMinute;
    this.employee = employee;
  }

  public long getID() {
    return id;
  }
}
