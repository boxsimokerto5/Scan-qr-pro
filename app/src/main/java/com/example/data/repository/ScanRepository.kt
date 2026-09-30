package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.local.ScanItemEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanItemEntity>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanItemEntity>> = scanDao.getFavoriteScans()

    fun searchScans(query: String): Flow<List<ScanItemEntity>> = scanDao.searchScans(query)

    suspend fun getScanById(id: Long): ScanItemEntity? = scanDao.getScanById(id)

    suspend fun insertScan(scan: ScanItemEntity): Long = scanDao.insertScan(scan)

    suspend fun updateScan(scan: ScanItemEntity) = scanDao.updateScan(scan)

    suspend fun deleteScan(scan: ScanItemEntity) = scanDao.deleteScan(scan)

    suspend fun deleteScanById(id: Long) = scanDao.deleteScanById(id)

    suspend fun clearAll() = scanDao.clearAllScans()

    suspend fun toggleFavorite(id: Long, currentFav: Boolean) =
        scanDao.updateFavorite(id, !currentFav)
}
