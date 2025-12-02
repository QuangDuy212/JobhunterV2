package vn.hoidanit.jobhunter.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Skill;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    boolean existsByModuleAndApiPathAndMethod(String module, String apiPath, String method);

    List<Permission> findByIdIn(List<Long> listIds);

    List<Permission> findAllByDeletedTrue();

    List<Permission> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);
    Permission findByApiPathAndMethod(String apiPath, String method);
}
