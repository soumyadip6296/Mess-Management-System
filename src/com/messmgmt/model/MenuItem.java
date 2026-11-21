package com.messmgmt.model;

public class MenuItem {
    private int menuId;
    private String day;        // Monday, Tuesday, Wednesday, etc.
    private String mealType;   // Breakfast, Lunch, Dinner
    private String items;      // Comma-separated list or description of dishes

    public MenuItem() {}

    public MenuItem(String day, String mealType, String items) {
        this.day = day;
        this.mealType = mealType;
        this.items = items;
    }

    public MenuItem(int menuId, String day, String mealType, String items) {
        this.menuId = menuId;
        this.day = day;
        this.mealType = mealType;
        this.items = items;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return day + " [" + mealType + "]: " + items;
    }
}
