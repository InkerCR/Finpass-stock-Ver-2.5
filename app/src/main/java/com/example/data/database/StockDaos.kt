package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY createdAt ASC")
    fun getAllWarehousesFlow(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses")
    suspend fun getAllWarehouses(): List<WarehouseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: WarehouseEntity)

    @Query("DELETE FROM warehouses WHERE id = :id")
    suspend fun deleteWarehouseById(id: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE warehouseId = :warehouseId ORDER BY id ASC")
    fun getProductsByWarehouseFlow(warehouseId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE warehouseId = :warehouseId")
    suspend fun getProductsByWarehouse(warehouseId: String): List<ProductEntity>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE warehouseId = :warehouseId AND id = :id")
    suspend fun deleteProductById(warehouseId: String, id: String)

    @Query("DELETE FROM products WHERE warehouseId = :warehouseId")
    suspend fun clearProductsByWarehouse(warehouseId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE warehouseId = :warehouseId ORDER BY timestamp DESC")
    fun getTransactionsByWarehouseFlow(warehouseId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE warehouseId = :warehouseId")
    suspend fun deleteTransactionsByWarehouse(warehouseId: String)
}
