// app/src/main/java/com/airmouse/di/ViewModelModule.kt
package com.airmouse.di

import androidx.lifecycle.ViewModel
import com.airmouse.presentation.ui.home.HomeViewModel
import com.airmouse.presentation.ui.calibration.CalibrationViewModel
import com.airmouse.presentation.ui.gesture.GestureStudioViewModel
import com.airmouse.presentation.ui.settings.SettingsViewModel
import com.airmouse.presentation.ui.statistics.StatisticsViewModel
import com.airmouse.presentation.ui.help.HelpViewModel
import com.airmouse.presentation.ui.about.AboutViewModel
import com.airmouse.presentation.ui.profiles.ProfilesViewModel
import com.airmouse.presentation.ui.themes.ThemesViewModel
import com.airmouse.presentation.ui.voice.VoiceCommandsViewModel
import com.airmouse.presentation.ui.edge.EdgeGesturesViewModel
import com.airmouse.presentation.ui.proximity.ProximityViewModel
import com.airmouse.presentation.ui.accessibility.AccessibilityViewModel
import com.airmouse.presentation.ui.battery.BatteryViewModel
import com.airmouse.presentation.ui.network.NetworkDiscoveryViewModel
import com.airmouse.presentation.ui.logs.ServerLogsViewModel
import com.airmouse.presentation.ui.onboarding.OnboardingViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CalibrationViewModel::class)
    abstract fun bindCalibrationViewModel(viewModel: CalibrationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GestureStudioViewModel::class)
    abstract fun bindGestureStudioViewModel(viewModel: GestureStudioViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StatisticsViewModel::class)
    abstract fun bindStatisticsViewModel(viewModel: StatisticsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HelpViewModel::class)
    abstract fun bindHelpViewModel(viewModel: HelpViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AboutViewModel::class)
    abstract fun bindAboutViewModel(viewModel: AboutViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfilesViewModel::class)
    abstract fun bindProfilesViewModel(viewModel: ProfilesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ThemesViewModel::class)
    abstract fun bindThemesViewModel(viewModel: ThemesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VoiceCommandsViewModel::class)
    abstract fun bindVoiceCommandsViewModel(viewModel: VoiceCommandsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EdgeGesturesViewModel::class)
    abstract fun bindEdgeGesturesViewModel(viewModel: EdgeGesturesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProximityViewModel::class)
    abstract fun bindProximityViewModel(viewModel: ProximityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AccessibilityViewModel::class)
    abstract fun bindAccessibilityViewModel(viewModel: AccessibilityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BatteryViewModel::class)
    abstract fun bindBatteryViewModel(viewModel: BatteryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkDiscoveryViewModel::class)
    abstract fun bindNetworkDiscoveryViewModel(viewModel: NetworkDiscoveryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ServerLogsViewModel::class)
    abstract fun bindServerLogsViewModel(viewModel: ServerLogsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OnboardingViewModel::class)
    abstract fun bindOnboardingViewModel(viewModel: OnboardingViewModel): ViewModel
}