package com.messmgmt.dao;

import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Complaint;

import java.util.List;

public class ComplaintDAO {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    public boolean addComplaint(Complaint complaint) {
        return manager.addComplaint(complaint);
    }

    public List<Complaint> getAllComplaints() {
        return manager.getAllComplaints();
    }

    public List<Complaint> getComplaintsByStatus(String status) {
        return manager.getComplaintsByStatus(status);
    }

    public boolean updateStatusAndNote(int complaintId, String status, String note) {
        return manager.updateComplaintStatusAndNote(complaintId, status, note);
    }

    public boolean deleteComplaint(int complaintId) {
        return manager.deleteComplaint(complaintId);
    }

    public int getPendingCount() {
        return manager.getPendingComplaintCount();
    }
}
