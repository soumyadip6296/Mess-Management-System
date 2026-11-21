package com.messmgmt.model;

public class AttendanceSummary {
    private String studentId;
    private String studentName;
    private String roomNo;
    private String month;
    private int presentCount;
    private int absentCount;
    private int totalMeals;

    public AttendanceSummary(String studentId, String studentName, String roomNo, String month, int presentCount, int absentCount) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomNo = roomNo;
        this.month = month;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalMeals = presentCount + absentCount;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public String getMonth() {
        return month;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public int getTotalMeals() {
        return totalMeals;
    }
}
