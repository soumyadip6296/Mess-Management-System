package com.messmgmt.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Student {
    private String studentId;
    private String name;
    private String roomNo;
    private String contact;
    private String status; // "Active" or "Inactive"
    private String registeredAt;

    private static final DateTimeFormatter DTM_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Student() {
        this.status = "Active";
        this.registeredAt = LocalDateTime.now().format(DTM_FMT);
    }

    public Student(String studentId, String name, String roomNo, String contact, String status) {
        this.studentId = studentId;
        this.name = name;
        this.roomNo = roomNo;
        this.contact = contact;
        this.status = status;
        this.registeredAt = LocalDateTime.now().format(DTM_FMT);
    }

    public Student(String studentId, String name, String roomNo, String contact, String status, String registeredAt) {
        this.studentId = studentId;
        this.name = name;
        this.roomNo = roomNo;
        this.contact = contact;
        this.status = status;
        this.registeredAt = registeredAt != null ? registeredAt : LocalDateTime.now().format(DTM_FMT);
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(this.status);
    }

    @Override
    public String toString() {
        return studentId + " - " + name + " (Room " + roomNo + ")";
    }
}
