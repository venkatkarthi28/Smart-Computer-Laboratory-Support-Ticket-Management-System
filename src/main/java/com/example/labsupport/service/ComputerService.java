package com.example.labsupport.service;

import com.example.labsupport.dto.request.ComputerRequest;
import com.example.labsupport.dto.response.ComputerResponse;
import com.example.labsupport.dto.response.MaintenanceHistoryResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.ComputerStatus;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.exception.ConflictException;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.ComputerMapper;
import com.example.labsupport.mapper.TicketMapper;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.MaintenanceHistoryRepository;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.util.Pages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ComputerService {

    private final ComputerRepository computerRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final TicketRepository ticketRepository;
    private final MaintenanceHistoryRepository maintenanceRepository;

    public ComputerService(ComputerRepository computerRepository, LaboratoryRepository laboratoryRepository,
                           TicketRepository ticketRepository, MaintenanceHistoryRepository maintenanceRepository) {
        this.computerRepository = computerRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.ticketRepository = ticketRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public PageResponse<ComputerResponse> search(Long laboratoryId, ComputerStatus status, int page, int size) {
        return PageResponse.from(
                computerRepository.search(laboratoryId, status, Pages.of(page, size, org.springframework.data.domain.Sort.by("computerCode"))).map(ComputerMapper::toResponse));
    }

    public ComputerResponse get(Long id) {
        return ComputerMapper.toResponse(find(id));
    }

    @Transactional
    public ComputerResponse create(ComputerRequest request) {
        String code = request.computerCode().trim().toUpperCase();
        if (computerRepository.existsByComputerCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A computer with this code already exists");
        }
        Computer computer = new Computer();
        apply(computer, request, code);
        return ComputerMapper.toResponse(computerRepository.save(computer));
    }

    @Transactional
    public ComputerResponse update(Long id, ComputerRequest request) {
        Computer computer = find(id);
        String code = request.computerCode().trim().toUpperCase();
        boolean taken = computerRepository.findByComputerCodeIgnoreCase(code)
                .filter(other -> !other.getId().equals(id)).isPresent();
        if (taken) {
            throw new DuplicateResourceException("A computer with this code already exists");
        }
        apply(computer, request, code);
        return ComputerMapper.toResponse(computerRepository.save(computer));
    }

    /** Blocked when the computer has tickets: mark it OUT_OF_SERVICE instead. */
    @Transactional
    public void delete(Long id) {
        Computer computer = find(id);
        if (ticketRepository.existsByComputerId(id)) {
            throw new ConflictException("This computer has tickets and cannot be deleted. Set it OUT_OF_SERVICE instead");
        }
        computerRepository.delete(computer);
        computerRepository.flush();
    }

    public List<MaintenanceHistoryResponse> maintenanceHistory(Long computerId) {
        find(computerId);
        return maintenanceRepository.findByComputerIdOrderByResolvedAtDesc(computerId).stream()
                .map(TicketMapper::toMaintenance).toList();
    }

    private void apply(Computer c, ComputerRequest r, String code) {
        Laboratory lab = laboratoryRepository.findById(r.laboratoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory not found"));
        c.setComputerCode(code);
        c.setComputerName(r.computerName().trim());
        c.setLaboratory(lab);
        c.setBrand(blankToNull(r.brand()));
        c.setModel(blankToNull(r.model()));
        c.setProcessor(blankToNull(r.processor()));
        c.setRam(blankToNull(r.ram()));
        c.setStorageCapacity(blankToNull(r.storageCapacity()));
        c.setOperatingSystem(blankToNull(r.operatingSystem()));
        c.setIpAddress(blankToNull(r.ipAddress()));
        c.setStatus(r.status() == null ? ComputerStatus.WORKING : r.status());
        c.setPurchaseDate(r.purchaseDate());
    }

    private Computer find(Long id) {
        return computerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Computer not found"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
