package com.messmgmt;

import com.messmgmt.config.FirebaseConfig;
import com.messmgmt.config.FirebaseManager;
import com.messmgmt.dao.*;
import com.messmgmt.model.*;
import com.messmgmt.util.ReportGenerator;
import com.messmgmt.util.ValidationUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class MainConsole {

    private static final Scanner scanner = new Scanner(System.in);
    private static final AdminDAO adminDAO = new AdminDAO();
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final MenuDAO menuDAO = new MenuDAO();
    private static final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private static final BillingDAO billingDAO = new BillingDAO();
    private static final ComplaintDAO complaintDAO = new ComplaintDAO();

    private static Admin loggedInAdmin = null;

    private static String readLine() {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1).trim();
            }
            return line;
        }
        System.exit(0);
        return "";
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    WELCOME TO MESS MANAGEMENT SYSTEM (CLI)");
        System.out.println("=================================================");

        // Test Firebase connection
        System.out.println("[Connecting to Firebase Cloud Realtime Database...]");
        String testMsg = FirebaseManager.getInstance().getFirebaseClient().testConnectionWithMessage();
        System.out.println(testMsg);

        // Login loop
        while (loggedInAdmin == null) {
            System.out.println("\n--- ADMIN LOGIN ---");
            System.out.print("Username (default: admin): ");
            String user = readLine();
            System.out.print("Password (default: admin123): ");
            String pass = readLine();

            loggedInAdmin = adminDAO.authenticate(user, pass);
            if (loggedInAdmin != null) {
                System.out.println("\n[+] Welcome back, " + loggedInAdmin.getFullName() + "!");
            } else {
                System.out.println("[-] Invalid credentials. Please try again.");
                System.out.print("Options: (1) Retry, (2) Configure Firebase Settings, (3) Exit: ");
                String opt = readLine();
                if ("2".equals(opt)) {
                    configureFirebaseSettings();
                } else if ("3".equals(opt)) {
                    System.out.println("Exiting system. Goodbye!");
                    return;
                }
            }
        }

        // Main Menu
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Enter your choice: ");
            String choice = readLine();

            switch (choice) {
                case "1" -> handleStudentMenu();
                case "2" -> handleMenuManagement();
                case "3" -> handleAttendanceMenu();
                case "4" -> handleBillingMenu();
                case "5" -> handleComplaintMenu();
                case "6" -> handleReportsMenu();
                case "7" -> configureFirebaseSettings();
                case "0" -> {
                    System.out.println("\nLogging out and exiting. Have a great day!");
                    running = false;
                }
                default -> System.out.println("[!] Invalid choice. Please select from 0 to 7.");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n=================================================");
        System.out.println("            MESS MANAGEMENT DASHBOARD");
        System.out.println(" Logged In: " + loggedInAdmin.getFullName() + " (" + loggedInAdmin.getUsername() + ")");
        System.out.println("=================================================");
        System.out.println(" 1. Student Management (Add, Update, Delete, View)");
        System.out.println(" 2. Menu Management (Weekly Schedule, Items)");
        System.out.println(" 3. Attendance Management (Daily Meal Marking)");
        System.out.println(" 4. Fee & Billing Management (Generate, Pay, Invoice)");
        System.out.println(" 5. Complaint & Feedback Management");
        System.out.println(" 6. Reports & Statement Exports");
        System.out.println(" 7. Firebase Cloud Configuration & Sync");
        System.out.println(" 0. Exit System");
        System.out.println("=================================================");
    }

    // =========================================================
    // 1. STUDENT MANAGEMENT
    // =========================================================
    private static void handleStudentMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- STUDENT MANAGEMENT ---");
            System.out.println("1. View All Students");
            System.out.println("2. Search Student (by ID, Name, Room)");
            System.out.println("3. Register New Student");
            System.out.println("4. Update Student Details");
            System.out.println("5. Delete Student");
            System.out.println("0. Back to Main Menu");
            System.out.print("Select an option: ");
            String opt = readLine();

            switch (opt) {
                case "1" -> {
                    List<Student> list = studentDAO.getAllStudents();
                    printStudentTable(list);
                }
                case "2" -> {
                    System.out.print("Enter search keyword: ");
                    String q = readLine();
                    List<Student> list = studentDAO.searchStudents(q);
                    printStudentTable(list);
                }
                case "3" -> {
                    System.out.println("\n[Register New Student]");
                    System.out.print("Enter Student ID (e.g. STU107): ");
                    String id = readLine();
                    if (!ValidationUtil.isValidStudentId(id)) {
                        System.out.println("[-] Invalid ID format (must be 3-20 alphanumeric chars).");
                        break;
                    }
                    if (studentDAO.exists(id)) {
                        System.out.println("[-] Student with ID '" + id + "' already exists!");
                        break;
                    }

                    System.out.print("Enter Full Name: ");
                    String name = readLine();
                    if (!ValidationUtil.isValidName(name)) {
                        System.out.println("[-] Name must be between 2 and 100 characters.");
                        break;
                    }

                    System.out.print("Enter Room Number: ");
                    String room = readLine();
                    if (!ValidationUtil.isValidRoomNo(room)) {
                        System.out.println("[-] Room number cannot be empty.");
                        break;
                    }

                    System.out.print("Enter Contact Number: ");
                    String contact = readLine();
                    if (!ValidationUtil.isValidContact(contact)) {
                        System.out.println("[-] Invalid contact number format (10-15 digits).");
                        break;
                    }

                    System.out.print("Mess Status (Active / Inactive) [Default Active]: ");
                    String status = readLine();
                    if (status.isEmpty()) status = "Active";

                    Student s = new Student(id, name, room, contact, status);
                    if (studentDAO.addStudent(s)) {
                        System.out.println("[+] Student registered successfully!");
                    } else {
                        System.out.println("[-] Failed to register student.");
                    }
                }
                case "4" -> {
                    System.out.print("Enter Student ID to update: ");
                    String id = readLine();
                    Student s = studentDAO.getStudentById(id);
                    if (s == null) {
                        System.out.println("[-] Student not found.");
                        break;
                    }
                    System.out.println("Current details: " + s.getName() + " | Room: " + s.getRoomNo() + " | Contact: " + s.getContact() + " | Status: " + s.getStatus());
                    
                    System.out.print("New Name (press Enter to keep '" + s.getName() + "'): ");
                    String name = readLine();
                    if (!name.isEmpty()) s.setName(name);

                    System.out.print("New Room No (press Enter to keep '" + s.getRoomNo() + "'): ");
                    String room = readLine();
                    if (!room.isEmpty()) s.setRoomNo(room);

                    System.out.print("New Contact (press Enter to keep '" + s.getContact() + "'): ");
                    String contact = readLine();
                    if (!contact.isEmpty()) s.setContact(contact);

                    System.out.print("New Status (Active/Inactive, press Enter to keep '" + s.getStatus() + "'): ");
                    String status = readLine();
                    if (!status.isEmpty()) s.setStatus(status);

                    if (studentDAO.updateStudent(s)) {
                        System.out.println("[+] Student details updated successfully!");
                    } else {
                        System.out.println("[-] Failed to update student.");
                    }
                }
                case "5" -> {
                    System.out.print("Enter Student ID to delete: ");
                    String id = readLine();
                    System.out.print("Are you sure you want to delete student " + id + " and related records? (y/n): ");
                    String conf = readLine();
                    if ("y".equalsIgnoreCase(conf)) {
                        if (studentDAO.deleteStudent(id)) {
                            System.out.println("[+] Student deleted successfully.");
                        } else {
                            System.out.println("[-] Failed to delete student.");
                        }
                    }
                }
                case "0" -> back = true;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static void printStudentTable(List<Student> list) {
        if (list.isEmpty()) {
            System.out.println("[i] No students found.");
            return;
        }
        System.out.printf("%-10s | %-22s | %-10s | %-15s | %-8s%n",
                "ID", "Name", "Room No", "Contact", "Status");
        System.out.println("-".repeat(72));
        for (Student s : list) {
            System.out.printf("%-10s | %-22s | %-10s | %-15s | %-8s%n",
                    s.getStudentId(), s.getName(), s.getRoomNo(), s.getContact(), s.getStatus());
        }
        System.out.println("Total: " + list.size() + " student(s)");
    }

    // =========================================================
    // 2. MENU MANAGEMENT
    // =========================================================
    private static void handleMenuManagement() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- WEEKLY MENU MANAGEMENT ---");
            System.out.println("1. View Full Weekly Menu");
            System.out.println("2. View Menu for a Specific Day");
            System.out.println("3. Set / Update Menu for Day & Meal");
            System.out.println("4. Delete a Menu Entry");
            System.out.println("0. Back to Main Menu");
            System.out.print("Select an option: ");
            String opt = readLine();

            switch (opt) {
                case "1" -> {
                    List<MenuItem> items = menuDAO.getAllMenuItems();
                    printMenuTable(items);
                }
                case "2" -> {
                    System.out.print("Enter Day (Monday, Tuesday, etc.): ");
                    String day = readLine();
                    List<MenuItem> items = menuDAO.getMenuByDay(day);
                    printMenuTable(items);
                }
                case "3" -> {
                    System.out.print("Enter Day (Monday to Sunday): ");
                    String day = capitalize(readLine());
                    System.out.print("Enter Meal Type (Breakfast, Lunch, Dinner): ");
                    String meal = capitalize(readLine());
                    System.out.print("Enter Menu Dishes/Items: ");
                    String items = readLine();

                    if (items.isEmpty()) {
                        System.out.println("[-] Menu items description cannot be empty.");
                        break;
                    }

                    if (menuDAO.saveOrUpdateMenu(day, meal, items)) {
                        System.out.println("[+] Menu updated successfully for " + day + " [" + meal + "]!");
                    } else {
                        System.out.println("[-] Failed to update menu.");
                    }
                }
                case "4" -> {
                    System.out.print("Enter Menu ID to delete: ");
                    try {
                        int id = Integer.parseInt(readLine());
                        if (menuDAO.deleteMenuItem(id)) {
                            System.out.println("[+] Menu item deleted successfully.");
                        } else {
                            System.out.println("[-] Failed to delete menu item.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[-] Invalid ID.");
                    }
                }
                case "0" -> back = true;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static void printMenuTable(List<MenuItem> items) {
        if (items.isEmpty()) {
            System.out.println("[i] No menu items configured.");
            return;
        }
        System.out.printf("%-5s | %-10s | %-10s | %-50s%n", "ID", "Day", "Meal", "Items");
        System.out.println("-".repeat(80));
        for (MenuItem m : items) {
            System.out.printf("%-5d | %-10s | %-10s | %-50s%n",
                    m.getMenuId(), m.getDay(), m.getMealType(), m.getItems());
        }
    }

    // =========================================================
    // 3. ATTENDANCE MANAGEMENT
    // =========================================================
    private static void handleAttendanceMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- DAILY ATTENDANCE MANAGEMENT ---");
            System.out.println("1. Mark Single Student Attendance");
            System.out.println("2. Batch Mark Meal Attendance for All Students");
            System.out.println("3. View Attendance for Date & Meal");
            System.out.println("4. View Attendance History of a Student");
            System.out.println("5. Monthly Attendance Summary Report");
            System.out.println("0. Back to Main Menu");
            System.out.print("Select an option: ");
            String opt = readLine();

            switch (opt) {
                case "1" -> {
                    System.out.print("Student ID: ");
                    String id = readLine();
                    if (!studentDAO.exists(id)) {
                        System.out.println("[-] Student does not exist.");
                        break;
                    }
                    System.out.print("Date (YYYY-MM-DD, press Enter for Today [" + LocalDate.now() + "]): ");
                    String dtStr = readLine();
                    LocalDate date = dtStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dtStr);

                    System.out.print("Meal Type (Breakfast / Lunch / Dinner): ");
                    String meal = capitalize(readLine());

                    System.out.print("Status (1 for Present, 2 for Absent) [Default Present]: ");
                    String stOpt = readLine();
                    String status = "2".equals(stOpt) ? "Absent" : "Present";

                    if (attendanceDAO.markAttendance(id, date, meal, status)) {
                        System.out.println("[+] Attendance marked: " + id + " -> " + meal + " = " + status);
                    } else {
                        System.out.println("[-] Failed to mark attendance.");
                    }
                }
                case "2" -> {
                    System.out.print("Date (YYYY-MM-DD, press Enter for Today [" + LocalDate.now() + "]): ");
                    String dtStr = readLine();
                    LocalDate date = dtStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dtStr);

                    System.out.print("Meal Type (Breakfast / Lunch / Dinner): ");
                    String meal = capitalize(readLine());

                    List<Student> students = studentDAO.getActiveStudents();
                    if (students.isEmpty()) {
                        System.out.println("[-] No active students found.");
                        break;
                    }

                    System.out.println("\nMark attendance for " + students.size() + " active students on " + date + " [" + meal + "].");
                    System.out.print("Mark ALL as Present? (y/n): ");
                    String allPresent = readLine();

                    if ("y".equalsIgnoreCase(allPresent)) {
                        for (Student s : students) {
                            attendanceDAO.markAttendance(s.getStudentId(), date, meal, "Present");
                        }
                        System.out.println("[+] All active students marked Present for " + meal + "!");
                    } else {
                        for (Student s : students) {
                            System.out.print(s.getStudentId() + " (" + s.getName() + ") - [P]resent / [A]bsent (default P): ");
                            String in = readLine();
                            String st = "a".equalsIgnoreCase(in) ? "Absent" : "Present";
                            attendanceDAO.markAttendance(s.getStudentId(), date, meal, st);
                        }
                        System.out.println("[+] Attendance batch recording complete!");
                    }
                }
                case "3" -> {
                    System.out.print("Date (YYYY-MM-DD, press Enter for Today [" + LocalDate.now() + "]): ");
                    String dtStr = readLine();
                    LocalDate date = dtStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dtStr);

                    System.out.print("Meal Type (Breakfast / Lunch / Dinner): ");
                    String meal = capitalize(readLine());

                    List<Attendance> list = attendanceDAO.getAttendanceByDateAndMeal(date, meal);
                    if (list.isEmpty()) {
                        System.out.println("[i] No attendance records for " + date + " [" + meal + "].");
                    } else {
                        System.out.printf("%-10s | %-22s | %-10s | %-8s%n", "Student ID", "Name", "Room", "Status");
                        System.out.println("-".repeat(56));
                        for (Attendance a : list) {
                            System.out.printf("%-10s | %-22s | %-10s | %-8s%n",
                                    a.getStudentId(), a.getStudentName(), a.getRoomNo(), a.getStatus());
                        }
                    }
                }
                case "4" -> {
                    System.out.print("Student ID: ");
                    String id = readLine();
                    System.out.print("Month (YYYY-MM, e.g. 2026-09): ");
                    String month = readLine();

                    List<Attendance> list = attendanceDAO.getAttendanceByStudentAndMonth(id, month);
                    if (list.isEmpty()) {
                        System.out.println("[i] No records found for " + id + " in " + month);
                    } else {
                        System.out.printf("%-12s | %-10s | %-8s%n", "Date", "Meal Type", "Status");
                        System.out.println("-".repeat(34));
                        int present = 0;
                        for (Attendance a : list) {
                            System.out.printf("%-12s | %-10s | %-8s%n", a.getDate(), a.getMealType(), a.getStatus());
                            if (a.isPresent()) present++;
                        }
                        System.out.println("Total Present Meals: " + present + " / " + list.size());
                    }
                }
                case "5" -> {
                    System.out.print("Month (YYYY-MM, e.g. 2026-09): ");
                    String month = readLine();
                    List<AttendanceSummary> summaries = attendanceDAO.getMonthlyAttendanceSummary(month);
                    System.out.println(ReportGenerator.generateAttendanceSummaryReport(month, summaries));
                }
                case "0" -> back = true;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    // =========================================================
    // 4. FEE & BILLING MANAGEMENT
    // =========================================================
    private static void handleBillingMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- FEE & BILLING MANAGEMENT ---");
            System.out.println("1. Calculate & Generate Monthly Bills for All Students");
            System.out.println("2. Calculate & Generate Bill for Single Student");
            System.out.println("3. View All Bills / Filter by Month");
            System.out.println("4. Mark Bill as Paid / Unpaid");
            System.out.println("5. View & Print Student Invoice Receipt");
            System.out.println("6. View Monthly Revenue Summary");
            System.out.println("0. Back to Main Menu");
            System.out.print("Select an option: ");
            String opt = readLine();

            switch (opt) {
                case "1" -> {
                    System.out.print("Enter Billing Month (YYYY-MM, e.g. 2026-09): ");
                    String month = readLine();
                    if (!ValidationUtil.isValidMonth(month)) {
                        System.out.println("[-] Invalid month format. Use YYYY-MM.");
                        break;
                    }
                    System.out.print("Per-Meal Rate in INR [Default: " + FirebaseConfig.getDefaultMealRate() + "]: ");
                    String rateStr = readLine();
                    double rate = rateStr.isEmpty() ? FirebaseConfig.getDefaultMealRate() : Double.parseDouble(rateStr);

                    int count = billingDAO.generateBillsForMonth(month, rate);
                    System.out.println("[+] Generated " + count + " student bills for month " + month + " @ INR " + rate + " per meal!");
                }
                case "2" -> {
                    System.out.print("Student ID: ");
                    String id = readLine();
                    if (!studentDAO.exists(id)) {
                        System.out.println("[-] Student does not exist.");
                        break;
                    }
                    System.out.print("Enter Billing Month (YYYY-MM): ");
                    String month = readLine();
                    System.out.print("Per-Meal Rate in INR [Default: " + FirebaseConfig.getDefaultMealRate() + "]: ");
                    String rateStr = readLine();
                    double rate = rateStr.isEmpty() ? FirebaseConfig.getDefaultMealRate() : Double.parseDouble(rateStr);

                    if (billingDAO.generateBillForStudent(id, month, rate)) {
                        Bill b = billingDAO.getBillByStudentAndMonth(id, month);
                        System.out.println("[+] Bill generated successfully!");
                        System.out.println("Meals: " + b.getTotalMeals() + " × INR " + b.getPerMealRate() + " = Total: INR " + b.getAmount());
                    } else {
                        System.out.println("[-] Failed to generate bill.");
                    }
                }
                case "3" -> {
                    System.out.print("Filter by Month (YYYY-MM, press Enter for all): ");
                    String month = readLine();
                    List<Bill> list = month.isEmpty() ? billingDAO.getAllBills() : billingDAO.getBillsByMonth(month);
                    printBillingTable(list);
                }
                case "4" -> {
                    System.out.print("Enter Bill ID to update: ");
                    try {
                        int billId = Integer.parseInt(readLine());
                        Bill b = billingDAO.getBillById(billId);
                        if (b == null) {
                            System.out.println("[-] Bill not found.");
                            break;
                        }
                        System.out.println("Current status: " + b.getPaidStatus() + " | Amount: INR " + b.getAmount());
                        System.out.print("Update to (1 for Paid, 2 for Unpaid): ");
                        String st = readLine();
                        String newStatus = "1".equals(st) ? "Paid" : "Unpaid";
                        LocalDate paidDate = "Paid".equals(newStatus) ? LocalDate.now() : null;

                        if (billingDAO.updatePaymentStatus(billId, newStatus, paidDate)) {
                            System.out.println("[+] Bill payment status updated to " + newStatus + "!");
                        } else {
                            System.out.println("[-] Failed to update payment status.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[-] Invalid ID.");
                    }
                }
                case "5" -> {
                    System.out.print("Enter Bill ID: ");
                    try {
                        int billId = Integer.parseInt(readLine());
                        Bill b = billingDAO.getBillById(billId);
                        if (b == null) {
                            System.out.println("[-] Bill not found.");
                            break;
                        }
                        Student s = studentDAO.getStudentById(b.getStudentId());
                        List<Attendance> atts = attendanceDAO.getAttendanceByStudentAndMonth(b.getStudentId(), b.getMonth());
                        String invoice = ReportGenerator.generateStudentInvoice(b, s, atts);
                        System.out.println("\n" + invoice);

                        System.out.print("Save invoice to text file? (y/n): ");
                        String save = readLine();
                        if ("y".equalsIgnoreCase(save)) {
                            String filename = "Invoice_" + b.getStudentId() + "_" + b.getMonth() + ".txt";
                            try (FileWriter fw = new FileWriter(filename)) {
                                fw.write(invoice);
                                System.out.println("[+] Saved invoice to " + filename);
                            } catch (IOException e) {
                                System.out.println("[-] Error saving file: " + e.getMessage());
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[-] Invalid ID.");
                    }
                }
                case "6" -> {
                    System.out.print("Enter Month (YYYY-MM): ");
                    String month = readLine();
                    List<Bill> bills = billingDAO.getBillsByMonth(month);
                    System.out.println(ReportGenerator.generateMonthlyBillingReport(month, bills));
                }
                case "0" -> back = true;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static void printBillingTable(List<Bill> list) {
        if (list.isEmpty()) {
            System.out.println("[i] No bills found.");
            return;
        }
        System.out.printf("%-6s | %-10s | %-20s | %-8s | %-6s | %-10s | %-8s | %-10s%n",
                "BillID", "StudentID", "Name", "Month", "Meals", "Amount(INR)", "Status", "Paid Date");
        System.out.println("-".repeat(92));
        for (Bill b : list) {
            System.out.printf("%-6d | %-10s | %-20s | %-8s | %-6d | %10.2f | %-8s | %-10s%n",
                    b.getBillId(),
                    b.getStudentId(),
                    b.getStudentName() != null ? b.getStudentName() : "",
                    b.getMonth(),
                    b.getTotalMeals(),
                    b.getAmount(),
                    b.getPaidStatus(),
                    b.getPaidDate() != null ? b.getPaidDate().toString() : "-");
        }
    }

    // =========================================================
    // 5. COMPLAINT & FEEDBACK MANAGEMENT
    // =========================================================
    private static void handleComplaintMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- COMPLAINT & FEEDBACK MODULE ---");
            System.out.println("1. View All Complaints");
            System.out.println("2. Filter Complaints by Status (Pending / Resolved)");
            System.out.println("3. Submit New Complaint / Feedback");
            System.out.println("4. Update Complaint Status & Resolution Note");
            System.out.println("5. Delete Complaint");
            System.out.println("0. Back to Main Menu");
            System.out.print("Select an option: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1" -> {
                    List<Complaint> list = complaintDAO.getAllComplaints();
                    printComplaintTable(list);
                }
                case "2" -> {
                    System.out.print("Enter status (Pending / In Progress / Resolved): ");
                    String st = scanner.nextLine().trim();
                    List<Complaint> list = complaintDAO.getComplaintsByStatus(st);
                    printComplaintTable(list);
                }
                case "3" -> {
                    System.out.print("Student ID: ");
                    String id = scanner.nextLine().trim();
                    if (!studentDAO.exists(id)) {
                        System.out.println("[-] Student ID does not exist.");
                        break;
                    }
                    System.out.println("Category options: (1) Food Quality, (2) Cleanliness, (3) Service, (4) Other");
                    System.out.print("Select category [1-4]: ");
                    String catOpt = scanner.nextLine().trim();
                    String cat = switch (catOpt) {
                        case "1" -> "Food Quality";
                        case "2" -> "Cleanliness";
                        case "3" -> "Service";
                        default -> "Other";
                    };
                    System.out.print("Enter Complaint / Feedback Description: ");
                    String msg = scanner.nextLine().trim();
                    if (msg.isEmpty()) {
                        System.out.println("[-] Message cannot be empty.");
                        break;
                    }
                    Complaint c = new Complaint(id, cat, msg);
                    if (complaintDAO.addComplaint(c)) {
                        System.out.println("[+] Complaint submitted successfully (Status: Pending).");
                    } else {
                        System.out.println("[-] Failed to submit complaint.");
                    }
                }
                case "4" -> {
                    System.out.print("Enter Complaint ID: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        System.out.println("New Status: (1) Pending, (2) In Progress, (3) Resolved");
                        System.out.print("Select [1-3]: ");
                        String stOpt = scanner.nextLine().trim();
                        String st = switch (stOpt) {
                            case "2" -> "In Progress";
                            case "3" -> "Resolved";
                            default -> "Pending";
                        };
                        System.out.print("Enter Resolution Note / Action Taken: ");
                        String note = scanner.nextLine().trim();

                        if (complaintDAO.updateStatusAndNote(id, st, note)) {
                            System.out.println("[+] Complaint updated successfully!");
                        } else {
                            System.out.println("[-] Failed to update complaint.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[-] Invalid ID.");
                    }
                }
                case "5" -> {
                    System.out.print("Enter Complaint ID to delete: ");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        if (complaintDAO.deleteComplaint(id)) {
                            System.out.println("[+] Complaint deleted.");
                        } else {
                            System.out.println("[-] Failed to delete complaint.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("[-] Invalid ID.");
                    }
                }
                case "0" -> back = true;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static void printComplaintTable(List<Complaint> list) {
        if (list.isEmpty()) {
            System.out.println("[i] No complaints found.");
            return;
        }
        System.out.printf("%-5s | %-10s | %-15s | %-12s | %-12s | %-30s%n",
                "ID", "StudentID", "Category", "Status", "Date", "Message");
        System.out.println("-".repeat(95));
        for (Complaint c : list) {
            System.out.printf("%-5d | %-10s | %-15s | %-12s | %-12s | %-30s%n",
                    c.getComplaintId(),
                    c.getStudentId(),
                    c.getCategory(),
                    c.getStatus(),
                    c.getCreatedAt(),
                    c.getMessage().length() > 30 ? c.getMessage().substring(0, 27) + "..." : c.getMessage());
            if (c.getResolutionNote() != null && !c.getResolutionNote().isEmpty()) {
                System.out.println("      └─ Resolution Note: " + c.getResolutionNote());
            }
        }
    }

    // =========================================================
    // 6. REPORTS & EXPORTS
    // =========================================================
    private static void handleReportsMenu() {
        System.out.println("\n--- REPORT & AUDIT GENERATOR ---");
        System.out.println("1. Monthly Revenue & Collection Statement");
        System.out.println("2. Monthly Attendance Audit Summary");
        System.out.print("Select report type [1-2]: ");
        String type = scanner.nextLine().trim();

        System.out.print("Enter Month (YYYY-MM, e.g. 2026-09): ");
        String month = scanner.nextLine().trim();

        String report = "";
        String filename = "";

        if ("1".equals(type)) {
            List<Bill> bills = billingDAO.getBillsByMonth(month);
            report = ReportGenerator.generateMonthlyBillingReport(month, bills);
            filename = "Mess_Revenue_Report_" + month + ".txt";
        } else if ("2".equals(type)) {
            List<AttendanceSummary> summaries = attendanceDAO.getMonthlyAttendanceSummary(month);
            report = ReportGenerator.generateAttendanceSummaryReport(month, summaries);
            filename = "Mess_Attendance_Report_" + month + ".txt";
        } else {
            System.out.println("[-] Invalid selection.");
            return;
        }

        System.out.println("\n" + report);
        System.out.print("Export report to disk (" + filename + ")? (y/n): ");
        String exp = scanner.nextLine().trim();
        if ("y".equalsIgnoreCase(exp)) {
            try (FileWriter fw = new FileWriter(filename)) {
                fw.write(report);
                System.out.println("[+] Report successfully exported to " + filename);
            } catch (IOException e) {
                System.out.println("[-] Export failed: " + e.getMessage());
            }
        }
    }

    // =========================================================
    // 7. FIREBASE CONFIGURATION & SYNC
    // =========================================================
    private static void configureFirebaseSettings() {
        System.out.println("\n--- FIREBASE REALTIME DATABASE CONFIGURATION ---");
        System.out.println("Current URL: " + FirebaseConfig.getDbUrl());
        String token = FirebaseConfig.getAuthToken();
        System.out.println("Auth Token: " + (token.isEmpty() ? "(None / Public Rules)" : "********"));
        System.out.println("Connection Test: " + FirebaseManager.getInstance().getFirebaseClient().testConnectionWithMessage());

        boolean inSettings = true;
        while (inSettings) {
            System.out.println("\n[Firebase Options]");
            System.out.println("1. Update Firebase Database URL & Auth Token");
            System.out.println("2. Test Connection to Firebase");
            System.out.println("3. Push All Local Data to Firebase Cloud");
            System.out.println("4. Pull All Data from Firebase Cloud to Local");
            System.out.println("0. Return to Main Menu");
            System.out.print("Select option: ");
            String opt = readLine();

            switch (opt) {
                case "1" -> {
                    System.out.print("Enter Firebase Realtime Database URL [" + FirebaseConfig.getDbUrl() + "]: ");
                    String url = readLine();
                    if (url.isEmpty()) url = FirebaseConfig.getDbUrl();

                    System.out.print("Enter Auth Token / Secret (press Enter to keep existing): ");
                    String tok = readLine();
                    if (tok.isEmpty()) tok = FirebaseConfig.getAuthToken();

                    System.out.print("Default Meal Rate (INR) [" + FirebaseConfig.getDefaultMealRate() + "]: ");
                    String rateStr = readLine();
                    double rate = rateStr.isEmpty() ? FirebaseConfig.getDefaultMealRate() : Double.parseDouble(rateStr);

                    FirebaseConfig.updateConfig(url, tok, rate);
                    System.out.println("[+] Firebase settings saved and applied!");
                    System.out.println("[Testing new connection...]");
                    System.out.println(FirebaseManager.getInstance().getFirebaseClient().testConnectionWithMessage());
                }
                case "2" -> {
                    System.out.println("[Testing connection...]");
                    System.out.println(FirebaseManager.getInstance().getFirebaseClient().testConnectionWithMessage());
                }
                case "3" -> {
                    System.out.print("Are you sure you want to push all local data to Firebase? (y/n): ");
                    if ("y".equalsIgnoreCase(readLine())) {
                        System.out.println(FirebaseManager.getInstance().pushAllToFirebase());
                    }
                }
                case "4" -> {
                    System.out.print("Are you sure you want to pull data from Firebase and overwrite local store? (y/n): ");
                    if ("y".equalsIgnoreCase(readLine())) {
                        System.out.println(FirebaseManager.getInstance().pullAllFromFirebase());
                    }
                }
                case "0" -> inSettings = false;
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
