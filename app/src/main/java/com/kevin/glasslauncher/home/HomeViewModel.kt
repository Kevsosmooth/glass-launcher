package com.kevin.glasslauncher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kevin.glasslauncher.LauncherApp
import com.kevin.glasslauncher.apps.AppRepository
import com.kevin.glasslauncher.data.LauncherPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Home screen state: installed apps + saved prefs + screen time; null until apps and prefs have loaded. */
class HomeViewModel(apps: AppRepository, prefs: LauncherPrefs) : ViewModel() {
    val state: StateFlow<HomeState?> = combine(apps.apps.filterNotNull(), prefs.prefs, apps.usage) { list, p, usage ->
        buildHomeState(list, p, usage = usage)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as LauncherApp).container
                HomeViewModel(container.apps, container.prefs)
            }
        }
    }
}
