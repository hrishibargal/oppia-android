package org.oppia.android.util.logging

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.data.DataProviderTestMonitor
import org.oppia.android.testing.robolectric.RobolectricModule
import org.oppia.android.testing.threading.TestDispatcherModule
import org.oppia.android.testing.time.FakeOppiaClockModule
import org.oppia.android.util.data.DataProvidersInjector
import org.oppia.android.util.data.DataProvidersInjectorProvider
import org.oppia.android.util.locale.LocaleProdModule
import org.oppia.android.util.logging.SyncStatusManager.SyncStatus.DATA_UPLOADED
import org.oppia.android.util.logging.SyncStatusManager.SyncStatus.DATA_UPLOADING
import org.oppia.android.util.logging.SyncStatusManager.SyncStatus.INITIAL_UNKNOWN
import org.oppia.android.util.logging.SyncStatusManager.SyncStatus.NETWORK_ERROR
import org.oppia.android.util.logging.SyncStatusManager.SyncStatus.NO_CONNECTIVITY
import org.oppia.android.util.networking.NetworkConnectionUtilDebugModule
import org.oppia.android.util.platformparameter.ENABLE_LANGUAGE_SELECTION_UI_DEFAULT_VALUE
import org.oppia.android.util.platformparameter.EnableLanguageSelectionUi
import org.oppia.android.util.platformparameter.LearnerStudyAnalytics
import org.oppia.android.util.platformparameter.PlatformParameterValue
import org.oppia.android.util.platformparameter.SPLASH_SCREEN_WELCOME_MSG_DEFAULT_VALUE
import org.oppia.android.util.platformparameter.SYNC_UP_WORKER_TIME_PERIOD_IN_HOURS_DEFAULT_VALUE
import org.oppia.android.util.platformparameter.SplashScreenWelcomeMsg
import org.oppia.android.util.platformparameter.SyncUpWorkerTimePeriodHours
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import javax.inject.Inject
import javax.inject.Singleton

/** Tests for [SyncStatusManagerImpl]. */
// FunctionName: test names are conventionally named with underscores.
@Suppress("FunctionName")
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(application = SyncStatusManagerImplTest.TestApplication::class)
class SyncStatusManagerImplTest {
  @Inject lateinit var syncStatusManager: SyncStatusManager
  @Inject lateinit var monitorFactory: DataProviderTestMonitor.Factory

  @Before
  fun setUp() {
    setUpTestApplicationComponent()
  }

  @Test
  fun testGetSyncStatus_initialState_returnsUnknown() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(INITIAL_UNKNOWN)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toDataUploading_returnsUploading() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()

    syncStatusManager.setSyncStatus(DATA_UPLOADING)

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(DATA_UPLOADING)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toDataUploaded_returnsUploaded() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()

    syncStatusManager.setSyncStatus(DATA_UPLOADED)

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(DATA_UPLOADED)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toNoConnectivity_returnsNoConnectivity() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()

    syncStatusManager.setSyncStatus(NO_CONNECTIVITY)

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(NO_CONNECTIVITY)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toNetworkError_returnsNetworkError() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()

    syncStatusManager.setSyncStatus(NETWORK_ERROR)

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(NETWORK_ERROR)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toDataUploading_thenNetworkError_returnsNetworkError() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()
    syncStatusManager.setSyncStatus(DATA_UPLOADING)

    syncStatusManager.setSyncStatus(NETWORK_ERROR)

    val syncStatus = monitorFactory.waitForNextSuccessfulResult(syncStatusProvider)
    assertThat(syncStatus).isEqualTo(NETWORK_ERROR)
  }

  @Test
  fun testGetSyncStatus_setSyncStatus_toUploading_thenNetworkError_existingSub_notifiesChange() {
    val syncStatusProvider = syncStatusManager.getSyncStatus()
    val monitor = monitorFactory.createMonitor(syncStatusProvider)
    syncStatusManager.setSyncStatus(DATA_UPLOADING)
    monitor.waitForNextResult()

    syncStatusManager.setSyncStatus(NETWORK_ERROR)

    // The latest value should be sent to the existing observer as a notification.
    val syncStatus = monitor.waitForNextSuccessResult()
    assertThat(syncStatus).isEqualTo(NETWORK_ERROR)
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  // TODO(#89): Move this to a common test application component.
  @Module
  class TestModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
      return application
    }

    // TODO(#59): Either isolate these to their own shared test module, or use the real logging
    // module in tests to avoid needing to specify these settings for tests.
    @EnableConsoleLog
    @Provides
    fun provideEnableConsoleLog(): Boolean = true

    @EnableFileLog
    @Provides
    fun provideEnableFileLog(): Boolean = false

    @GlobalLogLevel
    @Provides
    fun provideGlobalLogLevel(): LogLevel = LogLevel.VERBOSE
  }

  @Module
  class TestPlatformParameterModule {

    companion object {
      var forceLearnerAnalyticsStudy: Boolean = false
    }

    @Provides
    @SplashScreenWelcomeMsg
    fun provideSplashScreenWelcomeMsgParam(): PlatformParameterValue<Boolean> {
      return PlatformParameterValue.createDefaultParameter(SPLASH_SCREEN_WELCOME_MSG_DEFAULT_VALUE)
    }

    @Provides
    @SyncUpWorkerTimePeriodHours
    fun provideSyncUpWorkerTimePeriod(): PlatformParameterValue<Int> {
      return PlatformParameterValue.createDefaultParameter(
        SYNC_UP_WORKER_TIME_PERIOD_IN_HOURS_DEFAULT_VALUE
      )
    }

    @Provides
    @EnableLanguageSelectionUi
    fun provideEnableLanguageSelectionUi(): PlatformParameterValue<Boolean> {
      return PlatformParameterValue.createDefaultParameter(
        ENABLE_LANGUAGE_SELECTION_UI_DEFAULT_VALUE
      )
    }

    @Provides
    @LearnerStudyAnalytics
    fun provideLearnerStudyAnalytics(): PlatformParameterValue<Boolean> {
      return PlatformParameterValue.createDefaultParameter(forceLearnerAnalyticsStudy)
    }
  }

  // TODO(#89): Move this to a common test application component.
  @Singleton
  @Component(
    modules = [
      TestModule::class, TestLogReportingModule::class,
      TestDispatcherModule::class, RobolectricModule::class, FakeOppiaClockModule::class,
      NetworkConnectionUtilDebugModule::class, LocaleProdModule::class,
      TestPlatformParameterModule::class, SyncStatusModule::class
    ]
  )
  interface TestApplicationComponent : DataProvidersInjector {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder
      fun build(): TestApplicationComponent
    }

    fun inject(syncStatusControllerTest: SyncStatusManagerImplTest)
  }

  class TestApplication : Application(), DataProvidersInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerSyncStatusManagerImplTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build()
    }

    fun inject(syncStatusControllerTest: SyncStatusManagerImplTest) {
      component.inject(syncStatusControllerTest)
    }

    override fun getDataProvidersInjector(): DataProvidersInjector = component
  }
}
