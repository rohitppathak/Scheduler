# Scheduler
Resnet Auto Scheduler

In Progress: 

The goal of this program is to make a weekly schedule that schedules employees of ResNet weekly using their availabilities from WhenIWork.

High level overview:
1. Gather all employee information, including availabilities and unavailabilities, and store the information.
2. Schedule front-desk employees.
3. Schedule hardware technicians.
4. Schedule software technicians.

Approach: Use a dynamic programming algorithm to make schedule up to a certain point, and then go back and fix the shift conflicts.
