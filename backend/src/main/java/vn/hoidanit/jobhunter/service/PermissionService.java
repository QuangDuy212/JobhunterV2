package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.util.BaseSpecs;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public boolean isSameName(Permission p) {
        Permission pDB = this.fetchPermissionById(p.getId());
        if (pDB == null)
            return false;
        return pDB.getName().equals(p.getName());
    }

    public boolean isPermissionExist(Permission p) {
        return this.permissionRepository.existsByModuleAndApiPathAndMethod(
                p.getModule(),
                p.getApiPath(),
                p.getMethod());
    }

    public Permission fetchPermissionById(long id) {
        Optional<Permission> pOptional = this.permissionRepository.findById(id);
        if (pOptional.isPresent())
            return pOptional.get();
        return null;
    }

    public List<Permission> fetchPermissionsByIds(List<Long> listIds) {
        return this.permissionRepository.findByIdIn(listIds);
    }

    public Permission handleCreatePermission(Permission reqPermission) {
        // special
        return this.permissionRepository.save(reqPermission);
    }

    public Permission handleUpdatePermission(Permission reqPermission) {
        Permission currentPermission = this.fetchPermissionById(reqPermission.getId());
        if (currentPermission != null) {
            if (reqPermission.getName() != null)
                currentPermission.setName(reqPermission.getName());
            if (reqPermission.getApiPath() != null)
                currentPermission.setApiPath(reqPermission.getApiPath());
            if (reqPermission.getMethod() != null)
                currentPermission.setMethod(reqPermission.getMethod());
            if (reqPermission.getModule() != null)
                currentPermission.setModule(reqPermission.getModule());
        }
        return this.permissionRepository.save(currentPermission);
    }

    public ResultPaginationDTO fetchAllPermissions(Specification<Permission> spec, Pageable pageable) {
        Specification<Permission> permissionsSpec = BaseSpecs.isActive();
        Page<Permission> pagePermission = this.permissionRepository.findAll(permissionsSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pagePermission.getTotalPages());
        mt.setTotal(pagePermission.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pagePermission.getContent());
        return rs;
    }
    public ResultPaginationDTO fetchDeletedPermissions(Specification<Permission> spec, Pageable pageable) {
        Specification<Permission> delPermissionsSpec = BaseSpecs.isDeleted();
        Page<Permission> pageDelPermission = this.permissionRepository.findAll(delPermissionsSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDelPermission.getTotalPages());
        mt.setTotal(pageDelPermission.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageDelPermission.getContent());
        return rs;
    }

    public void handleDeletePermission(long id) {
        // delete permission_role
        Optional<Permission> permissionOptional = this.permissionRepository.findById(id);
        Permission currentPermission = permissionOptional.get();
        currentPermission.getRoles().forEach(i -> i.getPermissions().remove(currentPermission));

        // delete permission
        this.permissionRepository.delete(currentPermission);
    }

    public void softDeletePermission(long id) {
        Permission permission = permissionRepository.findById(id).orElse(null);

        // soft delete
        permission.setDeleted(true);
        permission.setDeletedAt(LocalDateTime.now());

        permissionRepository.save(permission);
    }

    public void restorePermission(long id){
        Permission permission = this.permissionRepository.findById(id).orElse(null);
        permission.setDeleted(false);
        permission.setDeletedAt(null);
        this.permissionRepository.save(permission);
    }
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoHardDeletePermission() {

        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        List<Permission> expiredPermissions = this.permissionRepository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        for (Permission permission : expiredPermissions) {

            // XÓA QUAN HỆ TRONG permission_role
            permission.getRoles().forEach(job -> job.getPermissions().remove(permission));
            permission.getRoles().clear();
            //xóa hẳn permission
            this.permissionRepository.delete(permission);
        }
        System.out.println("Auto hard delete permissions executed, removed: " + expiredPermissions.size() + " records");
    }
}
