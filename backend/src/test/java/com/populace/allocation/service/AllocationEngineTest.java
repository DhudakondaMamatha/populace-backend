package com.populace.allocation.service;

import com.populace.allocation.dto.AllocationResultDto;
import com.populace.domain.*;
import com.populace.domain.enums.*;
import com.populace.repository.*;
import com.populace.shiftstatus.service.ShiftStatusService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AllocationEngine covering full allocation, partial allocation, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationEngineTest {

	// --- Mocks ---
	@Mock private ShiftRepository shiftRepository;
	@Mock private TimeBlockRepository timeBlockRepository;
	@Mock private StaffMemberRepository staffMemberRepository;
	@Mock private StaffRoleRepository staffRoleRepository;
	@Mock private StaffSiteRepository staffSiteRepository;
	@Mock private StaffAvailabilityRepository staffAvailabilityRepository;
	@Mock private StaffCompensationRepository staffCompensationRepository;
	@Mock private StaffWorkParametersRepository staffWorkParametersRepository;
	@Mock private WorkParametersRepository workParametersRepository;
	@Mock private AllocationRuleRepository allocationRuleRepository;
	@Mock private BusinessRepository businessRepository;
	@Mock private ShiftRoleRepository shiftRoleRepository;
	@Mock private RoleRepository roleRepository;
	@Mock private RoleComboRoleRepository roleComboRoleRepository;
	@Mock private LeaveRequestRepository leaveRequestRepository;
	@Mock private AllocationRunRepository allocationRunRepository;
	// JdbcTemplate is a concrete class that Java 24 cannot mock inline.
	// Use a simple subclass that always returns true for the advisory lock.
	private final JdbcTemplate jdbcTemplate = new JdbcTemplate() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
			return (T) Boolean.TRUE;
		}
	};

	private ShiftStatusService shiftStatusService;
	private AllocationEngine engine;

	// --- Test fixtures ---
	private static final Long BUSINESS_ID = 1L;
	private static final Long SITE_ID = 10L;
	private static final Long ROLE_ID = 100L;
	private static final Long SHIFT_ID = 1000L;
	private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 4, 15); // Wednesday
	private static final LocalTime SHIFT_START = LocalTime.of(7, 0);
	private static final LocalTime SHIFT_END = LocalTime.of(17, 0); // 10h shift

	private Business business;
	private Site site;
	private Role role;

	@BeforeEach
	void setUp() {
		// Construct real ShiftStatusService with mocked repos (avoids Java 24 mock limitation)
		shiftStatusService = new ShiftStatusService(shiftRepository, shiftRoleRepository, timeBlockRepository);

		engine = new AllocationEngine(
				shiftRepository, timeBlockRepository, staffMemberRepository,
				staffRoleRepository, staffSiteRepository, staffAvailabilityRepository,
				staffCompensationRepository, staffWorkParametersRepository,
				workParametersRepository, allocationRuleRepository,
				businessRepository, shiftStatusService, roleRepository,
				roleComboRoleRepository, leaveRequestRepository,
				allocationRunRepository, jdbcTemplate);

		business = createBusiness();
		site = createSite();
		role = createRole();
	}

	// =========================================================================
	// FULL ALLOCATION TESTS
	// =========================================================================

	@Nested
	@DisplayName("Full Allocation")
	class FullAllocationTests {

		@Test
		@DisplayName("should allocate full shift when staff is available")
		void should_allocate_full_shift_when_staff_is_available() {
			Shift shift = createShift(1);
			StaffMember staff = createStaff(1L);
			StaffRole staffRole = createStaffRole(staff.getId(), ROLE_ID);

			stubCommonForAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));
			stubStaffPassesAllChecks(staff.getId(), staffRole);

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.status()).isEqualTo("completed");
			assertThat(result.totalAllocations()).isEqualTo(1);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());
			TimeBlock saved = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.findFirst().orElseThrow();
			assertThat(saved.getCreatedBy()).isEqualTo("AUTO");
			assertThat(saved.getShift()).isEqualTo(shift);
			assertThat(saved.getStaff()).isEqualTo(staff);
		}

		@Test
		@DisplayName("should not allocate when constraints fail")
		void should_not_allocate_when_constraints_fail() {
			Shift shift = createShift(1);
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));
			// Staff is unavailable
			when(staffAvailabilityRepository.existsUnavailableOnDate(
					eq(staff.getId()), eq(SHIFT_DATE), anyShort(), eq(AvailabilityType.unavailable)))
					.thenReturn(true);
			stubStaffRole(staff.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(0);
		}

		@Test
		@DisplayName("should respect max daily hours")
		void should_respect_max_daily_hours() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));
			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			// Staff has maxHoursPerDay = 5.0 → 10h shift exceeds it
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(0);
		}

		@Test
		@DisplayName("should respect weekly cap")
		void should_respect_weekly_cap() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);
			StaffRole staffRole = createStaffRole(staff.getId(), ROLE_ID);

			stubCommonForAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));
			stubStaffPassesAllChecks(staff.getId(), staffRole);

			// Already worked 42h this week, weekly cap 48h, shift is 10h net → 42+10=52 > 48
			when(timeBlockRepository.sumWorkMinutesByStaffAndWeek(eq(staff.getId()), any(), any()))
					.thenReturn(42L * 60);
			when(timeBlockRepository.sumBreakMinutesByStaffAndWeek(eq(staff.getId()), any(), any()))
					.thenReturn(0L);

			// Set weekly cap
			business.setMaxWeeklyHoursCap(BigDecimal.valueOf(48));

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(0);
		}

		@Test
		@DisplayName("should insert breaks for full shift")
		void should_insert_breaks_for_full_shift() {
			Shift shift = createShift(1);
			StaffMember staff = createStaff(1L);
			StaffRole staffRole = createStaffRole(staff.getId(), ROLE_ID);

			stubCommonForAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));
			stubStaffPassesAllChecks(staff.getId(), staffRole);
			// Break config: break after 300 min continuous work
			business.setMaxContinuousWorkMinutes(300);
			business.setMinBreakDurationMinutes(30);
			business.setMinWorkBeforeBreakMinutes(180);

			engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());

			long workBlocks = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work).count();
			long breakBlocks = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.break_period).count();

			assertThat(workBlocks).isGreaterThanOrEqualTo(1);
			assertThat(breakBlocks).isGreaterThanOrEqualTo(1);
		}
	}

	// =========================================================================
	// PARTIAL ALLOCATION TESTS
	// =========================================================================

	@Nested
	@DisplayName("Partial Allocation")
	class PartialAllocationTests {

		@Test
		@DisplayName("should allocate partial when full fails due to duration constraint")
		void should_allocate_partial_when_full_fails() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			// Full allocation fails: shift net (10h) exceeds staff max daily (5h)
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(1);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());

			TimeBlock partialBlock = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.findFirst().orElseThrow();
			assertThat(partialBlock.getCreatedBy()).isEqualTo("PARTIAL_AUTO");
			// Partial covers 5h = 300 minutes from shift start
			long durationMin = Duration.between(partialBlock.getStartTime(), partialBlock.getEndTime()).toMinutes();
			assertThat(durationMin).isEqualTo(300);
		}

		@Test
		@DisplayName("should split shift between two staff when each has limited hours")
		void should_split_shift_between_two_staff() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staffA = createStaff(1L);
			StaffMember staffB = createStaff(2L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staffA, staffB));

			// Both staff: max 5h per day → 10h shift split into two 5h partials
			StaffWorkParameters swp5h = new StaffWorkParameters();
			swp5h.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(anyLong()))
					.thenReturn(Optional.of(swp5h));

			stubStaffRole(staffA.getId());
			stubStaffRole(staffB.getId());
			stubStaffPassesNonDurationChecks(staffA.getId());
			stubStaffPassesNonDurationChecks(staffB.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(2);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());

			List<TimeBlock> workBlocks = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.sorted(Comparator.comparing(TimeBlock::getStartTime))
					.toList();
			assertThat(workBlocks).hasSize(2);

			// Block A: 07:00 - 12:00
			assertThat(workBlocks.get(0).getCreatedBy()).isEqualTo("PARTIAL_AUTO");
			assertThat(Duration.between(workBlocks.get(0).getStartTime(), workBlocks.get(0).getEndTime()).toMinutes())
					.isEqualTo(300);

			// Block B: 12:00 - 17:00 (contiguous, no overlap)
			assertThat(workBlocks.get(1).getCreatedBy()).isEqualTo("PARTIAL_AUTO");
			assertThat(workBlocks.get(1).getStartTime()).isEqualTo(workBlocks.get(0).getEndTime());
		}

		@Test
		@DisplayName("should allocate multiple partials until shift is filled")
		void should_allocate_multiple_partials_until_filled() {
			Shift shift = createShift(1); // 10h shift
			StaffMember s1 = createStaff(1L);
			StaffMember s2 = createStaff(2L);
			StaffMember s3 = createStaff(3L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(s1, s2, s3));

			// Each staff can work 4h → need 3 partials (4+4+2=10h)
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(4.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(anyLong()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(s1.getId());
			stubStaffRole(s2.getId());
			stubStaffRole(s3.getId());
			stubStaffPassesNonDurationChecks(s1.getId());
			stubStaffPassesNonDurationChecks(s2.getId());
			stubStaffPassesNonDurationChecks(s3.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(3);
		}

		@Test
		@DisplayName("should stop when no candidates available")
		void should_stop_when_no_candidates_available() {
			Shift shift = createShift(1);
			// No staff at all
			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(0);
		}

		@Test
		@DisplayName("should ignore min daily hours for partial allocation")
		void should_ignore_min_daily_hours_for_partial() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			// minHoursPerDay = 8, maxHoursPerDay = 3 → full fails (10h > 3h max + 10h min)
			// But partial should still work with 3h available
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMinHoursPerDay(BigDecimal.valueOf(8.0));
			swp.setMaxHoursPerDay(BigDecimal.valueOf(3.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			// Partial allocation should create 1 block of 3h (180 min >= 60 min threshold)
			assertThat(result.totalAllocations()).isEqualTo(1);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());
			TimeBlock partialBlock = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.findFirst().orElseThrow();
			assertThat(partialBlock.getCreatedBy()).isEqualTo("PARTIAL_AUTO");
		}

		@Test
		@DisplayName("should not insert breaks for partial blocks")
		void should_not_insert_breaks_for_partial() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());
			// Break config present
			business.setMaxContinuousWorkMinutes(120);
			business.setMinBreakDurationMinutes(30);

			engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());

			long breakBlocks = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.break_period).count();
			assertThat(breakBlocks).isZero();
		}

		@Test
		@DisplayName("should use uncovered window for overlap check")
		void should_use_uncovered_window_for_overlap() {
			Shift shift = createShift(1); // 10h shift 07:00-17:00
			StaffMember staffA = createStaff(1L);
			StaffMember staffB = createStaff(2L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staffA, staffB));

			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(anyLong()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staffA.getId());
			stubStaffRole(staffB.getId());
			stubStaffPassesNonDurationChecks(staffA.getId());
			stubStaffPassesNonDurationChecks(staffB.getId());

			// staffB has an existing block 07:00-12:00 from another shift
			// → should be rejected for 07:00-17:00 overlap check
			Instant overlapStart = SHIFT_DATE.atTime(7, 0).atZone(ZoneOffset.UTC).toInstant();
			Instant overlapEnd = SHIFT_DATE.atTime(12, 0).atZone(ZoneOffset.UTC).toInstant();
			TimeBlock existingBlock = new TimeBlock();
			existingBlock.setBlockType(BlockType.work);
			existingBlock.setStartTime(overlapStart);
			existingBlock.setEndTime(overlapEnd);
			when(timeBlockRepository.findOverlappingBlocksForStaff(eq(staffB.getId()), any(), any()))
					.thenReturn(List.of(existingBlock));

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			// Only staffA should be allocated (staffB blocked by overlap)
			assertThat(result.totalAllocations()).isEqualTo(1);
		}

		@Test
		@DisplayName("should use available minutes for cap computation")
		void should_use_available_minutes_for_caps() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			// Set AFTER stubStaffPassesNonDurationChecks (Mockito last-match wins)
			// Already worked 9h today. Max daily = 12h. Only 3h remaining
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(12.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));
			when(timeBlockRepository.sumWorkMinutesByStaffAndDate(eq(staff.getId()), any()))
					.thenReturn(9 * 60);

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(1);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());
			TimeBlock partialBlock = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.findFirst().orElseThrow();

			long durationMin = Duration.between(partialBlock.getStartTime(), partialBlock.getEndTime()).toMinutes();
			assertThat(durationMin).isEqualTo(180); // 3h = 180 min
		}
	}

	// =========================================================================
	// EDGE CASE TESTS
	// =========================================================================

	@Nested
	@DisplayName("Edge Cases")
	class EdgeCaseTests {

		@Test
		@DisplayName("should skip partial when remaining window < 60 min threshold")
		void should_skip_partial_when_remaining_less_than_min_threshold() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			// Staff can only work 50 min (below 60 min threshold)
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(0.83)); // ~50 min
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(0);
		}

		@Test
		@DisplayName("should handle staff hitting weekly cap mid-shift")
		void should_handle_staff_hitting_weekly_cap_mid_shift() {
			Shift shift = createShift(1); // 10h shift
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			// Set AFTER stubStaffPassesNonDurationChecks (Mockito last-match wins)
			// Daily cap: 12h (no issue). Weekly cap: 45h. Already worked 43h.
			// Available from weekly: 45-43=2h=120min ≥ 60 ✓
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(12.0));
			swp.setMaxHoursPerWeek(BigDecimal.valueOf(45.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));
			when(timeBlockRepository.sumWorkMinutesByStaffAndWeek(eq(staff.getId()), any(), any()))
					.thenReturn(43L * 60);
			when(timeBlockRepository.sumBreakMinutesByStaffAndWeek(eq(staff.getId()), any(), any()))
					.thenReturn(0L);

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			assertThat(result.totalAllocations()).isEqualTo(1);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());
			TimeBlock block = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.findFirst().orElseThrow();
			long minutes = Duration.between(block.getStartTime(), block.getEndTime()).toMinutes();
			assertThat(minutes).isEqualTo(120); // Only 2h of weekly capacity
		}

		@Test
		@DisplayName("should not create overlapping blocks between partials")
		void should_not_create_overlapping_blocks() {
			Shift shift = createShift(1); // 10h shift
			StaffMember s1 = createStaff(1L);
			StaffMember s2 = createStaff(2L);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(s1, s2));

			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(anyLong()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(s1.getId());
			stubStaffRole(s2.getId());
			stubStaffPassesNonDurationChecks(s1.getId());
			stubStaffPassesNonDurationChecks(s2.getId());

			engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
			verify(timeBlockRepository, atLeastOnce()).save(captor.capture());
			List<TimeBlock> workBlocks = captor.getAllValues().stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.sorted(Comparator.comparing(TimeBlock::getStartTime))
					.toList();

			// Verify no overlaps: each block's start >= previous block's end
			for (int i = 1; i < workBlocks.size(); i++) {
				assertThat(workBlocks.get(i).getStartTime())
						.isAfterOrEqualTo(workBlocks.get(i - 1).getEndTime());
			}
		}

		@Test
		@DisplayName("should terminate when no progress can be made")
		void should_terminate_when_no_progress() {
			Shift shift = createShift(1);

			stubCommonForAllocation();
			enablePartialAllocation();
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			// Staff with only 30 min available (below threshold) — should terminate, not loop
			StaffMember staff = createStaff(1L);
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(0.5)); // 30 min
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());
			stubStaffPassesNonDurationChecks(staff.getId());

			// Should terminate quickly without infinite loop
			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);
			assertThat(result.totalAllocations()).isEqualTo(0);
			assertThat(result.status()).isEqualTo("completed");
		}

		@Test
		@DisplayName("should not run partial when feature flag is OFF")
		void should_not_run_partial_when_feature_flag_off() {
			Shift shift = createShift(1);
			StaffMember staff = createStaff(1L);

			stubCommonForAllocation();
			// Do NOT enable partial_allocation rule
			when(shiftRepository.findUnfilledShifts(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE))
					.thenReturn(List.of(shift));
			when(staffRoleRepository.findByBussinessIdAndRoldId(BUSINESS_ID, ROLE_ID, EmploymentStatus.active))
					.thenReturn(List.of(staff));

			// Staff can't do full shift (max 5h vs 10h shift)
			StaffWorkParameters swp = new StaffWorkParameters();
			swp.setMaxHoursPerDay(BigDecimal.valueOf(5.0));
			when(staffWorkParametersRepository.findCurrentByStaffId(staff.getId()))
					.thenReturn(Optional.of(swp));

			stubStaffRole(staff.getId());

			AllocationResultDto result = engine.allocate(BUSINESS_ID, SHIFT_DATE, SHIFT_DATE, false);

			// No allocations — full fails, partial not enabled
			assertThat(result.totalAllocations()).isEqualTo(0);
		}
	}

	// =========================================================================
	// HELPER METHODS
	// =========================================================================

	private Business createBusiness() {
		Business b = new Business();
		ReflectionTestUtils.setField(b, "id", BUSINESS_ID);
		b.setName("Test Business");
		b.setEmail("test@business.com");
		b.setTimezone("UTC");
		b.setMaxDailyHoursCap(BigDecimal.valueOf(12));
		b.setMaxContinuousWorkMinutes(300);
		b.setMinBreakDurationMinutes(30);
		b.setMinWorkBeforeBreakMinutes(180);
		b.setDefaultMaxBreaksPerShift(2);
		return b;
	}

	private Site createSite() {
		Site s = new Site();
		ReflectionTestUtils.setField(s, "id", SITE_ID);
		s.setName("Test Site");
		return s;
	}

	private Role createRole() {
		Role r = new Role();
		ReflectionTestUtils.setField(r, "id", ROLE_ID);
		r.setName("Test Role");
		return r;
	}

	private Shift createShift(int staffRequired) {
		Shift s = new Shift();
		ReflectionTestUtils.setField(s, "id", SHIFT_ID);
		s.setBusiness(business);
		s.setSite(site);
		s.setRole(role);
		s.setShiftDate(SHIFT_DATE);
		s.setStartTime(SHIFT_START);
		s.setEndTime(SHIFT_END);
		s.setStaffRequired(staffRequired);
		s.setStaffAllocated(0);
		s.setStatus(ShiftStatus.open);
		return s;
	}

	private StaffMember createStaff(Long id) {
		StaffMember sm = new StaffMember();
		ReflectionTestUtils.setField(sm, "id", id);
		sm.setFirstName("Staff");
		sm.setLastName("" + id);
		sm.setEmploymentStatus(EmploymentStatus.active);
		return sm;
	}

	private StaffRole createStaffRole(Long staffId, Long roleId) {
		StaffRole sr = new StaffRole();
		ReflectionTestUtils.setField(sr, "id", staffId * 1000 + roleId);
		return sr;
	}

	private void stubCommonForAllocation() {
		when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));
		when(allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(BUSINESS_ID))
				.thenReturn(List.of());
		when(shiftRepository.countShiftsByStatusInRange(eq(BUSINESS_ID), any(), any()))
				.thenReturn(List.of());
		when(roleComboRoleRepository.findComboSiblingRoleIds(anyLong(), anyLong()))
				.thenReturn(List.of());
		when(workParametersRepository.findByBusiness_IdAndIsDefaultTrue(BUSINESS_ID))
				.thenReturn(Optional.empty());
		when(staffWorkParametersRepository.findCurrentByStaffId(anyLong()))
				.thenReturn(Optional.empty());
		when(timeBlockRepository.findByShift_IdAndRole_Id(anyLong(), anyLong()))
				.thenReturn(List.of());
		when(timeBlockRepository.findByShift_Id(anyLong()))
				.thenReturn(List.of());
		// Stubs for ShiftStatusService.updateShiftMetrics() (real instance, not mocked)
		when(shiftRepository.findById(SHIFT_ID)).thenReturn(Optional.of(createShift(1)));
		when(shiftRoleRepository.findByShift_Id(anyLong())).thenReturn(List.of());
		when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private void enablePartialAllocation() {
		AllocationRule partialRule = new AllocationRule();
		partialRule.setRuleKey("partial_allocation");
		partialRule.setEnabled(true);
		partialRule.setPriority(10);
		when(allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(BUSINESS_ID))
				.thenReturn(List.of(partialRule));
	}

	private void stubStaffRole(Long staffId) {
		StaffRole sr = createStaffRole(staffId, ROLE_ID);
		when(staffRoleRepository.findActiveStaffRole(staffId, ROLE_ID))
				.thenReturn(Optional.of(sr));
	}

	private void stubStaffPassesAllChecks(Long staffId, StaffRole staffRole) {
		stubStaffPassesNonDurationChecks(staffId);
		when(staffRoleRepository.findActiveStaffRole(staffId, ROLE_ID))
				.thenReturn(Optional.of(staffRole));
	}

	private void stubStaffPassesNonDurationChecks(Long staffId) {
		when(staffAvailabilityRepository.existsUnavailableOnDate(
				eq(staffId), any(LocalDate.class), anyShort(), eq(AvailabilityType.unavailable)))
				.thenReturn(false);
		when(staffSiteRepository.existsByStaffIdAndSiteIdAndIsActiveTrue(staffId, SITE_ID))
				.thenReturn(true);
		when(timeBlockRepository.findOverlappingBlocksForStaff(eq(staffId), any(), any()))
				.thenReturn(List.of());
		when(timeBlockRepository.findByStaffIdAndDate(eq(staffId), any()))
				.thenReturn(List.of());
		when(timeBlockRepository.sumWorkMinutesByStaffAndDate(eq(staffId), any()))
				.thenReturn(0);
		when(timeBlockRepository.sumBreakMinutesByStaffAndDate(eq(staffId), any()))
				.thenReturn(0L);
		when(timeBlockRepository.sumWorkMinutesByStaffAndWeek(eq(staffId), any(), any()))
				.thenReturn(0L);
		when(timeBlockRepository.sumBreakMinutesByStaffAndWeek(eq(staffId), any(), any()))
				.thenReturn(0L);
		when(timeBlockRepository.findByStaffIdAndDateRange(eq(staffId), any(), any()))
				.thenReturn(List.of());
		when(timeBlockRepository.findByStaffIdAndTimeRange(eq(staffId), any(), any()))
				.thenReturn(List.of());
		when(leaveRequestRepository.existsApprovedLeaveOnDate(eq(staffId), any(), eq(LeaveRequestStatus.approved)))
				.thenReturn(false);
		when(timeBlockRepository.findByRoleAndTimeRangeAndBlockType(anyLong(), any(), any(), any()))
				.thenReturn(List.of());
	}
}
