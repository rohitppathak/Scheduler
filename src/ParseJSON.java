import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ParseJSON {

  public static Object[] toAvailabilitiesUnavailabilitiesAndUserMap() throws IOException {
    ArrayList<Availability> availabilityItems = new ArrayList<>();
    ArrayList<Unavailability> unavailabilityItems = new ArrayList<>();

    String json = new GetRequest(
        "https://api.wheniwork.com/2/availabilities/items?start=2019-01-28 00:00:00&end=2019-02-01 23:59:59")
        .getOutput();
    JSONObject list = new JSONObject(json);
    JSONArray items = list.getJSONArray("availabilityitems");

    HashMap<Long, Employee> idToEmployee = new HashMap<>();

    for (int i = 0; i < items.length(); i += 1) {
      JSONObject item = items.getJSONObject(i);
      if (item.getLong("user_id") != 6191974 && item.getInt("type") == 2) {
        long id = item.getLong("id");
        //System.out.println(id);
        int day = item.getInt("day") - 1;
        int startMinute = toMinutes(item.getString("start_time"));
        int endMinute = toMinutes(item.getString("end_time"));
        long employeeID = item.getLong("user_id");
        Employee employee;
        if (idToEmployee.get(employeeID) == null) {
          employee = toEmployee(item.getLong("user_id"));
          if (employee == null) {
            continue;
          }
          idToEmployee.put(employeeID, employee);
        } else {
          employee = idToEmployee.get(employeeID);
        }
        Availability avail = new Availability(id, day, startMinute, endMinute, employee);
        employee.addAvailability(avail);
        availabilityItems.add(avail);
      }
    }

    for (int i = 0; i < items.length(); i += 1) {
      JSONObject item = items.getJSONObject(i);
      if (item.getLong("user_id") != 6191974 && item.getInt("type") == 1) {
        long id = item.getLong("id");
        int day = item.getInt("day") - 1;
        int startMinute = toMinutes(item.getString("start_time"));
        int endMinute = toMinutes(item.getString("end_time"));
        long employeeID = item.getLong("user_id");
        Employee employee;
        if (idToEmployee.get(employeeID) == null) {
          continue;
        } else {
          employee = idToEmployee.get(employeeID);
        }
        Unavailability unavail = new Unavailability(id, day, startMinute, endMinute, employee);
        employee.addUnavailability(unavail);
        unavailabilityItems.add(unavail);
      }
    }

    Object[] availabilitiesUnavailabilitiesAndUserMap = {availabilityItems, unavailabilityItems,
        idToEmployee};
    return availabilitiesUnavailabilitiesAndUserMap;
  }

  public static Employee toEmployee(long id) throws IOException {
    JSONObject employeeInfo = new JSONObject(
        new GetRequest("https://api.wheniwork.com/2/users/" + id).getOutput());
    JSONObject user = employeeInfo.getJSONObject("user");
    if (user.getBoolean("is_deleted")) {
      return null;
    }
    String firstName = user.getString("first_name");
    String lastName = user.getString("last_name");
    long userID = user.getLong("id");
    //System.out.println(firstName + " " + lastName + " " + userID);
    ArrayList<String> positions = new ArrayList<String>();
    try {
      JSONArray positionsArr = employeeInfo.getJSONArray("positions");
      for (int i = 0; i < positionsArr.length(); i += 1) {
        JSONObject position = positionsArr.getJSONObject(i);
        positions.add(position.getString("name"));
      }
    } catch (org.json.JSONException e) {
      positions.add("Technician");
    }

    Employee employee = new Employee(firstName, lastName, userID, new ArrayList<Shift>(),
        new ArrayList<Availability>(), new ArrayList<Unavailability>(), new ArrayList<TimeOff>(),
        positions, 0, 20);
    return employee;

  }

  public ArrayList<TimeOff> getTimeOffs(HashMap<Long, Employee> idToEmployee) {
    ArrayList<TimeOff> timeOffs = new ArrayList<TimeOff>();
    String url = "https://api.wheniwork.com/2/requests";
    String param = "?start=2019-01-14 00:00:00&end=2019-01-18 23:59:59&user_id=";
    Iterator it = idToEmployee.entrySet().iterator();
    while (it.hasNext()) {
      HashMap.Entry pair = (HashMap.Entry) it.next();
      param += pair.getKey() + ",";
    }
    String fullUrl = url + param;
    String finalUrl = fullUrl.substring(0, fullUrl.length() - 1);
    System.out.println(finalUrl);
    return timeOffs;
  }

  public static int toMinutes(String time) {
    String[] parts = time.split(":");
    int hours = Integer.parseInt(parts[0]);
    int minutes = Integer.parseInt(parts[1]);

    return hours * 60 + minutes;
  }

  public double countHours(Long employeeID) throws IOException {
    String json = new GetRequest(
        "https://api.wheniwork.com/2/shifts?start=2019-01-14 00:00:00&end=2019-01-18 23:59:59&user_id="
            + employeeID)
        .getOutput();
    JSONObject info = new JSONObject(json);
    JSONArray shifts = info.getJSONArray("shifts");
    double count = 0;
    for (int i = 0; i < shifts.length(); i++) {
      JSONObject shift = shifts.getJSONObject(i);
      String start = shift.getString("start_time");
      String end = shift.getString("end_time");
      String[] split1 = start.split(":");
      String[] split2 = split1[0].split(" ");
      String[] split3 = end.split(":");
      String[] split4 = split3[0].split(" ");
      double hourDif =
          Integer.parseInt(split4[split4.length - 1]) - Integer.parseInt(split2[split2.length - 1]);
      double minDif = (Double.parseDouble(split3[1]) - Double.parseDouble(split1[1])) / 60;
      hourDif = hourDif + minDif;
      count = count + hourDif;
    }
    return count;
  }

  public double[] countHoursAvailability(Long employeeID) throws IOException {
    String json = new GetRequest(
        "https://api.wheniwork.com/2/availabilities/items?start=2019-01-14 00:00:00&end=2019-01-18 23:59:59&user_id="
            + employeeID)
        .getOutput();
    JSONObject info = new JSONObject(json);
    JSONArray shifts = info.getJSONArray("availabilityitems");
    double[] counts = {0, 0};
    for (int i = 0; i < shifts.length(); i++) {
      JSONObject shift = shifts.getJSONObject(i);
      String start = shift.getString("start_time");
      String end = shift.getString("end_time");
      String[] split1 = start.split(":");
      String[] split3 = end.split(":");
      int hourStart = Math.max(Integer.parseInt(split1[0]), 8);
      int hourEnd = Integer.parseInt(split3[0]);
      if (hourEnd > 19 || hourEnd == 0) {
        hourEnd = 19;
      }
      double hourDif = hourEnd - hourStart;
      double minDif = (Double.parseDouble(split3[1]) - Double.parseDouble(split1[1])) / 60;
      hourDif = hourDif + minDif;
      if (shift.getInt("type") == 1) {
        counts[1] += hourDif;
      } else {
        counts[0] += hourDif;
      }
    }
    return counts;
  }
}
