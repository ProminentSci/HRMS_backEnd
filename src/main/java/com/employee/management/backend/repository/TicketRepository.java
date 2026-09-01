package com.employee.management.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.employee.management.backend.Entity.Ticket;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByEmployeeEmpIdOrderByCreatedAtDesc(Long empId);

    // Scoped to the admin's own client so one company's admin can never see or touch
    // another company's tickets - same isolation principle as EmployeeRepository's
    // clientId-scoped queries.
    List<Ticket> findByEmployeeClientIdOrderByCreatedAtDesc(Long clientId);
}
