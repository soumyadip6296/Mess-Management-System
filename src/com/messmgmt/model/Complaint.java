package com.messmgmt.model;

import java.time.LocalDate;

public class Complaint {
    private int complaintId;
    private String studentId;
    private String studentName; // Helper
    private String category;    // Food Quality, Cleanliness, Service, Other
    private String message;
    private String status;      // Pending, In Progress, Resolved
    private String resolutionNote;
    private LocalDate createdAt;

    public Complaint() {
        this.status = "Pending";
        this.createdAt = LocalDate.now();
    }

    public Complaint(String studentId, String category, String message) {
        this.studentId = studentId;
        this.category = category;
        this.message = message;
        this.status = "Pending";
        this.createdAt = LocalDate.now();
    }

    public Complaint(int complaintId, String studentId, String category, String message, String status, String resolutionNote, LocalDate createdAt) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.category = category;
        this.message = message;
        this.status = status;
        this.resolutionNote = resolutionNote;
        this.createdAt = createdAt;
    }

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Complaint #" + complaintId + " [" + category + "] - " + status;
    }
}
