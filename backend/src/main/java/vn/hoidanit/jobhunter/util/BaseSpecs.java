package vn.hoidanit.jobhunter.util;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class BaseSpecs {
    public static <T> Specification<T> isDeleted() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("deleted"), true);
        };
    }
    public static <T> Specification<T> isActive() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("deleted"), false);
        };
    }
}