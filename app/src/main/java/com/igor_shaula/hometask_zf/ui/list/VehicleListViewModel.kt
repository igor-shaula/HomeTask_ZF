package com.igor_shaula.hometask_zf.ui.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.igor_shaula.hometask_zf.ThisApp
import com.igor_shaula.hometask_zf.data.VehiclesRepository

class VehicleListViewModel : ViewModel() {

    private var repository: VehiclesRepository = ThisApp.getVehiclesRepository()

    val vehiclesMap = repository.vehiclesMapMLD

    val timeToUpdateVehicleStatus = MutableLiveData<Unit>()

    fun getAllVehiclesIds() {
        repository.getAllVehiclesIds()
    }

    fun startGettingVehiclesDetails(size: Int) {
        repository.startGettingVehiclesDetails(size, ::updateTheViewModel)
    }

    fun stopGettingVehiclesDetails() {
        repository.stopGettingVehiclesDetails()
    }

    fun getNumberOfNearVehicles() = repository.getNumberOfNearVehicles()

    private fun updateTheViewModel() {
        timeToUpdateVehicleStatus.value = Unit // just to show new statuses on UI
    }
}
