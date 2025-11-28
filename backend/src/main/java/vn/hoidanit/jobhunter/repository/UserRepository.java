package vn.hoidanit.jobhunter.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.User;

import java.time.LocalDateTime;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.SpecialAction;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(long id);

    User findByRefreshTokenAndEmail(String token, String email);

    List<User> findByCompany(Company company);

    List<User> findAllByDeletedTrue();

    List<User> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime limit);

}
