package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Student;

import java.util.List;

public class StudentDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public boolean addStudent(Student student) {
        return manager.addStudent(student);
    }

    public boolean updateStudent(Student student) {
        return manager.updateStudent(student);
    }

    public boolean deleteStudent(String studentId) {
        return manager.deleteStudent(studentId);
    }

    public Student getStudentById(String studentId) {
        return manager.getStudentById(studentId);
    }

    public List<Student> getAllStudents() {
        return manager.getAllStudents();
    }

    public List<Student> getActiveStudents() {
        return manager.getActiveStudents();
    }

    public List<Student> searchStudents(String query) {
        return manager.searchStudents(query);
    }

    public int getTotalStudentsCount() {
        return manager.getTotalStudentsCount();
    }

    public int getActiveStudentsCount() {
        return manager.getActiveStudentsCount();
    }

    public boolean exists(String studentId) {
        return manager.studentExists(studentId);
    }
}
