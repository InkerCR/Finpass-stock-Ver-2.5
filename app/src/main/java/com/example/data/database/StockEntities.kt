package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val isMain: Boolean,
    val createdAt: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String,
    val role: String, // admin, manager, viewer
    val fullname: String
)

@Entity(tableName = "products", primaryKeys = ["warehouseId", "id"])
data class ProductEntity(
    val warehouseId: String,
    val id: String, // Product Code (STK001, etc.)
    val name: String,
    val unit: String,
    val category: String, // ວັດສະດຸ, ອຸປະກອນ, ອື່ນໆ
    val price: Int,
    val cost: Int,
    val stock: Int,
    val minStock: Int
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val warehouseId: String,
    val productId: String,
    val type: String, // "in", "out"
    val quantity: Int,
    val note: String,
    val timestamp: Long,
    val user: String,
    val profit: Int
)
