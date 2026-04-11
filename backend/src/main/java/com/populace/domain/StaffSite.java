package com.populace.domain;

import com.populace.domain.enums.SitePreference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "staff_sites")
public class StaffSite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "is_primary")
    private boolean primary = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preference")
    private SitePreference preference;

    @Column(name = "travel_distance_km", precision = 8, scale = 2)
    private BigDecimal travelDistanceKm;

    @Column(name = "is_active")
    private boolean active = true;

    public StaffSite() {
    }

    public Long getId() {
        return id;
    }

    public StaffMember getStaff() {
        return staff;
    }

    public void setStaff(StaffMember staff) {
        this.staff = staff;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public SitePreference getPreference() {
        // Backward compatibility: derive from legacy is_primary if preference not set
        if (preference == null) {
            return SitePreference.fromLegacyPrimary(primary);
        }
        return preference;
    }

    public void setPreference(SitePreference preference) {
        this.preference = preference;
        // Keep legacy field in sync for backward compatibility
        if (preference != null) {
            this.primary = (preference == SitePreference.primary);
        }
    }

    /**
     * Get the ranking multiplier for this site preference.
     * Used in weighted candidate scoring.
     */
    public BigDecimal getRankingMultiplier() {
        return getPreference().getRankingMultiplier();
    }

    public BigDecimal getTravelDistanceKm() {
        return travelDistanceKm;
    }

    public void setTravelDistanceKm(BigDecimal travelDistanceKm) {
        this.travelDistanceKm = travelDistanceKm;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
