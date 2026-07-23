package com.medsupply.platform.modules.salesman.service;

import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import com.medsupply.platform.modules.salesman.repository.SalesmanLeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesmanServiceImpl implements SalesmanService {

    private final SalesmanLeadRepository leadRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalesmanLead> getAllLeads() {
        return leadRepository.findAll().stream()
                .filter(l -> !l.isDeleted())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesmanLead> getLeadsBySalesman(UUID salesmanId) {
        return leadRepository.findBySalesmanIdAndIsDeletedFalse(salesmanId);
    }

    @Override
    public SalesmanLead createLead(UUID salesmanId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes) {
        User salesman = userRepository.findById(salesmanId)
                .orElseThrow(() -> new IllegalArgumentException("Salesman user not found: " + salesmanId));

        SalesmanLead lead = SalesmanLead.builder()
                .salesman(salesman)
                .name(name)
                .email(email)
                .phone(phone)
                .company(company)
                .pipeValue(pipeValue != null ? pipeValue : BigDecimal.ZERO)
                .source(source)
                .notes(notes)
                .status("LEAD")
                .build();

        return leadRepository.save(lead);
    }

    @Override
    public SalesmanLead updateLeadStatus(UUID leadId, String status) {
        SalesmanLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("CRM Lead not found: " + leadId));
        lead.setStatus(status);
        return leadRepository.save(lead);
    }
}
