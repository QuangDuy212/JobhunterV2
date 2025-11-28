package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.util.BaseSpecs;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public RoleService(RoleRepository roleRepository, PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
    }

    public Role fetchRoleById(long id) {
        Optional<Role> rOptional = this.roleRepository.findById(id);
        if (rOptional.isPresent())
            return rOptional.get();
        return null;
    }

    public List<Role> fetchListRolesByListIds(List<Long> listIds) {
        return this.roleRepository.findByIdIn(listIds);
    }

    public boolean existByName(String name) {
        return this.roleRepository.existsByName(name);
    }

    public boolean handleCheckUpdate(Role reqRole) {
        String oldName = this.fetchRoleById(reqRole.getId()).getName();
        boolean check = reqRole.getName().equals(oldName);
        if (this.existByName(reqRole.getName()) && !check)
            return false;
        return true;
    }

    public Role handleCreateRole(Role reqRole) {
        // special
        if (reqRole.getPermissions() != null) {
            List<Long> idPermissions = reqRole.getPermissions()
                    .stream().map(i -> i.getId()).collect(Collectors.toList());
            reqRole.setPermissions(this.permissionService.fetchPermissionsByIds(idPermissions));
        }
        return this.roleRepository.save(reqRole);
    }

    public Role handleUpdateRole(Role reqRole) {
        Role currentRole = this.fetchRoleById(reqRole.getId());
        currentRole.setActive(reqRole.isActive());
        if (reqRole.getName() != null && this.existByName(reqRole.getName())) {
            currentRole.setName(reqRole.getName());
        }
        if (reqRole.getDescription() != null) {
            currentRole.setDescription(reqRole.getDescription());
        }
        if (reqRole.getPermissions() != null) {
            List<Long> idPermissions = reqRole.getPermissions()
                    .stream().map(i -> i.getId()).collect(Collectors.toList());
            currentRole.setPermissions(this.permissionService.fetchPermissionsByIds(idPermissions));
        }
        return this.roleRepository.save(currentRole);
    }

    public ResultPaginationDTO fetchAllRoles(Specification<Role> spec, Pageable pageable) {
        Specification<Role> rolesSpec = BaseSpecs.isActive();
        Page<Role> pageRoles = this.roleRepository.findAll(rolesSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageRoles.getTotalPages());
        mt.setTotal(pageRoles.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageRoles.getContent());
        return rs;
    }
    public ResultPaginationDTO fetchDeletedRoles(Specification<Role> spec, Pageable pageable) {
        Specification<Role> deletedRolesSpec = BaseSpecs.isDeleted();
        Page<Role> pageDelRoles = this.roleRepository.findAll(deletedRolesSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDelRoles.getTotalPages());
        mt.setTotal(pageDelRoles.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageDelRoles.getContent());
        return rs;
    }
    public List<Role> fetchDeletedRoles(){
        return this.roleRepository.findAllByDeletedTrue();
    }
    public void handleDeleteARole(long id) {
        this.roleRepository.deleteById(id);
    }
    public void softDeleteRole(long id){
        Role role = this.roleRepository.findById(id).orElse(null);
        role.setDeleted(true);
        role.setDeletedAt(LocalDateTime.now());
        this.roleRepository.save(role);
    }
    public void restoreRole(long id){
        Role role = this.roleRepository.findById(id).orElse(null);
        role.setDeleted(false);
        role.setDeletedAt(null);
        this.roleRepository.save(role);
    }
    @Scheduled(cron = "0 0 2 * * *") 
    public void autoHardDelete() {
        LocalDateTime limit = LocalDateTime.now().minusDays(30);
        List<Role> expired = roleRepository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        if (!expired.isEmpty()) {
            roleRepository.deleteAll(expired);
            System.out.println("Auto hard delete roles executed, removed: " + expired.size() + " records");
        }
    }
}
