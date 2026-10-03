package com.example.labsupport.service;

import com.example.labsupport.dto.request.LaboratoryRequest;
import com.example.labsupport.dto.request.LaboratoryTechniciansRequest;
import com.example.labsupport.dto.response.LaboratoryResponse;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.BadRequestException;
import com.example.labsupport.exception.ConflictException;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.LaboratoryMapper;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final ComputerRepository computerRepository;
    private final UserRepository userRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository, ComputerRepository computerRepository,
                             UserRepository userRepository) {
        this.laboratoryRepository = laboratoryRepository;
        this.computerRepository = computerRepository;
        this.userRepository = userRepository;
    }

    public List<LaboratoryResponse> listAll() {
        return laboratoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Laboratory::getName))
                .map(this::toResponse)
                .toList();
    }

    public LaboratoryResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public LaboratoryResponse create(LaboratoryRequest request) {
        String name = request.name().trim();
        if (laboratoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A laboratory with this name already exists");
        }
        Laboratory lab = new Laboratory();
        apply(lab, request);
        return toResponse(laboratoryRepository.save(lab));
    }

    @Transactional
    public LaboratoryResponse update(Long id, LaboratoryRequest request) {
        Laboratory lab = find(id);
        String name = request.name().trim();
        boolean taken = laboratoryRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id)).isPresent();
        if (taken) {
            throw new DuplicateResourceException("A laboratory with this name already exists");
        }
        apply(lab, request);
        return toResponse(laboratoryRepository.save(lab));
    }

    @Transactional
    public void delete(Long id) {
        Laboratory lab = find(id);
        if (computerRepository.existsByLaboratoryId(id)) {
            throw new ConflictException("This laboratory still has computers. Remove or move them first");
        }
        laboratoryRepository.delete(lab);
        laboratoryRepository.flush();
    }

    /** Replaces the lab's technician list with exactly the given ids. */
    @Transactional
    public LaboratoryResponse setTechnicians(Long id, LaboratoryTechniciansRequest request) {
        Laboratory lab = laboratoryRepository.findWithTechniciansById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory not found"));
        Set<Long> wantedIds = request.technicianIds();

        List<User> wanted = userRepository.findAllById(wantedIds);
        if (wanted.size() != wantedIds.size()) {
            throw new BadRequestException("One or more technician ids do not exist");
        }
        for (User u : wanted) {
            if (u.getRole() != Role.TECHNICIAN || !u.isActive()) {
                throw new BadRequestException("User " + u.getId() + " is not an active technician");
            }
        }
        for (User current : new ArrayList<>(lab.getTechnicians())) {
            if (!wantedIds.contains(current.getId())) {
                lab.removeTechnician(current);
            }
        }
        for (User u : wanted) {
            lab.addTechnician(u);
        }
        return toResponse(laboratoryRepository.save(lab));
    }

    private void apply(Laboratory lab, LaboratoryRequest request) {
        lab.setName(request.name().trim());
        lab.setLocation(blankToNull(request.location()));
        lab.setDescription(blankToNull(request.description()));
    }

    private Laboratory find(Long id) {
        return laboratoryRepository.findWithTechniciansById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory not found"));
    }

    private LaboratoryResponse toResponse(Laboratory lab) {
        return LaboratoryMapper.toResponse(lab, computerRepository.countByLaboratoryId(lab.getId()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
