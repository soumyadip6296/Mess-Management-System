package com.messmgmt.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bill {
    private int billId;
    private String studentId;
    private String studentName; // Helper
    private String roomNo;      // Helper
    private String month;       // Format: YYYY-MM
    private int totalMeals;
    private double perMealRate;
    private double amount;
    private String paidStatus;  // "Unpaid", "Paid"
    private LocalDate paidDate;
    private String generatedAt;

    private static final DateTimeFormatter DTM_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Bill() {
        this.paidStatus = "Unpaid";
        this.perMealRate = 50.00;
        this.generatedAt = LocalDateTime.now().format(DTM_FMT);
    }

    public Bill(String studentId, String month, int totalMeals, double perMealRate, double amount, String paidStatus) {
        this.studentId = studentId;
        this.month = month;
        this.totalMeals = totalMeals;
        this.perMealRate = perMealRate;
        this.amount = amount;
        this.paidStatus = paidStatus;
        this.generatedAt = LocalDateTime.now().format(DTM_FMT);
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
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

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getTotalMeals() {
        return totalMeals;
    }

    public void setTotalMeals(int totalMeals) {
        this.totalMeals = totalMeals;
    }

    public double getPerMealRate() {
        return perMealRate;
    }

    public void setPerMealRate(double perMealRate) {
        this.perMealRate = perMealRate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaidStatus() {
        return paidStatus;
    }

    public void setPaidStatus(String paidStatus) {
        this.paidStatus = paidStatus;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public boolean isPaid() {
        return "Paid".equalsIgnoreCase(this.paidStatus);
    }

    @Override
    public String toString() {
        return "Bill #" + billId + " | " + studentId + " (" + month + "): " + totalMeals + " meals @ " + perMealRate + " = " + amount + " [" + paidStatus + "]";
    }
}
