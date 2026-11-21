package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Bill;

import java.time.LocalDate;
import java.util.List;

public class BillingDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public boolean generateBillForStudent(String studentId, String month, double perMealRate) {
        return manager.generateBillForStudent(studentId, month, perMealRate);
    }

    public int generateBillsForMonth(String month, double perMealRate) {
        return manager.generateBillsForMonth(month, perMealRate);
    }

    public List<Bill> getAllBills() {
        return manager.getAllBills();
    }

    public List<Bill> getBillsByMonth(String month) {
        return manager.getBillsByMonth(month);
    }

    public Bill getBillById(int billId) {
        return manager.getBillById(billId);
    }

    public Bill getBillByStudentAndMonth(String studentId, String month) {
        return manager.getBillByStudentAndMonth(studentId, month);
    }

    public boolean updatePaymentStatus(int billId, String status, LocalDate paidDate) {
        return manager.updatePaymentStatus(billId, status, paidDate);
    }

    public boolean deleteBill(int billId) {
        return manager.deleteBill(billId);
    }

    public double getTotalRevenue(String month) {
        return manager.getTotalRevenue(month);
    }

    public double getUnpaidTotal(String month) {
        return manager.getUnpaidTotal(month);
    }
}
