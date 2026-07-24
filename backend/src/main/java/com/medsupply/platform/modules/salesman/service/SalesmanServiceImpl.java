package com.medsupply.platform.modules.salesman.service;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.model.UserStatus;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import com.medsupply.platform.modules.salesman.model.LeadStatus;
import com.medsupply.platform.modules.salesman.repository.SalesmanLeadRepository;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesmanServiceImpl implements SalesmanService {

    private final SalesmanLeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    @Override
    @Transactional(readOnly = true)
    public List<SalesmanLead> getAllLeads() {
        // Move soft-delete filtering to repository query for superior performance
        return leadRepository.findByIsDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesmanLead> getAllLeads(Pageable pageable) {
        // Soft-delete filtering in repository with full pagination support
        return leadRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesmanLead> getLeadsBySalesman(UUID salesmanId) {
        return leadRepository.findBySalesmanIdAndIsDeletedFalse(salesmanId);
    }

    @Override
    public SalesmanLead createLead(UUID salesmanId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes) {
        // Validate salesman exists and is ACTIVE
        User salesman = userRepository.findById(salesmanId)
                .orElseThrow(() -> new DomainException("SALESMAN_NOT_FOUND", "Salesman user not found: " + salesmanId, HttpStatus.NOT_FOUND));
        
        if (salesman.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("INACTIVE_SALESMAN", "Salesman account is not ACTIVE. Current status: " + salesman.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Validate formats
        validateEmailFormat(email);
        validatePhoneFormat(phone);

        // Reject negative pipeline value
        if (pipeValue != null && pipeValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("INVALID_PIPELINE_VALUE", "Pipeline value cannot be negative.", HttpStatus.BAD_REQUEST);
        }

        // Validate duplicate Lead (email / phone)
        if (leadRepository.existsByEmailIgnoreCaseAndIsDeletedFalse(email)) {
            throw new DomainException("DUPLICATE_LEAD_EMAIL", "A lead with this email already exists: " + email, HttpStatus.BAD_REQUEST);
        }
        if (leadRepository.existsByPhoneAndIsDeletedFalse(phone)) {
            throw new DomainException("DUPLICATE_LEAD_PHONE", "A lead with this phone number already exists: " + phone, HttpStatus.BAD_REQUEST);
        }

        SalesmanLead lead = SalesmanLead.builder()
                .salesman(salesman)
                .name(name)
                .email(email)
                .phone(phone)
                .company(company)
                .pipeValue(pipeValue != null ? pipeValue : BigDecimal.ZERO)
                .source(source)
                .notes(notes)
                .status(LeadStatus.LEAD)
                .build();

        SalesmanLead savedLead = leadRepository.save(lead);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "LEAD_CREATED", "SALESMAN",
                "CRM Lead '" + savedLead.getName() + "' created for salesman " + salesman.getFirstName() + " " + salesman.getLastName(), getClientIp());

        return savedLead;
    }

    @Override
    public SalesmanLead updateLead(UUID leadId, String name, String email, String phone, String company, BigDecimal pipeValue, String source, String notes) {
        SalesmanLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new DomainException("LEAD_NOT_FOUND", "CRM Lead not found: " + leadId, HttpStatus.NOT_FOUND));

        if (lead.isDeleted()) {
            throw new DomainException("LEAD_INACTIVE", "CRM Lead is deleted/inactive: " + leadId, HttpStatus.BAD_REQUEST);
        }

        // Validate formats
        validateEmailFormat(email);
        validatePhoneFormat(phone);

        // Reject negative pipeline value
        if (pipeValue != null && pipeValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("INVALID_PIPELINE_VALUE", "Pipeline value cannot be negative.", HttpStatus.BAD_REQUEST);
        }

        // Validate duplicate Lead (email / phone) if they changed
        if (!lead.getEmail().equalsIgnoreCase(email) && leadRepository.existsByEmailIgnoreCaseAndIsDeletedFalse(email)) {
            throw new DomainException("DUPLICATE_LEAD_EMAIL", "A lead with this email already exists: " + email, HttpStatus.BAD_REQUEST);
        }
        if (!lead.getPhone().equals(phone) && leadRepository.existsByPhoneAndIsDeletedFalse(phone)) {
            throw new DomainException("DUPLICATE_LEAD_PHONE", "A lead with this phone number already exists: " + phone, HttpStatus.BAD_REQUEST);
        }

        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setCompany(company);
        lead.setPipeValue(pipeValue != null ? pipeValue : BigDecimal.ZERO);
        lead.setSource(source);
        lead.setNotes(notes);

        SalesmanLead savedLead = leadRepository.save(lead);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "LEAD_UPDATED", "SALESMAN",
                "CRM Lead '" + savedLead.getName() + "' updated details", getClientIp());

        return savedLead;
    }

    @Override
    public SalesmanLead updateLeadStatus(UUID leadId, String status) {
        SalesmanLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new DomainException("LEAD_NOT_FOUND", "CRM Lead not found: " + leadId, HttpStatus.NOT_FOUND));

        if (lead.isDeleted()) {
            throw new DomainException("LEAD_INACTIVE", "CRM Lead is deleted/inactive: " + leadId, HttpStatus.BAD_REQUEST);
        }

        // Convert and validate against LeadStatus Enum
        LeadStatus oldStatus = lead.getStatus();
        LeadStatus newStatus;
        try {
            newStatus = LeadStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_LEAD_STATUS", "Invalid lead status: " + status + ". Supported: LEAD, CONTACTED, NEGOTIATING, ONBOARDED", HttpStatus.BAD_REQUEST);
        }

        lead.setStatus(newStatus);
        SalesmanLead savedLead = leadRepository.save(lead);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "STATUS_CHANGED", "SALESMAN",
                "CRM Lead '" + lead.getName() + "' status changed from " + oldStatus + " to " + newStatus, getClientIp());

        return savedLead;
    }

    @Override
    public void deleteLead(UUID leadId) {
        SalesmanLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new DomainException("LEAD_NOT_FOUND", "CRM Lead not found: " + leadId, HttpStatus.NOT_FOUND));

        if (lead.isDeleted()) {
            throw new DomainException("LEAD_ALREADY_DELETED", "CRM Lead is already deleted: " + leadId, HttpStatus.BAD_REQUEST);
        }

        lead.softDelete();
        leadRepository.save(lead);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "LEAD_DELETED", "SALESMAN",
                "CRM Lead '" + lead.getName() + "' soft-deleted", getClientIp());
    }

    private void validateEmailFormat(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("INVALID_EMAIL_FORMAT", "Invalid email format: " + email, HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePhoneFormat(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new DomainException("INVALID_PHONE_FORMAT", "Invalid mobile/phone number format. Must contain 10 to 15 digits.", HttpStatus.BAD_REQUEST);
        }
    }

    private User getCurrentUser() {
        if (userRepository == null) return null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return userRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // Log or ignore gracefully
        }
        return null;
    }

    private String getCurrentUserRole(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "ANONYMOUS";
        }
        return "ROLE_" + user.getRoles().iterator().next().getName().name();
    }

    private String getClientIp() {
        if (httpServletRequest == null) return "127.0.0.1";
        try {
            String ipList = httpServletRequest.getHeader("X-Forwarded-For");
            if (ipList != null && !ipList.isEmpty()) {
                return ipList.split(",")[0].trim();
            }
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
