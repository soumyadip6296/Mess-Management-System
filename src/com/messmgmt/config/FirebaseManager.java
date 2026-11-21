package com.messmgmt.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messmgmt.model.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance Firebase Realtime Database Data Manager.
 * Features:
 * - Direct cloud synchronization with Firebase Realtime Database
 * - Resilient local offline snapshot caching
 * - Non-blocking asynchronous background writes for zero UI latency
 * - Pre-seeded default data for immediate out-of-the-box readiness
 * - Full cascade deletions (matches MySQL relational integrity)
 */
public class FirebaseManager {

    private static final FirebaseManager INSTANCE = new FirebaseManager();

    private final FirebaseClient firebaseClient = new FirebaseClient();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Firebase-Sync-Thread");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Admin> admins = new ConcurrentHashMap<>();
    private final Map<String, Student> students = new ConcurrentHashMap<>();
    private final Map<String, MenuItem> menuItems = new ConcurrentHashMap<>();
    private final Map<String, Attendance> attendances = new ConcurrentHashMap<>();
    private final Map<String, Bill> bills = new ConcurrentHashMap<>();
    private final Map<String, Complaint> complaints = new ConcurrentHashMap<>();

    private final AtomicInteger nextMenuId = new AtomicInteger(100);
    private final AtomicInteger nextAttendanceId = new AtomicInteger(100);
    private final AtomicInteger nextBillId = new AtomicInteger(100);
    private final AtomicInteger nextComplaintId = new AtomicInteger(100);

    private static final String CACHE_FILE_PATH = "resources/firebase-client-cache.json";

    private boolean initialized = false;

    private FirebaseManager() {
        init();
    }

    public static FirebaseManager getInstance() {
        return INSTANCE;
    }

    public FirebaseClient getFirebaseClient() {
        return firebaseClient;
    }

    private synchronized void init() {
        if (initialized) return;

        // 1. Try to load local cache file
        boolean loaded = loadFromLocalCache(new File(CACHE_FILE_PATH));
        if (!loaded) {
            loaded = loadFromLocalCache(new File("../" + CACHE_FILE_PATH));
        }

        // 2. If no cache, load default seed data from firebase/initial-data.json or classpath
        if (!loaded) {
            loadInitialSeedData();
        }

        recalculateCounters();
        saveLocalSnapshot();

        // 3. Kick off background cloud synchronization with Firebase
        syncExecutor.submit(() -> {
            try {
                if (firebaseClient.isOnline()) {
                    System.out.println("[FirebaseManager] Online Firebase connection established. Syncing cloud data...");
                    // Check if Firebase root is empty; if so, push seed data up
                    String remoteStudents = firebaseClient.get("students");
                    if (remoteStudents == null || remoteStudents.trim().isEmpty() || "null".equals(remoteStudents.trim())) {
                        System.out.println("[FirebaseManager] Remote Firebase database is empty. Seeding initial data to cloud...");
                        pushAllToFirebase();
                    } else {
                        System.out.println("[FirebaseManager] Pulling latest data from Firebase cloud...");
                        pullAllFromFirebase();
                    }
                } else {
                    System.out.println("[FirebaseManager] Operating in local offline cache mode. Cloud sync ready upon connection.");
                }
            } catch (Exception e) {
                System.out.println("[FirebaseManager] Initial cloud sync notice: " + e.getMessage());
            }
        });

        initialized = true;
    }

    private void recalculateCounters() {
        int maxMenu = 0;
        for (MenuItem m : menuItems.values()) {
            if (m.getMenuId() > maxMenu) maxMenu = m.getMenuId();
        }
        nextMenuId.set(Math.max(maxMenu + 1, 22));

        int maxAtt = 0;
        for (Attendance a : attendances.values()) {
            if (a.getAttendanceId() > maxAtt) maxAtt = a.getAttendanceId();
        }
        nextAttendanceId.set(Math.max(maxAtt + 1, 25));

        int maxBill = 0;
        for (Bill b : bills.values()) {
            if (b.getBillId() > maxBill) maxBill = b.getBillId();
        }
        nextBillId.set(Math.max(maxBill + 1, 5));

        int maxComp = 0;
        for (Complaint c : complaints.values()) {
            if (c.getComplaintId() > maxComp) maxComp = c.getComplaintId();
        }
        nextComplaintId.set(Math.max(maxComp + 1, 4));
    }

    // =========================================================
    // LOCAL SNAPSHOT PERSISTENCE
    // =========================================================

    public synchronized void saveLocalSnapshot() {
        try {
            File f = new File(CACHE_FILE_PATH);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Map<String, Object> root = new HashMap<>();
            root.put("admin", admins);
            root.put("students", students);
            root.put("menu", menuItems);
            root.put("attendance", attendances);
            root.put("billing", bills);
            root.put("complaints", complaints);

            Map<String, Integer> counters = new HashMap<>();
            counters.put("menuId", nextMenuId.get());
            counters.put("attendanceId", nextAttendanceId.get());
            counters.put("billId", nextBillId.get());
            counters.put("complaintId", nextComplaintId.get());
            root.put("counters", counters);

            try (FileWriter writer = new FileWriter(f, StandardCharsets.UTF_8)) {
                FirebaseClient.getGson().toJson(root, writer);
            }
        } catch (Exception e) {
            System.err.println("[FirebaseManager] Warning: Failed to save local cache: " + e.getMessage());
        }
    }

    private boolean loadFromLocalCache(File file) {
        if (!file.exists()) return false;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> root = FirebaseClient.getGson().fromJson(reader, type);
            if (root != null) {
                parseDataTree(root);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[FirebaseManager] Failed to read local cache " + file + ": " + e.getMessage());
        }
        return false;
    }

    private void loadInitialSeedData() {
        File dataFile = new File("resources/firebase-data.json");
        if (dataFile.exists()) {
            if (loadFromLocalCache(dataFile)) return;
        }

        File seedFile = new File("firebase/initial-data.json");
        if (seedFile.exists()) {
            if (loadFromLocalCache(seedFile)) return;
        }

        // Fallback: seed programmatically
        seedAdmins();
        seedStudents();
        seedWeeklyMenu();
        seedAttendance();
        seedBills();
        seedComplaints();
    }

    @SuppressWarnings("unchecked")
    private void parseDataTree(Map<String, Object> root) {
        Gson gson = FirebaseClient.getGson();

        if (root.containsKey("admin")) {
            String json = gson.toJson(root.get("admin"));
            Type t = new TypeToken<Map<String, Admin>>() {}.getType();
            Map<String, Admin> map = gson.fromJson(json, t);
            if (map != null) admins.putAll(map);
        }
        if (root.containsKey("students")) {
            String json = gson.toJson(root.get("students"));
            Type t = new TypeToken<Map<String, Student>>() {}.getType();
            Map<String, Student> map = gson.fromJson(json, t);
            if (map != null) students.putAll(map);
        }
        if (root.containsKey("menu")) {
            String json = gson.toJson(root.get("menu"));
            Type t = new TypeToken<Map<String, MenuItem>>() {}.getType();
            Map<String, MenuItem> map = gson.fromJson(json, t);
            if (map != null) menuItems.putAll(map);
        }
        if (root.containsKey("attendance")) {
            String json = gson.toJson(root.get("attendance"));
            Type t = new TypeToken<Map<String, Attendance>>() {}.getType();
            Map<String, Attendance> map = gson.fromJson(json, t);
            if (map != null) attendances.putAll(map);
        }
        if (root.containsKey("billing")) {
            String json = gson.toJson(root.get("billing"));
            Type t = new TypeToken<Map<String, Bill>>() {}.getType();
            Map<String, Bill> map = gson.fromJson(json, t);
            if (map != null) bills.putAll(map);
        }
        if (root.containsKey("complaints")) {
            String json = gson.toJson(root.get("complaints"));
            Type t = new TypeToken<Map<String, Complaint>>() {}.getType();
            Map<String, Complaint> map = gson.fromJson(json, t);
            if (map != null) complaints.putAll(map);
        }
    }

    // =========================================================
    // DEFAULT SEED DATA HELPERS
    // =========================================================

    private void seedAdmins() {
        admins.put("admin", new Admin(1, "admin", "admin123", "Chief Warden", "admin@mess.hostel.edu"));
        admins.put("manager", new Admin(2, "manager", "manager123", "Mess Supervisor", "manager@mess.hostel.edu"));
    }

    private void seedStudents() {
        students.put("STU101", new Student("STU101", "Aarav Sharma", "A-201", "9876543210", "Active", "2026-09-01 10:00:00"));
        students.put("STU102", new Student("STU102", "Diya Patel", "B-104", "9876543211", "Active", "2026-09-01 10:05:00"));
        students.put("STU103", new Student("STU103", "Rohan Gupta", "A-305", "9876543212", "Active", "2026-09-01 10:10:00"));
        students.put("STU104", new Student("STU104", "Sneha Roy", "C-102", "9876543213", "Active", "2026-09-01 10:15:00"));
        students.put("STU105", new Student("STU105", "Vikram Singh", "B-208", "9876543214", "Inactive", "2026-09-01 10:20:00"));
        students.put("STU106", new Student("STU106", "Ananya Verma", "C-210", "9876543215", "Active", "2026-09-01 10:25:00"));
    }

    private void seedWeeklyMenu() {
        String[][] data = {
                {"Monday", "Breakfast", "Idli, Sambar, Coconut Chutney, Tea/Coffee"},
                {"Monday", "Lunch", "Rice, Dal Tadka, Paneer Butter Masala, Roti, Salad"},
                {"Monday", "Dinner", "Jeera Rice, Rajma, Mixed Veg, Chapati, Kheer"},
                {"Tuesday", "Breakfast", "Poha, Sev, Boiled Eggs / Banana, Tea/Coffee"},
                {"Tuesday", "Lunch", "Rice, Yellow Dal, Aloo Gobi, Roti, Curd"},
                {"Tuesday", "Dinner", "Veg Pulao, Dal Makhani, Roti, Gulab Jamun"},
                {"Wednesday", "Breakfast", "Aloo Paratha, Curd, Pickle, Tea/Coffee"},
                {"Wednesday", "Lunch", "Steamed Rice, Sambar, Bhindi Fry, Roti, Papad"},
                {"Wednesday", "Dinner", "Fried Rice, Veg Manchurian / Chilli Chicken, Soup"},
                {"Thursday", "Breakfast", "Upma, Coconut Chutney, Fruit Bowl, Tea/Coffee"},
                {"Thursday", "Lunch", "Rice, Chana Masala, Puri, Boondi Raita, Salad"},
                {"Thursday", "Dinner", "Khichdi, Kadhi, Aloo Chokha, Papad, Pickle"},
                {"Friday", "Breakfast", "Masala Dosa, Sambar, Tomato Chutney, Tea/Coffee"},
                {"Friday", "Lunch", "Rice, Dal Fry, Kadai Paneer, Butter Roti, Ice Cream"},
                {"Friday", "Dinner", "Lemon Rice, Egg Curry / Malai Kofta, Chapati, Halwa"},
                {"Saturday", "Breakfast", "Bread Omelette / Veg Sandwich, Cornflakes, Milk/Tea"},
                {"Saturday", "Lunch", "Veg Biryani, Mirchi Ka Salan, Onion Raita, Sweet"},
                {"Saturday", "Dinner", "Tawa Roti, Dal Palak, Soyabean Curry, Rice"},
                {"Sunday", "Breakfast", "Chole Bhature, Lassi / Tea"},
                {"Sunday", "Lunch", "Special Thali: Pulao, Paneer Tikka Masala, Dal Makhani, Naan, Rasgulla"},
                {"Sunday", "Dinner", "Light Dinner: Khichdi, Papad, Curd, Seasonal Fruits"}
        };
        int id = 1;
        for (String[] row : data) {
            String key = row[0] + "_" + row[1];
            menuItems.put(key, new MenuItem(id++, row[0], row[1], row[2]));
        }
    }

    private void seedAttendance() {
        String[] dates = {"2026-09-01", "2026-09-02"};
        String[] meals = {"Breakfast", "Lunch", "Dinner"};
        int id = 1;

        for (String dStr : dates) {
            LocalDate d = LocalDate.parse(dStr);
            for (String m : meals) {
                // Default present for sample students
                for (String stuId : List.of("STU101", "STU102", "STU103", "STU104")) {
                    String status = "Present";
                    if ("STU101".equals(stuId) && "2026-09-02".equals(dStr) && "Dinner".equals(m)) status = "Absent";
                    if ("STU102".equals(stuId) && "2026-09-02".equals(dStr) && "Lunch".equals(m)) status = "Absent";
                    if ("STU103".equals(stuId) && "2026-09-01".equals(dStr) && "Breakfast".equals(m)) status = "Absent";

                    String key = stuId + "_" + dStr + "_" + m;
                    attendances.put(key, new Attendance(id++, stuId, d, m, status));
                }
            }
        }
    }

    private void seedBills() {
        Bill b1 = new Bill("STU101", "2026-08", 75, 50.0, 3750.0, "Paid");
        b1.setBillId(1);
        b1.setPaidDate(LocalDate.parse("2026-09-03"));
        bills.put("STU101_2026-08", b1);

        Bill b2 = new Bill("STU102", "2026-08", 68, 50.0, 3400.0, "Paid");
        b2.setBillId(2);
        b2.setPaidDate(LocalDate.parse("2026-09-05"));
        bills.put("STU102_2026-08", b2);

        Bill b3 = new Bill("STU103", "2026-08", 80, 50.0, 4000.0, "Unpaid");
        b3.setBillId(3);
        bills.put("STU103_2026-08", b3);

        Bill b4 = new Bill("STU104", "2026-08", 70, 50.0, 3500.0, "Unpaid");
        b4.setBillId(4);
        bills.put("STU104_2026-08", b4);
    }

    private void seedComplaints() {
        complaints.put("1", new Complaint(1, "STU101", "Food Quality",
                "Dinner rice on Wednesday was slightly undercooked.", "Resolved",
                "Notified the head chef to monitor cooking duration.", LocalDate.parse("2026-09-02")));
        complaints.put("2", new Complaint(2, "STU103", "Cleanliness",
                "Dining table 4 needs more frequent wiping after peak lunch hours.", "In Progress",
                "Staff scheduled for continuous table sanitization.", LocalDate.parse("2026-09-10")));
        complaints.put("3", new Complaint(3, "STU104", "Service",
                "Drinking water dispenser on the 1st floor mess area was empty around 1:30 PM.", "Pending",
                null, LocalDate.parse("2026-09-15")));
    }

    // =========================================================
    // ASYNCHRONOUS FIREBASE CLOUD SYNC METHODS
    // =========================================================

    public void asyncPut(String path, Object entity) {
        saveLocalSnapshot();
        syncExecutor.submit(() -> {
            try {
                String json = FirebaseClient.getGson().toJson(entity);
                firebaseClient.put(path, json);
            } catch (Exception e) {
                // Silently logged, changes remain safe in local snapshot
                // System.err.println("[FirebaseManager] Cloud sync notice: " + e.getMessage());
            }
        });
    }

    public void asyncDelete(String path) {
        saveLocalSnapshot();
        syncExecutor.submit(() -> {
            try {
                firebaseClient.delete(path);
            } catch (Exception ignored) {}
        });
    }

    public synchronized String pushAllToFirebase() {
        try {
            Gson gson = FirebaseClient.getGson();
            firebaseClient.put("admin", gson.toJson(admins));
            firebaseClient.put("students", gson.toJson(students));
            firebaseClient.put("menu", gson.toJson(menuItems));
            firebaseClient.put("attendance", gson.toJson(attendances));
            firebaseClient.put("billing", gson.toJson(bills));
            firebaseClient.put("complaints", gson.toJson(complaints));

            Map<String, Integer> counters = new HashMap<>();
            counters.put("menuId", nextMenuId.get());
            counters.put("attendanceId", nextAttendanceId.get());
            counters.put("billId", nextBillId.get());
            counters.put("complaintId", nextComplaintId.get());
            firebaseClient.put("counters", gson.toJson(counters));

            saveLocalSnapshot();
            return "SUCCESS: All data successfully pushed to Firebase Realtime Database!";
        } catch (Exception e) {
            return "ERROR: Failed to push to Firebase: " + e.getMessage();
        }
    }

    public synchronized String pullAllFromFirebase() {
        try {
            Gson gson = FirebaseClient.getGson();

            String admJson = firebaseClient.get("admin");
            if (admJson != null) {
                Type t = new TypeToken<Map<String, Admin>>() {}.getType();
                Map<String, Admin> map = gson.fromJson(admJson, t);
                if (map != null) {
                    admins.clear();
                    admins.putAll(map);
                }
            }

            String stuJson = firebaseClient.get("students");
            if (stuJson != null) {
                Type t = new TypeToken<Map<String, Student>>() {}.getType();
                Map<String, Student> map = gson.fromJson(stuJson, t);
                if (map != null) {
                    students.clear();
                    students.putAll(map);
                }
            }

            String menuJson = firebaseClient.get("menu");
            if (menuJson != null) {
                Type t = new TypeToken<Map<String, MenuItem>>() {}.getType();
                Map<String, MenuItem> map = gson.fromJson(menuJson, t);
                if (map != null) {
                    menuItems.clear();
                    menuItems.putAll(map);
                }
            }

            String attJson = firebaseClient.get("attendance");
            if (attJson != null) {
                Type t = new TypeToken<Map<String, Attendance>>() {}.getType();
                Map<String, Attendance> map = gson.fromJson(attJson, t);
                if (map != null) {
                    attendances.clear();
                    attendances.putAll(map);
                }
            }

            String billJson = firebaseClient.get("billing");
            if (billJson != null) {
                Type t = new TypeToken<Map<String, Bill>>() {}.getType();
                Map<String, Bill> map = gson.fromJson(billJson, t);
                if (map != null) {
                    bills.clear();
                    bills.putAll(map);
                }
            }

            String compJson = firebaseClient.get("complaints");
            if (compJson != null) {
                Type t = new TypeToken<Map<String, Complaint>>() {}.getType();
                Map<String, Complaint> map = gson.fromJson(compJson, t);
                if (map != null) {
                    complaints.clear();
                    complaints.putAll(map);
                }
            }

            recalculateCounters();
            saveLocalSnapshot();
            return "SUCCESS: Pulled all data from Firebase Realtime Database!";
        } catch (Exception e) {
            return "ERROR: Failed to pull from Firebase: " + e.getMessage();
        }
    }

    // =========================================================
    // ADMIN OPERATIONS
    // =========================================================

    public Admin authenticateAdmin(String username, String password) {
        if (username == null || password == null) return null;
        String cleanUser = username.replace("\uFEFF", "").trim();
        String cleanPass = password.replace("\uFEFF", "").trim();
        Admin admin = admins.get(cleanUser);
        if (admin != null && admin.getPassword().trim().equals(cleanPass)) {
            return admin;
        }
        return null;
    }

    public boolean addAdmin(Admin admin) {
        if (admin == null || admin.getUsername() == null) return false;
        String key = admin.getUsername().trim();
        if (admins.containsKey(key)) return false;
        admins.put(key, admin);
        asyncPut("admin/" + key, admin);
        return true;
    }

    public boolean changeAdminPassword(String username, String newPassword) {
        if (username == null || newPassword == null) return false;
        Admin a = admins.get(username.trim());
        if (a != null) {
            a.setPassword(newPassword.trim());
            asyncPut("admin/" + username.trim(), a);
            return true;
        }
        return false;
    }

    public List<Admin> getAllAdmins() {
        List<Admin> list = new ArrayList<>(admins.values());
        list.sort(Comparator.comparingInt(Admin::getAdminId));
        return list;
    }

    // =========================================================
    // STUDENT OPERATIONS
    // =========================================================

    public boolean addStudent(Student student) {
        if (student == null || student.getStudentId() == null) return false;
        String key = student.getStudentId().trim().toUpperCase();
        if (students.containsKey(key)) return false;
        student.setStudentId(key);
        students.put(key, student);
        asyncPut("students/" + key, student);
        return true;
    }

    public boolean updateStudent(Student student) {
        if (student == null || student.getStudentId() == null) return false;
        String key = student.getStudentId().trim().toUpperCase();
        if (!students.containsKey(key)) return false;
        student.setStudentId(key);
        students.put(key, student);
        asyncPut("students/" + key, student);
        return true;
    }

    public boolean deleteStudent(String studentId) {
        if (studentId == null) return false;
        String key = studentId.trim().toUpperCase();
        if (!students.containsKey(key)) return false;

        students.remove(key);
        asyncDelete("students/" + key);

        // CASCADE cleanup: attendance
        attendances.entrySet().removeIf(entry -> {
            if (entry.getValue().getStudentId().equalsIgnoreCase(key)) {
                asyncDelete("attendance/" + entry.getKey());
                return true;
            }
            return false;
        });

        // CASCADE cleanup: billing
        bills.entrySet().removeIf(entry -> {
            if (entry.getValue().getStudentId().equalsIgnoreCase(key)) {
                asyncDelete("billing/" + entry.getKey());
                return true;
            }
            return false;
        });

        // CASCADE cleanup: complaints
        complaints.entrySet().removeIf(entry -> {
            if (entry.getValue().getStudentId().equalsIgnoreCase(key)) {
                asyncDelete("complaints/" + entry.getKey());
                return true;
            }
            return false;
        });

        saveLocalSnapshot();
        return true;
    }

    public Student getStudentById(String studentId) {
        if (studentId == null) return null;
        return students.get(studentId.trim().toUpperCase());
    }

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>(students.values());
        list.sort(Comparator.comparing(Student::getStudentId));
        return list;
    }

    public List<Student> getActiveStudents() {
        List<Student> list = new ArrayList<>();
        for (Student s : students.values()) {
            if (s.isActive()) list.add(s);
        }
        list.sort(Comparator.comparing(Student::getStudentId));
        return list;
    }

    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) return getAllStudents();
        String q = query.trim().toLowerCase();
        List<Student> list = new ArrayList<>();
        for (Student s : students.values()) {
            if ((s.getStudentId() != null && s.getStudentId().toLowerCase().contains(q)) ||
                (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                (s.getRoomNo() != null && s.getRoomNo().toLowerCase().contains(q)) ||
                (s.getContact() != null && s.getContact().toLowerCase().contains(q))) {
                list.add(s);
            }
        }
        list.sort(Comparator.comparing(Student::getStudentId));
        return list;
    }

    public int getTotalStudentsCount() {
        return students.size();
    }

    public int getActiveStudentsCount() {
        int count = 0;
        for (Student s : students.values()) {
            if (s.isActive()) count++;
        }
        return count;
    }

    public boolean studentExists(String studentId) {
        if (studentId == null) return false;
        return students.containsKey(studentId.trim().toUpperCase());
    }

    // =========================================================
    // MENU OPERATIONS
    // =========================================================

    public boolean saveOrUpdateMenu(String day, String mealType, String items) {
        if (day == null || mealType == null || items == null) return false;
        String key = day.trim() + "_" + mealType.trim();
        MenuItem existing = menuItems.get(key);
        if (existing != null) {
            existing.setItems(items.trim());
            asyncPut("menu/" + key, existing);
        } else {
            int id = nextMenuId.getAndIncrement();
            MenuItem m = new MenuItem(id, day.trim(), mealType.trim(), items.trim());
            menuItems.put(key, m);
            asyncPut("menu/" + key, m);
        }
        return true;
    }

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> list = new ArrayList<>(menuItems.values());
        List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        List<String> meals = List.of("Breakfast", "Lunch", "Dinner");

        list.sort((m1, m2) -> {
            int d1 = days.indexOf(m1.getDay());
            int d2 = days.indexOf(m2.getDay());
            if (d1 != d2) return Integer.compare(d1, d2);
            int meal1 = meals.indexOf(m1.getMealType());
            int meal2 = meals.indexOf(m2.getMealType());
            return Integer.compare(meal1, meal2);
        });
        return list;
    }

    public List<MenuItem> getMenuByDay(String day) {
        List<MenuItem> list = new ArrayList<>();
        if (day == null) return list;
        for (MenuItem m : menuItems.values()) {
            if (day.equalsIgnoreCase(m.getDay())) {
                list.add(m);
            }
        }
        List<String> meals = List.of("Breakfast", "Lunch", "Dinner");
        list.sort(Comparator.comparingInt(m -> meals.indexOf(m.getMealType())));
        return list;
    }

    public MenuItem getMenuItem(String day, String mealType) {
        if (day == null || mealType == null) return null;
        String key = day.trim() + "_" + mealType.trim();
        return menuItems.get(key);
    }

    public boolean deleteMenuItem(int menuId) {
        for (Map.Entry<String, MenuItem> entry : menuItems.entrySet()) {
            if (entry.getValue().getMenuId() == menuId) {
                menuItems.remove(entry.getKey());
                asyncDelete("menu/" + entry.getKey());
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // ATTENDANCE OPERATIONS
    // =========================================================

    public boolean markAttendance(String studentId, LocalDate date, String mealType, String status) {
        if (studentId == null || date == null || mealType == null || status == null) return false;
        String sId = studentId.trim().toUpperCase();
        String key = sId + "_" + date + "_" + mealType.trim();

        Attendance a = attendances.get(key);
        if (a != null) {
            a.setStatus(status.trim());
        } else {
            int id = nextAttendanceId.getAndIncrement();
            a = new Attendance(id, sId, date, mealType.trim(), status.trim());
            attendances.put(key, a);
        }
        asyncPut("attendance/" + key, a);
        return true;
    }

    public boolean saveBatchAttendance(List<Attendance> list) {
        if (list == null || list.isEmpty()) return false;
        for (Attendance item : list) {
            markAttendance(item.getStudentId(), item.getDate(), item.getMealType(), item.getStatus());
        }
        return true;
    }

    public List<Attendance> getAttendanceByDateAndMeal(LocalDate date, String mealType) {
        List<Attendance> list = new ArrayList<>();
        if (date == null || mealType == null) return list;

        for (Attendance a : attendances.values()) {
            if (date.equals(a.getDate()) && mealType.equalsIgnoreCase(a.getMealType())) {
                Student s = students.get(a.getStudentId());
                if (s != null) {
                    a.setStudentName(s.getName());
                    a.setRoomNo(s.getRoomNo());
                }
                list.add(a);
            }
        }
        list.sort(Comparator.comparing(Attendance::getStudentId));
        return list;
    }

    public List<Attendance> getAttendanceByStudentAndMonth(String studentId, String yearMonth) {
        List<Attendance> list = new ArrayList<>();
        if (studentId == null || yearMonth == null) return list;
        String sId = studentId.trim().toUpperCase();

        for (Attendance a : attendances.values()) {
            if (sId.equalsIgnoreCase(a.getStudentId())) {
                String ym = a.getDate().toString().substring(0, 7);
                if (yearMonth.equals(ym)) {
                    Student s = students.get(sId);
                    if (s != null) {
                        a.setStudentName(s.getName());
                        a.setRoomNo(s.getRoomNo());
                    }
                    list.add(a);
                }
            }
        }
        List<String> mealOrder = List.of("Breakfast", "Lunch", "Dinner");
        list.sort((a1, a2) -> {
            int dc = a1.getDate().compareTo(a2.getDate());
            if (dc != 0) return dc;
            return Integer.compare(mealOrder.indexOf(a1.getMealType()), mealOrder.indexOf(a2.getMealType()));
        });
        return list;
    }

    public int countStudentPresentMeals(String studentId, String yearMonth) {
        if (studentId == null || yearMonth == null) return 0;
        String sId = studentId.trim().toUpperCase();
        int count = 0;
        for (Attendance a : attendances.values()) {
            if (sId.equalsIgnoreCase(a.getStudentId()) && a.isPresent()) {
                String ym = a.getDate().toString().substring(0, 7);
                if (yearMonth.equals(ym)) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getTodayPresentCount(LocalDate date) {
        if (date == null) return 0;
        int count = 0;
        for (Attendance a : attendances.values()) {
            if (date.equals(a.getDate()) && a.isPresent()) {
                count++;
            }
        }
        return count;
    }

    public List<AttendanceSummary> getMonthlyAttendanceSummary(String yearMonth) {
        List<AttendanceSummary> summaries = new ArrayList<>();
        List<Student> stuList = getAllStudents();

        for (Student s : stuList) {
            int present = 0;
            int absent = 0;
            for (Attendance a : attendances.values()) {
                if (s.getStudentId().equalsIgnoreCase(a.getStudentId())) {
                    String ym = a.getDate().toString().substring(0, 7);
                    if (yearMonth.equals(ym)) {
                        if (a.isPresent()) present++;
                        else absent++;
                    }
                }
            }
            summaries.add(new AttendanceSummary(s.getStudentId(), s.getName(), s.getRoomNo(), yearMonth, present, absent));
        }
        return summaries;
    }

    // =========================================================
    // BILLING OPERATIONS
    // =========================================================

    public boolean generateBillForStudent(String studentId, String month, double perMealRate) {
        if (studentId == null || month == null) return false;
        String sId = studentId.trim().toUpperCase();
        int meals = countStudentPresentMeals(sId, month);
        double totalAmount = meals * perMealRate;
        String key = sId + "_" + month.trim();

        Bill b = bills.get(key);
        if (b != null) {
            b.setTotalMeals(meals);
            b.setPerMealRate(perMealRate);
            b.setAmount(totalAmount);
        } else {
            int id = nextBillId.getAndIncrement();
            b = new Bill(sId, month.trim(), meals, perMealRate, totalAmount, "Unpaid");
            b.setBillId(id);
            bills.put(key, b);
        }
        asyncPut("billing/" + key, b);
        return true;
    }

    public int generateBillsForMonth(String month, double perMealRate) {
        List<Student> stuList = getAllStudents();
        int count = 0;
        for (Student s : stuList) {
            if (generateBillForStudent(s.getStudentId(), month, perMealRate)) {
                count++;
            }
        }
        return count;
    }

    public List<Bill> getAllBills() {
        List<Bill> list = new ArrayList<>(bills.values());
        for (Bill b : list) {
            Student s = students.get(b.getStudentId());
            if (s != null) {
                b.setStudentName(s.getName());
                b.setRoomNo(s.getRoomNo());
            }
        }
        list.sort((b1, b2) -> {
            int mc = b2.getMonth().compareTo(b1.getMonth());
            if (mc != 0) return mc;
            return Integer.compare(b2.getBillId(), b1.getBillId());
        });
        return list;
    }

    public List<Bill> getBillsByMonth(String month) {
        List<Bill> list = new ArrayList<>();
        if (month == null) return list;

        for (Bill b : bills.values()) {
            if (month.equals(b.getMonth())) {
                Student s = students.get(b.getStudentId());
                if (s != null) {
                    b.setStudentName(s.getName());
                    b.setRoomNo(s.getRoomNo());
                }
                list.add(b);
            }
        }
        list.sort(Comparator.comparingInt(Bill::getBillId));
        return list;
    }

    public Bill getBillById(int billId) {
        for (Bill b : bills.values()) {
            if (b.getBillId() == billId) {
                Student s = students.get(b.getStudentId());
                if (s != null) {
                    b.setStudentName(s.getName());
                    b.setRoomNo(s.getRoomNo());
                }
                return b;
            }
        }
        return null;
    }

    public Bill getBillByStudentAndMonth(String studentId, String month) {
        if (studentId == null || month == null) return null;
        String key = studentId.trim().toUpperCase() + "_" + month.trim();
        Bill b = bills.get(key);
        if (b != null) {
            Student s = students.get(b.getStudentId());
            if (s != null) {
                b.setStudentName(s.getName());
                b.setRoomNo(s.getRoomNo());
            }
        }
        return b;
    }

    public boolean updatePaymentStatus(int billId, String status, LocalDate paidDate) {
        for (Map.Entry<String, Bill> entry : bills.entrySet()) {
            Bill b = entry.getValue();
            if (b.getBillId() == billId) {
                b.setPaidStatus(status.trim());
                if (paidDate != null && "Paid".equalsIgnoreCase(status)) {
                    b.setPaidDate(paidDate);
                } else {
                    b.setPaidDate(null);
                }
                asyncPut("billing/" + entry.getKey(), b);
                return true;
            }
        }
        return false;
    }

    public boolean deleteBill(int billId) {
        for (Map.Entry<String, Bill> entry : bills.entrySet()) {
            if (entry.getValue().getBillId() == billId) {
                bills.remove(entry.getKey());
                asyncDelete("billing/" + entry.getKey());
                return true;
            }
        }
        return false;
    }

    public double getTotalRevenue(String month) {
        double total = 0.0;
        for (Bill b : bills.values()) {
            if (b.isPaid()) {
                if (month == null || month.trim().isEmpty() || month.trim().equals(b.getMonth())) {
                    total += b.getAmount();
                }
            }
        }
        return total;
    }

    public double getUnpaidTotal(String month) {
        double total = 0.0;
        for (Bill b : bills.values()) {
            if (!b.isPaid()) {
                if (month == null || month.trim().isEmpty() || month.trim().equals(b.getMonth())) {
                    total += b.getAmount();
                }
            }
        }
        return total;
    }

    // =========================================================
    // COMPLAINT OPERATIONS
    // =========================================================

    public boolean addComplaint(Complaint complaint) {
        if (complaint == null) return false;
        int id = nextComplaintId.getAndIncrement();
        complaint.setComplaintId(id);
        String key = String.valueOf(id);
        complaints.put(key, complaint);
        asyncPut("complaints/" + key, complaint);
        return true;
    }

    public List<Complaint> getAllComplaints() {
        List<Complaint> list = new ArrayList<>(complaints.values());
        for (Complaint c : list) {
            Student s = students.get(c.getStudentId());
            if (s != null) {
                c.setStudentName(s.getName());
            }
        }
        list.sort((c1, c2) -> {
            int dc = c2.getCreatedAt().compareTo(c1.getCreatedAt());
            if (dc != 0) return dc;
            return Integer.compare(c2.getComplaintId(), c1.getComplaintId());
        });
        return list;
    }

    public List<Complaint> getComplaintsByStatus(String status) {
        List<Complaint> list = new ArrayList<>();
        if (status == null) return list;

        for (Complaint c : complaints.values()) {
            if (status.equalsIgnoreCase(c.getStatus())) {
                Student s = students.get(c.getStudentId());
                if (s != null) {
                    c.setStudentName(s.getName());
                }
                list.add(c);
            }
        }
        list.sort((c1, c2) -> {
            int dc = c2.getCreatedAt().compareTo(c1.getCreatedAt());
            if (dc != 0) return dc;
            return Integer.compare(c2.getComplaintId(), c1.getComplaintId());
        });
        return list;
    }

    public boolean updateComplaintStatusAndNote(int complaintId, String status, String note) {
        String key = String.valueOf(complaintId);
        Complaint c = complaints.get(key);
        if (c != null) {
            c.setStatus(status.trim());
            c.setResolutionNote(note != null ? note.trim() : null);
            asyncPut("complaints/" + key, c);
            return true;
        }
        return false;
    }

    public boolean deleteComplaint(int complaintId) {
        String key = String.valueOf(complaintId);
        if (complaints.containsKey(key)) {
            complaints.remove(key);
            asyncDelete("complaints/" + key);
            return true;
        }
        return false;
    }

    public int getPendingComplaintCount() {
        int count = 0;
        for (Complaint c : complaints.values()) {
            if ("Pending".equalsIgnoreCase(c.getStatus())) count++;
        }
        return count;
    }
}
