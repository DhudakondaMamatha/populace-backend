package com.populace.shift.specification;

import com.populace.domain.Shift;
import com.populace.domain.enums.ShiftStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ShiftSpecification {

    private ShiftSpecification() {
    }

    public static Specification<Shift> withFilters(
            Long businessId,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> siteIds,
            List<Long> roleIds,
            List<ShiftStatus> statuses,
            Boolean excludeCancelled) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("business").get("id"), businessId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("shiftDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("shiftDate"), endDate));
            }

            if (isNotEmpty(siteIds)) {
                predicates.add(root.get("site").get("id").in(siteIds));
            }

            if (isNotEmpty(roleIds)) {
                predicates.add(root.get("role").get("id").in(roleIds));
            }

            if (isNotEmpty(statuses)) {
                predicates.add(root.get("status").in(statuses));
            }

            if (Boolean.TRUE.equals(excludeCancelled)) {
                predicates.add(cb.notEqual(root.get("status"), ShiftStatus.cancelled));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("site");
                root.fetch("role");
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
