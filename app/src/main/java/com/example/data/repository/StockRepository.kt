package com.example.data.repository

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StockRepository {

    suspend fun checkAndInitializeDefaultData(context: Context) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val userDao = database.userDao()
        val warehouseDao = database.warehouseDao()
        val productDao = database.productDao()

        // Check users
        val existingUsers = userDao.getAllUsersFlow().first()
        if (existingUsers.isEmpty()) {
            userDao.insertUser(UserEntity("admin", "admin123", "admin", "ຜູ້ບໍລິຫານລະບົບ"))
            userDao.insertUser(UserEntity("manager", "manager123", "manager", "ຜູ້ຈັດການສາງ"))
            userDao.insertUser(UserEntity("viewer", "viewer123", "viewer", "ຜູ້ເບິ່ງລາຍງານ"))
        }

        // Check warehouses
        val existingWarehouses = warehouseDao.getAllWarehousesFlow().first()
        if (existingWarehouses.isEmpty()) {
            val defaultWh = WarehouseEntity(
                id = "wh1",
                name = "ສາງຫຼັກ ນະຄອນຫຼວງ",
                location = "ນະຄອນຫຼວງວຽງຈັນ",
                isMain = true,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            warehouseDao.insertWarehouse(defaultWh)

            // Insert initial products for wh1
            productDao.insertProduct(ProductEntity("wh1", "STK001", "ສະຕິ້ກເກີ້", "ກໍ້", "ວັດສະດຸ", 0, 0, 484, 10))
            productDao.insertProduct(ProductEntity("wh1", "BLL001", "ກໍ້ເຈ້ຍບິນ", "ກໍ້", "ວັດສະດຸ", 0, 0, 245, 10))
            productDao.insertProduct(ProductEntity("wh1", "PT001", "ປິ້ນເຕີ້", "ເຄື່ອງ", "ອຸປະກອນ", 0, 0, 3, 10))
            productDao.insertProduct(ProductEntity("wh1", "LB001", "ລິບບອນ", "ກໍ້", "ວັດສະດຸ", 60000, 60000, 484, 30))
        }
    }

    // --- Warehouses Operations ---
    fun getAllWarehousesFlow(context: Context): Flow<List<WarehouseEntity>> {
        val db = AppDatabase.getDatabase(context)
        return db.warehouseDao().getAllWarehousesFlow()
    }

    suspend fun addWarehouse(context: Context, id: String, name: String, location: String, isMain: Boolean = false) {
        val db = AppDatabase.getDatabase(context)
        val wh = WarehouseEntity(
            id = id,
            name = name,
            location = location,
            isMain = isMain,
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        )
        db.warehouseDao().insertWarehouse(wh)
    }

    suspend fun deleteWarehouse(context: Context, id: String) {
        val db = AppDatabase.getDatabase(context)
        db.warehouseDao().deleteWarehouseById(id)
        // Also clear products of that warehouse to maintain integrity
        db.productDao().clearProductsByWarehouse(id)
        db.transactionDao().deleteTransactionsByWarehouse(id)
    }

    // --- Users Operations ---
    fun getAllUsersFlow(context: Context): Flow<List<UserEntity>> {
        val db = AppDatabase.getDatabase(context)
        return db.userDao().getAllUsersFlow()
    }

    suspend fun saveUser(context: Context, user: UserEntity) {
        val db = AppDatabase.getDatabase(context)
        db.userDao().insertUser(user)
    }

    suspend fun deleteUser(context: Context, username: String) {
        val db = AppDatabase.getDatabase(context)
        db.userDao().deleteUserByUsername(username)
    }

    // --- Products Operations ---
    fun getProductsByWarehouseFlow(context: Context, warehouseId: String): Flow<List<ProductEntity>> {
        val db = AppDatabase.getDatabase(context)
        return db.productDao().getProductsByWarehouseFlow(warehouseId)
    }

    suspend fun saveProduct(context: Context, product: ProductEntity) {
        val db = AppDatabase.getDatabase(context)
        db.productDao().insertProduct(product)
    }

    suspend fun deleteProduct(context: Context, warehouseId: String, id: String) {
        val db = AppDatabase.getDatabase(context)
        db.productDao().deleteProductById(warehouseId, id)
    }

    // --- Transactions Operations ---
    fun getTransactionsByWarehouseFlow(context: Context, warehouseId: String): Flow<List<TransactionEntity>> {
        val db = AppDatabase.getDatabase(context)
        return db.transactionDao().getTransactionsByWarehouseFlow(warehouseId)
    }

    suspend fun recordAndApplyTransaction(
        context: Context,
        warehouseId: String,
        productId: String,
        type: String, // "in", "out"
        qty: Int,
        note: String,
        currentUser: String
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        
        // Find existing product inside the specific warehouse
        val products = db.productDao().getProductsByWarehouse(warehouseId)
        val product = products.firstOrNull { it.id == productId } ?: return@withContext

        // Calculate direct updates
        val updatedStock = if (type == "in") {
            product.stock + qty
        } else {
            product.stock - qty
        }

        // Calculate profit
        // Profit = type is out -> qty * (price - cost), else 0
        val profit = if (type == "out") {
            qty * (product.price - product.cost)
        } else {
            0
        }

        // Apply product update
        db.productDao().insertProduct(
            product.copy(stock = updatedStock)
        )

        // Save Transaction log
        val trans = TransactionEntity(
            warehouseId = warehouseId,
            productId = productId,
            type = type,
            quantity = qty,
            note = note,
            timestamp = System.currentTimeMillis(),
            user = currentUser,
            profit = profit
        )
        db.transactionDao().insertTransaction(trans)
    }

    // Bulk Restore Data
    suspend fun restoreDatabase(
        context: Context,
        warehouses: List<WarehouseEntity>,
        users: List<UserEntity>,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        // Clear everything first
        db.clearAllTables()

        // Insert Warehouses
        warehouses.forEach { db.warehouseDao().insertWarehouse(it) }

        // Insert Users
        users.forEach { db.userDao().insertUser(it) }

        // Insert Products
        products.forEach { db.productDao().insertProduct(it) }

        // Insert Transactions
        transactions.forEach { db.transactionDao().insertTransaction(it) }
    }

    suspend fun getAllProducts(context: Context): List<ProductEntity> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).productDao().getAllProducts()
    }

    suspend fun getAllTransactions(context: Context): List<TransactionEntity> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).transactionDao().getAllTransactions()
    }

    suspend fun getAllWarehouses(context: Context): List<WarehouseEntity> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).warehouseDao().getAllWarehouses()
    }

    suspend fun getAllUsers(context: Context): List<UserEntity> = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).userDao().getAllUsers()
    }
}
