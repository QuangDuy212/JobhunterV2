package vn.hoidanit.jobhunter.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Resume;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {
    boolean existsByUserAndJob(User user, Job job);
    List<Resume> findAllByCreatedAtBetween(Instant startDate, Instant endDate);
    long countByStatus(ResumeStateEnum status);
    List<Resume> findAllByDeletedTrue();

    List<Resume> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime limit);

}
