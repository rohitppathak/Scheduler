import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Scheduler {

  public static HashMap<Long, Employee> idToEmployee;
  public static ArrayList<Availability> availabilities;
  public static ArrayList<Availability> unavailabilities;
  private ArrayList<TimeOff> timeOffs;

  private final static ParseJSON parse = new ParseJSON();

  public Scheduler() throws IOException {
    this.availabilities = (ArrayList<Availability>) parse
        .toAvailabilitiesUnavailabilitiesAndUserMap()[0];
    this.unavailabilities = (ArrayList<Availability>) parse
        .toAvailabilitiesUnavailabilitiesAndUserMap()[1];
    this.idToEmployee = (HashMap<Long, Employee>) parse
        .toAvailabilitiesUnavailabilitiesAndUserMap()[2];
    this.timeOffs = parse.getTimeOffs(idToEmployee);
  }

  public static void main(String[] args) throws IOException {
    Scheduler s = new Scheduler();
    Iterator it = s.idToEmployee.entrySet().iterator();
    while (it.hasNext()) {
      HashMap.Entry pair = (HashMap.Entry) it.next();
      Employee e = (Employee) pair.getValue();
      Long employeeID = (Long) pair.getKey();
      String avail = "";
      /*for (int i = 0; i < e.getAvailabilities().size(); i += 1) {
        String id = String.valueOf(e.getAvailabilities().get(i).getID());
        avail+=id;
        avail+=" ";
      }*/
      double[] ava = parse.countHoursAvailability(employeeID);
      e.softMinHours = (int) ava[0] / 4;
      System.out.println(
          pair.getKey() + " = " + e.getFirstName() + " " + e.getLastName() + "--" + parse
              .countHours(employeeID) + " " + ava[0] + " " + ava[1]);
      System.out.println();
    }
    scheduleFrontDesk();
  }

  public static void getAvailabilitiesAtTime() {
    int frontDeskMin = 0;
    int frontDeskMax = 1;
    int technicianMin = 1;
    int technicianMax = 2;
    int hardwareMin = 0;
    int hardwareMax = 2;
    //int[][][]
    for (int day = 0; day < 5; day++) {
      System.out.println("Day: " + day);
      for (int startMinute = 480; startMinute < 1140; startMinute += 15) {
        ArrayList<Availability> availables = new ArrayList<>();
        for (int i = 0; i < availabilities.size(); i++) {
          if (availabilities.get(i).doesStartWithTime(day, startMinute)) {
            availables.add(availabilities.get(i));
          }
        }
        System.out.println(startMinute);
        for (int i = 0; i < availables.size(); i++) {
          Employee e = availables.get(i).employee;
          List<String> positions = e.positions;
          String position = positions.get(0);
          if (positions.contains("Hardware")) {
            position = "Hardware";
          }
          System.out.println(e.getFirstName() + " " + e.getLastName() + " - " + position);
        }
      }
      System.out.println();
    }
  }

  /**
   * Schedule curAvail for a shift in schedule. The shift will go for two hours from the
   * startMinute.
   *
   * @param schedule the current schedule
   * @param curAvail the availability from which to create a new shift from
   * @param day the day to schedule the shift
   * @param startMinute the start minute of the shift
   */
  public static void scheduleShift(List<Shift> schedule, Availability curAvail, int day,
      int startMinute) {
    int endingMinute = Math.min(curAvail.endMinute, startMinute + 120);
    Shift toSchedule = new Shift(0, day, startMinute,
        endingMinute, curAvail.employee, "Front Desk");
    curAvail.inSchedule = true;
    toSchedule.associatedAvailability = curAvail;
    curAvail.scheduledShifts.add(toSchedule);
    toSchedule.maxEnding = Math.min(curAvail.endMinute, curAvail.startMinute + 300);
    // TODO make sure this doesn't go over the end time
    schedule.add(toSchedule);
    curAvail.employee.minutesWorked += (endingMinute - toSchedule.startMinute);
    fixPastConflicts(schedule, day, startMinute, toSchedule.employee);
  }

  /**
   * Should the shiftConflict be taken out of the schedule and replaced with a new Shift from
   * curAvail? This is computed by comparing how many hours each employee has worked, and if
   * switching would make a more equal amount of hours for both employee while taking into
   * consideration the hours that might be lost when switching shifts (e.g. there should not be a
   * one hour shift).
   *
   * @param shiftConflict the currently scheduled Shift
   * @param curAvail the availability to compare
   * @param accountForEndTime whether to compare how much potential each availability has to extend
   * its end time
   */
  public static boolean shouldSwitch(Shift shiftConflict, Availability curAvail,
      boolean accountForEndTime) {
    return shouldSwitch(shiftConflict, curAvail, curAvail.startMinute, accountForEndTime);
  }

  /**
   * Should the shiftConflict be taken out of the schedule and replaced with a new Shift from
   * curAvail? This is computed by comparing how many hours each employee has worked, and if
   * switching would make a more equal amount of hours for both employee while taking into
   * consideration the hours that might be lost when switching shifts (e.g. there should not be a
   * one hour shift).
   *
   * @param shiftConflict the currently scheduled Shift
   * @param curAvail the availability to compare
   * @param startMinute the potential start time of the new shift
   * @param accountForEndTime whether to compare how much potential each availability has to extend
   * its end time
   */
  public static boolean shouldSwitch(Shift shiftConflict, Availability curAvail, int startMinute,
      boolean accountForEndTime) {
    int penalty = startMinute - shiftConflict.startMinute;
    int soFar = Math
        .abs(shiftConflict.employee.minutesWorked - curAvail.employee.minutesWorked);
    soFar -= penalty;
    if (accountForEndTime) {
      int potential = curAvail.endMinute - shiftConflict.associatedAvailability.endMinute;
      soFar += potential;
    }
    return (Math.abs(shiftConflict.employee.minutesWorked - shiftConflict.getDuration()
        - curAvail.employee.minutesWorked - 120) < soFar);
  }


  /**
   * Remove shiftConflict from schedule and replace it with a shift made from curAvail on the given
   * day at the given startMinute, ending at at endMinute.
   *
   * @param schedule the current schedule so far
   * @param shiftConflict the shift to remove
   * @param curAvail the availability from which to create a new shift from
   * @param day the day to create the shift
   * @param startMinute the minute to start the shift
   * @param endMinute the minute to end the shift
   */
  public static void switchShift(List<Shift> schedule, Shift shiftConflict, Availability curAvail,
      int day, int startMinute, int endMinute) {
    int toInsert = schedule.indexOf(shiftConflict);
    schedule.remove(shiftConflict);
    shiftConflict.associatedAvailability.scheduledShifts.remove(shiftConflict);
    shiftConflict.employee.minutesWorked -= (shiftConflict.endMinute
        - shiftConflict.startMinute);
    Shift toSchedule = new Shift(0, day, startMinute,
        endMinute, curAvail.employee, "Front Desk");
    toSchedule.conflicts.add(shiftConflict.associatedAvailability);
    toSchedule.associatedAvailability = curAvail;
    curAvail.scheduledShifts.add(toSchedule);
    schedule.add(toInsert, toSchedule);
    curAvail.employee.minutesWorked += (endMinute - toSchedule.startMinute);
    curAvail.inSchedule = true;
    shiftConflict.associatedAvailability.inSchedule = false;
    fixPastConflicts(schedule, day, startMinute, toSchedule.employee);
  }

  /**
   * Remove shiftConflict from schedule and replace it with a shift made from curAvail on the given
   * day at the given startMinute. The new shift will have a duration of 120 minutes.
   *
   * @param schedule the current schedule so far
   * @param shiftConflict the shift to remove
   * @param curAvail the availability from which to create a new shift from
   * @param day the day to create the shift
   * @param startMinute the minute to start the shift
   */
  public static void switchShift(List<Shift> schedule, Shift shiftConflict, Availability curAvail,
      int day, int startMinute) {
    int endingMinute = Math.min(curAvail.endMinute, startMinute + 120);
    switchShift(schedule, shiftConflict, curAvail, day, startMinute, endingMinute);
  }

  public static void scheduleFrontDesk() {
    ArrayList<Availability> onlyFrontDesk = new ArrayList<>();
    for (int i = 0; i < availabilities.size(); i++) {
      if (availabilities.get(i).employee.positions.get(0).equals("Front Desk")) {
        onlyFrontDesk.add(availabilities.get(i));
      }
    }
    ArrayList<Shift> schedule = new ArrayList<>();
    for (int day = 0; day < 5; day++) {
      System.out.println("Day: " + day);
      for (int startMinute = 480; startMinute < 1140; startMinute += 15) {
        ArrayList<Availability> availables = new ArrayList<>();
        for (int i = 0; i < onlyFrontDesk.size(); i++) {
          if (19 * 60 - startMinute < 120) {
            if (!onlyFrontDesk.get(i).inSchedule) {
              continue;
            }
          }
          if (onlyFrontDesk.get(i).inSchedule) {
            if (checkForExtendedShift(onlyFrontDesk.get(i), day, startMinute)
                && onlyFrontDesk.get(i).endMinute - startMinute > 0) {
              availables.add(onlyFrontDesk.get(i));
            }
          }
          if (!onlyFrontDesk.get(i).inSchedule
              && onlyFrontDesk
              .get(i).doesStartWithTime(day, startMinute)
              && onlyFrontDesk.get(i).endMinute - startMinute >= 120) {
            availables.add(onlyFrontDesk.get(i));
          }
        }
        System.out.println(startMinute);
        for (int i = 0; i < availables.size(); i++) {
          if (availables.get(i).endMinute - 120 < availables.get(i).startMinute) {
            availables.remove(i);
            i--;
            continue;
          }
          Employee e = availables.get(i).employee;
          List<String> positions = e.positions;
          String position = positions.get(0);
          System.out.println(e.getFirstName() + " " + e.getLastName() + " - " + position);
        }

        if (availables.isEmpty()) {
          tryToExtendShift(schedule, startMinute, day);
        } else if (availables.size() == 1) { // if there is only one new availability
          Availability curAvail = availables.get(0);
          if (!conflictsWithSchedule(schedule,
              curAvail, startMinute)) {
            if (curAvail.inSchedule) {
              tryToExtendShift(schedule, startMinute, day);
            } else {
              scheduleShift(schedule, curAvail, day, startMinute);
            }
          } else {
            Shift shiftConflict = shiftConflictsWithSchedule(schedule, curAvail);
            if (shiftConflict.conflicts.contains(curAvail)) {
              continue; // if the availability and its conflict has already been accounted for
            }
            if (shiftConflict.maxEnding >= curAvail.endMinute) {
              if (shouldSwitch(shiftConflict, curAvail, false)) {
                switchShift(schedule, shiftConflict, curAvail, day, startMinute);
              } else { // if we shouldn't switch it
                curAvail.inSchedule = false;
                shiftConflict.conflicts.add(curAvail);
              }
            } else {
              // TODO if the conflict has a greater ending time than the scheduled shift
              if (shouldSwitch(shiftConflict, curAvail, true)) {
                switchShift(schedule, shiftConflict, curAvail, day, startMinute);
              } else {
                curAvail.inSchedule = false;
                shiftConflict.conflicts.add(curAvail);
              }
            }
          }
        } else {
          if (!shiftAtMinute(schedule, day, startMinute)) {
            Availability availabilityWithLeastEmployeeWorked = getAvailabilityWithLeastEmployeeWorked(
                availables);
            if (availabilityWithLeastEmployeeWorked.inSchedule) {
              tryToExtendShift(schedule, startMinute,
                  day); // TODO fix this, shifts can be spread thru availabailities
            } else {
              scheduleShift(schedule, availabilityWithLeastEmployeeWorked, day, startMinute);
            }
          } else {
            // TODO if there is more than 1 availability and there is already a shift scheduled
            System.out.println("here");
          }
        }
      }
      System.out.println();
    }

    // Printing the schedule
    for (Shift shift : schedule) {
      System.out.println(
          "Day: " + shift.day + " -- Start: " + shift.startMinute / 60 + ":"
              + shift.startMinute % 60 + " -- End: " + shift.endMinute / 60 + ":"
              + shift.endMinute % 60
              + " -- Employee: " + shift.employee.getFirstName() + " " + shift.employee
              .getLastName() + " -- Minutes worked: " + shift.employee.minutesWorked);
    }
  }

  /**
   * Iterates backward through the current schedule and switches shifts when appropriate. This is
   * used mainly when we have scheduled an employee, and now we need to go back and make the
   * schedule fairer, which means finding all the shifts that this newly scheduled employee has, and
   * switching them for other shifts which would balance the hours between employees.
   *
   * @param startMinute the minute to iterate backward from and the start minute of the newest shift
   * was scheduled
   * @param justScheduled the employee to potentially drop shifts for in order to make a fairer
   * schedule
   */
  public static void fixPastConflicts(List<Shift> schedule, int day, int startMinute,
      Employee justScheduled) {
    int index = 0;
    for (index = 0; index < schedule.size(); index++) {
      if (schedule.get(index).employee == justScheduled && schedule.get(index).day == day
          && schedule.get(index).startMinute == startMinute) {
        break;
      }
    }
    for (int i = index - 1; i >= 0; i--) {
      if (schedule.get(i).employee == justScheduled && !schedule.get(i).conflicts.isEmpty()) {
        int[] potentialTimes = findNewPotentialStartTime(schedule, i,
            schedule.get(i).conflicts.get(0));
        int potentialStartTime = potentialTimes[0];
        int potentialEndTime = potentialTimes[1];

        // if adding this shift will cause the employee to work more than 5 hours
        if (i > 0 && schedule.get(i).day == schedule.get(i - 1).day
            && schedule.get(i - 1).employee == schedule.get(i).conflicts.get(0).employee
            && schedule.get(i - 1).endMinute + 15 == potentialStartTime
            && potentialEndTime - schedule.get(i - 1).startMinute > 300) {
          continue;
        }
        if (i < schedule.size() - 1 && schedule.get(i).day == schedule.get(i + 1).day
            && schedule.get(i + 1).employee == schedule.get(i).conflicts
            .get(0).employee
            && schedule.get(i + 1).startMinute - 15 == potentialEndTime
            && schedule.get(i - 1).endMinute - potentialStartTime > 300) {
          continue;
        }

        if (shouldSwitch(schedule.get(i), schedule.get(i).conflicts.get(0), potentialStartTime,
            false)) {
          switchShift(schedule, schedule.get(i), schedule.get(i).conflicts.get(0),
              schedule.get(i).day, potentialStartTime, potentialEndTime);
        }
      }
    }
  }

  private static int[] findNewPotentialStartTime(List<Shift> schedule, int index,
      Availability availability) {
    int potentialStartTime = availability.startMinute;
    if (index > 0 && schedule.get(index).day == schedule.get(index - 1).day) {
      potentialStartTime = Math.max(potentialStartTime, schedule.get(index - 1).endMinute + 15);
    }
    int potentialEnd = Math.min(availability.endMinute, schedule.get(index).endMinute);
    return new int[]{potentialStartTime, potentialEnd};
  }


  /**
   * Check if the given availability has a shift scheduled that ends on the given day and the given
   * endMinute.
   *
   * @param availability the availability to check shifts on.
   * @param day the day to check the shifts for
   * @param endMinute the ending minute to check for the shifts
   * @return true if there are any shifts in availability that end on endMinute and are on day day,
   * false otherwise
   */
  private static boolean checkForExtendedShift(Availability availability, int day, int endMinute) {
    for (Shift shift : availability.scheduledShifts) {
      if ((shift.day == day) && (shift.endMinute == endMinute)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the availability which corresponds to the employee which has worked the fewest hours so
   * far.
   *
   * @param availabilities the list of availabilities
   * @return the Availability which corresponds to the employee which has worked the fewest hours so
   * far.
   */
  public static Availability getAvailabilityWithLeastEmployeeWorked(
      List<Availability> availabilities) {
    Availability availabilityWithLeastEmployeeWorked = availabilities.get(0);
    for (int i = 1; i < availabilities.size(); i++) {
      if (availabilities.get(i).employee.minutesWorked
          < availabilityWithLeastEmployeeWorked.employee.minutesWorked) {
        availabilityWithLeastEmployeeWorked = availabilities.get(i);
      } else if (availabilities.get(i).employee.minutesWorked
          == availabilityWithLeastEmployeeWorked.employee.minutesWorked && availabilities
          .get(i).inSchedule) {
        availabilityWithLeastEmployeeWorked = availabilities.get(i);
      }
    }
    return availabilityWithLeastEmployeeWorked;
  }

  private static void tryToExtendShift(ArrayList<Shift> schedule,
      int startMinute, int day) {
    Shift toExtend = null;
    for (Shift shift : schedule) {
      if (shift.day == day && shift.endMinute == startMinute) {
        toExtend = shift;
        break;
      }
    }
    if (toExtend != null) {
      Availability availability = toExtend.associatedAvailability;
      if (availability.endMinute >= startMinute + 15 && toExtend.getDuration() < 300) {
        toExtend.endMinute = startMinute + 15;
        toExtend.employee.minutesWorked += 15;
        fixPastConflicts(schedule, day, toExtend.startMinute, toExtend.employee);
      }
    }
  }

  /**
   * Is there a shift during this minute?
   *
   * @param schedule the shifts already scheduled
   * @param day the day to check conflict
   * @param minute the minute to check conflict
   * @return true if there is already a shift scheduled during this minute
   */
  public static boolean shiftAtMinute(ArrayList<Shift> schedule, int day, int minute) {
    for (Shift shift : schedule) {
      if (shift.day == day && shift.startMinute <= minute && shift.endMinute > minute) {
        return true;
      }
    }
    return false;
  }

  public static boolean conflictsWithSchedule(ArrayList<Shift> schedule,
      Availability availability, int startMinute) {
    for (Shift shift : schedule) {
      if (shift.day != availability.day) {
        continue;
      }
      if ((shift.startMinute <= availability.endMinute
          && shift.endMinute > availability.endMinute)
          || (shift.endMinute > startMinute
          && shift.endMinute <= availability.endMinute) || (
          shift.startMinute <= startMinute
              && shift.endMinute >= availability.endMinute)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the shift in schedule that conflicts with the given availability.
   *
   * @param schedule the scheduled shifts to look through
   * @param availability the availability to check for conflict
   * @return the Shift which conflicts with the given availability
   * @throws IllegalArgumentException if there is no Shift which conflicts with the given
   * availability
   */
  public static Shift shiftConflictsWithSchedule(ArrayList<Shift> schedule,
      Availability availability) {
    for (Shift shift : schedule) {
      if (shift.day != availability.day) {
        continue;
      }
      if ((shift.day == availability.day) && (shift.startMinute <= availability.endMinute
          && shift.endMinute > availability.endMinute)
          || (shift.endMinute > availability.startMinute
          && shift.endMinute <= availability.endMinute) || (
          shift.startMinute <= availability.startMinute
              && shift.endMinute >= availability.endMinute)) {
        return shift;
      }
    }
    throw new IllegalArgumentException("no conflicts");
  }
}
