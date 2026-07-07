package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Kural
import com.example.data.KuralRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class ThirukkuralViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = KuralRepository(database.kuralDao())

    sealed interface DbLoadState {
        object Idle : DbLoadState
        object Loading : DbLoadState
        object Success : DbLoadState
        data class Error(val message: String) : DbLoadState
    }

    private val _dbLoadState = MutableStateFlow<DbLoadState>(DbLoadState.Idle)
    val dbLoadState: StateFlow<DbLoadState> = _dbLoadState.asStateFlow()

    enum class Tab { RANDOM, SEARCH, BROWSE, FAVORITES }
    private val _currentTab = MutableStateFlow(Tab.RANDOM)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    val allKurals: StateFlow<List<Kural>> = repository.allKurals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteKurals: StateFlow<List<Kural>> = repository.favoriteKurals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _randomKural = MutableStateFlow<Kural?>(null)
    val randomKural: StateFlow<Kural?> = _randomKural.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<Kural?>(null)
    val searchResult: StateFlow<Kural?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _selectedKuralForDetails = MutableStateFlow<Kural?>(null)
    val selectedKuralForDetails: StateFlow<Kural?> = _selectedKuralForDetails.asStateFlow()

    init {
        initDatabase()
    }

    fun initDatabase() {
        viewModelScope.launch {
            _dbLoadState.value = DbLoadState.Loading
            val count = repository.getKuralCount()
            if (count == 0) {
                val result = repository.fetchAndPopulate()
                if (result.isSuccess) {
                    _dbLoadState.value = DbLoadState.Success
                    generateRandomKural()
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    _dbLoadState.value = DbLoadState.Error("Failed to download Kural data: $errorMsg")
                }
            } else {
                _dbLoadState.value = DbLoadState.Success
                generateRandomKural()
            }
        }
    }

    fun selectTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun generateRandomKural() {
        val kurals = allKurals.value
        if (kurals.isNotEmpty()) {
            val randomIndex = Random.nextInt(kurals.size)
            _randomKural.value = kurals[randomIndex]
        } else {
            viewModelScope.launch {
                val count = repository.getKuralCount()
                if (count > 0) {
                    val randNumber = Random.nextInt(1, count + 1)
                    _randomKural.value = repository.getKuralByNumber(randNumber)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _searchError.value = null
        if (query.isBlank()) {
            _searchResult.value = null
            return
        }
        val number = query.toIntOrNull()
        if (number == null || number !in 1..1330) {
            _searchError.value = "Please enter a valid Kural number between 1 and 1330."
            _searchResult.value = null
            return
        }

        viewModelScope.launch {
            val kural = repository.getKuralByNumber(number)
            if (kural != null) {
                _searchResult.value = kural
            } else {
                _searchError.value = "Kural not found."
                _searchResult.value = null
            }
        }
    }

    fun toggleFavorite(kural: Kural) {
        viewModelScope.launch {
            val newFavStatus = !kural.isFavorite
            repository.updateFavoriteStatus(kural.number, newFavStatus)
            
            val curRand = _randomKural.value
            if (curRand != null && curRand.number == kural.number) {
                _randomKural.value = curRand.copy(isFavorite = newFavStatus)
            }
            
            val curSearch = _searchResult.value
            if (curSearch != null && curSearch.number == kural.number) {
                _searchResult.value = curSearch.copy(isFavorite = newFavStatus)
            }

            val curDetail = _selectedKuralForDetails.value
            if (curDetail != null && curDetail.number == kural.number) {
                _selectedKuralForDetails.value = curDetail.copy(isFavorite = newFavStatus)
            }
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
            _randomKural.value = _randomKural.value?.copy(isFavorite = false)
            _searchResult.value = _searchResult.value?.copy(isFavorite = false)
            _selectedKuralForDetails.value = _selectedKuralForDetails.value?.copy(isFavorite = false)
        }
    }

    fun selectKuralForDetails(kural: Kural?) {
        _selectedKuralForDetails.value = kural
    }
}
