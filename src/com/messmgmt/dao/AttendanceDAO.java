package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Attendance;
import com.messmgmt.model.AttendanceSummary;

import java.time.LocalDate;
import java.util.List;

public class AttendanceDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public boolean markAttendance(String studentId, LocalDate date, String mealType, String status) {
        return manager.markAttendance(studentId, date, mealType, status);
    }

    public boolean saveBatchAttendance(List<Attendance> list) {
        return manager.saveBatchAttendance(list);
    }

    public List<Attendance> getAttendanceByDateAndMeal(LocalDate date, String mealType) {
        return manager.getAttendanceByDateAndMeal(date, mealType);
    }

    public List<Attendance> getAttendanceByStudentAndMonth(String studentId, String yearMonth) {
        return manager.getAttendanceByStudentAndMonth(studentId, yearMonth);
    }

    public int countStudentPresentMeals(String studentId, String yearMonth) {
        return manager.countStudentPresentMeals(studentId, yearMonth);
    }

    public int getTodayPresentCount(LocalDate date) {
        return manager.getTodayPresentCount(date);
    }

    public List<AttendanceSummary> getMonthlyAttendanceSummary(String yearMonth) {
        return manager.getMonthlyAttendanceSummary(yearMonth);
    }
}
