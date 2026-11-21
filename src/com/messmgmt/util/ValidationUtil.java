package com.messmgmt.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    public static boolean isValidStudentId(String id) {
        return id != null && STUDENT_ID_PATTERN.matcher(id.trim()).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100;
    }

    public static boolean isValidRoomNo(String room) {
        return room != null && !room.trim().isEmpty() && room.trim().length() <= 20;
    }

    public static boolean isValidContact(String contact) {
        return contact != null && CONTACT_PATTERN.matcher(contact.trim()).matches();
    }

    public static boolean isValidMonth(String month) {
        return month != null && MONTH_PATTERN.matcher(month.trim()).matches();
    }

    public static boolean isValidMealRate(double rate) {
        return rate > 0.0 && rate <= 10000.0;
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
