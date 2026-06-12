// app/src/main/java/com/airmouse/di/ViewModelModule.kt
package com.airmouse.di

import androidx.lifecycle.ViewModel
import com.airmouse.presentation.ui.home.HomeViewModel
import com.airmouse.presentation.ui.calibration.CalibrationViewModel
import com.airmouse.presentation.ui.gesture.GestureStudioViewModel
import com.airmouse.presentation.ui.settings.SettingsViewModel
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
}