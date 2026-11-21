# Mess Management System (Java + Google Firebase Cloud)

A modern desktop and console-based **Mess Management System** built with **Java** and powered by **Google Firebase Realtime Database**. The system completely replaces legacy SQL/relational database requirements with cloud-native JSON persistence, live HTTP REST synchronization, offline caching, automated monthly billing, meal attendance tracking, weekly meal planning, feedback management, and administrative control for college and hostel messes.

---

## 📌 Features & Modules

1. **Student Management**
   - Register students with Student ID, Full Name, Room Number, Contact Number, and Mess Status (`Active` / `Inactive`).
   - Search by ID, Name, Room Number, or Contact.
   - Filter active and inactive students.
   - Update details or delete student records (with cascade cleanup across attendance, billing, and complaints).

2. **Weekly Menu Management**
   - Configure breakfast, lunch, and dinner menus for all 7 days of the week (Monday – Sunday).
   - Real-time updates and weekly schedule viewer.

3. **Daily Meal Attendance Management**
   - Record daily meal attendance (`Present` / `Absent`) per student for Breakfast, Lunch, and Dinner.
   - Quick bulk operations: "Mark All Present", "Mark All Absent", and individual toggles.
   - View student monthly meal attendance history and audit summary sheet.

4. **Fee & Monthly Billing Management**
   - Automatically calculate monthly mess bills:
     $$\text{Total Bill} = \text{Meals Taken (Present)} \times \text{Per-Meal Cost}$$
   - Configurable per-meal rate (default: ₹50.00).
   - Generate bills for all active students or individually.
   - Toggle payment status (`Unpaid` $\leftrightarrow$ `Paid`) with payment timestamp.
   - **Printable Invoices & Receipts**: View, copy to clipboard, or export itemized student invoices to `.txt`.

5. **Feedback & Complaint Management**
   - Students can file grievances across categories: *Food Quality*, *Cleanliness*, *Service*, *Other*.
   - Staff/Admin can update complaint status (`Pending` $\rightarrow$ `In Progress` $\rightarrow$ `Resolved`) and attach resolution notes.

6. **Admin Authentication & Access Control**
   - Secure login portal with password protection.
   - Pre-seeded default credentials (`admin` / `admin123` & `manager` / `manager123`).

7. **Dual Interface Support**
   - **Modern Swing GUI**: Desktop application with tabbed navigation, KPI dashboard cards, dialogs, and instant search.
   - **Interactive CLI Console**: Complete command-line menu for headless servers, terminals, or quick terminal access.

8. **Google Firebase Integration & Resilient Offline Caching**
   - Direct REST API communication with Google Firebase Realtime Database.
   - Non-blocking asynchronous cloud synchronization for zero UI lag.
   - Local JSON snapshot cache (`resources/firebase-data.json`) ensuring the system operates reliably even when offline or before setting up a Firebase account.
   - 1-click "Push to Cloud" and "Pull from Cloud" buttons inside the GUI & CLI.

---

## 🗄️ Firebase Realtime Database JSON Architecture

All data is structured in a clean, hierarchical JSON tree inside Firebase Realtime Database:

```json
{
  "admin": {
    "admin": { "adminId": 1, "username": "admin", "password": "...", "fullName": "Chief Warden", "email": "admin@mess.hostel.edu" },
    "manager": { "adminId": 2, "username": "manager", "password": "...", "fullName": "Mess Supervisor", "email": "manager@mess.hostel.edu" }
  },
  "students": {
    "STU101": { "studentId": "STU101", "name": "Aarav Sharma", "roomNo": "A-201", "contact": "9876543210", "status": "Active", "registeredAt": "2026-09-01 10:00:00" },
    "STU102": { ... }
  },
  "menu": {
    "Monday_Breakfast": { "menuId": 1, "day": "Monday", "mealType": "Breakfast", "items": "Idli, Sambar, Coconut Chutney, Tea/Coffee" },
    "Monday_Lunch": { ... }
  },
  "attendance": {
    "STU101_2026-09-01_Breakfast": { "attendanceId": 1, "studentId": "STU101", "date": "2026-09-01", "mealType": "Breakfast", "status": "Present" },
    ...
  },
  "billing": {
    "STU101_2026-08": { "billId": 1, "studentId": "STU101", "month": "2026-08", "totalMeals": 75, "perMealRate": 50.0, "amount": 3750.0, "paidStatus": "Paid", "paidDate": "2026-09-03" },
    ...
  },
  "complaints": {
    "1": { "complaintId": 1, "studentId": "STU101", "category": "Food Quality", "message": "...", "status": "Resolved", "resolutionNote": "...", "createdAt": "2026-09-02" },
    ...
  },
  "counters": {
    "menuId": 22,
    "attendanceId": 25,
    "billId": 5,
    "complaintId": 4
  }
}
```

---

## 🏗️ Project Structure

```
f:\project\
  ├── bin\                                # Compiled .class files and active configs
  ├── firebase\
  │   ├── rules.json                      # Firebase Realtime Database Security Rules
  │   └── initial-data.json               # Seed dataset for Firebase Console import
  ├── lib\
  │   └── gson-2.10.1.jar                 # Google Gson JSON library
  ├── resources\
  │   ├── firebase.properties             # Firebase connection configuration
  │   └── firebase-data.json              # Local persistent offline snapshot
  ├── src\
  │   └── com\
  │       └── messmgmt\
  │           ├── Main.java               # Entry point (auto-detects GUI/CLI)
  │           ├── MainConsole.java        # Interactive CLI Console
  │           ├── config\
  │           │   ├── FirebaseConfig.java # Properties manager & runtime updater
  │           │   ├── FirebaseClient.java # REST client using Java 11+ HttpClient
  │           │   └── FirebaseManager.java# Cloud synchronization & data hub
  │           ├── model\
  │           │   ├── Admin.java
  │           │   ├── Student.java
  │           │   ├── MenuItem.java
  │           │   ├── Attendance.java
  │           │   ├── AttendanceSummary.java
  │           │   ├── Bill.java
  │           │   └── Complaint.java
  │           ├── dao\
  │           │   ├── AdminDAO.java
  │           │   ├── StudentDAO.java
  │           │   ├── MenuDAO.java
  │           │   ├── AttendanceDAO.java
  │           │   ├── BillingDAO.java
  │           │   └── ComplaintDAO.java
  │           ├── util\
  │           │   ├── ValidationUtil.java # Input regex validator
  │           │   └── ReportGenerator.java# Formatted receipts & statements
  │           └── ui\
  │               ├── LoginDialog.java    # Admin login screen
  │               ├── MainFrame.java      # Main application window
  │               ├── DashboardPanel.java # KPI cards & overview
  │               ├── StudentPanel.java   # Student management CRUD
  │               ├── MenuPanel.java      # Weekly meal manager
  │               ├── AttendancePanel.java# Daily attendance marker
  │               ├── BillingPanel.java   # Billing & invoicing
  │               ├── ComplaintPanel.java # Grievance tracker
  │               ├── ReportPanel.java    # Audit sheet & reports
  │               ├── InvoiceDialog.java  # Printable receipt modal
  │               └── FirebaseSettingsDialog.java # Firebase cloud configuration modal
  ├── firebase-local-server.js            # Standalone localhost Firebase REST emulator server
  ├── run-local-firebase.bat              # 1-click localhost Firebase server launcher
  ├── build.bat                           # 1-click Windows compilation script
  ├── run.bat                             # 1-click Windows GUI launcher
  ├── run-cli.bat                         # 1-click Windows CLI launcher
  └── README.md
```

---

## ⚙️ Prerequisites & Setup

### 1. Requirements
- **Java Development Kit (JDK)**: Version 17 or higher (Java 25 verified).
- **No MySQL or SQL Server Required**: Everything runs directly via Firebase and native Java!


### 2. 🖥️ Running on Localhost (100% Offline with Local Firebase Server)
You can run this application entirely on **localhost** without any cloud account or internet connection:

1. **Launch the Local Firebase Server**:
   Double-click [`run-local-firebase.bat`](file:///f:/project/run-local-firebase.bat) or run in terminal:
   ```cmd
   run-local-firebase.bat
   ```
   *This starts a local Firebase REST API server on `http://localhost:9000/` backed by `resources/firebase-data.json`.*

2. **Configure App for Localhost**:
   In [`resources/firebase.properties`](file:///f:/project/resources/firebase.properties) (or via the **🔥 Firebase Settings** dialog in-app), set:
   ```properties
   firebase.db.url=http://localhost:9000/
   firebase.auth.token=
   ```

3. **Launch the Application**:
   Run [`run.bat`](file:///f:/project/run.bat) (GUI) or [`run-cli.bat`](file:///f:/project/run-cli.bat) (CLI). The app connects to your local server with zero latency!

### 3. (Optional) Connecting to Google Firebase Cloud
If you want to connect to a live cloud Firebase database instead of localhost:
1. Go to [Firebase Console](https://console.firebase.google.com/) and click **"Add project"**.
2. Under **Build**, select **Realtime Database** &rarr; **Create Database**.
3. Choose a location and select **Test Mode** (or paste rules from [`firebase/rules.json`](file:///f:/project/firebase/rules.json)).
4. Copy your Realtime Database URL (e.g. `https://my-hostel-mess-default-rtdb.firebaseio.com/`).
5. Open [`resources/firebase.properties`](file:///f:/project/resources/firebase.properties) or use the in-app **"🔥 Firebase Settings"** button:
   ```properties
   firebase.db.url=https://my-hostel-mess-default-rtdb.firebaseio.com/
   firebase.auth.token=
   mess.default.meal.rate=50.00
   ```
6. Click **"⬆ Push All to Cloud"** to sync your local data into your new Firebase instance!

---

## 🚀 How to Build and Run

### Option A: Using Windows Batch Scripts (1-Click)
1. **Compile**: Double-click [`build.bat`](file:///f:/project/build.bat) or run in terminal:
   ```cmd
   build.bat
   ```
2. **Run GUI Desktop App**: Double-click [`run.bat`](file:///f:/project/run.bat) or run:
   ```cmd
   run.bat
   ```
3. **Run Console CLI App**: Double-click [`run-cli.bat`](file:///f:/project/run-cli.bat) or run:
   ```cmd
   run-cli.bat
   ```

---

### Option B: Using Command Line (PowerShell / Command Prompt)

#### 1. Compile Java Source:
```powershell
javac -cp "lib/gson-2.10.1.jar;resources" -d bin (Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName)
Copy-Item "resources/firebase.properties" -Destination "bin/firebase.properties" -Force
```

#### 2. Run GUI Mode:
```powershell
java -cp "bin;lib/*;resources" com.messmgmt.Main
```

#### 3. Run CLI Console Mode:
```powershell
java -cp "bin;lib/*;resources" com.messmgmt.Main --console
```

---

## 🔑 Default Login Credentials

| Username | Password | Role |
| :--- | :--- | :--- |
| `admin` | `admin123` | Chief Warden / Super Admin |
| `manager` | `manager123` | Mess Supervisor |

---

## 🧪 Quick Walkthrough & Verification

1. **Login**: Enter `admin` and `admin123`.
2. **Dashboard**: View high-level metrics (active students, meals served today, outstanding dues, open complaints).
3. **Students**: View seeded students (`STU101`–`STU106`). Try searching or registering a new student.
4. **Attendance**: Select date and meal (e.g. today's Lunch), click "Mark All Present", or check/uncheck individuals, and click **"💾 Save Attendance"**.
5. **Fee & Billing**:
   - Enter current month (e.g., `2026-09`), meal rate (`50.00`), and click **"⚡ Generate Month Bills"**.
   - Select a student row and click **"📄 View / Print Invoice"** to inspect the itemized receipt with meal breakdown.
   - Click **"Mark as Paid"** to register student fee collection.
6. **Complaints**: Review grievances, click **"Resolve / Update Status"**, and add resolution feedback.
7. **Reports**: Generate and export the **"Monthly Revenue & Billing Summary"** or **"Attendance Audit Sheet"**.
8. **Firebase Settings**: Open **"🔥 Firebase Settings"**, test connection, or push/pull data from Firebase Realtime Database.
