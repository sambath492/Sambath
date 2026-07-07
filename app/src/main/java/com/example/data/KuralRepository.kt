package com.example.data

import kotlinx.coroutines.flow.Flow

class KuralRepository(private val kuralDao: KuralDao) {
    val allKurals: Flow<List<Kural>> = kuralDao.getAllKurals()
    val favoriteKurals: Flow<List<Kural>> = kuralDao.getFavoriteKurals()

    suspend fun getKuralByNumber(number: Int): Kural? = kuralDao.getKuralByNumber(number)

    suspend fun getKuralCount(): Int = kuralDao.getKuralCount()

    suspend fun insertKurals(kurals: List<Kural>) = kuralDao.insertKurals(kurals)

    suspend fun updateFavoriteStatus(number: Int, isFavorite: Boolean) =
        kuralDao.updateFavoriteStatus(number, isFavorite)

    suspend fun clearAllFavorites() = kuralDao.clearAllFavorites()

    suspend fun fetchAndPopulate(): Result<Unit> {
        return try {
            val count = kuralDao.getKuralCount()
            if (count == 0) {
                val response = RetrofitClient.apiService.getThirukkural()
                val remoteKurals = response.thirukkural
                if (remoteKurals.isNotEmpty()) {
                    kuralDao.insertKurals(remoteKurals)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
