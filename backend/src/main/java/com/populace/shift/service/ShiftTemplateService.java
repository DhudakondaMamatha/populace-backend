package com.populace.shift.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.*;
import com.populace.domain.enums.ShiftStatus;
import com.populace.repository.*;
import com.populace.shift.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

    private final ScheduleTemplateRepository templateRepository;
    private final ScheduleTemplateShiftRepository templateShiftRepository;
    private final ShiftRepository shiftRepository;
    private final BusinessRepository businessRepository;
    private final SiteRepository siteRepository;
    private final RoleRepository roleRepository;
    private final RoleComboRepository roleComboRepository;

    public ShiftTemplateService(ScheduleTemplateRepository templateRepository,
                                ScheduleTemplateShiftRepository templateShiftRepository,
                                ShiftRepository shiftRepository,
                                BusinessRepository businessRepository,
                                SiteRepository siteRepository,
                                RoleRepository roleRepository,
                                RoleComboRepository roleComboRepository) {
        this.templateRepository = templateRepository;
        this.templateShiftRepository = templateShiftRepository;
        this.shiftRepository = shiftRepository;
        this.businessRepository = businessRepository;
        this.siteRepository = siteRepository;
        this.roleRepository = roleRepository;
        this.roleComboRepository = roleComboRepository;
    }

    @Transactional
    public ScheduleTemplateDto getOrCreateForSite(Long businessId, Long siteId) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new ResourceNotFoundException("Site", siteId));

        // Look for existing active template for this business that has entries for this site
        List<ScheduleTemplate> templates = templateRepository.findByBusiness_IdAndActiveTrue(businessId);
        for (ScheduleTemplate t : templates) {
            List<ScheduleTemplateShift> entries = templateShiftRepository.findByTemplate_Id(t.getId());
            if (entries.isEmpty() || entries.stream().anyMatch(e -> e.getSite().getId().equals(siteId))) {
                return toDto(t, entries, siteId, site.getName());
            }
        }

        // No template found - create one
        return createEmptyTemplate(businessId, site);
    }

    private ScheduleTemplateDto createEmptyTemplate(Long businessId, Site site) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));

        ScheduleTemplate template = new ScheduleTemplate();
        template.setBusiness(business);
        template.setName("Weekly Template - " + site.getName());
        template.setActive(true);
        template = templateRepository.save(template);

        return toDto(template, List.of(), site.getId(), site.getName());
    }

    @Transactional
    public ScheduleTemplateDto saveTemplate(Long businessId, ScheduleTemplateSaveRequest request) {
        Site site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new ResourceNotFoundException("Site", request.siteId()));

        // Find or create template
        ScheduleTemplate template;
        List<ScheduleTemplate> templates = templateRepository.findByBusiness_IdAndActiveTrue(businessId);
        ScheduleTemplate existing = templates.stream()
            .filter(t -> {
                List<ScheduleTemplateShift> entries = templateShiftRepository.findByTemplate_Id(t.getId());
                return entries.isEmpty() || entries.stream().anyMatch(e -> e.getSite().getId().equals(request.siteId()));
            })
            .findFirst()
            .orElse(null);

        if (existing != null) {
            template = existing;
            // Delete existing entries for this site
            List<ScheduleTemplateShift> oldEntries = templateShiftRepository.findByTemplate_Id(template.getId());
            List<ScheduleTemplateShift> siteEntries = oldEntries.stream()
                .filter(e -> e.getSite().getId().equals(request.siteId()))
                .toList();
            templateShiftRepository.deleteAll(siteEntries);
        } else {
            Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
            template = new ScheduleTemplate();
            template.setBusiness(business);
            template.setActive(true);
        }

        // Update name if provided
        if (request.name() != null && !request.name().isBlank()) {
            template.setName(request.name());
        } else if (template.getName() == null) {
            template.setName("Weekly Template - " + site.getName());
        }
        template = templateRepository.save(template);

        // Insert new entries
        List<ScheduleTemplateShift> newEntries = new ArrayList<>();
        if (request.entries() != null) {
            for (ScheduleTemplateSaveRequest.Entry entry : request.entries()) {
                validateEntry(entry);

                ScheduleTemplateShift shift = new ScheduleTemplateShift();
                shift.setTemplate(template);
                shift.setSite(site);
                shift.setDayOfWeek(entry.dayOfWeek());
                shift.setStartTime(entry.startTime());
                shift.setEndTime(entry.endTime());
                shift.setBreakDurationMinutes(entry.breakDurationMinutes() != null ? entry.breakDurationMinutes() : 0);
                shift.setStaffRequired(entry.staffRequired() != null ? entry.staffRequired() : 1);

                if (entry.roleComboId() != null) {
                    RoleCombo combo = roleComboRepository.findById(entry.roleComboId())
                        .orElseThrow(() -> new ResourceNotFoundException("RoleCombo", entry.roleComboId()));
                    shift.setRoleCombo(combo);
                } else {
                    Role role = roleRepository.findById(entry.roleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Role", entry.roleId()));
                    shift.setRole(role);
                }

                newEntries.add(shift);
            }
        }
        newEntries = templateShiftRepository.saveAll(newEntries);

        log.info("Saved template {} with {} entries for site {}", template.getId(), newEntries.size(), site.getName());
        return toDto(template, newEntries, site.getId(), site.getName());
    }

    @Transactional
    public GenerateShiftsFromTemplateResponse generateShifts(Long businessId, Long templateId,
                                                              GenerateShiftsFromTemplateRequest request) {
        ScheduleTemplate template = templateRepository.findByIdAndBusiness_Id(templateId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduleTemplate", templateId));

        Business business = template.getBusiness();
        List<ScheduleTemplateShift> entries = templateShiftRepository.findByTemplate_Id(templateId);

        int created = 0;
        int skipped = 0;
        List<String> skippedDates = new ArrayList<>();

        LocalDate currentDate = request.startDate();
        while (!currentDate.isAfter(request.endDate())) {
            int dbDayOfWeek = convertToDatabaseDayOfWeek(currentDate);

            List<ScheduleTemplateShift> dayEntries = entries.stream()
                .filter(e -> e.getDayOfWeek() == dbDayOfWeek)
                .toList();

            for (ScheduleTemplateShift entry : dayEntries) {
                // For combo entries, use the first role from the combo
                Role effectiveRole = entry.getRole();
                if (effectiveRole == null && entry.getRoleCombo() != null) {
                    List<RoleComboRole> comboRoles = entry.getRoleCombo().getRoleComboRoles();
                    if (!comboRoles.isEmpty()) {
                        effectiveRole = comboRoles.get(0).getRole();
                    }
                }
                if (effectiveRole == null) {
                    skipped++;
                    continue;
                }

                if (shiftExists(businessId, entry.getSite().getId(), effectiveRole.getId(),
                        currentDate, entry.getStartTime(), entry.getEndTime())) {
                    skipped++;
                    if (!skippedDates.contains(currentDate.toString())) {
                        skippedDates.add(currentDate.toString());
                    }
                } else {
                    Shift shift = new Shift();
                    shift.setBusiness(business);
                    shift.setSite(entry.getSite());
                    shift.setRole(effectiveRole);
                    shift.setShiftDate(currentDate);
                    shift.setStartTime(entry.getStartTime());
                    shift.setEndTime(entry.getEndTime());
                    shift.setBreakDurationMinutes(entry.getBreakDurationMinutes());
                    shift.setStaffRequired(entry.getStaffRequired());
                    shift.setStaffAllocated(0);
                    shift.setStatus(ShiftStatus.open);
                    shift.setTemplateShiftId(entry.getId());
                    shiftRepository.save(shift);
                    created++;
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        log.info("Generated {} shifts from template {}, skipped {}", created, templateId, skipped);
        return GenerateShiftsFromTemplateResponse.of(created, skipped, skippedDates);
    }

    @Transactional
    public void deleteTemplate(Long businessId, Long templateId) {
        ScheduleTemplate template = templateRepository.findByIdAndBusiness_Id(templateId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduleTemplate", templateId));
        templateRepository.delete(template);
        log.info("Deleted template {}", templateId);
    }

    private boolean shiftExists(Long businessId, Long siteId, Long roleId,
                                 LocalDate date, LocalTime startTime, LocalTime endTime) {
        return shiftRepository.findByBusiness_IdAndShiftDateBetween(businessId, date, date)
            .stream()
            .anyMatch(s -> s.getSite().getId().equals(siteId)
                && s.getRole().getId().equals(roleId)
                && s.getStartTime().equals(startTime)
                && s.getEndTime().equals(endTime)
                && s.getStatus() != ShiftStatus.cancelled);
    }

    private void validateEntry(ScheduleTemplateSaveRequest.Entry entry) {
        if (entry.dayOfWeek() < 0 || entry.dayOfWeek() > 6) {
            throw new ValidationException("dayOfWeek", "Day of week must be between 0 (Sunday) and 6 (Saturday)");
        }
        if (!entry.startTime().isBefore(entry.endTime())) {
            throw new ValidationException("time", "Start time must be before end time");
        }
    }

    private int convertToDatabaseDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7;
    }

    private ScheduleTemplateDto toDto(ScheduleTemplate template, List<ScheduleTemplateShift> entries,
                                       Long siteId, String siteName) {
        List<ScheduleTemplateEntryDto> entryDtos = entries.stream()
            .filter(e -> e.getSite().getId().equals(siteId))
            .map(e -> new ScheduleTemplateEntryDto(
                e.getRole() != null ? e.getRole().getId() : null,
                e.getRole() != null ? e.getRole().getName() : null,
                e.getRole() != null ? e.getRole().getColor() : null,
                e.getRoleCombo() != null ? e.getRoleCombo().getId() : null,
                e.getRoleCombo() != null ? e.getRoleCombo().getName() : null,
                e.getRoleCombo() != null ? e.getRoleCombo().getColor() : null,
                e.getDayOfWeek(),
                e.getStartTime(),
                e.getEndTime(),
                e.getBreakDurationMinutes(),
                e.getStaffRequired()
            ))
            .collect(Collectors.toList());

        return new ScheduleTemplateDto(
            template.getId(),
            siteId,
            siteName,
            template.getName(),
            template.getActive(),
            entryDtos
        );
    }
}
