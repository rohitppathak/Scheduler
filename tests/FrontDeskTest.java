import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FrontDeskTest {

  @Test
  public void simpleNoConflicts() {
    List<String> p = Arrays.asList("Front Desk");
    Employee e1 = new Employee("E1", "L1", 0, new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>(), new ArrayList<>(), p, 0, 20);
    Employee e2 = new Employee("E2", "L2", 0, new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>(), new ArrayList<>(), p, 0, 20);
    List<Availability> a1 = Arrays.asList(new Availability(0, 0, 500, 700, e1));
    List<Availability> a2 = Arrays.asList(new Availability(0, 0, 900, 1020, e2));
    e1.addAvailabilities(a1);
    e2.addAvailabilities(a2);
    List<Availability> a = new ArrayList<>(a1);
    a.addAll(a2);

    Scheduler s = new Scheduler(a);
    assertEquals(
        "Day: 0 -- Start: 8:30 -- End: 11:30 -- Employee: E1 L1 -- Minutes worked: 180\nDay: 0 -- Start: 15:0 -- End: 17:0 -- Employee: E2 L2 -- Minutes worked: 120",
        s.printSchedule(s.scheduleFrontDesk()));
  }
}
