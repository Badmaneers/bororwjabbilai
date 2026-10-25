package com.heliactyl.bororwjabbilai.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.heliactyl.bororwjabbilai",
        includeInStartupProfile = true,
    ) {
        // 1. App Startup
        pressHome()
        startActivityAndWait()

        // Wait for main UI to render
        device.wait(Until.hasObject(By.pkg(packageName)), 5_000)

        // 2. Perform smooth swipe gesture on screen
        val width = device.displayWidth
        val height = device.displayHeight

        device.swipe(width / 2, height * 3 / 4, width / 2, height / 4, 10)
        device.waitForIdle()
        device.swipe(width / 2, height / 4, width / 2, height * 3 / 4, 10)
        device.waitForIdle()

        // 3. Open Song Detail View if available
        val firstSong = device.findObject(By.textContains("."))
        if (firstSong != null) {
            firstSong.click()
            device.waitForIdle()

            // 4. Back gesture from Detail View
            device.pressBack()
            device.waitForIdle()
        }
    }
}
