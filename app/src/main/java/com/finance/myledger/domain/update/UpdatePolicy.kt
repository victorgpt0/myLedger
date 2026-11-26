package com.finance.myledger.domain.update

/** How to offer an available Play update. */
enum class UpdateMode {
    /** Don't prompt (yet). */
    NONE,

    /** Download in the background while the app stays usable; offer a restart when ready. */
    FLEXIBLE,

    /** Full-screen, blocking update — reserved for critical releases. */
    IMMEDIATE
}

/**
 * Decides whether and how to prompt for an update Play reports as available.
 *
 * [priority] is Play's in-app update priority (0–5), set per release through the Play
 * Developer Publishing API (`inAppUpdatePriority`); the Play Console UI can't set it, so it's 0
 * unless a release sets it. [stalenessDays] is how long Play has known about the update on this
 * device (null if unknown).
 *
 * - priority ≥ [immediatePriority]: IMMEDIATE (critical fix — e.g. a parser bug corrupting totals)
 * - priority ≥ [flexiblePriority], or the update is at least [flexibleAfterDays] old: FLEXIBLE
 * - otherwise NONE, so a routine release doesn't nag on its first day of a staged rollout
 *
 * Falls back to the other mode when Play says the preferred one isn't allowed.
 */
object UpdatePolicy {

    fun decide(
        priority: Int,
        stalenessDays: Int?,
        immediateAllowed: Boolean,
        flexibleAllowed: Boolean,
        immediatePriority: Int = 4,
        flexiblePriority: Int = 2,
        flexibleAfterDays: Int = 3
    ): UpdateMode {
        val wanted = when {
            priority >= immediatePriority -> UpdateMode.IMMEDIATE
            priority >= flexiblePriority -> UpdateMode.FLEXIBLE
            stalenessDays != null && stalenessDays >= flexibleAfterDays -> UpdateMode.FLEXIBLE
            else -> UpdateMode.NONE
        }
        return when (wanted) {
            UpdateMode.IMMEDIATE -> when {
                immediateAllowed -> UpdateMode.IMMEDIATE
                flexibleAllowed -> UpdateMode.FLEXIBLE
                else -> UpdateMode.NONE
            }
            UpdateMode.FLEXIBLE -> if (flexibleAllowed) UpdateMode.FLEXIBLE else UpdateMode.NONE
            UpdateMode.NONE -> UpdateMode.NONE
        }
    }
}
