package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CyberPosRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CyberPos", appName)
  }

  @Test
  fun `verify seed data on first launch with lembar unit and no raw materials`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CyberPosRepository(context)
    val products = repository.getProducts()

    // Must have the 3 exact seed items with unit "lembar"
    val v2k = products.find { it.name == "V 2K" }
    assertNotNull("V 2K should exist", v2k)
    assertEquals(2000L, v2k?.sellingPrice)
    assertEquals(1800L, v2k?.costPrice)
    assertEquals(100.0, v2k?.stock ?: 0.0, 0.01)
    assertEquals("lembar", v2k?.unit)

    val v5k = products.find { it.name == "V 5K" }
    assertNotNull("V 5K should exist", v5k)
    assertEquals(5000L, v5k?.sellingPrice)
    assertEquals(4500L, v5k?.costPrice)
    assertEquals(100.0, v5k?.stock ?: 0.0, 0.01)
    assertEquals("lembar", v5k?.unit)

    val mem50k = products.find { it.name == "Member 50K" }
    assertNotNull("Member 50K should exist", mem50k)
    assertEquals(55000L, mem50k?.sellingPrice)
    assertEquals(50000L, mem50k?.costPrice)
    assertEquals(100.0, mem50k?.stock ?: 0.0, 0.01)
    assertEquals("lembar", mem50k?.unit)

    // Verify Bahan Baku does not exist
    val rawMat1 = products.find { it.id == "mat_thermal" }
    val rawMat2 = products.find { it.id == "mat_plastic" }
    assertNull("mat_thermal raw material should not exist", rawMat1)
    assertNull("mat_plastic raw material should not exist", rawMat2)
    assertEquals(3, products.size)

    // Check default users
    val users = repository.getUsers()
    val owner = users.find { it.name == "Owner" }
    val kasir = users.find { it.name == "Kasir" }
    assertNotNull("Owner should exist", owner)
    assertEquals("1234", owner?.pin)
    assertNotNull("Kasir should exist", kasir)
    assertEquals("0000", kasir?.pin)
  }
}
