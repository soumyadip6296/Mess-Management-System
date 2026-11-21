package com.messmgmt.model;

import java.time.LocalDate;

public class Attendance {
    private int attendanceId;
    private String studentId;
    private String studentName; // Helper for display
    private String roomNo;      // Helper for display
    private LocalDate date;
    private String mealType;    // Breakfast, Lunch, Dinner
    private String status;      // Present, Absent

    public Attendance() {
        this.status = "Present";
    }

    public Attendance(String studentId, LocalDate date, String mealType, String status) {
        this.studentId = studentId;
        this.date = date;
        this.mealType = mealType;
        this.status = status;
    }

    public Attendance(int attendanceId, String studentId, LocalDate date, String mealType, String status) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.date = date;
        this.mealType = mealType;
        this.status = status;
    }

    public int getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(int attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPresent() {
        return "Present".equalsIgnoreCase(this.status);
    }

    @Override
    public String toString() {
        return date + " [" + mealType + "] " + studentId + ": " + status;
    }
}
