package com.shizq.bika.dashboard

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.shizq.bika.flingElementDownUp

fun MacrobenchmarkScope.dashboardWaitForContent() {
    device.wait(Until.hasObject(By.res("dashboard:grid")), 10_000)

    device.wait(Until.hasObject(By.res("dashboard:channel:Cosplay")), 10_000)
    device.waitForIdle()
}

fun MacrobenchmarkScope.dashboardScrollFeedDownUp() {
    val feedList = device.findObject(By.res("dashboard:grid"))
    device.flingElementDownUp(feedList)
}