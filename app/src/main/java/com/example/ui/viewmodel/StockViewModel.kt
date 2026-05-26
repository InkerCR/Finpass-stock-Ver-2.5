package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.StockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class WarehouseTab {
    INVENTORY,
    TRANSACTIONS,
    REPORTS,
    ALERTS
}

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    // Logged in User State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Tab State
    private val _currentTab = MutableStateFlow(WarehouseTab.INVENTORY)
    val currentTab: StateFlow<WarehouseTab> = _currentTab.asStateFlow()

    // Active Warehouse ID State
    private val _activeWarehouseId = MutableStateFlow("wh1")
    val activeWarehouseId: StateFlow<String> = _activeWarehouseId.asStateFlow()

    // Search query for active products list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Cloud Synchronization Room State Group (Lao Multi-User Room Online)
    private val prefs = context.getSharedPreferences("aif_stock_sync_prefs", Context.MODE_PRIVATE)

    private val _syncRoomId = MutableStateFlow(prefs.getString("sync_room_id", "") ?: "")
    val syncRoomId: StateFlow<String> = _syncRoomId.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("is_auto_sync", false))
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Date Range Filters for transactions
    private val _transFilterStart = MutableStateFlow<Long?>(null)
    val transFilterStart: StateFlow<Long?> = _transFilterStart.asStateFlow()

    private val _transFilterEnd = MutableStateFlow<Long?>(null)
    val transFilterEnd: StateFlow<Long?> = _transFilterEnd.asStateFlow()

    // Toast/Alert message triggers
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Observe DB source lists
    val warehouses: StateFlow<List<WarehouseEntity>> = StockRepository.getAllWarehousesFlow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = StockRepository.getAllUsersFlow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products of selected active warehouse
    val products: StateFlow<List<ProductEntity>> = _activeWarehouseId
        .flatMapLatest { whId ->
            StockRepository.getProductsByWarehouseFlow(context, whId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered products list based on search query
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(products, _searchQuery) { prodList, query ->
        if (query.isBlank()) {
            prodList
        } else {
            val q = query.lowercase().trim()
            prodList.filter { it.id.lowercase().contains(q) || it.name.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions of selected active warehouse
    val transactions: StateFlow<List<TransactionEntity>> = _activeWarehouseId
        .flatMapLatest { whId ->
            StockRepository.getTransactionsByWarehouseFlow(context, whId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions list based on date ranges
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(transactions, _transFilterStart, _transFilterEnd) { transList, start, end ->
        transList.filter { trans ->
            val matchesStart = start == null || trans.timestamp >= start
            val matchesEnd = end == null || trans.timestamp <= end
            matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI summary analysis status
    private val _aiReportState = MutableStateFlow<UiState<String>?>(null)
    val aiReportState: StateFlow<UiState<String>?> = _aiReportState.asStateFlow()

    init {
        // Initialize DB data if empty on start
        viewModelScope.launch {
            StockRepository.checkAndInitializeDefaultData(context)
            if (_isAutoSyncEnabled.value && _syncRoomId.value.isNotEmpty()) {
                pullFromCloudSilent()
            }
        }
    }

    // --- Authentication Actions ---
    fun login(username: String, password: String): Boolean {
        val foundUser = users.value.find { it.username.lowercase() == username.lowercase() && it.password == password }
        return if (foundUser != null) {
            _currentUser.value = foundUser
            showToast("🎉 ຍິນດີຕ້ອນຮັບເຂົ້າສູ່ລະບົບ, ${foundUser.fullname}!")
            true
        } else {
            showToast("❌ ຊື່ຜູ້ໃຊ້ ຫຼື ລະຫັດຜ່ານບໍ່ຖືກຕ້ອງ!")
            false
        }
    }

    fun logout() {
        _currentUser.value = null
        showToast("👋 ອອກຈາກລະບົບ")
    }

    fun selectTab(tab: WarehouseTab) {
        _currentTab.value = tab
    }

    fun setActiveWarehouse(id: String) {
        _activeWarehouseId.value = id
        _aiReportState.value = null // reset AI analysis on warehouse switch
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilter(start: Long?, end: Long?) {
        _transFilterStart.value = start
        _transFilterEnd.value = end
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    // --- DB Modification Actions (Guarded by User Roles as required) ---

    fun saveWarehouse(id: String, name: String, location: String) {
        viewModelScope.launch {
            try {
                StockRepository.addWarehouse(context, id, name, location)
                showToast("✅ ເພີ່ມສາງ $name ສຳເລັດ!")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດເພີ່ມສາງ: ${e.localizedMessage}")
            }
        }
    }

    fun deleteWarehouse(id: String) {
        viewModelScope.launch {
            try {
                StockRepository.deleteWarehouse(context, id)
                showToast("🗑️ ລຶບສາງສຳເລັດ!")
                // If active warehouse deleted, select the first remaining
                val remaining = warehouses.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    _activeWarehouseId.value = remaining[0].id
                }
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດລຶບສາງ: ${e.localizedMessage}")
            }
        }
    }

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            try {
                StockRepository.saveUser(context, user)
                showToast("✅ ບັນທຶກຜູ້ໃຊ້ ${user.username} ສຳເລັດ!")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດບັນທຶກຜູ້ໃຊ້: ${e.localizedMessage}")
            }
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            try {
                StockRepository.deleteUser(context, username)
                showToast("🗑️ ລຶບຜູ້ໃຊ້ $username ສຳເລັດ!")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດລຶບຜູ້ໃຊ້: ${e.localizedMessage}")
            }
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                StockRepository.saveProduct(context, product)
                showToast("✅ ບັນທຶກສິນຄ້າ ${product.id} ສຳເລັດ!")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດບັນທຶກສິນຄ້າ: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        val whId = _activeWarehouseId.value
        viewModelScope.launch {
            try {
                StockRepository.deleteProduct(context, whId, productId)
                showToast("🗑️ ລຶບສິນຄ້າ $productId ສຳເລັດ!")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດລຶບສິນຄ້າ: ${e.localizedMessage}")
            }
        }
    }

    fun executeTransaction(productId: String, type: String, quantity: Int, note: String) {
        val whId = _activeWarehouseId.value
        val userStr = _currentUser.value?.let { it.fullname.ifBlank { it.username } } ?: "Unknown"

        viewModelScope.launch {
            try {
                StockRepository.recordAndApplyTransaction(
                    context = context,
                    warehouseId = whId,
                    productId = productId,
                    type = type,
                    qty = quantity,
                    note = note,
                    currentUser = userStr
                )
                val typeLa = if (type == "in") "ນຳເຂົ້າ" else "ສົ່ງອອກ"
                showToast("✅ $typeLa ສຳເລັດ: $quantity ຣາຍການ")
                triggerAutoSyncPush()
            } catch (e: Exception) {
                showToast("❌ ເກີດຂໍ້ຜິດພາດໃນການບັນທຶກ: ${e.localizedMessage}")
            }
        }
    }

    // --- Bulk Restore and Backup ---

    fun generateBackupJson(onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Retrieve data completely in background from repository
                val whList = StockRepository.getAllWarehouses(context)
                val uList = StockRepository.getAllUsers(context)
                val pList = StockRepository.getAllProducts(context)
                val tList = StockRepository.getAllTransactions(context)

                val root = JSONObject()

                val whArr = JSONArray()
                whList.forEach { wh ->
                    val obj = JSONObject()
                    obj.put("id", wh.id)
                    obj.put("name", wh.name)
                    obj.put("location", wh.location)
                    obj.put("isMain", wh.isMain)
                    obj.put("createdAt", wh.createdAt)
                    whArr.put(obj)
                }
                root.put("warehouses", whArr)

                val uArr = JSONArray()
                uList.forEach { u ->
                    val obj = JSONObject()
                    obj.put("username", u.username)
                    obj.put("password", u.password)
                    obj.put("role", u.role)
                    obj.put("fullname", u.fullname)
                    uArr.put(obj)
                }
                root.put("users", uArr)

                val pArr = JSONArray()
                pList.forEach { p ->
                    val obj = JSONObject()
                    obj.put("warehouseId", p.warehouseId)
                    obj.put("id", p.id)
                    obj.put("name", p.name)
                    obj.put("unit", p.unit)
                    obj.put("category", p.category)
                    obj.put("price", p.price)
                    obj.put("cost", p.cost)
                    obj.put("stock", p.stock)
                    obj.put("minStock", p.minStock)
                    pArr.put(obj)
                }
                root.put("products", pArr)

                val tArr = JSONArray()
                tList.forEach { t ->
                    val obj = JSONObject()
                    obj.put("id", t.id)
                    obj.put("warehouseId", t.warehouseId)
                    obj.put("productId", t.productId)
                    obj.put("type", t.type)
                    obj.put("quantity", t.quantity)
                    obj.put("note", t.note)
                    obj.put("timestamp", t.timestamp)
                    obj.put("user", t.user)
                    obj.put("profit", t.profit)
                    tArr.put(obj)
                }
                root.put("transactions", tArr)

                onCompleted(root.toString(2))
                showToast("💾 ສຳຮອງຂໍ້ມູນສຳເລັດ!")
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດສຳຮອງຂໍ້ມູນ: ${e.localizedMessage}")
                onCompleted("")
            }
        }
    }

    fun restoreDatabase(jsonStr: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)

                val warehouses = mutableListOf<WarehouseEntity>()
                val users = mutableListOf<UserEntity>()
                val products = mutableListOf<ProductEntity>()
                val transactions = mutableListOf<TransactionEntity>()

                if (root.has("warehouses")) {
                    val arr = root.getJSONArray("warehouses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        warehouses.add(
                            WarehouseEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                location = obj.getString("location"),
                                isMain = obj.optBoolean("isMain", false),
                                createdAt = obj.optString("createdAt", "")
                            )
                        )
                    }
                }

                if (root.has("users")) {
                    val arr = root.getJSONArray("users")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        users.add(
                            UserEntity(
                                username = obj.getString("username"),
                                password = obj.getString("password"),
                                role = obj.getString("role"),
                                fullname = obj.optString("fullname", "")
                            )
                        )
                    }
                }

                if (root.has("products")) {
                    val arr = root.getJSONArray("products")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        products.add(
                            ProductEntity(
                                warehouseId = obj.getString("warehouseId"),
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                unit = obj.getString("unit"),
                                category = obj.getString("category"),
                                price = obj.getInt("price"),
                                cost = obj.getInt("cost"),
                                stock = obj.getInt("stock"),
                                minStock = obj.getInt("minStock")
                            )
                        )
                    }
                }

                if (root.has("transactions")) {
                    val arr = root.getJSONArray("transactions")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        transactions.add(
                            TransactionEntity(
                                id = obj.optInt("id", 0),
                                warehouseId = obj.getString("warehouseId"),
                                productId = obj.getString("productId"),
                                type = obj.getString("type"),
                                quantity = obj.getInt("quantity"),
                                note = obj.optString("note", ""),
                                timestamp = obj.getLong("timestamp"),
                                user = obj.optString("user", "System"),
                                profit = obj.optInt("profit", 0)
                            )
                        )
                    }
                }

                StockRepository.restoreDatabase(context, warehouses, users, products, transactions)
                showToast("✅ ກູ້ຄືນຂໍ້ມູນສຳເລັດ!")
                // Reset active warehouse
                if (warehouses.isNotEmpty()) {
                    _activeWarehouseId.value = warehouses[0].id
                }
            } catch (e: Exception) {
                showToast("❌ ບໍ່ສາມາດກູ້ຄືນ: ໄຟລ໌ບໍ່ຖືກຕ້ອງ!")
            }
        }
    }

    // AI Analysis capability inside WAREHOUSE Reports tab using Gemini
    fun requestAiWarehouseAnalysis() {
        _aiReportState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val whList = warehouses.value
                val currentWh = whList.find { it.id == _activeWarehouseId.value }
                val pList = products.value
                val tList = transactions.value

                val totalProducts = pList.size
                val totalStockVal = pList.sumOf { it.stock * it.price }
                val lowStockCount = pList.filter { it.stock <= it.minStock }.size

                // Produce beautiful summary context for Gemini
                val contextPrompt = """
                    You are AIF Stock Intelligent AI Analyst. Detail an executive warehouse stock assessment based on this real-time inventory summary (written in clear, elegant, readable Lao language representing business professionalism):
                    Warehouse Name: ${currentWh?.name ?: "Unknown Warehouse"}
                    Location: ${currentWh?.location ?: "Unknown Location"}
                    Total Goods/Products: $totalProducts items
                    Total Valuation (LAK): $totalStockVal Kip
                    Low stock or critical products needing replenishment: $lowStockCount items.
                    
                    Write a highly visual, structured text analysis with bullet lists in Lao language on these parts:
                    1. 🔍 Overview Assessment (ສະຫຼຸບສະຖານະສາງ)
                    2. ⚠️ Stock Alerts / Action items (ລາຍການເຕືອນໄພຄົງເຫຼືອຕ່ຳ)
                    3. 💡 Storage Optimization Advice (ຄຳແນະນຳການຈັດວາງ ແລະ ທຸລະກຳ)
                """.trimIndent()

                // Call the Gemini API model
                val response = com.example.data.api.GeminiService.callGeminiRawText(contextPrompt)
                _aiReportState.value = UiState.Success(response)
            } catch (e: Exception) {
                _aiReportState.value = UiState.Error(e.localizedMessage ?: "AI consultation failed.")
            }
        }
    }

    // --- Cloud Synchronization Methods ---

    fun setSyncRoomId(roomId: String) {
        _syncRoomId.value = roomId
        prefs.edit().putString("sync_room_id", roomId).apply()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_sync", enabled).apply()
    }

    fun triggerAutoSyncPush() {
        if (_isAutoSyncEnabled.value && _syncRoomId.value.isNotEmpty()) {
            pushToCloudSilent()
        }
    }

    private fun pushToCloudSilent() {
        val roomId = _syncRoomId.value.trim()
        if (roomId.isEmpty()) return
        generateBackupJson { jsonString ->
            if (jsonString.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        com.example.data.api.CloudSyncService.pushToCloud(roomId, jsonString)
                    } catch (e: Exception) {
                        // Silent push failure
                    }
                }
            }
        }
    }

    fun pushToCloud() {
        val roomId = _syncRoomId.value.trim()
        if (roomId.isEmpty()) {
            showToast("⚠️ ກະລຸນາຕັ້ງຄ່າ ແລະ ໃສ່ລະຫັດຫ້ອງອອນໄລກ່ອນ!")
            return
        }

        _isSyncing.value = true
        generateBackupJson { jsonString ->
            if (jsonString.isEmpty()) {
                _isSyncing.value = false
                return@generateBackupJson
            }
            viewModelScope.launch {
                try {
                    val success = com.example.data.api.CloudSyncService.pushToCloud(roomId, jsonString)
                    if (success) {
                        showToast("☁️ ສົ່ງຂໍ້ມູນຂຶ້ນຄລາວຫ້ອງ $roomId ສຳເລັດແລ້ວ!")
                    } else {
                        showToast("❌ ບໍ່ສາມາດສົ່ງຂໍ້ມູນຂຶ້ນຄລາວ. ກະລຸນາກວດສອບອິນເຕີເນັດ!")
                    }
                } catch (e: Exception) {
                    showToast("❌ ຜິດພາດ: ${e.localizedMessage}")
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    fun pullFromCloud(onResult: (Boolean) -> Unit = {}) {
        val roomId = _syncRoomId.value.trim()
        if (roomId.isEmpty()) {
            showToast("⚠️ ກະລຸນາຕັ້ງຄ່າ ແລະ ໃສ່ລະຫັດຫ້ອງອອນໄລກ່ອນ!")
            onResult(false)
            return
        }

        _isSyncing.value = true
        viewModelScope.launch {
            try {
                val jsonResult = com.example.data.api.CloudSyncService.pullFromCloud(roomId)
                if (jsonResult != null) {
                    if (jsonResult == "EMPTY") {
                        showToast("ℹ️ ຫ້ອງ $roomId ຍັງບໍ່ທັນມີຂໍ້ມູນເທິງຄລາວເທື່ອ!")
                        onResult(false)
                    } else {
                        restoreDatabase(jsonResult)
                        showToast("☁️ ດຶງຂໍ້ມູນຈາກຄລາວຫ້ອງ $roomId ສຳເລັດແລ້ວ!")
                        onResult(true)
                    }
                } else {
                    showToast("❌ ບໍ່ສາມາດດຶງຂໍ້ມູນ. ກະລຸນາກວດສອບອິນເຕີເນັດ ຫຼື ລະຫັດຫ້ອງ!")
                    onResult(false)
                }
            } catch (e: Exception) {
                showToast("❌ ຜິດພາດ: ${e.localizedMessage}")
                onResult(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun pullFromCloudSilent() {
        val roomId = _syncRoomId.value.trim()
        if (roomId.isEmpty()) return
        
        try {
            val jsonResult = com.example.data.api.CloudSyncService.pullFromCloud(roomId)
            if (jsonResult != null && jsonResult != "EMPTY") {
                restoreDatabase(jsonResult)
            }
        } catch (e: Exception) {
            // Siltent pull failure, ignored to avoid annoying popups
        }
    }
}
