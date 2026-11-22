package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.example.data.DeviceTelemetryRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PowerForge", appName)
  }

  @Test
  fun `device identity populates package name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val identity = DeviceTelemetryRepository(context).getDeviceIdentity()
    assertEquals(context.packageName, identity.packageName)
  }
}
