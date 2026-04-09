# Allocation Decision Logger - Sample Output

This document shows sample log output from the Allocation Decision Logger.

---

## 1. Successful Allocation

A shift fully filled with all eligible candidates.

```
============================================
[ALLOCATION START]
Run ID        : 12345678-abcd-efgh-ijkl-mnopqrstuvwx
Shift         : 101
Date          : 2026-02-15
Time          : 09:00 - 17:00
Site          : Downtown Branch
Role          : Cashier
Required Staff: 2
--------------------------------------------

[CANDIDATE EVALUATION]
Staff         : 1001 - Sarah Johnson

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Cashier (ID: 5)
- Actual       = [Role-5, Role-7] | Proficiency: competent
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Downtown Branch (ID: 3)
- Actual       = [Site-3, Site-4]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 09:00 - 17:00
- Actual       = Not on leave
- Result       = PASSED

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Existing allocations today: 0, Not in current run pool
- Result       = PASSED

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 16.0, Available: 8.0, Shifts today: 0
- Result       = PASSED

Scoring Summary
- Score calculated = 42.50
- Priority rank    = #1

FINAL DECISION
- Decision     = ALLOCATED
- Final Reason = Allocated 8.0 hours at rank #1
--------------------------------------------

[CANDIDATE EVALUATION]
Staff         : 1002 - Michael Chen

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Cashier (ID: 5)
- Actual       = [Role-5, Role-8] | Proficiency: expert
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Downtown Branch (ID: 3)
- Actual       = [Site-3]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 09:00 - 17:00
- Actual       = Not on leave
- Result       = PASSED

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Existing allocations today: 0, Not in current run pool
- Result       = PASSED

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 8.0, Available: 8.0, Shifts today: 0
- Result       = PASSED

Scoring Summary
- Score calculated = 48.00
- Priority rank    = #2

FINAL DECISION
- Decision     = ALLOCATED
- Final Reason = Allocated 8.0 hours at rank #2
--------------------------------------------

[SHIFT RESULT]
- Required staff   = 2
- Allocated staff  = 2
- Unfilled positions = 0
- Allocation status  = FILLED

[ALLOCATION END]
============================================
```

---

## 2. Skipped Staff Due to Role Mismatch

A candidate fails the role eligibility check.

```
[CANDIDATE EVALUATION]
Staff         : 1003 - Emily Davis

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Cashier (ID: 5)
- Actual       = [Role-7, Role-9]
- Result       = FAILED
- Reason       = Staff is not approved for the required role

Step 3: Site Eligibility Check
- Required     = Downtown Branch (ID: 3)
- Actual       = [Site-3, Site-4, Site-5]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 09:00 - 17:00
- Actual       = Not on leave
- Result       = PASSED

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Existing allocations today: 0, Not in current run pool
- Result       = PASSED

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 16.0, Available: 8.0, Shifts today: 0
- Result       = PASSED

FINAL DECISION
- Decision     = SKIPPED
- Final Reason = Staff is not approved for the required role
--------------------------------------------
```

---

## 3. Skipped Staff Due to Conflict

A candidate is already allocated to another shift.

```
[CANDIDATE EVALUATION]
Staff         : 1004 - James Wilson

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Cashier (ID: 5)
- Actual       = [Role-5, Role-6] | Proficiency: competent
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Downtown Branch (ID: 3)
- Actual       = [Site-3]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 09:00 - 17:00
- Actual       = Not on leave
- Result       = PASSED

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Already allocated to another shift in this run
- Result       = FAILED
- Reason       = Staff was allocated to a prior shift in this allocation run

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 8.0, Available: 8.0, Shifts today: 1
- Result       = PASSED

FINAL DECISION
- Decision     = SKIPPED
- Final Reason = Staff was allocated to a prior shift in this allocation run
--------------------------------------------
```

---

## 4. Skipped Staff Due to Leave

A candidate is on approved leave.

```
[CANDIDATE EVALUATION]
Staff         : 1005 - Lisa Martinez

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Cashier (ID: 5)
- Actual       = [Role-5] | Proficiency: expert
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Downtown Branch (ID: 3)
- Actual       = [Site-3, Site-4]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 09:00 - 17:00
- Actual       = On approved leave
- Result       = FAILED
- Reason       = Staff has approved leave for this date

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Existing allocations today: 0, Not in current run pool
- Result       = PASSED

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 16.0, Available: 8.0, Shifts today: 0
- Result       = PASSED

FINAL DECISION
- Decision     = SKIPPED
- Final Reason = Staff has approved leave for this date
--------------------------------------------
```

---

## 5. Partial Allocation Scenario

A shift that could only be partially filled.

```
============================================
[ALLOCATION START]
Run ID        : 12345678-abcd-efgh-ijkl-mnopqrstuvwx
Shift         : 102
Date          : 2026-02-15
Time          : 18:00 - 02:00
Site          : Airport Terminal
Role          : Security Guard
Required Staff: 3
--------------------------------------------

[CANDIDATE EVALUATION]
Staff         : 2001 - Robert Brown

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Security Guard (ID: 12)
- Actual       = [Role-12] | Proficiency: competent
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Airport Terminal (ID: 8)
- Actual       = [Site-8]
- Result       = PASSED

Step 4: Availability Check
- Required     = Available on 2026-02-15 18:00 - 02:00
- Actual       = Not on leave
- Result       = PASSED

Step 5: Conflict Check
- Required     = Not already allocated in this run
- Actual       = Existing allocations today: 0, Not in current run pool
- Result       = PASSED

Step 6: Allocation Limits Check
- Required     = Min hours: 4.0, Max hours: 8.0, Max shifts/day: 2
- Actual       = Remaining needed: 24.0, Available: 8.0, Shifts today: 0
- Result       = PASSED

Scoring Summary
- Score calculated = 35.00
- Priority rank    = #1

FINAL DECISION
- Decision     = ALLOCATED
- Final Reason = Allocated 8.0 hours at rank #1
--------------------------------------------

[CANDIDATE EVALUATION]
Staff         : 2002 - Patricia Anderson

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Security Guard (ID: 12)
- Actual       = [Role-10, Role-11]
- Result       = FAILED
- Reason       = Staff is not approved for the required role

FINAL DECISION
- Decision     = SKIPPED
- Final Reason = Staff is not approved for the required role
--------------------------------------------

[CANDIDATE EVALUATION]
Staff         : 2003 - Thomas Garcia

Step 1: Employment Status Check
- Required     = ACTIVE
- Actual       = ACTIVE
- Result       = PASSED

Step 2: Role Eligibility Check
- Required     = Security Guard (ID: 12)
- Actual       = [Role-12] | Proficiency: trainee
- Result       = PASSED

Step 3: Site Eligibility Check
- Required     = Airport Terminal (ID: 8)
- Actual       = [Site-7, Site-9]
- Result       = FAILED
- Reason       = Staff is not approved for the required site

FINAL DECISION
- Decision     = SKIPPED
- Final Reason = Staff is not approved for the required site
--------------------------------------------

[SHIFT RESULT]
- Required staff   = 3
- Allocated staff  = 1
- Unfilled positions = 2
- Allocation status  = PARTIAL

[ALLOCATION END]
============================================
```

---

## 6. Allocation Run Summary

Summary logged at the end of an allocation run.

```
============================================
[ALLOCATION RUN SUMMARY]
Run ID            : 12345678-abcd-efgh-ijkl-mnopqrstuvwx
Total Shifts      : 15
Shifts Filled     : 10
Shifts Partial    : 3
Shifts Unfilled   : 2
Total Allocations : 25
Coverage          : 66.7%
============================================
```

---

## 7. Allocation Error

When an allocation run encounters an error.

```
[ALLOCATION ERROR]
Run ID  : 12345678-abcd-efgh-ijkl-mnopqrstuvwx
Shift ID: 105
Error   : Database connection timeout while loading staff candidates
--------------------------------------------
```

---

## Log Level Usage

| Level | When Used |
|-------|-----------|
| INFO  | Allocation start/end, successful allocations, run summaries |
| DEBUG | Per-rule evaluation, skipped candidates, detailed decisions |
| WARN  | Partial or failed allocations, unfilled shifts |
| ERROR | System errors that abort allocation |

---

## Configuration

### Enable DEBUG logs in application.yml

```yaml
logging:
  level:
    AllocationDecisions: DEBUG
```

### Log file location

Logs are written to: `logs/allocation-decisions.log`

Daily rotation is configured, keeping 30 days of history.
