package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.MenuItem;

import java.util.List;

public class MenuDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public boolean saveOrUpdateMenu(String day, String mealType, String items) {
        return manager.saveOrUpdateMenu(day, mealType, items);
    }

    public List<MenuItem> getAllMenuItems() {
        return manager.getAllMenuItems();
    }

    public List<MenuItem> getMenuByDay(String day) {
        return manager.getMenuByDay(day);
    }

    public MenuItem getMenuItem(String day, String mealType) {
        return manager.getMenuItem(day, mealType);
    }

    public boolean deleteMenuItem(int menuId) {
        return manager.deleteMenuItem(menuId);
    }
}
