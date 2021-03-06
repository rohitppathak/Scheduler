import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Scheduler {

  static HashMap<Long, Employee> idToEmployee;
  static List<Availability> availabilities;
  static List<Availability> unavailabilities;
  static List<TimeOff> timeOffs;

  private final static ParseJSON parse = new ParseJSON();

  public Scheduler(List<Availability> availabilities) {
    this.availabilities = availabilities;
  }

  public static void main(String[] args) throws IOException {
    //Scheduler s = new Scheduler();
    //Iterator it = s.idToEmployee.entrySet().iterator();
    /*while (it.hasNext()) {
      HashMap.Entry pair = (HashMap.Entry) it.next();
      Employee e = (Employee) pair.getValue();
      Long employeeID = (Long) pair.getKey();
      String avail = "";
      *//*for (int i = 0; i < e.getAvailabilities().size(); i += 1) {
        String id = String.valueOf(e.getAvailabilities().get(i).getID());
        avail+=id;
        avail+=" ";
      }*//*
      double[] ava = parse.countHoursAvailability(employeeID);
      e.softMinHours = (int) ava[0] / 4;
      System.out.println(
          pair.getKey() + " = " + e.getFirstName() + " " + e.getLastName() + "--" + parse
              .countHours(employeeID) + " " + ava[0] + " " + ava[1]);
      System.out.println();
    }*/
    if (args.length < 2) {
      throw new IllegalArgumentException("please specify two dates");
    }
    Object[] data = parse.toAvailabilitiesUnavailabilitiesAndUserMap(args[0], args[1]);
    availabilities = (ArrayList<Availability>) data[0];
    unavailabilities = (ArrayList<Availability>) data[1];
    idToEmployee = (HashMap<Long, Employee>) data[2];
    timeOffs = parse.getTimeOffs(idToEmployee);

    System.out.println(printSchedule(scheduleFrontDesk()));
  }

  public static void getAvailabilitiesAtTime() {
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
  public static Shift scheduleShift(List<Shift> schedule, Availability curAvail, int day,
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
    return toSchedule;
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
  public static Shift switchShift(List<Shift> schedule, Shift shiftConflict, Availability curAvail,
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
    return toSchedule;
  }

  static void adjustShifts(Shift toAdjust) {
    if (toAdjust.conflicts.isEmpty()) {
      return;
    }
    Availability conflict = toAdjust.conflicts.get(0); //TODO update this
    Shift other = endingAtTime(conflict.scheduledShifts, toAdjust.startMinute);
    if (other == null) {
      return;
    }
    //TODO this needs to go the other way too
    while (toAdjust.employee.minutesWorked - other.employee.minutesWorked >= 30
        && other.endMinute - other.startMinute < 300 && toAdjust.endMinute - toAdjust.startMinute > 120) {
      toAdjust.endMinute -= 15;
      other.startMinute -= 15;
      toAdjust.employee.minutesWorked -= 15;
      other.employee.minutesWorked += 15;
    }
  }

  static Shift endingAtTime(List<Shift> shifts, int endTime) {
    for (Shift shift : shifts) {
      if (shift.endMinute == endTime) {
        return shift;
      }
    }
    return null;
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
  public static Shift switchShift(List<Shift> schedule, Shift shiftConflict, Availability curAvail,
      int day, int startMinute) {
    int endingMinute = Math.min(curAvail.endMinute, startMinute + 120);
    return switchShift(schedule, shiftConflict, curAvail, day, startMinute, endingMinute);
  }

  /**
   * Schedules front desk employees. For front desk, there should only be one scheduled at a time,
   * and the hours worked should be spread evenly. In addition, each shift should be at least 2
   * hours long.
   *
   * NOTE: the code right now is gross, but it is helpful for debugging and will be refactored in
   * the future.
   */
  public static List<Shift> scheduleFrontDesk() {
    ArrayList<Availability> onlyFrontDesk = new ArrayList<>();
    for (int i = 0; i < availabilities.size(); i++) {
      if (availabilities.get(i).employee.positions.get(0).equals("Front Desk")) {
        onlyFrontDesk.add(availabilities.get(i));
      }
    }
    ArrayList<Shift> schedule = new ArrayList<>();
    for (int day = 0; day < 5; day++) {
      //System.out.println("Day: " + day);
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
        //System.out.println(startMinute);
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
              if (shouldSwitch(shiftConflict, curAvail, true)) {
                switchShift(schedule, shiftConflict, curAvail, day, startMinute);
              } else {
                curAvail.inSchedule = false;
                shiftConflict.conflicts.add(curAvail);
              }
            }
          }
        } else {
          Availability availabilityWithLeastEmployeeWorked = getAvailabilityWithLeastEmployeeWorked(
              availables);
          if (!shiftAtMinute(schedule, day, startMinute)) {
            if (availabilityWithLeastEmployeeWorked.inSchedule) {
              Shift toExtend = tryToExtendShift(schedule, startMinute,
                  day);
              if (toExtend != null) {
                availables.remove(toExtend.associatedAvailability);
                toExtend.conflicts.addAll(availables);
              }
            } else if (canShiftBeMoved(schedule, availabilityWithLeastEmployeeWorked, day,
                startMinute)) {
              moveShift(schedule, availabilityWithLeastEmployeeWorked, day, startMinute);
              availables.remove(schedule.get(schedule.size() - 1).associatedAvailability);
              schedule.get(schedule.size() - 1).conflicts.addAll(availables);
            } else {
              Shift toSchedule = scheduleShift(schedule, availabilityWithLeastEmployeeWorked, day,
                  startMinute);
              availables.remove(availabilityWithLeastEmployeeWorked);
              toSchedule.conflicts.addAll(availables);
            }
          } else {
            Shift shiftConflict = shiftConflictsWithSchedule(schedule,
                availabilityWithLeastEmployeeWorked);
            if (shiftConflict.maxEnding >= availabilityWithLeastEmployeeWorked.endMinute) {
              if (shouldSwitch(shiftConflict, availabilityWithLeastEmployeeWorked, false)) {
                Shift toSchedule = switchShift(schedule, shiftConflict,
                    availabilityWithLeastEmployeeWorked, day,
                    startMinute);
                availables.remove(toSchedule.associatedAvailability);
                toSchedule.conflicts.addAll(availables);
              } else { // if we shouldn't switch it
                availabilityWithLeastEmployeeWorked.inSchedule = false;
                availables.remove(shiftConflict.associatedAvailability);
                shiftConflict.conflicts.addAll(availables);
              }
            } else {
              if (shouldSwitch(shiftConflict, availabilityWithLeastEmployeeWorked, true)) {
                Shift toSchedule = switchShift(schedule, shiftConflict,
                    availabilityWithLeastEmployeeWorked, day,
                    startMinute);
                availables.remove(toSchedule.associatedAvailability);
                toSchedule.conflicts.addAll(availables);
              } else {
                availabilityWithLeastEmployeeWorked.inSchedule = false;
                availables.remove(shiftConflict.associatedAvailability);
                shiftConflict.conflicts.addAll(availables);
              }
            }
          }
        }
      }
      //System.out.println();
    }
    return schedule;
  }

  /**
   * Prints the given schedule.
   *
   * @param schedule the given schedule to print
   */
  public static String printSchedule(List<Shift> schedule) {
    StringBuilder sb = new StringBuilder();
    for (Shift shift : schedule) {
      sb.append(
          "\nDay: " + shift.day + " -- Start: " + shift.startMinute / 60 + ":"
              + shift.startMinute % 60 + " -- End: " + shift.endMinute / 60 + ":"
              + shift.endMinute % 60
              + " -- Employee: " + shift.employee.getFirstName() + " " + shift.employee
              .getLastName() + " -- Minutes worked: " + shift.employee.minutesWorked);
    }
    return sb.toString().substring(1);
  }

  /**
   * Moves the last shift in schedule in order to accommodate another shift made from availability
   * before.
   *
   * @param schedule the schedule of shifts so far
   * @param availability the availability to form a new shift from
   * @param day the day to schedule this new shift
   * @param startMinute the minute we are currently scheduling for
   */
  private static void moveShift(ArrayList<Shift> schedule, Availability availability, int day,
      int startMinute) {
    Shift lastShift = schedule.remove(schedule.size() - 1);
    int toMove = lastShift.startMinute - availability.startMinute;
    lastShift.startMinute += toMove;
    lastShift.endMinute += toMove;
    Shift newShift = new Shift(0, day, availability.startMinute, lastShift.startMinute,
        availability.employee, "Front desk");
    schedule.add(newShift);
    schedule.add(lastShift);
    availability.inSchedule = true;
    newShift.associatedAvailability = availability;
    availability.scheduledShifts.add(newShift);
    newShift.maxEnding = Math.min(availability.endMinute, availability.startMinute + 300);
    newShift.conflicts.add(lastShift.associatedAvailability);
    // TODO make sure this doesn't go over the end time
    availability.employee.minutesWorked += (newShift.endMinute - newShift.startMinute);
  }

  /**
   * If there is a shift in schedule, is it more optimal to move it in order to schedule the given
   * availability before it?
   *
   * @param schedule the current shifts so far
   * @param availability the availability that should be scheduled next
   * @param day the day to schedule
   * @param startMinute the minute that we are currently scheduling for
   */
  public static boolean canShiftBeMoved(List<Shift> schedule, Availability availability, int day,
      int startMinute) {
    if (!schedule.isEmpty()) {
      Shift lastShift = schedule.get(schedule.size() - 1);
      if (lastShift.day == day && lastShift.endMinute == startMinute) {
        if (schedule.size() >= 2
            && schedule.get(schedule.size() - 2).day == day
            && schedule.get(schedule.size() - 2).endMinute > availability.startMinute) {
          return false;
        } else {
          return (lastShift.startMinute
              - availability.startMinute + (
              lastShift.associatedAvailability.endMinute - lastShift.endMinute) >= 120);
        }
      }
      return false;
    }
    return false;
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
    int index;
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
            && schedule.get(i - 1).endMinute == potentialStartTime
            && potentialEndTime - schedule.get(i - 1).startMinute > 300) {
          continue;
        }
        if (i < schedule.size() - 1 && schedule.get(i).day == schedule.get(i + 1).day
            && schedule.get(i + 1).employee == schedule.get(i).conflicts
            .get(0).employee
            && schedule.get(i + 1).startMinute == potentialEndTime
            && schedule.get(i + 1).endMinute - potentialStartTime > 300) {
          continue;
        }

        adjustShifts(schedule.get(i));

        if (shouldSwitch(schedule.get(i), schedule.get(i).conflicts.get(0), potentialStartTime,
            false)) {
          switchShift(schedule, schedule.get(i), schedule.get(i).conflicts.get(0),
              schedule.get(i).day, potentialStartTime, potentialEndTime);
        }
      }
    }
  }

  /**
   * Finds the potential start time and end time that a new
   *
   * @param schedule the shifts scheduled so far
   * @param index the index of the
   * @return an array containing the potential start time and the potential end time
   */
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

  /**
   * If permitting, extend the last shift to include startMinute on the current day. This extends
   * the shift for 15 minutes.
   *
   * @param schedule the shifts scheduled so far
   * @param startMinute the minute to try and extend if possible
   * @param day the day to extend the shift
   */
  private static Shift tryToExtendShift(ArrayList<Shift> schedule,
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
    return toExtend;
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

  /**
   * Does the given availability conflict with the current schedule, given the start minute of the
   * potential new shift?
   *
   * @param schedule the shifts scheduled so far
   * @param availability the availability to check against the schedule
   * @param startMinute the starting minute to check for the conflict
   */
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
