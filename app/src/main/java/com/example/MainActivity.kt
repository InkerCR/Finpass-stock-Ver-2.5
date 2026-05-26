package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StockViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WarehouseTab
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WarehouseApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WarehouseApp(
    modifier: Modifier = Modifier,
    viewModel: StockViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsState(initial = "")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(toastMessage)
        }
    }

    Box(modifier = modifier) {
        if (currentUser == null) {
            LoginScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WarehouseDashboard(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

// ==========================================
// LOGIN VIEW SCREEN WITH REGISTER CAPABILITY
// ==========================================
@Composable
fun LoginScreen(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullname by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("manager") } // "admin", "manager", "viewer"

    val focusManager = LocalFocusManager.current

    val mainGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF2E1065))
    )

    Box(
        modifier = modifier
            .background(mainGradient)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("login_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // AIF Logo Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AIF",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AIF Stock FinPass",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF312E81)
                )

                Text(
                    text = if (isRegisterMode) "ລົງທະບຽນສ້າງບັນຊີຜູ້ໃຊ້ໃຫມ່" else "ລະບົບຈັດການສາງອັດສະລິຍະ",
                    fontSize = 13.sp,
                    color = if (isRegisterMode) Color(0xFF4F46E5) else Color.Gray,
                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isRegisterMode) {
                    // --- REGISTER FORM ---
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim().lowercase() },
                        label = { Text("ຊື່ເຂົ້າໃຊ້ (Username)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("reg_username_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fullname,
                        onValueChange = { fullname = it },
                        label = { Text("ຊື່ເຕັມ ແລະ ນາມສະກຸນ (Full Name)") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("reg_fullname_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("ລະຫັດຜ່ານ (Password)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reg_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("ຢືນຢັນລະຫັດຜ່ານ (Confirm Password)") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Selector Group
                    Text(
                        text = "ເລືອກບົດບາດ (Select Role):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("admin", "manager", "viewer").forEach { r ->
                            val isSel = role == r
                            val label = when(r) {
                                "admin" -> "Admin ລະບົບ"
                                "manager" -> "Manager ສາງ"
                                else -> "Viewer ທົ່ວໄປ"
                            }
                            Button(
                                onClick = { role = r },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                    contentColor = if (isSel) Color.White else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (username.isBlank() || password.isBlank() || fullname.isBlank()) {
                                viewModel.showToast("⚠️ ກະລຸນາປ້ອນຂໍ້ມູນໃຫ້ຄົບຖ້ວນ!")
                                return@Button
                            }
                            if (password != confirmPassword) {
                                viewModel.showToast("❌ ລະຫັດຜ່ານ ແລະ ຢືນຢັນລະຫັດຜ່ານບໍ່ກົງກັນ!")
                                return@Button
                            }
                            if (password.length < 4) {
                                viewModel.showToast("⚠️ ລະຫັດຜ່ານຕ້ອງມີຢ່າງໜ້ອຍ 4 ຕົວອັກສອນ!")
                                return@Button
                            }
                            val newUser = UserEntity(
                                username = username.trim().lowercase(),
                                password = password,
                                role = role,
                                fullname = fullname.trim()
                            )
                            viewModel.saveUser(newUser)
                            viewModel.login(newUser.username, newUser.password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ລົງທະບຽນ ແລະ ເຂົ້າໃຊ້",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { isRegisterMode = false }
                    ) {
                        Text("ມີບັນຊີຜູ້ໃຊ້ແລ້ວບໍ? ເຂົ້າສູ່ລະບົບ", color = Color(0xFF4F46E5), fontSize = 13.sp)
                    }

                } else {
                    // --- SIGN IN FORM ---
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("ຊື່ຜູ້ໃຊ້ (Username)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("username_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("ລະຫັດຜ່ານ (Password)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.login(username, password)
                        }),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(username, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ເຂົ້າສູ່ລະບົບ",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { isRegisterMode = true }
                    ) {
                        Text("ບໍ່ທັນມີບັນຊີບໍ? ລົງທະບຽນຜູ້ໃຊ້ໃໝ່", color = Color(0xFF0F766E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ລະບົບຊິ້ງຄລາວ & ຖານຂໍ້ມູນຄວາມປອດໄພ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ==========================================
// MAIN WAREHOUSE DASHBOARD SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDashboard(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeWarehouseId by viewModel.activeWarehouseId.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val usersList by viewModel.users.collectAsStateWithLifecycle()
    val productsList by viewModel.products.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()

    val activeWh = warehouses.find { it.id == activeWarehouseId }

    // Dialog flags
    var showUserMgmt by remember { mutableStateOf(false) }
    var showWarehouseMgmt by remember { mutableStateOf(false) }
    var showImportExcelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.background(Color(0xFFF1F5F9))
    ) {
        // App Header Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AIF", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AIF Stock FinPass Pro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "ລະບົບຈັດການສາງອັດສະລິຍະ",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            actions = {
                // Header action buttons depending on role
                val isAdmin = currentUser?.role == "admin"
                if (isAdmin) {
                    IconButton(onClick = { showWarehouseMgmt = true }) {
                        Icon(Icons.Default.Warehouse, contentDescription = "ຈັດການສາງ", tint = Color(0xFF0F766E))
                    }
                    IconButton(onClick = { showUserMgmt = true }) {
                        Icon(Icons.Default.People, contentDescription = "ຈັດການຜູ້ໃຊ້", tint = Color(0xFF7C3AED))
                    }
                }

                IconButton(onClick = { showImportExcelDialog = true }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "ນຳເຂົ້າຂໍ້ມູນ", tint = Color(0xFFD97706))
                }

                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Default.Logout, contentDescription = "ອອກຈາກລະບົບ", tint = Color(0xFFE11D48))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF1E293B)
            )
        )

        // Main scrollable host body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First item: Welcome User & Connection status banner
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF4F46E5))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentUser?.fullname ?: currentUser?.username ?: "Viewer",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "ບົດບາດ: ${currentUser?.role?.uppercase()}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Cloud status tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFFD1FAE5))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ລາຍງານຫຼ້າສຸດ",
                                    color = Color(0xFF065F46),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Second item: Analytical metrics dashboards (4 stat cards in a responsive grid equivalent)
            item {
                val totalProducts = productsList.size
                val totalValue = productsList.sumOf { it.stock * it.price }
                val totalInQty = transactionsList.filter { it.type == "in" }.sumOf { it.quantity }
                val totalOutQty = transactionsList.filter { it.type == "out" }.sumOf { it.quantity }

                val df = DecimalFormat("#,###")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "ສິນຄ້າທັງໝົດ",
                            value = totalProducts.toString(),
                            icon = Icons.Default.Inventory,
                            containerColor = Color(0xFFEEF2FF),
                            iconColor = Color(0xFF4F46E5),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "ມູນຄ່າສາງ",
                            value = "${df.format(totalValue)} K",
                            icon = Icons.Default.Savings,
                            containerColor = Color(0xFFECFDF5),
                            iconColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "ຍອດນຳເຂົ້າ",
                            value = df.format(totalInQty),
                            icon = Icons.Default.Download,
                            containerColor = Color(0xFFECFEFF),
                            iconColor = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "ຍອດສົ່ງອອກ",
                            value = df.format(totalOutQty),
                            icon = Icons.Default.Upload,
                            containerColor = Color(0xFFFFF7ED),
                            iconColor = Color(0xFFF97316),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Third item: Active Warehouse Selector Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF4F46E5))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ເລືອກສາງສະຖານທີ່",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        if (currentUser?.role == "admin") {
                            TextButton(onClick = { showWarehouseMgmt = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("ເພີ່ມສາງໃໝ່", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        warehouses.forEach { wh ->
                            val isSelected = wh.id == activeWarehouseId
                            Card(
                                onClick = { viewModel.setActiveWarehouse(wh.id) },
                                modifier = Modifier.width(180.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
                                ),
                                border = if (isSelected) BorderStroke(2.dp, Color(0xFF4F46E5)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = wh.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isSelected) Color(0xFF312E81) else Color(0xFF475569)
                                        )
                                        if (wh.isMain) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB000), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = wh.location,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fourth item: Tab navigations bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WarehouseTabChip(
                        tab = WarehouseTab.INVENTORY,
                        label = "ສິນຄ້າ & ເຄື່ອນໄຫວ",
                        icon = Icons.Default.Inventory,
                        selected = currentTab == WarehouseTab.INVENTORY,
                        onSelected = { viewModel.selectTab(it) }
                    )
                    WarehouseTabChip(
                        tab = WarehouseTab.TRANSACTIONS,
                        label = "ປະຫວັດການເຄື່ອນໄຫວ",
                        icon = Icons.Default.History,
                        selected = currentTab == WarehouseTab.TRANSACTIONS,
                        onSelected = { viewModel.selectTab(it) }
                    )
                    WarehouseTabChip(
                        tab = WarehouseTab.REPORTS,
                        label = "ລາຍງານ & ວິເຄາະ",
                        icon = Icons.Default.PieChart,
                        selected = currentTab == WarehouseTab.REPORTS,
                        onSelected = { viewModel.selectTab(it) }
                    )
                    WarehouseTabChip(
                        tab = WarehouseTab.ALERTS,
                        label = "ສິນຄ້າໃກ້ໝົດ",
                        icon = Icons.Default.NotificationsActive,
                        selected = currentTab == WarehouseTab.ALERTS,
                        onSelected = { viewModel.selectTab(it) }
                    )
                }
            }

            // Tabs Content injection
            when (currentTab) {
                WarehouseTab.INVENTORY -> {
                    item {
                        InventoryScreen(viewModel = viewModel, currentUser = currentUser)
                    }
                }
                WarehouseTab.TRANSACTIONS -> {
                    item {
                        TransactionsLogScreen(viewModel = viewModel)
                    }
                }
                WarehouseTab.REPORTS -> {
                    item {
                        ReportsScreen(viewModel = viewModel)
                    }
                }
                WarehouseTab.ALERTS -> {
                    item {
                        AlertsStockScreen(viewModel = viewModel)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal dialogs overlays
    if (showUserMgmt) {
        UserMgmtDialog(viewModel = viewModel, onDismiss = { showUserMgmt = false })
    }

    if (showWarehouseMgmt) {
        WarehouseMgmtDialog(viewModel = viewModel, onDismiss = { showWarehouseMgmt = false })
    }

    if (showImportExcelDialog) {
        BackupRestoreDialog(viewModel = viewModel, onDismiss = { showImportExcelDialog = false })
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun WarehouseTabChip(
    tab: WarehouseTab,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelected: (WarehouseTab) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onSelected(tab) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4F46E5),
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            containerColor = Color.White,
            labelColor = Color(0xFF475569)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color(0xFFE2E8F0),
            selectedBorderColor = Color(0xFF4F46E5),
            borderWidth = 1.dp
        ),
        shape = RoundedCornerShape(50.dp)
    )
}

// ==========================================
// SCREEN: INVENTORY WORKSPACE SCREEN
// ==========================================
@Composable
fun InventoryScreen(
    viewModel: StockViewModel,
    currentUser: UserEntity?
) {
    val activeWarehouseId by viewModel.activeWarehouseId.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val canModify = currentUser?.role == "admin" || currentUser?.role == "manager"

    // Local form states: Add/Edit products
    var pId by remember { mutableStateOf("") }
    var pName by remember { mutableStateOf("") }
    var pUnit by remember { mutableStateOf("ກໍ້") }
    var pCategory by remember { mutableStateOf("ວັດສະດຸ") }
    var pPrice by remember { mutableStateOf("") }
    var pCost by remember { mutableStateOf("") }
    var pStock by remember { mutableStateOf("") }
    var pMinStock by remember { mutableStateOf("10") }

    // Transaction Panel forms
    var transSelectedProductId by remember { mutableStateOf("") }
    var transType by remember { mutableStateOf("in") } // "in" or "out"
    var transQty by remember { mutableStateOf("1") }
    var transNote by remember { mutableStateOf("") }

    val df = DecimalFormat("#,###")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Form: Add and update products (Visible to admins and managers)
        if (canModify) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ເພີ່ມ / ແກ້ໄຂ ລາຍການສິນຄ້າ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    OutlinedTextField(
                        value = pId,
                        onValueChange = { pId = it.uppercase().trim() },
                        label = { Text("ລະຫັດສິນຄ້າ (ເຊັ່ນ: LB001)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_id_field")
                    )

                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("ຊື່ສິນຄ້າ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dropdown choice
                        var unitExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { unitExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("ໜ່ວຍ: $pUnit")
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                listOf("ກໍ້", "ເຄື່ອງ", "ອັນ", "ກ່ອງ", "ແກັດ", "ຊອງ").forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = { pUnit = it; unitExpanded = false }
                                    )
                                }
                            }
                        }

                        var catExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { catExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("ໝວດ: $pCategory")
                            }
                            DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                listOf("ວັດສະດຸ", "ອຸປະກອນ", "ອື່ນໆ").forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = { pCategory = it; catExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pPrice,
                            onValueChange = { pPrice = it },
                            label = { Text("ລາຄາຂາຍ (ກີບ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pCost,
                            onValueChange = { pCost = it },
                            label = { Text("ຕົ້ນທຶນ (ກີບ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = pMinStock,
                            onValueChange = { pMinStock = it },
                            label = { Text("ຈຸດສັ່ງໃໝ່ (Min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pStock,
                            onValueChange = { pStock = it },
                            label = { Text("ຄົງເຫຼືອເລີ່ມຕົ້ນ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pId.isBlank() || pName.isBlank()) {
                                    viewModel.showToast("⚠️ ກະລຸນາປ້ອນລະຫັດ ແລະ ຊື່ສິນຄ້າ!")
                                    return@Button
                                }
                                val p = ProductEntity(
                                    warehouseId = activeWarehouseId,
                                    id = pId,
                                    name = pName,
                                    unit = pUnit,
                                    category = pCategory,
                                    price = pPrice.toIntOrNull() ?: 0,
                                    cost = pCost.toIntOrNull() ?: 0,
                                    stock = pStock.toIntOrNull() ?: 0,
                                    minStock = pMinStock.toIntOrNull() ?: 10
                                )
                                viewModel.saveProduct(p)
                                // Reset form
                                pId = ""
                                pName = ""
                                pPrice = ""
                                pCost = ""
                                pStock = ""
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ບັນທຶກສິນຄ້າ", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                pId = ""
                                pName = ""
                                pPrice = ""
                                pCost = ""
                                pStock = ""
                                pMinStock = "10"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ຣີເຊັດ", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Form: Record Transaction Flow (Incoming or Outgoing stock launcher)
        if (canModify) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ເຄື່ອນໄຫວສິນຄ້າ (ທຸລະກຳ)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Choose Product Dialog selector
                    var showProductSelectDialog by remember { mutableStateOf(false) }
                    val currentProdName = filteredProducts.find { it.id == transSelectedProductId }?.name ?: "ເລືອກສິນຄ້າ"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showProductSelectDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("ສິນຄ້າ: $currentProdName (${transSelectedProductId.ifBlank { "ບໍ່ມີ" }})")
                        }
                    }

                    if (showProductSelectDialog) {
                        ProductSelectSearchDialog(
                            products = filteredProducts,
                            selectedId = transSelectedProductId,
                            onSelect = { id ->
                                transSelectedProductId = id
                                showProductSelectDialog = false
                            },
                            onDismiss = { showProductSelectDialog = false }
                        )
                    }

                    // Toggles: In or Out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val inSelected = transType == "in"
                        OutlinedButton(
                            onClick = { transType = "in" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (inSelected) Color(0xFFE6F4EA) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (inSelected) Color(0xFF10B981) else Color.LightGray
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ນຳເຂົ້າ", color = Color(0xFF0F5132), fontWeight = FontWeight.Bold)
                        }

                        val outSelected = transType == "out"
                        OutlinedButton(
                            onClick = { transType = "out" },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (outSelected) Color(0xFFFCE8E6) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (outSelected) Color(0xFFEF4444) else Color.LightGray
                            )
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ສົ່ງອອກ", color = Color(0xFF842029), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = transQty,
                        onValueChange = { transQty = it },
                        label = { Text("ຈຳນວນ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transNote,
                        onValueChange = { transNote = it },
                        label = { Text("ໝາຍເຫດ / ຜູ້ຮັບ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (transSelectedProductId.isBlank()) {
                                viewModel.showToast("⚠️ ກະລຸນາເລືອກສິນຄ້າ!")
                                return@Button
                            }
                            val qtyInt = transQty.toIntOrNull() ?: 0
                            if (qtyInt <= 0) {
                                viewModel.showToast("⚠️ ຈຳນວນຕ້ອງຫຼາຍກວ່າ 0!")
                                return@Button
                            }
                            // Call execute
                            viewModel.executeTransaction(
                                transSelectedProductId,
                                transType,
                                qtyInt,
                                transNote
                            )
                            // Reset transaction entries
                            transQty = "1"
                            transNote = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ບັນທຶກທຸລະກຳ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Display Products Lists with filtering
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ລາຍການສິນຄ້າຄົງເຫຼືອ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "ທັງໝົດ: ${filteredProducts.size} ລາຍການ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Search Bar field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("ຄົ້ນຫາສິນຄ້າ...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ບໍ່ມີລາຍການສິນຄ້າໃນຄັງນີ້", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredProducts.forEach { p ->
                            val isCriticalStock = p.stock <= p.minStock
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = if (isCriticalStock) BorderStroke(1.2.dp, Color(0xFFEF4444)) else null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = p.id,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF1F5F9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(p.category, fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = p.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "ລາຄາ: ${df.format(p.price)} Kip / ໜ່ວຍ: ${p.unit}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    // Valuation column
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isCriticalStock) Color(0xFFFEE2E2) else Color(0xFFD1FAE5))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${p.stock} ${p.unit}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCriticalStock) Color(0xFF991B1B) else Color(0xFF065F46)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ມູນຄ່າ: " + df.format(p.stock * p.price) + " K",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }

                                    // Action icons (visible to admins/managers)
                                    if (canModify) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            IconButton(
                                                onClick = {
                                                    // Quick load into form to edit
                                                    pId = p.id
                                                    pName = p.name
                                                    pUnit = p.unit
                                                    pCategory = p.category
                                                    pPrice = p.price.toString()
                                                    pCost = p.cost.toString()
                                                    pMinStock = p.minStock.toString()
                                                    pStock = p.stock.toString()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            IconButton(
                                                onClick = { viewModel.deleteProduct(p.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: TRANSACTIONS HISTORY SCREEN
// ==========================================
@Composable
fun TransactionsLogScreen(
    viewModel: StockViewModel
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val df = DecimalFormat("#,###")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ປະຫວັດການເຄື່ອນໄຫວສາງ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Update, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ບໍ່ມີປະຫວັດການເຄື່ອນໄຫວ", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                filteredTransactions.forEach { trans ->
                    val isIn = trans.type == "in"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isIn) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isIn) "ນຳເຂົ້າ" else "ສົ່ງອອກ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIn) Color(0xFF065F46) else Color(0xFF991B1B)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = trans.productId,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ຜູ້ຮັບ/ໝາຍເຫດ: " + trans.note.ifBlank { "ບໍ່ມີ" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ຜູ້ບັນທຶກ: ${trans.user} | " + sdf.format(Date(trans.timestamp)),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isIn) "+" else "-"}${trans.quantity}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isIn) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            if (trans.profit > 0) {
                                Text(
                                    text = "+" + df.format(trans.profit) + " K",
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: REPORTS & COOPERATIVE AI SCREEN
// ==========================================
@Composable
fun ReportsScreen(
    viewModel: StockViewModel
) {
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()
    val aiReportState by viewModel.aiReportState.collectAsStateWithLifecycle()

    val totalValue = filteredProducts.sumOf { it.stock * it.price }
    val totalProfit = transactionsList.filter { it.type == "out" }.sumOf { it.profit }

    val activeCount = filteredProducts.filter { it.stock > 0 }.size
    val emptyCount = filteredProducts.filter { it.stock <= 0 }.size

    val df = DecimalFormat("#,###")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Doughnut Canvas Chart representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ສະຖານະສິນຄ້າໃນສາງ (Chart)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas Ring Pie Chart representing Active vs Empty stock levels
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val total = (activeCount + emptyCount).toFloat()
                    val activeAngle = if (total > 0) (activeCount.toFloat() / total) * 360f else 360f
                    val emptyAngle = if (total > 0) (emptyCount.toFloat() / total) * 360f else 0f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing circles
                        if (total == 0f) {
                            drawCircle(color = Color.LightGray, style = Stroke(width = 30f))
                        } else {
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = activeAngle,
                                useCenter = false,
                                style = Stroke(width = 30f)
                            )
                            drawArc(
                                color = Color(0xFFEF4444),
                                startAngle = -90f + activeAngle,
                                sweepAngle = emptyAngle,
                                useCenter = false,
                                style = Stroke(width = 30f)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (total > 0) "${((activeCount / total) * 100).toInt()}%" else "0%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4F46E5)
                        )
                        Text("ມີສິນຄ້າ", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ມີສິນຄ້າ ($activeCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ໝົດສິນຄ້າ ($emptyCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary financial card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "ສະຫຼຸບມູນຄ່າ ແລະ ກຳໄລລວມ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ມູນຄ່າສາງລວມ (Valuation):", fontSize = 13.sp, color = Color.Gray)
                    Text("${df.format(totalValue)} LAK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Divider(color = Color(0xFFF1F5F9))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ກຳໄລລວມຈາກການສົ່ງອອກ (Profit):", fontSize = 13.sp, color = Color.Gray)
                    Text("${df.format(totalProfit)} LAK", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                }
            }
        }

        // Beautiful Gemini AI Consultation system
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Premium dark violet theme
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AIF Stock AI Analyst (Gemini)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ປະມວນຜົນສະຖານະສາງ, ແນະນຳການເຕີມສິນຄ້າ ແລະ ວິເຄາະຈຸດສັ່ງໃໝ່ອັດສະລິຍະຜ່ານ AI ດ້ວຍປຸ່ມດຽວ.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Button(
                    onClick = { viewModel.requestAiWarehouseAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ເລີ່ມການວິເຄາະດ້ວຍ AI", color = Color.White, fontWeight = FontWeight.Bold)
                }

                aiReportState?.let { state ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    when (state) {
                        is UiState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI ກຳລັງປະມວນຜົນ...", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        is UiState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "ຜົນວິເຄາະຈາກ AI:",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.data,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = "❌ Fail: ${state.message}",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: STOCK WARNS AND ALERTS SCREEN
// ==========================================
@Composable
fun AlertsStockScreen(
    viewModel: StockViewModel
) {
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val lowStockList = filteredProducts.filter { it.stock <= it.minStock }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ລາຍການສິນຄ້າໃກ້ໝົດສາງ / ຕ່ຳກວ່າເກນ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            if (lowStockList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ບໍ່ມີສິນຄ້າໃກ້ໝົດສາງ", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                lowStockList.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(BorderStroke(1.dp, Color(0xFFFFEDD5)), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF97316))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = p.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF7C2D12)
                                )
                                Text(
                                    text = "ລະຫັດ: ${p.id} | Min Limit: ${p.minStock}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFE0B2))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ຄົງເຫຼືອ: ${p.stock}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL: USER SECURITY CONFIG MODAL
// ==========================================
@Composable
fun UserMgmtDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    val usersList by viewModel.users.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullname by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("viewer") } // "admin", "manager", "viewer"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ຈັດການຄວາມປອດໄພ ແລະ ຜູ້ໃຊ້", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim().lowercase() },
                    label = { Text("ຊື່ເຂົ້າໃຊ້ (Username)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fullname,
                    onValueChange = { fullname = it },
                    label = { Text("ຊື່ເຕັມ (Fullname)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("ລະຫັດຜ່ານ (Password)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("admin", "manager", "viewer").forEach { r ->
                        val isSel = role == r
                        Button(
                            onClick = { role = r },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF4F46E5) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(r.uppercase(), fontSize = 10.sp, color = if (isSel) Color.White else Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            viewModel.showToast("⚠️ ກະລຸນາປ້ອນຊື່ ແລະ ລະຫັດຜ່ານ!")
                            return@Button
                        }
                        val u = UserEntity(username, password, role, fullname)
                        viewModel.saveUser(u)
                        username = ""
                        password = ""
                        fullname = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("ບັນທຶກຜູ້ໃຊ້", fontWeight = FontWeight.Bold)
                }

                Divider()

                Text("ລາຍຊື່ຜູ້ໃຊ້ງານລະບົບ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                Box(modifier = Modifier.height(140.dp).verticalScroll(rememberScrollState())) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        usersList.forEach { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = u.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "ບົດບາດ: " + u.role.uppercase(), fontSize = 10.sp, color = Color.Gray)
                                }
                                if (u.username != "admin") {
                                    IconButton(onClick = { viewModel.deleteUser(u.username) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ປິດ")
            }
        }
    )
}

// ==========================================
// MODAL: WAREHOUSE STRUCTURE STRUCTURE MODAL
// ==========================================
@Composable
fun WarehouseMgmtDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ຈັດການສ້າງ-ລຶບສາງ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ຊື່ສາງສິນຄ້າ (Warehouse Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("ສະຖານທີ່ (Location)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (name.isBlank() || location.isBlank()) {
                            viewModel.showToast("⚠️ ກະລຸນາປ້ອນຂໍ້ມູນໃຫ້ຄົບ!")
                            return@Button
                        }
                        val uniqueId = "wh_" + System.currentTimeMillis()
                        viewModel.saveWarehouse(uniqueId, name, location)
                        name = ""
                        location = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("ເພີ່ມສາງໃໝ່", fontWeight = FontWeight.Bold)
                }

                Divider()

                Text("ລາຍການສາງທັງໝົດ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                Box(modifier = Modifier.height(140.dp).verticalScroll(rememberScrollState())) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        warehouses.forEach { wh ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = wh.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "ສະຖານທີ່: " + wh.location, fontSize = 10.sp, color = Color.Gray)
                                }
                                if (!wh.isMain) {
                                    IconButton(onClick = { viewModel.deleteWarehouse(wh.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ປິດ")
            }
        }
    )
}

// ==========================================
// MODAL: BACKUP AND RESTORE JSON CONFIGS
// ==========================================
// MODAL: DATA HUB - CLOUD SYNC AND JSON BACKUP
// ==========================================
@Composable
fun BackupRestoreDialog(
    viewModel: StockViewModel,
    onDismiss: () -> Unit
) {
    var rawJson by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Cloud Sync, 1: Local JSON Backup
    
    val syncRoomId by viewModel.syncRoomId.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ສູນຄຸ້ມຄອງຂໍ້ມູນ & ລະບົບຄລາວ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dialog Navigation Tabs (Material-Styled Selection Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 0) Color.White else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ຊິ້ງຄລາວອອນໄລ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Gray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 1) Color.White else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFF0F766E) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ສຳຮອງໄຟລ໌ JSON",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color(0xFF0F766E) else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // TAB: CLOUD ONLINE SYNC WORKSPACE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (syncRoomId.isNotBlank()) Color(0xFFEEF2FF) else Color(0xFFFFF7ED)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (syncRoomId.isNotBlank()) Icons.Default.Cloud else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (syncRoomId.isNotBlank()) Color(0xFF4F46E5) else Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (syncRoomId.isNotBlank()) "🟢 ກຳລັງເຊື່ອມຕໍ່ຫ້ອງອອນໄລແລ້ວ" else "🔴 ໃຊ້ງານສະເພາະເຄື່ອງນີ້ (Offline)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (syncRoomId.isNotBlank()) Color(0xFF312E81) else Color(0xFF7C2D12)
                                )
                                Text(
                                    text = if (syncRoomId.isNotBlank()) "ລະຫັດຫ້ອງ: $syncRoomId" else "ຕັ້ງຄ່າ Room ID ເພື່ອໃຊ້ງານຮ່ວມກັນໄດ້ຫຼາຍຄົນ",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = syncRoomId,
                        onValueChange = { viewModel.setSyncRoomId(it) },
                        label = { Text("ລະຫັດຫ້ອງອອນໄລ (Online Room ID)") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF4F46E5)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                                    val randomCode = (1..6)
                                        .map { charset.random() }
                                        .joinToString("")
                                    viewModel.setSyncRoomId("ROOM_$randomCode")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "ສ້າງລະຫັດສຸ່ມ",
                                    tint = Color.Gray
                                )
                            }
                        },
                        placeholder = { Text("ຕົວຢ່າງ: stocks_lao_hq") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "💡 ຄຳແນະນຳ: ແຊຣ໌ລະຫັດຫ້ອງ Room ID ນີ້ໃຫ້ທີມງານ ຫຼື ມືຖືເຄື່ອງອື່ນໆ ປ້ອນໃສ່ຄືກັນ ເພື່ອເຂົ້າເຖິງ ແລະ ອັບເດດສາງສິນຄ້າອັນດຽວກັນແບບ Realtime!",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )

                    // Auto Sync Setting Toggle Switch
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ຊິ້ງຂໍ້ມູນອັດຕະໂນມັດ (Live Sync)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "ອັບໂຫຼດຂຶ້ນຄລາວທັນທີເມື່ອມີການ ບັນທຶກສິນຄ້າ, ສາງ ຫຼື ທຸລະກຳ",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                            )
                        }
                    }

                    // Push / Pull Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.pullFromCloud() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            enabled = !isSyncing && syncRoomId.isNotBlank()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("ດຶງຂໍ້ມູນຄລາວ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.pushToCloud() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            enabled = !isSyncing && syncRoomId.isNotBlank()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("ສົ່ງຂຶ້ນຄລາວ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isSyncing) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF4F46E5), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ກຳລັງປະມວນຜົນການຊິ້ງຂໍ້ມູນ...", fontSize = 11.sp, color = Color(0xFF4F46E5))
                        }
                    }

                } else {
                    // TAB: LOCAL JSON BACKUP & RESTORE
                    Text(
                        text = "ທ່ານສາມາດສຳຮອງຂໍ້ມູນທັງໝົດເປັນຮູບແບບ JSON ໄປເກັບໄວ້ ຫຼື ນຳ JSON ມາປ້ອນເພື່ອເຮັດການກູ້ຄືນຖານຂໍ້ມູນໄດ້ທັນທີ.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.generateBackupJson { backupText ->
                                    rawJson = backupText
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("ສ້າງ JSON ສຳຮອງ", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (rawJson.isBlank()) {
                                    viewModel.showToast("⚠️ ກະລຸນາປ້ອນ JSON ຂໍ້ມູນກູ້ຄືນ!")
                                    return@Button
                                }
                                viewModel.restoreDatabase(rawJson)
                                rawJson = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("ກູ້ຄືນຈາກ JSON", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = rawJson,
                        onValueChange = { rawJson = it },
                        placeholder = { Text("ປ້ອນ ຫຼື ຄັດລອກ JSON ຂໍ້ມູນສຳຮອງຂອງທ່ານ... (Copy paste text)", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ປິດໜ້າຕ່າງ")
            }
        }
    )
}

// ==========================================
// DIALOG: SEARCHABLE PRODUCT SELECT SEARCH DIALOG
// ==========================================
@Composable
fun ProductSelectSearchDialog(
    products: List<ProductEntity>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = products.filter {
        it.id.lowercase().contains(searchQuery.lowercase().trim()) ||
        it.name.lowercase().contains(searchQuery.lowercase().trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ຄົ້ນຫາ ແລະ ເລືອກສິນຄ້າ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("ຄົ້ນຫາຊື່ ຫຼື ລະຫັດສິນຄ້າ") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.height(300.dp)) {
                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ບໍ່ພົບລາຍການສິນຄ້າ", color = Color.Gray)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered) { prod ->
                                val isSelected = prod.id == selectedId
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onSelect(prod.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "ລະຫັດ: ${prod.id} | ໝວດ: ${prod.category}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Text(
                                            text = "ຄົງເຫຼືອ: ${prod.stock}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (prod.stock > prod.minStock) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ຍົກເລີກ")
            }
        }
    )
}
