package com.medsupply.platform.modules.salesman.service;

import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SalesmanService {
    List<SalesmanLead> getAllLeads();
    Page<SalesmanLead> getAllLeads(Pageable pageable);
    List<SalesmanLead> getLeadsBySalesman(UUID salesmanId);
    SalesmanLead createLead(UUID salesmanId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes);
    SalesmanLead updateLead(UUID leadId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes);
    SalesmanLead updateLeadStatus(UUID leadId, String status);
    void deleteLead(UUID leadId);
}
