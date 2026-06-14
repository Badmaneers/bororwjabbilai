package com.heliactyl.bororwjabbilai.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        includeInStartupProfile = true
    ) {
        // This block defines the user journey to profile.
        // For a basic profile, just starting the app is enough.
        pressHome()
        startActivityAndWait()
        
        // Add more interactions here if needed to profile critical paths.
    }
}
