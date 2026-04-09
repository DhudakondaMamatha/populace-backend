package com.populace.repository;

import com.populace.domain.RoleComboRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleComboRoleRepository extends JpaRepository<RoleComboRole, Long> {

    List<RoleComboRole> findByRoleCombo_Id(Long comboId);

    void deleteByRoleCombo_Id(Long comboId);

    /**
     * Returns the IDs of all roles that share an active combo with the given role
     * (siblings), excluding the role itself.
     */
    @Query("SELECT DISTINCT rcr2.role.id " +
           "FROM RoleComboRole rcr1 " +
           "JOIN RoleComboRole rcr2 ON rcr1.roleCombo.id = rcr2.roleCombo.id " +
           "WHERE rcr1.role.id = :roleId " +
           "AND rcr2.role.id != :roleId " +
           "AND rcr1.roleCombo.active = true " +
           "AND rcr1.roleCombo.business.id = :businessId")
    List<Long> findComboSiblingRoleIds(
            @Param("roleId") Long roleId,
            @Param("businessId") Long businessId);
}
