package com.example.cargostowing.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cargo_items")
data class CargoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manifestOwnerNo: String,
    val ptiNo: String,
    val pcsCly: Int,
    val weightPerPcs: Double?,
    val subTotalWeight: Double,
    val description: String,
    val customerName: String,
    val pagNo: String? = null,
    val isStowed: Boolean = false
)

@Dao
interface CargoDao {
    @Query("SELECT * FROM cargo_items WHERE manifestOwnerNo = :manifestNo")
    fun getCargoByManifest(manifestNo: String): Flow<List<CargoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(item: CargoItemEntity)

    @Update
    suspend fun updateCargo(item: CargoItemEntity)

    @Query("SELECT * FROM cargo_items WHERE manifestOwnerNo = :manifestNo AND UPPER(description) = UPPER(:description) AND UPPER(customerName) = UPPER(:customerName) AND (COALESCE(UPPER(pagNo), '') = COALESCE(UPPER(:pagNo), '')) LIMIT 1")
    suspend fun findCargoByDescAndCustomer(manifestNo: String, description: String, customerName: String, pagNo: String?): CargoItemEntity?

    @Query("UPDATE cargo_items SET isStowed = :isStowed WHERE id = :id")
    suspend fun updateStowStatus(id: Long, isStowed: Boolean)
}

@Database(entities = [CargoItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cargoDao(): CargoDao
}
