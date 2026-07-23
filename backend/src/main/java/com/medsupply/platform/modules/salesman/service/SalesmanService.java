package com.medsupply.platform.modules.salesman.service;

import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SalesmanService {
    List<SalesmanLead> getAllLeads();
    List<SalesmanLead> getLeadsBySalesman(UUID salesmanId);
    SalesmanLead createLead(UUID salesmanId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes);
    SalesmanLead updateLeadStatus(UUID leadId, String status);
}
