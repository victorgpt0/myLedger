package com.finance.myledger.domain.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdatePolicyTest {

    private fun decide(priority: Int = 0, staleness: Int? = null, immediate: Boolean = true, flexible: Boolean = true) =
        UpdatePolicy.decide(priority, staleness, immediate, flexible)

    @Test
    fun criticalPriority_isImmediate() {
        assertThat(decide(priority = 4)).isEqualTo(UpdateMode.IMMEDIATE)
        assertThat(decide(priority = 5)).isEqualTo(UpdateMode.IMMEDIATE)
    }

    @Test
    fun mediumPriority_isFlexibleRightAway() {
        assertThat(decide(priority = 2, staleness = 0)).isEqualTo(UpdateMode.FLEXIBLE)
    }

    @Test
    fun routineRelease_waitsBeforePrompting() {
        assertThat(decide(priority = 0, staleness = null)).isEqualTo(UpdateMode.NONE)
        assertThat(decide(priority = 0, staleness = 2)).isEqualTo(UpdateMode.NONE)
        assertThat(decide(priority = 0, staleness = 3)).isEqualTo(UpdateMode.FLEXIBLE)
    }

    @Test
    fun fallsBackWhenPreferredModeNotAllowed() {
        assertThat(decide(priority = 5, immediate = false)).isEqualTo(UpdateMode.FLEXIBLE)
        assertThat(decide(priority = 5, immediate = false, flexible = false)).isEqualTo(UpdateMode.NONE)
        assertThat(decide(priority = 2, flexible = false)).isEqualTo(UpdateMode.NONE)
    }
}
