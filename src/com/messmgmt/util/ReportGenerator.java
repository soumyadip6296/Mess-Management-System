package com.messmgmt.util;

import com.messmgmt.model.Attendance;
import com.messmgmt.model.AttendanceSummary;
import com.messmgmt.model.Bill;
import com.messmgmt.model.Student;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGenerator {

    private static final DateTimeFormatter DTM_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String generateStudentInvoice(Bill bill, Student student, List<Attendance> attendances) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(65);
        String subline = "-".repeat(65);

        sb.append(line).append("\n");
        sb.append("                  COLLEGE HOSTEL MESS COMMITTEE\n");
        sb.append("                   MONTHLY MESS BILL & INVOICE\n");
        sb.append(line).append("\n");

        sb.append(String.format(" Invoice No    : BILL-%05d%n", bill.getBillId()));
        sb.append(String.format(" Billing Month : %s%n", bill.getMonth()));
        sb.append(String.format(" Generated On  : %s%n", LocalDateTime.now().format(DTM_FMT)));
        sb.append(subline).append("\n");

        sb.append(String.format(" Student ID    : %s%n", bill.getStudentId()));
        sb.append(String.format(" Student Name  : %s%n", (student != null ? student.getName() : bill.getStudentName())));
        sb.append(String.format(" Room Number   : %s%n", (student != null ? student.getRoomNo() : bill.getRoomNo())));
        sb.append(String.format(" Contact No    : %s%n", (student != null ? student.getContact() : "N/A")));
        sb.append(String.format(" Mess Status   : %s%n", (student != null ? student.getStatus() : "Active")));
        sb.append(subline).append("\n");

        sb.append(" MEAL BREAKDOWN SUMMARY:\n");
        long bCount = 0, lCount = 0, dCount = 0;
        if (attendances != null) {
            for (Attendance a : attendances) {
                if (a.isPresent()) {
                    if ("Breakfast".equalsIgnoreCase(a.getMealType())) bCount++;
                    else if ("Lunch".equalsIgnoreCase(a.getMealType())) lCount++;
                    else if ("Dinner".equalsIgnoreCase(a.getMealType())) dCount++;
                }
            }
        }
        sb.append(String.format("   • Breakfast Meals Present : %d%n", bCount));
        sb.append(String.format("   • Lunch Meals Present     : %d%n", lCount));
        sb.append(String.format("   • Dinner Meals Present    : %d%n", dCount));
        sb.append(String.format("   • Total Meals Counted     : %d meals%n", bill.getTotalMeals()));
        sb.append(String.format("   • Per-Meal Charge         : INR %.2f%n", bill.getPerMealRate()));
        sb.append(subline).append("\n");

        sb.append(String.format(" TOTAL AMOUNT DUE         : INR %.2f%n", bill.getAmount()));
        sb.append(String.format(" PAYMENT STATUS           : [%s]%n", bill.getPaidStatus().toUpperCase()));
        if (bill.isPaid() && bill.getPaidDate() != null) {
            sb.append(String.format(" Paid Date                : %s%n", bill.getPaidDate()));
        }
        sb.append(line).append("\n");
        sb.append(" Note: Please clear pending dues by the 10th of every month.\n");
        sb.append(" For billing discrepancies, contact the Mess Admin office.\n");
        sb.append(line).append("\n");

        return sb.toString();
    }

    public static String generateMonthlyBillingReport(String month, List<Bill> bills) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(85);
        String subline = "-".repeat(85);

        sb.append(line).append("\n");
        sb.append(String.format("              MONTHLY MESS REVENUE & BILLING REPORT: %s%n", month));
        sb.append(String.format("              Generated: %s%n", LocalDateTime.now().format(DTM_FMT)));
        sb.append(line).append("\n");
        sb.append(String.format("%-8s | %-10s | %-20s | %-6s | %-6s | %-10s | %-8s%n",
                "Bill ID", "Student ID", "Student Name", "Room", "Meals", "Amount(INR)", "Status"));
        sb.append(subline).append("\n");

        double totalBilled = 0;
        double totalPaid = 0;
        double totalUnpaid = 0;
        int totalMeals = 0;

        for (Bill b : bills) {
            sb.append(String.format("%-8d | %-10s | %-20s | %-6s | %-6d | %10.2f | %-8s%n",
                    b.getBillId(),
                    b.getStudentId(),
                    truncate(b.getStudentName(), 20),
                    b.getRoomNo(),
                    b.getTotalMeals(),
                    b.getAmount(),
                    b.getPaidStatus()));

            totalBilled += b.getAmount();
            totalMeals += b.getTotalMeals();
            if (b.isPaid()) {
                totalPaid += b.getAmount();
            } else {
                totalUnpaid += b.getAmount();
            }
        }

        sb.append(subline).append("\n");
        sb.append(String.format(" TOTAL STUDENTS BILLED : %d%n", bills.size()));
        sb.append(String.format(" TOTAL MEALS SERVED    : %d%n", totalMeals));
        sb.append(String.format(" TOTAL BILLED AMOUNT   : INR %.2f%n", totalBilled));
        sb.append(String.format(" TOTAL COLLECTED (PAID): INR %.2f%n", totalPaid));
        sb.append(String.format(" TOTAL OUTSTANDING DUE : INR %.2f%n", totalUnpaid));
        sb.append(line).append("\n");

        return sb.toString();
    }

    public static String generateAttendanceSummaryReport(String month, List<AttendanceSummary> list) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(75);
        String subline = "-".repeat(75);

        sb.append(line).append("\n");
        sb.append(String.format("               HOSTEL MESS ATTENDANCE AUDIT SHEET: %s%n", month));
        sb.append(String.format("               Generated: %s%n", LocalDateTime.now().format(DTM_FMT)));
        sb.append(line).append("\n");
        sb.append(String.format("%-10s | %-22s | %-8s | %-9s | %-8s | %-10s%n",
                "Student ID", "Student Name", "Room No", "Present", "Absent", "Attendance%"));
        sb.append(subline).append("\n");

        int grandPresent = 0;
        int grandAbsent = 0;

        for (AttendanceSummary s : list) {
            double rate = s.getTotalMeals() > 0 ? (s.getPresentCount() * 100.0 / s.getTotalMeals()) : 0.0;
            sb.append(String.format("%-10s | %-22s | %-8s | %-9d | %-8d | %9.1f%%%n",
                    s.getStudentId(),
                    truncate(s.getStudentName(), 22),
                    s.getRoomNo(),
                    s.getPresentCount(),
                    s.getAbsentCount(),
                    rate));

            grandPresent += s.getPresentCount();
            grandAbsent += s.getAbsentCount();
        }

        sb.append(subline).append("\n");
        sb.append(String.format(" Total Students Evaluated: %d%n", list.size()));
        sb.append(String.format(" Total Present Marks     : %d%n", grandPresent));
        sb.append(String.format(" Total Absent Marks      : %d%n", grandAbsent));
        sb.append(line).append("\n");

        return sb.toString();
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) return "";
        if (val.length() <= maxLen) return val;
        return val.substring(0, maxLen - 2) + "..";
    }
}
