package com.shizq.bika.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import com.shizq.bika.PACKAGE_NAME
import com.shizq.bika.dashboard.dashboardScrollFeedDownUp
import com.shizq.bika.dashboard.dashboardWaitForContent
import com.shizq.bika.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test

class DashboardBaselineProfile {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() =
        baselineProfileRule.collect(PACKAGE_NAME) {
            startActivityAndAllowNotifications()

            dashboardWaitForContent()
            dashboardScrollFeedDownUp()
        }
}