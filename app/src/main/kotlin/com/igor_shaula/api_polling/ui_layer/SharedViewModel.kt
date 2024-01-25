package com.igor_shaula.api_polling.ui_layer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.igor_shaula.api_polling.ThisApp
import com.igor_shaula.api_polling.data_layer.VehicleDetailsRecord
import com.igor_shaula.api_polling.data_layer.VehicleRecord
import com.igor_shaula.api_polling.data_layer.VehicleStatus
import com.igor_shaula.api_polling.data_layer.VehiclesRepository
import com.igor_shaula.api_polling.data_layer.detectVehicleStatus
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedViewModel(repository: VehiclesRepository) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <SharedViewModelType : ViewModel> create(
                modelClass: Class<SharedViewModelType>,
                extras: CreationExtras
            ): SharedViewModelType {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()
                return SharedViewModel(
                    (application as ThisApp).getRepository()
                ) as SharedViewModelType
            }
        }
    }

    // todo: use data from vehiclesMapFlow on the UI layer
    val vehiclesMapFlow: Flow<MutableMap<String, VehicleRecord>?> = repository.vehiclesDataFlow
        .also { Timber.v("vehiclesMapFlow updated") }

    // for inner use - only inside this ViewModel - as Google recommends in its examples
    private val mutableVehiclesMap = MutableLiveData<MutableMap<String, VehicleRecord>>()

    // for outer use - mostly in Fragments & Activities - as Google recommends in its examples
    val vehiclesMap: LiveData<MutableMap<String, VehicleRecord>> get() = mutableVehiclesMap

    private val mutableVehiclesDetailsMap =
        MutableLiveData<MutableMap<String, VehicleDetailsRecord>>()
    val vehiclesDetailsMap: LiveData<MutableMap<String, VehicleDetailsRecord>>
        get() = mutableVehiclesDetailsMap

    // no need to make this LiveData private - it's only a trigger for update action
    val timeToUpdateVehicleStatus = MutableLiveData<Unit>()
    val timeToShowGeneralBusyState = MutableLiveData<Boolean>()
    val timeToShowFakeDataProposal = MutableLiveData<Boolean>()
    val timeToAdjustForFakeData = MutableLiveData<Unit>()

    private val repositoryObserver: Observer<MutableMap<String, VehicleRecord>> = Observer {
        mutableVehiclesMap.value = it
        Timber.i("mutableVehiclesMap.value = ${mutableVehiclesMap.value}")
        if (mutableVehiclesMap.value?.isEmpty() == true) processAlternativesForGettingData()
        getAllVehiclesJob?.cancel()
        getAllVehiclesJob = null
    }

    private var repository: VehiclesRepository by RepositoryProperty(repositoryObserver)

    private val coroutineScope = MainScope() + CoroutineName(this.javaClass.simpleName)

    private var getAllVehiclesJob: Job? = null

    private var firstTimeLaunched = true

    init {
        mutableVehiclesDetailsMap.value = mutableMapOf()
        coroutineScope.launch {
            vehiclesMapFlow.collect {
                Timber.v("vehiclesMapFlow new value = $it")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getAllVehiclesJob?.cancel()
        coroutineScope.cancel()
    }

    fun getAllVehiclesIds() {
        // i decided to have lazy subscription - only when we need it - here, not in init-block
        getAllVehiclesJob = coroutineScope.launch(Dispatchers.Main) { // only Main does work here
            repository.launchGetAllVehicleIdsRequest(::toggleMainBusyState)
        }
    }

    fun startGettingVehiclesDetails() {
        coroutineScope.launch {
            repository.startGettingVehiclesDetails(::updateTheViewModel, ::toggleBusyStateFor)
        }
    }

    fun stopGettingVehiclesDetails() {
        repository.stopGettingVehiclesDetails()
    }

    fun getNumberOfNearVehicles() = repository.getNumberOfNearVehicles()

    fun getNumberOfAllVehicles() = repository.getNumberOfAllVehicles()

    private fun processAlternativesForGettingData() {
        if (firstTimeLaunched) { // during the first launch showing dialog is not needed
            firstTimeLaunched = false
            return // optimization for avoiding excess IO request to the storage
        }
        coroutineScope.launch(Dispatchers.Main) {
            timeToShowFakeDataProposal.value = true
        }
    }

    private fun updateTheViewModel(vehicleId: String, vehicleDetails: VehicleDetailsRecord) {
        mutableVehiclesMap.value?.put(
            vehicleId,
            VehicleRecord(vehicleId, detectVehicleStatus(vehicleDetails), false)
        )
        mutableVehiclesDetailsMap.value?.put(vehicleId, vehicleDetails)
        mutableVehiclesDetailsMap.postValue(mutableVehiclesDetailsMap.value)
        // why postValue instead of setValue() -> https://www.geeksforgeeks.org/livedata-setvalue-vs-postvalue-in-android/
        timeToUpdateVehicleStatus.value = Unit // just to show new statuses on UI
    }

    private fun toggleBusyStateFor(vehicleId: String, isBusy: Boolean) {
        mutableVehiclesMap.value?.put(
            vehicleId,
            VehicleRecord(vehicleId, VehicleStatus.NEW_ROUND, isBusy)
        )
    }

    private fun toggleMainBusyState(isBusy: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            timeToShowGeneralBusyState.value = isBusy
        }
    }

    fun onReadyToUseFakeData() {
        stopGettingVehiclesDetails() // to avoid any possible resource leaks if this one still works
        repository =
            ThisApp.switchActiveDataSource(ThisApp.ActiveDataSource.FAKE) // must be a new value - with fake data
        timeToAdjustForFakeData.value = Unit
    }

    fun clearPreviousFakeDataSelection() {
        firstTimeLaunched = true
        timeToShowFakeDataProposal.value = false
        mutableVehiclesMap.value?.clear()
        coroutineScope.cancel()
        repository = ThisApp.switchActiveDataSource(ThisApp.ActiveDataSource.NETWORK)
    }
}

class RepositoryProperty(private val observer: Observer<MutableMap<String, VehicleRecord>>) :
    ReadWriteProperty<Any, VehiclesRepository> {

    private lateinit var repository: VehiclesRepository
    override fun getValue(thisRef: Any, property: KProperty<*>): VehiclesRepository {
        if (!this::repository.isInitialized) {
            repository = ThisApp.getRepository()
            repository.mainDataStorage.observeForever(observer)
        }
        return repository
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: VehiclesRepository) {
        if (this::repository.isInitialized) {
            repository.mainDataStorage.removeObserver(observer)
        }
        repository = value
        repository.mainDataStorage.observeForever(observer)
    }
}