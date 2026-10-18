package com.kevin.glasslauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TempUnitTest {
    @Test
    fun fahrenheitOnlyWhereItIsUsed() {
        listOf("US", "LR", "MM").forEach { assertEquals(TempUnit.Fahrenheit, TempUnit.forCountry(it)) }
        listOf("GB", "CA", "DE", "MX", "").forEach { assertEquals(TempUnit.Celsius, TempUnit.forCountry(it)) }
    }
}
