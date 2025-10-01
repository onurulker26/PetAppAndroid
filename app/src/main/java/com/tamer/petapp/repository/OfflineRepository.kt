package com.tamer.petapp.repository

import android.content.Context
import android.util.Log
import androidx.room.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Vaccination
import com.tamer.petapp.model.Treatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

// Room Entities
@Entity(tableName = "pets_cache")
data class PetCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val breed: String,
    val birthDate: Long,
    val weight: Double,
    val userId: String,
    val photoUrl: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "vaccinations_cache")
data class VaccinationCacheEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val name: String,
    val date: Long,
    val nextDate: Long?,
    val notes: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

// Room DAOs
@Dao
interface PetCacheDao {
    @Query("SELECT * FROM pets_cache WHERE userId = :userId")
    fun getUserPets(userId: String): Flow<List<PetCacheEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPets(pets: List<PetCacheEntity>)
    
    @Query("DELETE FROM pets_cache WHERE userId = :userId")
    fun deleteUserPets(userId: String): Int
}

@Dao
interface VaccinationCacheDao {
    @Query("SELECT * FROM vaccinations_cache WHERE petId = :petId")
    fun getPetVaccinations(petId: String): Flow<List<VaccinationCacheEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVaccinations(vaccinations: List<VaccinationCacheEntity>)
}

// Room Database
@Database(
    entities = [PetCacheEntity::class, VaccinationCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun petCacheDao(): PetCacheDao
    abstract fun vaccinationCacheDao(): VaccinationCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null
        
        fun getDatabase(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Offline Repository
class OfflineRepository(context: Context) {
    private val database = OfflineDatabase.getDatabase(context)
    private val petDao = database.petCacheDao()
    private val vaccinationDao = database.vaccinationCacheDao()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Sync strategy: Cache First, then Server
    fun getUserPetsWithSync(userId: String): Flow<List<Pet>> = flow {
        try {
            // 1. İlk önce cache'den emit et
            petDao.getUserPets(userId).collect { cachedPets ->
                if (cachedPets.isNotEmpty()) {
                    emit(cachedPets.map { it.toPet() })
                }
            }
            
            // 2. Server'dan güncelleme al
            val serverPets = firestore.collection("users")
                .document(userId)
                .collection("pets")
                .get(Source.SERVER)
                .await()
                .toObjects(Pet::class.java)
            
            // 3. Cache'i güncelle - artık suspend değil, ama IO thread'de çalıştırmalıyız
            Dispatchers.IO.let { ioDispatcher ->
                withContext(ioDispatcher) {
                    petDao.deleteUserPets(userId)
                    petDao.insertPets(serverPets.map { it.toCacheEntity(userId) })
                }
            }
            
            // 4. Güncel veriyi emit et
            emit(serverPets)
            
        } catch (e: Exception) {
            Log.e("OfflineRepository", "Sync error: ${e.message}")
            // Hata durumunda sadece cache'den ver
            petDao.getUserPets(userId).collect { cachedPets ->
                emit(cachedPets.map { it.toPet() })
            }
        }
    }
}

// Extension functions
private fun PetCacheEntity.toPet(): Pet {
    return Pet(
        id = id,
        name = name,
        type = type,
        breed = breed,
        birthDate = birthDate,
        weight = weight,
        imageUrl = photoUrl ?: ""
    )
}

private fun Pet.toCacheEntity(userId: String): PetCacheEntity {
    return PetCacheEntity(
        id = id,
        name = name,
        type = type,
        breed = breed,
        birthDate = birthDate,
        weight = weight,
        userId = userId,
        photoUrl = imageUrl
    )
} 