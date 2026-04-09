package com.populace.domain;

import com.populace.domain.enums.BlockType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "time_blocks")
public class TimeBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_minutes", insertable = false, updatable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "block_type", nullable = false)
    private BlockType blockType = BlockType.work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "covering_block_id")
    private TimeBlock coveringBlock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "break_block_id")
    private TimeBlock breakBlock;

    @Column(name = "break_sequence_number")
    private Integer breakSequenceNumber;

    @Column(name = "is_override_placed", nullable = false)
    private boolean overridePlaced = false;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "created_by", nullable = false)
    private String createdBy = "SYSTEM";

    public TimeBlock() {
    }

    public Long getId() {
        return id;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Long getShiftId() {
        return shift != null ? shift.getId() : null;
    }

    public StaffMember getStaff() {
        return staff;
    }

    public void setStaff(StaffMember staff) {
        this.staff = staff;
    }

    public Long getStaffId() {
        return staff != null ? staff.getId() : null;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Long getSiteId() {
        return site != null ? site.getId() : null;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }

    public TimeBlock getCoveringBlock() {
        return coveringBlock;
    }

    public void setCoveringBlock(TimeBlock coveringBlock) {
        this.coveringBlock = coveringBlock;
    }

    public Long getCoveringBlockId() {
        return coveringBlock != null ? coveringBlock.getId() : null;
    }

    public TimeBlock getBreakBlock() {
        return breakBlock;
    }

    public void setBreakBlock(TimeBlock breakBlock) {
        this.breakBlock = breakBlock;
    }

    public Long getBreakBlockId() {
        return breakBlock != null ? breakBlock.getId() : null;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isWorkBlock() {
        return blockType == BlockType.work;
    }

    public boolean isBreakBlock() {
        return blockType == BlockType.break_period;
    }

    public boolean isTravelBlock() {
        return blockType == BlockType.travel;
    }

    public boolean isCoverUpBlock() {
        return blockType == BlockType.cover_up;
    }

    public Integer getBreakSequenceNumber() {
        return breakSequenceNumber;
    }

    public void setBreakSequenceNumber(Integer breakSequenceNumber) {
        this.breakSequenceNumber = breakSequenceNumber;
    }

    public boolean isOverridePlaced() {
        return overridePlaced;
    }

    public void setOverridePlaced(boolean overridePlaced) {
        this.overridePlaced = overridePlaced;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

}
