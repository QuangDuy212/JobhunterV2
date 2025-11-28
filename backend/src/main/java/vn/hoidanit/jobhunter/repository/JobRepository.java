package vn.hoidanit.jobhunter.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findBySkillsIn(List<Skill> skills);

    List<Job> findBySkills(Skill skill);

    List<Job> findAllByDeletedTrue();

    List<Job> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime deletedAt);
}
