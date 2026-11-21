package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Admin;

import java.util.List;

public class AdminDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public Admin authenticate(String username, String password) {
        return manager.authenticateAdmin(username, password);
    }

    public boolean addAdmin(Admin admin) {
        return manager.addAdmin(admin);
    }

    public boolean changePassword(String username, String newPassword) {
        return manager.changeAdminPassword(username, newPassword);
    }

    public List<Admin> getAllAdmins() {
        return manager.getAllAdmins();
    }
}
