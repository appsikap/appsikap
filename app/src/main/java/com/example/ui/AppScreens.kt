package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Admin
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import com.example.data.model.Hadiah
import com.example.data.model.PengajuanHadiah
import java.text.SimpleDateFormat
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import java.io.File
import java.util.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

class TransparentBackgroundTransformation : coil.transform.Transformation {
    override val cacheKey: String = "transparent_auto_bg_v3"

    override suspend fun transform(input: android.graphics.Bitmap, size: coil.size.Size): android.graphics.Bitmap {
        val width = input.width
        val height = input.height
        val output = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Ambil warna latar belakang dari piksel pojok kiri atas (0, 0)
        val cornerColor = pixels[0]
        val cA = (cornerColor shr 24) and 0xFF
        val cR = (cornerColor shr 16) and 0xFF
        val cG = (cornerColor shr 8) and 0xFF
        val cB = cornerColor and 0xFF
        
        val isCornerTransparent = cA < 50
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            if (isCornerTransparent) {
                // Jika piksel pojok sudah transparan, kita tetap menghapus piksel dominan putih sebagai fallback
                if (r > 240 && g > 240 && b > 240) {
                    pixels[i] = 0x00000000
                }
            } else {
                // Gunakan jarak Euclidean/Manhattan sederhana untuk mendeteksi warna yang sangat mirip dengan warna latar pojok
                val diffR = Math.abs(r - cR)
                val diffG = Math.abs(g - cG)
                val diffB = Math.abs(b - cB)
                
                // Jika warna sangat dekat dengan warna latar belakang pojok, jadikan transparan sepenuhnya.
                // Hanya hapus warna putih jika warna pojoknya memang putih/hampir putih.
                val isCornerWhite = (cR > 220 && cG > 220 && cB > 220)
                if ((diffR < 65 && diffG < 65 && diffB < 65) || (isCornerWhite && r > 242 && g > 242 && b > 242)) {
                    pixels[i] = 0x00000000
                }
            }
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Handle session status messages
    val statusMsg by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadThemeSettings(context)
        viewModel.loadGoogleSettings(context)
    }

    LaunchedEffect(currentScreen, isLoggedIn) {
        if (isLoggedIn) {
            viewModel.checkAndTriggerAutoSync(context)
        }
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.statusMessage.value = null
        }
    }

    if (currentScreen == MainViewModel.Screen.PORTAL_SISWA) {
        StudentPortalScreen(viewModel)
    } else if (!isLoggedIn) {
        LoginScreen(viewModel)
    } else {
        MainLayout(viewModel, currentScreen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: MainViewModel, currentScreen: MainViewModel.Screen) {
    val context = LocalContext.current
    val config by viewModel.pengaturan.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    Scaffold(
        topBar = {
            if (currentScreen != MainViewModel.Screen.DASHBOARD) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = config.nama_sekolah,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Admin: ${adminSession?.nama_admin ?: "Guru"} | TP: ${config.tahun_pelajaran}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.logout(context) }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar Log",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BluePrimary
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple(MainViewModel.Screen.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                    Triple(MainViewModel.Screen.SISWA, Icons.Default.People, "Murid"),
                    Triple(MainViewModel.Screen.INPUT_POIN, Icons.Default.LibraryAdd, "Input Poin"),
                    Triple(MainViewModel.Screen.RIWAYAT, Icons.Default.History, "Riwayat"),
                    Triple(MainViewModel.Screen.REKAP, Icons.Default.Assessment, "Rekap"),
                    Triple(MainViewModel.Screen.PENGATURAN, Icons.Default.Settings, "Pengaturan")
                )

                items.forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        selected = currentScreen == screen || 
                                   (screen == MainViewModel.Screen.PENGATURAN && 
                                    (currentScreen == MainViewModel.Screen.KATEGORI_BAIK || 
                                     currentScreen == MainViewModel.Screen.KATEGORI_BURUK)),
                        onClick = { viewModel.currentScreen.value = screen },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BlueSecondary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                MainViewModel.Screen.DASHBOARD -> DashboardScreen(viewModel)
                MainViewModel.Screen.SISWA -> SiswaManagerScreen(viewModel)
                MainViewModel.Screen.INPUT_POIN -> InputPoinScreen(viewModel)
                MainViewModel.Screen.RIWAYAT -> RiwayatPoinScreen(viewModel)
                MainViewModel.Screen.REKAP -> RekapScreen(viewModel)
                MainViewModel.Screen.PENGATURAN -> PengaturanScreen(viewModel)
                MainViewModel.Screen.KATEGORI_BAIK -> KategoriManagerScreen(viewModel, isPositive = true)
                MainViewModel.Screen.KATEGORI_BURUK -> KategoriManagerScreen(viewModel, isPositive = false)
                MainViewModel.Screen.HADIAH_ADMIN -> HadiahAdminScreen(viewModel)
                MainViewModel.Screen.HADIAH_REQUESTS -> HadiahRequestsScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. ADMIN LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val errorMsg by viewModel.loginError.collectAsState()

    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState()
    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val googleAccountName by viewModel.googleAccountName.collectAsState()
    var showGoogleConnectDialog by remember { mutableStateOf(false) }

    var googleNameInput by remember { mutableStateOf("Budhy") }
    var googleEmailInput by remember { mutableStateOf("budhy92@gmail.com") }
    val context = LocalContext.current

    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud, 
                        contentDescription = null, 
                        tint = Color(0xFF4285F4), 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sambungkan Akun Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gunakan Akun Google untuk mengaktifkan sinkronisasi otomatis Google Drive awan.",
                        fontSize = 11.sp, 
                        color = Color.Gray
                    )
                    
                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Nama Pengguna Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Alamat Email Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleEmailInput.isNotBlank() && googleNameInput.isNotBlank()) {
                            viewModel.connectGoogleAccount(context, googleEmailInput, googleNameInput)
                            showGoogleConnectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Hubungkan & Masuk")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConnectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF031B4E), // Deep Blue Primordial
                            Color(0xFF0C101B), // Cosmic Onyx
                            Color(0xFF4A020F)  // Subtle Crimson Accent Red
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val logoUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiSZzd8HFgvU5Nk3-5H6IM0X98Q2Of0MQDQxp8ZON5bAERd6elusg3ir--zNcaNFROzxywcIEuCfHDl6YWrzBTBOVlZxhyFB-pmqROr7cNpaC9oj8SbRn0tsgFz1vXzqxlQCqf3ZbGRvj-WVxPHeDn7TjMptG87hzvaN5mPI0psUUvcS16v1KR-Cawgltvw/s1254/ChatGPT%20Image%20May%2027,%202026,%2008_37_40%20PM.png"
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(logoUrl)
                                .transformations(TransparentBackgroundTransformation())
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Logo SIKAP",
                        modifier = Modifier.size(110.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SIKAP",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BluePrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistem Informasi Karakter dan Poin Murid",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username Admin") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Ubah visibilitas"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = RedCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.loginAdmin(context, username, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "MASUK ADMIN/GURU",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (isGoogleConnected) {
                                viewModel.loginAdminWithGoogle(context)
                            } else {
                                showGoogleConnectDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4285F4)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Google Icon",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isGoogleConnected) "Masuk dengan Google" else "Hubungkan Google Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = " DUNIA MURID ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.currentScreen.value = MainViewModel.Screen.PORTAL_SISWA },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MASUK PORTAL SISWA (SCAN QR)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DarkModeSlideSwitch(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit
) {
    // Width is 68.dp, height 32.dp, corner radius 16.dp
    // Circle thumb size 25.dp
    val trackWidth = 68.dp
    val trackHeight = 32.dp
    val thumbSize = 25.dp
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxOffset = with(density) { (trackWidth - thumbSize - 6.dp).toPx() } // padding 3.dp each side
    val thumbOffset by animateFloatAsState(
        targetValue = if (isDark) maxOffset else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f)
            )
            .border(
                1.dp, 
                if (isDark) Color(0xFF475569) else Color.White.copy(alpha = 0.5f), 
                RoundedCornerShape(16.dp)
            )
            .clickable { onToggle(!isDark) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Mode Icons as background hints
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Mode Terang",
                tint = if (!isDark) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = "Mode Gelap",
                tint = if (isDark) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(13.dp)
            )
        }

        // Sliding Thumb
        Box(
            modifier = Modifier
                .graphicsLayer(translationX = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF38BDF8) else Color(0xFFF59E0B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ==========================================
// 2. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val siswas by viewModel.allSiswa.collectAsState()
    val riwayats by viewModel.allRiwayat.collectAsState()
    val config by viewModel.pengaturan.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    // Realtime metrics
    val totalSiswa = siswas.size
    
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val firstDayOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayBaik = riwayats.filter { it.jenis_catatan == "Perbuatan Baik" && it.tanggal >= todayStart }.size
    val todayBuruk = riwayats.filter { it.jenis_catatan == "Pelanggaran" && it.tanggal >= todayStart }.size

    val monthBaik = riwayats.filter { it.jenis_catatan == "Perbuatan Baik" && it.tanggal >= firstDayOfMonth }.size
    val monthBuruk = riwayats.filter { it.jenis_catatan == "Pelanggaran" && it.tanggal >= firstDayOfMonth }.size

    val highestSiswa = siswas.maxByOrNull { it.saldo_poin }
    val lowestSiswa = siswas.minByOrNull { it.saldo_poin }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Full-bleed Header Summary block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val logoUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhpdrTOku1VnRYjRzo0QQQ0ufKfHyC7vI3RNg-7vAC7nfSpKE6aVL7P57v_bzf3bRnDlIXPnSRnAZvuaruIhn82v-S_WlMzQFwR9J6LzfO41zdPdzzGJC1l1DW0OyujOz0S-cUzeNZ0c58Ei9YaUZ6T6Vn1xjZ7ePBoMi_37NECWhiGF3hzk6822mIh9zoB/s320/SIKAP%20DENGAN%20DI%20BACKGROUND%20BIRU.png"
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(logoUrl)
                                        .transformations(TransparentBackgroundTransformation())
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Logo SIKAP",
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SIKAP",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "${config.nama_sekolah} • TA ${config.tahun_pelajaran}",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Profile badge avatar
                        val adminName = adminSession?.nama_admin ?: "Guru"
                        val initials = if (adminName.length >= 2) adminName.substring(0, 2).uppercase() else adminName.uppercase()
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isDarkMode by viewModel.isDarkMode.collectAsState()
                            DarkModeSlideSwitch(isDark = isDarkMode) { enabled ->
                                viewModel.toggleDarkMode(context, enabled)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BlueSecondary)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { viewModel.logout(context) }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Keluar Log",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Glassy Metrics summaries Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val totalPosPoin = riwayats.filter { it.jenis_catatan == "Perbuatan Baik" }.sumOf { it.nilai_poin }
                        val totalNegPoin = riwayats.filter { it.jenis_catatan == "Pelanggaran" }.sumOf { Math.abs(it.nilai_poin) }

                        GlassyStatCard(
                            label = "Murid",
                            value = "$totalSiswa",
                            modifier = Modifier.weight(1f)
                        )
                        GlassyStatCard(
                            label = "Poin (+)",
                            value = "+$totalPosPoin",
                            modifier = Modifier.weight(1f)
                        )
                        GlassyStatCard(
                            label = "Poin (-)",
                            value = "-$totalNegPoin",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Notification for student prize redemption requests
        item {
            val pendingRequests by viewModel.pendingPengajuan.collectAsState()
            if (pendingRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { viewModel.currentScreen.value = MainViewModel.Screen.HADIAH_REQUESTS },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Pemberitahuan",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ada Pengajuan Hadiah!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "Ada ${pendingRequests.size} murid ingin menukar poinnya dengan hadiah. Klik di sini untuk memproses.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Buka",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Quick Action cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        subtitle = "Input",
                        title = "Poin",
                        icon = Icons.Default.Add,
                        iconBgColor = Color(0xFFECFDF5), // Emerald 50
                        iconColor = Color(0xFF059669), // Emerald 600
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = MainViewModel.Screen.INPUT_POIN }
                    )
                    QuickActionCard(
                        subtitle = "Kelola",
                        title = "Murid",
                        icon = Icons.Default.People,
                        iconBgColor = Color(0xFFEFF6FF), // Blue 50
                        iconColor = Color(0xFF2563EB), // Blue 600
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = MainViewModel.Screen.SISWA }
                    )
                }
            }
        }

        // Critical status warning banner
        item {
            val pembinaanSiswaCount = siswas.filter { it.saldo_poin <= config.batas_pembinaan }.size
            if (pembinaanSiswaCount > 0) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)), // Rose 50
                        border = BorderStroke(1.dp, Color(0xFFFFE4E6)) // Rose 100
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E)), // Rose 500
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$pembinaanSiswaCount Murid Memerlukan Pembinaan Khusus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9F1239) // Rose 800
                                )
                                Text(
                                    text = "Poin berada di bawah batas minimum (${config.batas_pembinaan} poin).",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE11D48) // Rose 600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Highlight Activity List section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktivitas Terbaru",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)) // Blue 50
                                    .clickable { viewModel.currentScreen.value = MainViewModel.Screen.RIWAYAT }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Lihat Semua",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB) // Blue 600
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val recentActivities = riwayats.sortedByDescending { it.tanggal }.take(4)
                        if (recentActivities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada aktivitas tercatat",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentActivities.forEach { riwayat ->
                                    val siswa = siswas.firstOrNull { it.id_siswa == riwayat.id_siswa }
                                    val siswaName = siswa?.nama ?: "Murid Terhapus"
                                    val initials = if (siswaName.length >= 2) siswaName.substring(0, 2).uppercase() else siswaName.uppercase()
                                    
                                    val isPostive = riwayat.jenis_catatan == "Perbuatan Baik"
                                    val badgeBg = if (isPostive) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
                                    val badgeText = if (isPostive) Color(0xFF047857) else Color(0xFFBE123C)
                                    val changePrefix = if (isPostive) "+" else ""

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { siswa?.let { viewModel.selectedSiswaForDetail.value = it } }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9))
                                                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = siswaName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = riwayat.keterangan.ifEmpty { "Pemberian Poin" },
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$changePrefix${riwayat.nilai_poin}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Statistics Panel (2x2 Grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Aktivitas Hari Ini & Bulan Ini",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard(
                        title = "Seluruh Murid",
                        valStr = "$totalSiswa Anak",
                        icon = Icons.Default.Group,
                        tint = BlueSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    StatsCard(
                        title = "Aksi Baik Hari Ini",
                        valStr = "+$todayBaik Kegiatan",
                        icon = Icons.Default.ThumbUp,
                        tint = GreenJade,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsCard(
                        title = "Pelanggaran Hari Ini",
                        valStr = "$todayBuruk Kasus",
                        icon = Icons.Default.Warning,
                        tint = RedCrimson,
                        modifier = Modifier.weight(1f)
                    )

                    StatsCard(
                        title = "Aksi Baik Bulan Ini",
                        valStr = "$monthBaik Kali",
                        icon = Icons.Default.TrendingUp,
                        tint = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Student Rankings (Highest and Lowest)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Prestasi & Pembinaan Murid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Extreme Performer: High
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { highestSiswa?.let { viewModel.selectedSiswaForDetail.value = it } }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = WarningYellow,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Murid Poin Tertinggi", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(highestSiswa?.nama ?: "Belum ada data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Text(
                                text = "${highestSiswa?.saldo_poin ?: 100} Poin",
                                color = GreenJade,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                        // Extreme Performer: Low
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { lowestSiswa?.let { viewModel.selectedSiswaForDetail.value = it } }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RedCrimson,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Murid Poin Terendah", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(lowestSiswa?.nama ?: "Belum ada data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Text(
                                text = "${lowestSiswa?.saldo_poin ?: 100} Poin",
                                color = RedCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Custom drawn graphical statistics representation
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Grafik Karakter & Tren Kegiatan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Statistik kuantitas perbuatan baik vs pelanggaran",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val totalDeeds = todayBaik + monthBaik + 5 // buffer to prevent division by 0
                        val totalFaults = todayBuruk + monthBuruk + 2

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(BluePrimary.copy(alpha = 0.03f))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw nice background grid lines
                                val gridCount = 4
                                for (c in 0..gridCount) {
                                    val y = (size.height / gridCount) * c
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // 2 styled columns:
                                // Left Col = Good Deeds (Jade Green)
                                // Right Col = Violations (Red Crimson)

                                val maxVal = Math.max(totalDeeds, totalFaults).toFloat()
                                val barWidth = 60.dp.toPx()
                                
                                // Left Bar (Good deeds)
                                val leftBarHeight = (totalDeeds / maxVal) * (size.height * 0.8f)
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(GreenJade, GreenJade.copy(alpha = 0.5f))),
                                    topLeft = Offset(size.width * 0.25f - barWidth / 2, size.height - leftBarHeight),
                                    size = Size(barWidth, leftBarHeight),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )

                                // Right Bar (Violations)
                                val rightBarHeight = (totalFaults / maxVal) * (size.height * 0.8f)
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(RedCrimson, RedCrimson.copy(alpha = 0.5f))),
                                    topLeft = Offset(size.width * 0.75f - barWidth / 2, size.height - rightBarHeight),
                                    size = Size(barWidth, rightBarHeight),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text("Perbuatan Baik ($totalDeeds)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenJade)
                            Text("Pelanggaran ($totalFaults)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedCrimson)
                        }
                    }
                }
            }
        }
        
        // Extra bottom safety offset
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Detail student bottom-overlay popup triggering
    val selectedSiswa by viewModel.selectedSiswaForDetail.collectAsState()
    if (selectedSiswa != null) {
        Dialog(onDismissRequest = { viewModel.selectedSiswaForDetail.value = null }) {
            DetailSiswaLayout(viewModel, selectedSiswa!!) {
                viewModel.selectedSiswaForDetail.value = null
            }
        }
    }
}

@Composable
fun GlassyStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickActionCard(
    subtitle: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = subtitle,
                    color = Color(0xFF64748B), // Slate 500
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = title.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatsCard(title: String, valStr: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
            Text(valStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ==========================================
// 3. SEJARAH RIWAYAT POIN SISWA
// ==========================================
@Composable
fun RiwayatPoinScreen(viewModel: MainViewModel) {
    val riwayats by viewModel.allRiwayat.collectAsState()
    val siswas by viewModel.allSiswa.collectAsState()

    val siswaQuery by viewModel.riwayatSearchSiswa.collectAsState()
    val classFilter by viewModel.riwayatFilterKelas.collectAsState()
    val typeFilter by viewModel.riwayatFilterJenis.collectAsState()

    val context = LocalContext.current

    // Compute distinct options
    val classes = listOf("Semua") + siswas.map { it.kelas }.distinct().sorted()

    // Filter computation
    val filteredHistory = riwayats.filter { r ->
        val associatedSiswa = siswas.find { it.id_siswa == r.id_siswa }
        val nameMatch = if (siswaQuery.isNotBlank() && associatedSiswa != null) {
            associatedSiswa.nama.contains(siswaQuery, ignoreCase = true)
        } else true

        val classMatch = if (classFilter != "Semua" && associatedSiswa != null) {
            associatedSiswa.kelas == classFilter
        } else true

        val typeMatch = if (typeFilter != "Semua") {
            r.jenis_catatan == typeFilter
        } else true

        nameMatch && classMatch && typeMatch
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Daftar Riwayat Perilaku Murid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
        Text("Semua mutasi perubahan saldo poin terekam di bawah ini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))

        Spacer(modifier = Modifier.height(14.dp))

        // Compact filters panel card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search siswa text input
                OutlinedTextField(
                    value = siswaQuery,
                    onValueChange = { viewModel.riwayatSearchSiswa.value = it },
                    placeholder = { Text("Cari nama murid...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Kelas Filter
                    SimpleDropdown(
                        selected = classFilter,
                        options = classes,
                        onSelected = { viewModel.riwayatFilterKelas.value = it },
                        modifier = Modifier.weight(1f)
                    )

                    // Jenis Filter (Baik / Pelanggaran)
                    SimpleDropdown(
                        selected = typeFilter,
                        options = listOf("Semua", "Perbuatan Baik", "Pelanggaran"),
                        onSelected = { viewModel.riwayatFilterJenis.value = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada data mutasi yang cocok", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHistory) { r ->
                    val matchedSiswa = siswas.find { it.id_siswa == r.id_siswa }
                    var showConfirmDelete by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val badgeColor = if (r.jenis_catatan == "Perbuatan Baik") GreenJade else RedCrimson
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            r.jenis_catatan,
                                            fontSize = 9.sp,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = viewModel.formatTimestampDate(r.tanggal, "dd MMM yyyy"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = matchedSiswa?.nama ?: "Murid Terhapus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Kelas: ${matchedSiswa?.kelas ?: "-"} | Guru: ${r.guru_pencatat}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (r.keterangan.isNotBlank()) {
                                    Text(
                                        text = "\"${r.keterangan}\"",
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val textCol = if (r.nilai_poin > 0) GreenJade else RedCrimson
                                val prefix = if (r.nilai_poin > 0) "+" else ""
                                Text(
                                    text = "$prefix${r.nilai_poin}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                IconButton(
                                    onClick = { showConfirmDelete = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Hapus Riwayat",
                                        tint = RedCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showConfirmDelete) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDelete = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteRiwayatRecord(r)
                                    showConfirmDelete = false
                                }) {
                                    Text("Ya, Batalkan", color = RedCrimson)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmDelete = false }) {
                                    Text("Batal")
                                }
                            },
                            title = { Text("Konfirmasi Pembatalan") },
                            text = { Text("Apakah Anda yakin ingin membatalkan/menghapus catatan aktivitas murid ${matchedSiswa?.nama}? Saldo poin murid juga akan disesuaikan kembali.") }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. MANAJEMEN SISWA SCREEN
// ==========================================
@Composable
fun SiswaManagerScreen(viewModel: MainViewModel) {
    val siswas by viewModel.allSiswa.collectAsState()
    val searchVal by viewModel.siswaSearchQuery.collectAsState()
    val classFilter by viewModel.siswaFilterKelas.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var showSelectedStudentQr by remember { mutableStateOf<Siswa?>(null) }

    val context = LocalContext.current

    // 1. Format TXT Download Launcher
    val downloadFormatLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val templateStr = viewModel.getStudentFormatTemplate()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(templateStr.toByteArray(Charsets.UTF_8))
                }
                viewModel.statusMessage.value = "Format template berhasil diunduh!"
            } catch (e: Exception) {
                viewModel.statusMessage.value = "Gagal mengunduh format: ${e.message}"
            }
        }
    }

    // 2. Bulk Students Upload TXT Launcher
    val uploadStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    viewModel.importStudentsFromTxt(content)
                }
            } catch (e: Exception) {
                viewModel.statusMessage.value = "Kemungkinan file salah atau gagal dibaca."
            }
        }
    }

    // Dropdown list categories
    val kelasList = listOf("Semua") + siswas.map { it.kelas }.distinct().sorted()

    val filteredList = siswas.filter {
        val matchesSearch = it.nama.contains(searchVal, ignoreCase = true)
        val matchesClass = if (classFilter == "Semua") true else it.kelas == classFilter
        matchesSearch && matchesClass
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daftar Anggota Murid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Text("Database murid terdaftar, kelas, dan status pembinaan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }

            Button(
                onClick = {
                    viewModel.editingSiswa.value = null
                    showFormDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bulk import section card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Pendaftaran Massal Murid via File .TXT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            downloadFormatLauncher.launch("format_murid.txt")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unduh Format", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            uploadStudentsLauncher.launch("text/plain")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload File", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search & Filter Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchVal,
                    onValueChange = { viewModel.siswaSearchQuery.value = it },
                    placeholder = { Text("Cari nama murid...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )

                SimpleDropdown(
                    selected = classFilter,
                    options = kelasList,
                    onSelected = { viewModel.siswaFilterKelas.value = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GroupOff, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum mendeteksi data murid", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { s ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectedSiswaForDetail.value = s },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar representing gender & initials
                            val isMale = s.jenis_kelamin.equals("Laki-laki", ignoreCase = true)
                            val initials = if (s.nama.length >= 2) s.nama.substring(0, 2).uppercase() else s.nama.uppercase()
                            
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isMale) Color(0xFFEFF6FF) else Color(0xFFFDF2F8)) // Blue 50 or Pink 50
                                    .border(1.dp, if (isMale) Color(0xFFBFDBFE) else Color(0xFFFBCFE8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMale) Color(0xFF1E40AF) else Color(0xFF9D174D) // Blue 800 or Pink 800
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.nama,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kelas: ${s.kelas} | ${s.jenis_kelamin}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B) // Slate 500
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))

                                val status = viewModel.getCharacterStatus(s.saldo_poin)
                                val (badgeBg, badgeText) = when (status) {
                                    "Sangat Baik" -> Pair(Color(0xFFECFDF5), Color(0xFF047857)) // Emerald 50 / emerald 700
                                    "Baik" -> Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8)) // Blue 50 / blue 700
                                    "Perlu Pembinaan" -> Pair(Color(0xFFFFFBEB), Color(0xFFB45309)) // Amber 50 / amber 700
                                    "Peringatan" -> Pair(Color(0xFFFFF7ED), Color(0xFFC2410C)) // Orange 50 / orange 700
                                    else -> Pair(Color(0xFFFFF1F2), Color(0xFFBE123C)) // Rose 50 / rose 700
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = status,
                                        color = badgeText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Saldo Poin",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "${s.saldo_poin}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (s.saldo_poin >= 70) Color(0xFF059669) else Color(0xFFE11D48)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))

                                IconButton(
                                    onClick = { showSelectedStudentQr = s }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "Lihat QR Murid",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.editingSiswa.value = s
                                        showFormDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit data",
                                        tint = Color(0xFF3B82F6), // Blue 500
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus data",
                                        tint = Color(0xFFEF4444), // Red 500
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.removeSiswa(s)
                                    showDeleteConfirm = false
                                }) {
                                    Text("Hapus", color = RedCrimson)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Batal")
                                }
                            },
                            title = { Text("Konfirmasi Hapus") },
                            text = { Text("Apakah Anda yakin ingin menghapus data murid ${s.nama}? Seluruh riwayat poin atas nama murid ini juga akan terhapus secara permanen.") }
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to add/edit siswa
    val editingTarget by viewModel.editingSiswa.collectAsState()
    if (showFormDialog || editingTarget != null) {
        Dialog(onDismissRequest = {
            viewModel.editingSiswa.value = null
            showFormDialog = false
        }) {
            SiswaFormLayout(
                viewModel = viewModel,
                initialSiswa = editingTarget,
                onDismiss = {
                    viewModel.editingSiswa.value = null
                    showFormDialog = false
                }
            )
        }
    }

    // Detail student bottom-overlay popup triggering
    val selectedSiswa by viewModel.selectedSiswaForDetail.collectAsState()
    if (selectedSiswa != null) {
        Dialog(onDismissRequest = { viewModel.selectedSiswaForDetail.value = null }) {
            DetailSiswaLayout(viewModel, selectedSiswa!!) {
                viewModel.selectedSiswaForDetail.value = null
            }
        }
    }

    if (showSelectedStudentQr != null) {
        val s = showSelectedStudentQr!!
        AlertDialog(
            onDismissRequest = { showSelectedStudentQr = null },
            confirmButton = {
                TextButton(onClick = { showSelectedStudentQr = null }) {
                    Text("Tutup Kartu", fontWeight = FontWeight.Bold, color = BluePrimary)
                }
            },
            title = {
                Text(
                    text = "KARTU LOGIN QR SISWA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = s.nama,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Kelas: ${s.kelas} • ID: ${s.id_siswa}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val cellSize = size.width / 14f
                            
                            // Draw finder squares
                            fun drawFinderPattern(x: Float, y: Float) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(cellSize * 5f, cellSize * 5f)
                                )
                                drawRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(x + cellSize, y + cellSize),
                                    size = androidx.compose.ui.geometry.Size(cellSize * 3f, cellSize * 3f)
                                )
                                drawRect(
                                    color = Color.Black,
                                    topLeft = androidx.compose.ui.geometry.Offset(x + cellSize * 1.5f, y + cellSize * 1.5f),
                                    size = androidx.compose.ui.geometry.Size(cellSize * 2f, cellSize * 2f)
                                )
                            }
                            
                            drawFinderPattern(0f, 0f)
                            drawFinderPattern(cellSize * 9f, 0f)
                            drawFinderPattern(0f, cellSize * 9f)
                            
                            drawRect(
                                color = Color.Black,
                                topLeft = androidx.compose.ui.geometry.Offset(cellSize * 10f, cellSize * 10f),
                                size = androidx.compose.ui.geometry.Size(cellSize * 2f, cellSize * 2f)
                            )

                            val idHash = (s.id_siswa * 7919) xor 3241
                            val ran = java.util.Random(idHash.toLong())
                            
                            for (row in 0 until 14) {
                                for (col in 0 until 14) {
                                    val isTopLeftRegion = (row < 6 && col < 6)
                                    val isTopRightRegion = (row < 6 && col > 7)
                                    val isBottomLeftRegion = (row > 7 && col < 6)
                                    val isBottomRightMarker = (row in 10..11 && col in 10..11)
                                    
                                    if (!isTopLeftRegion && !isTopRightRegion && !isBottomLeftRegion && !isBottomRightMarker) {
                                        if (ran.nextFloat() > 0.45f) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "poinkarakter://siswa/${s.id_siswa}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Murid dapat memindai atau mengetik ID QR ini pada menu awal Portal Murid untuk mengecek poin, melihat riwayat kebaikan, dan mengajukan penukaran hadiah.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SiswaFormLayout(viewModel: MainViewModel, initialSiswa: Siswa?, onDismiss: () -> Unit) {
    var namaInput by remember { mutableStateOf(initialSiswa?.nama ?: "") }
    var kelasInput by remember { mutableStateOf(initialSiswa?.kelas ?: "") }
    var genderInput by remember { mutableStateOf(initialSiswa?.jenis_kelamin ?: "Laki-laki") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialSiswa != null) "Edit Data Murid" else "Tambah Murid Baru",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )

            OutlinedTextField(
                value = namaInput,
                onValueChange = { namaInput = it },
                label = { Text("Nama Murid Lengkap") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siswa_nama_input"),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = kelasInput,
                onValueChange = { kelasInput = it },
                label = { Text("Kelas (e.g. VII-A, X-IPA1)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siswa_kelas_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Column {
                Text("Jenis Kelamin", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { genderInput = "Laki-laki" }) {
                        RadioButton(selected = genderInput == "Laki-laki", onClick = { genderInput = "Laki-laki" })
                        Text("Laki-laki", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { genderInput = "Perempuan" }) {
                        RadioButton(selected = genderInput == "Perempuan", onClick = { genderInput = "Perempuan" })
                        Text("Perempuan", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.saveSiswa(namaInput, kelasInput, genderInput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

// ==========================================
// 5. INPUT POIN SCREEN (CATAT AKTIVITAS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputPoinScreen(viewModel: MainViewModel, isInPortal: Boolean = false, petugasName: String = "") {
    val siswas by viewModel.allSiswa.collectAsState()
    val kategoris by viewModel.allKategori.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    var selectedSiswaIdx by remember { mutableStateOf(-1) }
    var selectedCatIdx by remember { mutableStateOf(-1) }
    var jenisCatatan by remember { mutableStateOf("Perbuatan Baik") } // "Perbuatan Baik" / "Pelanggaran"

    var isCustomCategory by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var customCategoryPoint by remember { mutableStateOf("") }

    var guruPencatat by remember { mutableStateOf(if (isInPortal) petugasName else (adminSession?.nama_admin ?: "")) }
    var catatanTambahan by remember { mutableStateOf("") }
    var timestampChosen by remember { mutableStateOf(System.currentTimeMillis()) }

    var searchSiswaQuery by remember { mutableStateOf("") }
    var chooseSiswaDialogOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Compute dropdown list items filtered by "Perbuatan Baik" or "Pelanggaran"
    val filteredKategori = kategoris.filter { it.jenis_kategori == jenisCatatan }

    val selectedStudent: Siswa? = if (selectedSiswaIdx != -1) siswas.getOrNull(selectedSiswaIdx) else null
    val selectedCategory: KategoriAktivitas? = if (selectedCatIdx != -1) filteredKategori.getOrNull(selectedCatIdx) else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Catat Poin Karakter Murid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
            Text("Pencatatan real-time perubahan integritas perilaku murid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Section Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    
                    // 1. CHOOSE SISWA
                    Column {
                        Text("1. Pilih Murid", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueSecondary.copy(alpha = 0.08f))
                                .clickable { chooseSiswaDialogOpen = true }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedStudent != null) {
                                    Text(selectedStudent.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Kelas: ${selectedStudent.kelas} (Saldo: ${selectedStudent.saldo_poin} Poin)", fontSize = 11.sp, color = Color.Gray)
                                } else {
                                    Text("Klik untuk memilih murid...", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BluePrimary)
                        }
                    }

                    // 2. CHOOSE CATATAN TYPE
                    Column {
                        Text("2. Jenis Catatan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Perbuatan Baik Check
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        jenisCatatan = "Perbuatan Baik"
                                        selectedCatIdx = -1 // Reset category
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (jenisCatatan == "Perbuatan Baik") GreenJade.copy(alpha = 0.15f) else Color.Transparent,
                                ),
                                border = if (jenisCatatan == "Perbuatan Baik") CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(GreenJade, GreenJade))) else CardDefaults.outlinedCardBorder()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    RadioButton(selected = jenisCatatan == "Perbuatan Baik", onClick = {
                                        jenisCatatan = "Perbuatan Baik"
                                        selectedCatIdx = -1
                                    })
                                    Text("Poin Positif", color = if (jenisCatatan == "Perbuatan Baik") GreenJade else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Pelanggaran Check
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        jenisCatatan = "Pelanggaran"
                                        selectedCatIdx = -1 // Reset category
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (jenisCatatan == "Pelanggaran") RedCrimson.copy(alpha = 0.15f) else Color.Transparent,
                                ),
                                border = if (jenisCatatan == "Pelanggaran") CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(RedCrimson, RedCrimson))) else CardDefaults.outlinedCardBorder()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    RadioButton(selected = jenisCatatan == "Pelanggaran", onClick = {
                                        jenisCatatan = "Pelanggaran"
                                        selectedCatIdx = -1
                                    })
                                    Text("Pelanggaran", color = if (jenisCatatan == "Pelanggaran") RedCrimson else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 4. CATEGORY DROPDOWN
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.clickable { isCustomCategory = !isCustomCategory },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isCustomCategory,
                                    onCheckedChange = { isCustomCategory = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Tulis Kategori & Poin Kustom Sendiri (+/-)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BluePrimary
                                )
                            }

                            if (!isCustomCategory) {
                                TextButton(
                                    onClick = { 
                                        viewModel.currentScreen.value = if (jenisCatatan == "Perbuatan Baik") MainViewModel.Screen.KATEGORI_BAIK else MainViewModel.Screen.KATEGORI_BURUK 
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = BlueSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Kategori", fontSize = 11.sp, color = BlueSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isCustomCategory) {
                            OutlinedTextField(
                                value = customCategoryName,
                                onValueChange = { customCategoryName = it },
                                label = { Text("Nama/Keterangan Pelanggaran Lainnya") },
                                placeholder = { Text("Contoh: Mengganggu ketertiban, Merusak tanaman") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customCategoryPoint,
                                onValueChange = { customCategoryPoint = it },
                                label = { Text("Atur Sendiri Bobot Poin (+ atau -)") },
                                placeholder = { Text("Contoh: -15 untuk pelanggaran atau +10 untuk kebaikan") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            if (filteredKategori.isEmpty()) {
                                Text(
                                    "Belum tersedia kategori untuk jenis ini! Tambahkan kategori terlebih dahulu di menu Pengaturan.",
                                    color = RedCrimson,
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BlueSecondary.copy(alpha = 0.08f))
                                            .clickable { dropdownExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Icon(Icons.Default.Assignment, contentDescription = null, tint = BluePrimary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val displayStr = if (selectedCategory != null) {
                                                "${selectedCategory.nama_kategori} (${if (selectedCategory.nilai_poin > 0) "+" else ""}${selectedCategory.nilai_poin} Poin)"
                                            } else {
                                                "Klik untuk memilih kategori..."
                                            }
                                            Text(text = displayStr, fontSize = 13.sp)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BluePrimary)
                                    }

                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        filteredKategori.forEachIndexed { index, kat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${kat.nama_kategori} (${if (kat.nilai_poin > 0) "+" else ""}${kat.nilai_poin} Poin)",
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                onClick = {
                                                    selectedCatIdx = index
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. DATE PICKER
                    Column {
                        Text("4. Tanggal Aktivitas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueSecondary.copy(alpha = 0.08f))
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    calendar.timeInMillis = timestampChosen
                                    DatePickerDialog(
                                        context,
                                        { _, yr, mo, dy ->
                                            val c = Calendar.getInstance()
                                            c.set(yr, mo, dy)
                                            timestampChosen = c.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = viewModel.formatTimestampDate(timestampChosen, "EEEE, dd MMMM yyyy"),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 6. GURU PENCATAT & CATATAN TAMBAHAN
                    Column {
                        Text(if (isInPortal) "5. Identitas Petugas & Keterangan Lapangan" else "5. Identitas Guru & Keterangan Lapangan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = guruPencatat,
                            onValueChange = { if (!isInPortal) guruPencatat = it },
                            label = { Text(if (isInPortal) "Nama Petugas Pencatat" else "Nama Guru Pencatat") },
                            singleLine = true,
                            readOnly = isInPortal,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = catatanTambahan,
                            onValueChange = { catatanTambahan = it },
                            label = { Text("Keterangan Tambahan / Detail Kasus") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    var showSaveConfirm by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showSaveConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedStudent != null && (if (isCustomCategory) (customCategoryName.isNotBlank() && customCategoryPoint.toIntOrNull() != null) else selectedCategory != null)
                    ) {
                        Text("SIMPAN CATATAN POIN SISWA", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (showSaveConfirm) {
                        AlertDialog(
                            onDismissRequest = { showSaveConfirm = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (selectedStudent != null) {
                                        if (isCustomCategory) {
                                            val pVal = customCategoryPoint.toIntOrNull() ?: 0
                                            val inferredJenis = if (pVal >= 0) "Perbuatan Baik" else "Pelanggaran"
                                            viewModel.recordCatatanPoinKustom(
                                                idSiswa = selectedStudent.id_siswa,
                                                namaKategoriKustom = customCategoryName,
                                                jenisCatatanKustom = inferredJenis,
                                                nilaiPoinKustom = pVal,
                                                customDate = timestampChosen,
                                                pencatat = guruPencatat,
                                                catatanTambahan = catatanTambahan
                                            )
                                            customCategoryName = ""
                                            customCategoryPoint = ""
                                        } else if (selectedCategory != null) {
                                            viewModel.recordCatatanPoin(
                                                idSiswa = selectedStudent.id_siswa,
                                                idKategori = selectedCategory.id_kategori,
                                                customDate = timestampChosen,
                                                pencatat = guruPencatat,
                                                catatanTambahan = catatanTambahan
                                            )
                                        }
                                        selectedSiswaIdx = -1
                                        selectedCatIdx = -1
                                        catatanTambahan = ""
                                    }
                                    showSaveConfirm = false
                                }) {
                                    Text("Ya, Simpan")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveConfirm = false }) {
                                    Text("Batal")
                                }
                            },
                            title = { Text("Konfirmasi Simpulan") },
                            text = {
                                val descText = if (isCustomCategory) {
                                    "Kategori kustom '$customCategoryName' dengan poin $customCategoryPoint"
                                } else {
                                    "${selectedCategory?.nama_kategori} (${if ((selectedCategory?.nilai_poin ?: 0) > 0) "+" else ""}${selectedCategory?.nilai_poin} Poin)"
                                }
                                Text("Sistem akan menyimpan catatan $descText kepada murid '${selectedStudent?.nama}'. Apakah data sudah benar?")
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal popup to choose student cleanly with searchable index
    if (chooseSiswaDialogOpen) {
        Dialog(onDismissRequest = { chooseSiswaDialogOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pilih Murid Sasaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = searchSiswaQuery,
                        onValueChange = { searchSiswaQuery = it },
                        placeholder = { Text("Cari murid target...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredChoice = siswas.filter { it.nama.contains(searchSiswaQuery, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredChoice) { itemSiswa ->
                            val realIndex = siswas.indexOf(itemSiswa)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSiswaIdx = realIndex
                                        chooseSiswaDialogOpen = false
                                        searchSiswaQuery = ""
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = BlueSecondary.copy(alpha = 0.05f)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBox, null, tint = BluePrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itemSiswa.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kelas: ${itemSiswa.kelas}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("${itemSiswa.saldo_poin} Poin", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { chooseSiswaDialogOpen = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Tutup Batal")
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. DETAIL SISWA LAYOUT
// ==========================================
@Composable
fun DetailSiswaLayout(viewModel: MainViewModel, s: Siswa, onClose: () -> Unit) {
    val riwayats by viewModel.allRiwayat.collectAsState()
    val associatedRiwayat = riwayats.filter { it.id_siswa == s.id_siswa }

    val posPoints = associatedRiwayat.filter { it.jenis_catatan == "Perbuatan Baik" }.sumOf { it.nilai_poin }
    val negPoints = associatedRiwayat.filter { it.jenis_catatan == "Pelanggaran" }.sumOf { it.nilai_poin }
    val totalBaik = associatedRiwayat.filter { it.jenis_catatan == "Perbuatan Baik" }.size
    val totalBuruk = associatedRiwayat.filter { it.jenis_catatan == "Pelanggaran" }.size

    val status = viewModel.getCharacterStatus(s.saldo_poin)
    val badgeBg = when (status) {
        "Sangat Baik" -> GreenJade
        "Baik" -> BlueSecondary
        "Perlu Pembinaan" -> WarningYellow
        "Peringatan" -> AlertOrange
        else -> RedCrimson
    }

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Profile Bio
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Kelas: ${s.kelas} | ${s.jenis_kelamin}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Points Summary Balances Panel
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BluePrimary.copy(alpha = 0.05f))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Saldo Selesai", fontSize = 9.sp, color = Color.Gray)
                    Text("${s.saldo_poin}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(status, color = if (badgeBg == WarningYellow) Color.DarkGray else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenJade.copy(alpha = 0.05f))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Positif", fontSize = 9.sp, color = Color.Gray)
                    Text("+$posPoints", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenJade)
                    Text("$totalBaik Tindakan", fontSize = 9.sp, color = Color.Gray)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RedCrimson.copy(alpha = 0.05f))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Negatif", fontSize = 9.sp, color = Color.Gray)
                    Text("$negPoints", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedCrimson)
                    Text("$totalBuruk Kasus", fontSize = 9.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Riwayat Khusus Murid ($totalBaik Aksi, $totalBuruk Pelanggaran)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BluePrimary
                )

                // Export Individual PDF
                IconButton(onClick = {
                    viewModel.invokeNativePrintHTML(context, "Per Murid", s.nama, s)
                }) {
                    Icon(Icons.Default.Print, contentDescription = "Cetak Rekap", tint = BluePrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (associatedRiwayat.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentAlignment = Alignment.Center) {
                    Text("Murid bersih dari riwayat mutasi", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(associatedRiwayat) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val dateStr = viewModel.formatTimestampDate(r.tanggal, "dd MMM yyyy")
                                    Text(
                                        text = "$dateStr | Guru: ${r.guru_pencatat}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(r.jenis_catatan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (r.keterangan.isNotBlank()) {
                                        Text("\"${r.keterangan}\"", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }

                                val textColor = if (r.nilai_poin > 0) GreenJade else RedCrimson
                                val pre = if (r.nilai_poin > 0) "+" else ""
                                Text(
                                    text = "$pre${r.nilai_poin}",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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
// 7. REKAPAN POIN & EXPORT REPORTS
// ==========================================
@Composable
fun RekapScreen(viewModel: MainViewModel) {
    val siswas by viewModel.allSiswa.collectAsState()
    val riwayats by viewModel.allRiwayat.collectAsState()
    val config by viewModel.pengaturan.collectAsState()

    var activeTabRank by remember { mutableStateOf(0) } // 0 = Peringatan Tinggi (Rank), 1 = Butuh Perhatian (Terendah)
    val targetClass by viewModel.rekapFilterKelas.collectAsState()

    val context = LocalContext.current

    val classes = listOf("Semua") + siswas.map { it.kelas }.distinct().sorted()

    // Filter computation
    val filteredSiswas = siswas.filter {
        if (targetClass == "Semua") true else it.kelas == targetClass
    }

    // Rank list
    val sortedList = if (activeTabRank == 0) {
        filteredSiswas.sortedByDescending { it.saldo_poin }
    } else {
        filteredSiswas.sortedBy { it.saldo_poin }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rekapan Poin & Unduh Laporan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Text("Cetak PDF atau Excel data rekapitulasi sekolah", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }

            // EXPORT PDF NATIVE HTML
            IconButton(onClick = {
                viewModel.invokeNativePrintHTML(context, "Per Kelas", targetClass)
            }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = RedCrimson, modifier = Modifier.size(28.dp))
            }

            // EXPORT EXCEL (CSV FILE FORMAT)
            IconButton(onClick = {
                val csvFile = viewModel.generateCSVReport(context, "Per Kelas", targetClass)
                if (csvFile != null) {
                    Toast.makeText(context, "Excel CSV berhasil disiapkan di berkas lokal kases!", Toast.LENGTH_LONG).show()
                    // Share standard intent to let user copy file
                    try {
                        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", csvFile)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Rekapitulasi Poin Karakter")
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Unduh Excel Laporan via"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Gagal membagikan berkas CSV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Gagal menghasilkan dokumen Excel!", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.Download, contentDescription = "Export Excel", tint = GreenJade, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filters UI
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Filter Kelas:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                SimpleDropdown(
                    selected = targetClass,
                    options = classes,
                    onSelected = { viewModel.rekapFilterKelas.value = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sub tab ranking controller
        TabRow(
            selectedTabIndex = activeTabRank,
            containerColor = Color.Transparent,
            contentColor = BluePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabRank]),
                    color = BluePrimary
                )
            }
        ) {
            Tab(selected = activeTabRank == 0, onClick = { activeTabRank = 0 }) {
                Text("Ranking Tertinggi", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = activeTabRank == 1, onClick = { activeTabRank = 1 }) {
                Text("Kategori Pembinaan (Terendah)", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Nama Murid & Kelas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1.5f))
            Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("Total Poin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
        }

        if (sortedList.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada data murid", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedList) { s ->
                    val status = viewModel.getCharacterStatus(s.saldo_poin)
                    val statusCol = when (status) {
                        "Sangat Baik" -> GreenJade
                        "Baik" -> BlueSecondary
                        "Perlu Pembinaan" -> WarningYellow
                        "Peringatan" -> AlertOrange
                        else -> RedCrimson
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectedSiswaForDetail.value = s },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Kelas: ${s.kelas}", fontSize = 11.sp, color = Color.Gray)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusCol.copy(alpha = 0.15f))
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status,
                                    color = if (statusCol == WarningYellow) Color.DarkGray else statusCol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = "${s.saldo_poin} Poin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BluePrimary,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail student bottom-overlay popup triggering
    val selectedSiswa by viewModel.selectedSiswaForDetail.collectAsState()
    if (selectedSiswa != null) {
        Dialog(onDismissRequest = { viewModel.selectedSiswaForDetail.value = null }) {
            DetailSiswaLayout(viewModel, selectedSiswa!!) {
                viewModel.selectedSiswaForDetail.value = null
            }
        }
    }
}

// ==========================================
// 8. PENGATURAN SCREEN (APP CONFIGURATION)
// ==========================================
@Composable
fun PengaturanScreen(viewModel: MainViewModel) {
    val config by viewModel.pengaturan.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()

    val isGoogleConnected by viewModel.isGoogleConnected.collectAsState()
    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val googleAccountName by viewModel.googleAccountName.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
    val googleDriveFiles by viewModel.googleDriveFiles.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()

    var showGoogleConnectDialog by remember { mutableStateOf(false) }
    var googleNameInput by remember { mutableStateOf("Budhy") }
    var googleEmailInput by remember { mutableStateOf("budhy92@gmail.com") }
    
    var showDriveRestoreConfirm by remember { mutableStateOf<MainViewModel.DriveBackupFile?>(null) }
    val context = LocalContext.current

    var showAdminDialog by remember { mutableStateOf(false) }

    // Forms settings
    var schoolNameInput by remember { mutableStateOf(config.nama_sekolah) }
    var tpInput by remember { mutableStateOf(config.tahun_pelajaran) }
    var kadekInput by remember { mutableStateOf(config.nama_kepala_sekolah) }

    var sangatBaikLimit by remember { mutableStateOf(config.batas_sangat_baik.toString()) }
    var baikLimit by remember { mutableStateOf(config.batas_baik.toString()) }
    var pembinaanLimit by remember { mutableStateOf(config.batas_pembinaan.toString()) }
    var peringatanLimit by remember { mutableStateOf(config.batas_peringatan.toString()) }

    var poinAwalInput by remember { mutableStateOf(config.poin_awal_siswa.toString()) }
    var namaGuruInput by remember { mutableStateOf(config.nama_guru_ttd) }
    var peranGuruInput by remember { mutableStateOf(config.peran_guru_ttd) }

    // JSON file download (export) launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val jsonStr = viewModel.exportToJSONString()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                viewModel.statusMessage.value = "Berhasil mendownload backup data JSON!"
            } catch (e: Exception) {
                viewModel.statusMessage.value = "Gagal mendownload backup: ${e.message}"
            }
        }
    }

    // JSON file upload (restore) launcher
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonStr = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    viewModel.restoreFromJSONString(jsonStr)
                }
            } catch (e: Exception) {
                viewModel.statusMessage.value = "File tidak valid atau gagal dibaca!"
            }
        }
    }

    var backupTextOutput by remember { mutableStateOf("") }
    var restoreTextInput by remember { mutableStateOf("") }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Pengaturan Sistem Aplikasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
            Text("Ubah identitas sekolah, konfigurasi poin, profil keamanan, dan backup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }

        // Action Quick Access config tags
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Manajemen Kategori Induk", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Kelola nama-nama aktivitas dan nominal pembobotan nilai di sini", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.currentScreen.value = MainViewModel.Screen.KATEGORI_BAIK },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenJade),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Poin Positif", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.currentScreen.value = MainViewModel.Screen.KATEGORI_BURUK },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCrimson),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pelanggaran", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section: Fitur Penukaran Hadiah Siswa
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fitur Penukaran Hadiah Murid", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Akumulasi poin berbuat baik oleh murid dapat ditukarkan dengan item hadiah nyata demi menguatkan motivasi belajar.", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Aktifkan Fitur Tukar Hadiah", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (config.aktifkan_tukar_hadiah) "Murid bisa login via QR untuk mengajukan hadiah." else "Fitur ditutup sementara.",
                                fontSize = 11.sp,
                                color = if (config.aktifkan_tukar_hadiah) GreenJade else Color.Red
                            )
                        }
                        Switch(
                            checked = config.aktifkan_tukar_hadiah,
                            onCheckedChange = { viewModel.toggleAktifkanTukarHadiah(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.currentScreen.value = MainViewModel.Screen.HADIAH_ADMIN },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kelola Daftar Hadiah", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.currentScreen.value = MainViewModel.Screen.HADIAH_REQUESTS },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            val pendingRequests by viewModel.pendingPengajuan.collectAsState()
                            BadgedBox(
                                badge = {
                                    if (pendingRequests.isNotEmpty()) {
                                        Badge { Text(pendingRequests.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PendingActions, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pengajuan (${pendingRequests.size})", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section A1: Sekolah Identity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Identitas Sekolah & Kepala Sekolah", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)

                    OutlinedTextField(
                        value = schoolNameInput,
                        onValueChange = { schoolNameInput = it },
                        label = { Text("Nama Lembaga Sekolah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tpInput,
                        onValueChange = { tpInput = it },
                        label = { Text("Tahun Pelajaran") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = kadekInput,
                        onValueChange = { kadekInput = it },
                        label = { Text("Nama Kepala Sekolah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                namaSekolah = schoolNameInput,
                                tahunPelajaran = tpInput,
                                namaKepalaSekolah = kadekInput,
                                batasSangatBaik = config.batas_sangat_baik,
                                batasBaik = config.batas_baik,
                                batasPembinaan = config.batas_pembinaan,
                                batasPeringatan = config.batas_peringatan,
                                poinAwalSiswa = config.poin_awal_siswa,
                                namaGuruTtd = config.nama_guru_ttd,
                                peranGuruTtd = config.peran_guru_ttd
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMPAN IDENTITAS SEKOLAH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section A2: Konfigurasi Guru Tanda Tangan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Konfigurasi Guru untuk Tanda Tangan Rekap", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Atur peran administrasi Anda sebagai pembuat penandatangan laporan (Guru Kelas / Wali Kelas / Guru Mapel)", fontSize = 11.sp, color = Color.Gray)

                    var rolesDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = peranGuruInput,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Jabatan / Peran Tanda Tangan") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { rolesDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = rolesDropdownExpanded,
                            onDismissRequest = { rolesDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            listOf("Guru Kelas", "Wali Kelas", "Guru Mapel").forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        peranGuruInput = role
                                        rolesDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = namaGuruInput,
                        onValueChange = { namaGuruInput = it },
                        label = { Text("Nama Guru Penandatangan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                namaSekolah = config.nama_sekolah,
                                tahunPelajaran = config.tahun_pelajaran,
                                namaKepalaSekolah = config.nama_kepala_sekolah,
                                batasSangatBaik = config.batas_sangat_baik,
                                batasBaik = config.batas_baik,
                                batasPembinaan = config.batas_pembinaan,
                                batasPeringatan = config.batas_peringatan,
                                poinAwalSiswa = config.poin_awal_siswa,
                                namaGuruTtd = namaGuruInput,
                                peranGuruTtd = peranGuruInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMPAN KONFIGURASI GURU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section A3: Poin Standar Pendaftaran Murid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pengaturan Poin Standar Pendaftaran Murid", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)

                    OutlinedTextField(
                        value = poinAwalInput,
                        onValueChange = { poinAwalInput = it },
                        label = { Text("Poin Awal Murid Baru") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                namaSekolah = config.nama_sekolah,
                                tahunPelajaran = config.tahun_pelajaran,
                                namaKepalaSekolah = config.nama_kepala_sekolah,
                                batasSangatBaik = config.batas_sangat_baik,
                                batasBaik = config.batas_baik,
                                batasPembinaan = config.batas_pembinaan,
                                batasPeringatan = config.batas_peringatan,
                                poinAwalSiswa = poinAwalInput.toIntOrNull() ?: 100,
                                namaGuruTtd = config.nama_guru_ttd,
                                peranGuruTtd = config.peran_guru_ttd
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMPAN POIN STANDAR PENDAFTARAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section B: Limits Status Point Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Konfigurasi Parameter Batas Karakter", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Sistem mengklasifikasi status murid berdasarkan saldo poin terakhir", fontSize = 11.sp, color = Color.Gray)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sangatBaikLimit,
                            onValueChange = { sangatBaikLimit = it },
                            label = { Text("Sangat Baik (>=)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = baikLimit,
                            onValueChange = { baikLimit = it },
                            label = { Text("Baik (>=)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pembinaanLimit,
                            onValueChange = { pembinaanLimit = it },
                            label = { Text("Pembinaan (>=)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = peringatanLimit,
                            onValueChange = { peringatanLimit = it },
                            label = { Text("Peringatan (>=)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val sbL = sangatBaikLimit.toIntOrNull() ?: 100
                            val bL = baikLimit.toIntOrNull() ?: 70
                            val pL = pembinaanLimit.toIntOrNull() ?: 40
                            val prL = peringatanLimit.toIntOrNull() ?: 0

                            viewModel.updateSettings(
                                namaSekolah = schoolNameInput,
                                tahunPelajaran = tpInput,
                                namaKepalaSekolah = kadekInput,
                                batasSangatBaik = sbL,
                                batasBaik = bL,
                                batasPembinaan = pL,
                                batasPeringatan = prL,
                                poinAwalSiswa = poinAwalInput.toIntOrNull() ?: 100,
                                namaGuruTtd = namaGuruInput,
                                peranGuruTtd = peranGuruInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMPAN KONFIGURASI PARAMETER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section C: Backup & Restore (JSON / Local persistence dump)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cadangan & Pemulihan Berkas JSON (Backup/Restore)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Ekspor seluruh database menjadi satu berkas JSON yang terdownload lengkap, atau upload berkas JSON untuk memulihkan seluruh aktivitas.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                exportJsonLauncher.launch("cadangan_poin_karakter_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download JSON", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                importJsonLauncher.launch("application/json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload / Restore", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Integrasi Awan Google Drive
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud, 
                            contentDescription = null, 
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Integrasi Awan Google Drive", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp, 
                            color = BluePrimary
                        )
                    }
                    Text(
                        text = "Sinkronisasikan database sekolah, data murid, poin & riwayat karakter secara otomatis ke file Google Drive.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (!isGoogleConnected) {
                        Button(
                            onClick = { showGoogleConnectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hubungkan Akun Google Drive", fontSize = 12.sp)
                        }
                    } else {
                        // User Profile Account Info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleAccountName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF),
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(googleAccountName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(googleAccountEmail, fontSize = 11.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.disconnectGoogleAccount(context) }) {
                                Icon(Icons.Default.LinkOff, "Putuskan Akun", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Auto-Sync Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sinkronisasi Otomatis", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Secara otomatis mencadangkan data ke Google Drive setiap 24 jam.", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = {
                                    viewModel.isAutoSyncEnabled.value = it
                                    viewModel.saveGoogleSettings(context)
                                }
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.syncDatabaseToGoogleDrive(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Sekarang", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "Sinkronisasi Terakhir: ${if (lastSyncedTime == 0L) "Belum pernah disinkronkan" else viewModel.formatTimestampDate(lastSyncedTime, "dd/MM/yyyy HH:mm:ss")}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Backup History Listing from GDrive
                        Text("Daftar File Backup di Drive", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)

                        if (googleDriveFiles.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat file backup di Google Drive Anda. Lakukan 'Backup Sekarang' untuk membuat.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                googleDriveFiles.forEach { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.Medium, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${file.formattedDate} | ${file.size}", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = { showDriveRestoreConfirm = file },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.SettingsBackupRestore, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore", fontSize = 10.sp)
                                            }
                                            IconButton(onClick = { viewModel.deleteBackupFromGoogleDrive(context, file) }) {
                                                Icon(Icons.Default.Delete, "Hapus File", tint = RedCrimson, modifier = Modifier.size(16.dp))
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

        // Section D: Security credentials profile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Autentikasi & Keamanan Kredensial", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text("Ubah username password login guru piket administrator", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showAdminDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Sandi & Profil Admin", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section E: Atur Murid Petugas Khusus
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentInd, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Murid Petugas Khusus", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    }
                    Text("Tunjuk murid tertentu dengan tugas khusus untuk mencatat perbuatan baik dan pelanggaran murid langsung dari Portal Murid.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(4.dp))

                    val allSiswaList by viewModel.allSiswa.collectAsState()
                    val petugasList = allSiswaList.filter { it.is_petugas }

                    if (allSiswaList.isEmpty()) {
                        Text("Belum ada data murid. Silakan tambahkan murid terlebih dahulu.", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                    } else {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        var selectedSiswaForPetugas by remember { mutableStateOf<Siswa?>(null) }
                        var isPetugasChecked by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedSiswaForPetugas?.nama ?: "Pilih Murid...",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Tunjuk Murid Sebagai Petugas") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f).height(240.dp)
                            ) {
                                allSiswaList.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.nama} (${s.kelas}) - ${if (s.is_petugas) "Petugas" else "Biasa"}") },
                                        onClick = {
                                            selectedSiswaForPetugas = s
                                            isPetugasChecked = s.is_petugas
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedSiswaForPetugas == null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { },
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SIMPAN STATUS PETUGAS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        selectedSiswaForPetugas?.let { s ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Ubah Hak Akses Petugas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(s.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Kelas: ${s.kelas}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(if (isPetugasChecked) "Petugas Khusus" else "Murid Biasa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isPetugasChecked) GreenJade else Color.Gray)
                                            Switch(
                                                checked = isPetugasChecked,
                                                onCheckedChange = { isPetugasChecked = it }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            viewModel.appointSiswaPetugas(s.id_siswa, isPetugasChecked)
                                            selectedSiswaForPetugas = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SIMPAN STATUS PETUGAS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (petugasList.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))
                            Text("Daftar Murid Bertugas:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = BlueSecondary.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, BlueSecondary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    petugasList.forEach { p ->
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${p.nama} (${p.kelas})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { viewModel.appointSiswaPetugas(p.id_siswa, false) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp))
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

    if (showAdminDialog) {
        Dialog(onDismissRequest = { showAdminDialog = false }) {
            AdminProfileFormLayout(viewModel = viewModel, currentAdmin = adminSession) {
                showAdminDialog = false
            }
        }
    }

    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, null, tint = Color(0xFF4285F4), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hubungkan Akun Google Drive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hubungkan ke akun Google Drive Anda untuk pencadangan aman (Backup/Restore).", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Nama Akun Google") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Alamat Email Akun") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleEmailInput.isNotBlank() && googleNameInput.isNotBlank()) {
                            viewModel.connectGoogleAccount(context, googleEmailInput, googleNameInput)
                            showGoogleConnectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Hubungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConnectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDriveRestoreConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirm = null },
            confirmButton = {
                TextButton(onClick = {
                    showDriveRestoreConfirm?.let {
                        viewModel.restoreFromGoogleDrive(context, it)
                    }
                    showDriveRestoreConfirm = null
                }) {
                    Text("Ya, Pulihkan Sekarang", color = RedCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = null }) {
                    Text("Batal")
                }
            },
            title = { Text("Yakin Melakukan Pemulihan?") },
            text = { Text("Tindakan ini akan mengunduh file cadangan dari Google Drive dan menyinkronkannya kembali ke database lokal Anda, menggantikan data saat ini. Kegiatan ini tidak dapat dibatalkan.") }
        )
    }

    if (syncProgress != null) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                    Text(
                        text = syncProgress ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AdminProfileFormLayout(viewModel: MainViewModel, currentAdmin: Admin?, onDismiss: () -> Unit) {
    var namaInput by remember { mutableStateOf(currentAdmin?.nama_admin ?: "") }
    var userLoginInput by remember { mutableStateOf(currentAdmin?.username ?: "") }
    var passLoginInput by remember { mutableStateOf(currentAdmin?.password ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perbarui Kredensial Admin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BluePrimary)

            OutlinedTextField(
                value = namaInput,
                onValueChange = { namaInput = it },
                label = { Text("Display Nama Pengurus") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = userLoginInput,
                onValueChange = { userLoginInput = it },
                label = { Text("Username Login Baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passLoginInput,
                onValueChange = { passLoginInput = it },
                label = { Text("Sandi Password Baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.updateAdminProfile(namaInput, userLoginInput, passLoginInput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Perbarui")
                }
            }
        }
    }
}

// ==========================================
// 9. KATEGORI MANAGER SCREEN (EDIT/ADD DEEP PARAMETERS)
// ==========================================
@Composable
fun KategoriManagerScreen(viewModel: MainViewModel, isPositive: Boolean) {
    var activeTabIsPositive by remember { mutableStateOf(isPositive) }
    val kategoris by viewModel.allKategori.collectAsState()
    val associatedKategori = kategoris.filter { 
        if (activeTabIsPositive) it.jenis_kategori == "Perbuatan Baik" else it.jenis_kategori == "Pelanggaran"
    }

    var showForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = MainViewModel.Screen.PENGATURAN }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kelola & Edit Kategori Aktivitas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text("Atur jenis perbuatan terpuji dan bobot sanksi pelanggaran", fontSize = 11.sp, color = Color.Gray)
            }

            Button(
                onClick = {
                    viewModel.editingKategori.value = null
                    showForm = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeTabIsPositive) GreenJade else RedCrimson)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (activeTabIsPositive) "+ Positif" else "+ Pelanggaran", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TAB SWITCHER: POIN POSITIF VS PELANGGARAN
        TabRow(
            selectedTabIndex = if (activeTabIsPositive) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BluePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (activeTabIsPositive) 0 else 1]),
                    color = if (activeTabIsPositive) GreenJade else RedCrimson
                )
            }
        ) {
            Tab(
                selected = activeTabIsPositive,
                onClick = { activeTabIsPositive = true },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ThumbUp, 
                            contentDescription = null, 
                            tint = if (activeTabIsPositive) GreenJade else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Poin Positif (${kategoris.count { it.jenis_kategori == "Perbuatan Baik" }})", 
                            fontWeight = FontWeight.Bold, 
                            color = if (activeTabIsPositive) GreenJade else Color.Gray
                        )
                    }
                }
            )
            Tab(
                selected = !activeTabIsPositive,
                onClick = { activeTabIsPositive = false },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = if (!activeTabIsPositive) RedCrimson else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Pelanggaran (${kategoris.count { it.jenis_kategori == "Pelanggaran" }})", 
                            fontWeight = FontWeight.Bold, 
                            color = if (!activeTabIsPositive) RedCrimson else Color.Gray
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (associatedKategori.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (activeTabIsPositive) "Belum ada kategori perbuatan baik" else "Belum ada kategori pelanggaran",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(associatedKategori) { k ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(k.nama_kategori, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (k.keterangan.isNotBlank()) {
                                    Text(k.keterangan, fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val textCol = if (k.jenis_kategori == "Perbuatan Baik") GreenJade else RedCrimson
                                val prefix = if (k.jenis_kategori == "Perbuatan Baik") "+" else ""
                                Text(
                                    text = "$prefix${k.nilai_poin} Poin",
                                    color = textCol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                IconButton(onClick = {
                                    viewModel.editingKategori.value = k
                                    showForm = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Kategori", tint = BlueSecondary)
                                }

                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Kategori", tint = RedCrimson)
                                }
                            }
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.removeKategori(k)
                                    showDeleteConfirm = false
                                }) {
                                    Text("Ya, Hapus", color = RedCrimson)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Batal")
                                }
                            },
                            title = { Text("Konfirmasi Hapus") },
                            text = { Text("Apakah Anda yakin ingin menghapus kategori '${k.nama_kategori}'? Perubahan ini akan mempengaruhi opsi input poin ke depan.") }
                        )
                    }
                }
            }
        }
    }

    val editingTarget by viewModel.editingKategori.collectAsState()
    if (showForm || editingTarget != null) {
        Dialog(onDismissRequest = {
            viewModel.editingKategori.value = null
            showForm = false
        }) {
            KategoriFormLayout(
                viewModel = viewModel,
                isPositive = activeTabIsPositive,
                initialKategori = editingTarget,
                onDismiss = {
                    viewModel.editingKategori.value = null
                    showForm = false
                }
            )
        }
    }
}

@Composable
fun KategoriFormLayout(viewModel: MainViewModel, isPositive: Boolean, initialKategori: KategoriAktivitas?, onDismiss: () -> Unit) {
    var namaInput by remember { mutableStateOf(initialKategori?.nama_kategori ?: "") }
    var jenisCategoryInput by remember { 
        mutableStateOf(initialKategori?.jenis_kategori ?: if (isPositive) "Perbuatan Baik" else "Pelanggaran") 
    }
    // Store as absolute positive integer string
    var pointInput by remember { mutableStateOf(if (initialKategori != null) Math.abs(initialKategori.nilai_poin).toString() else "") }
    var ketInput by remember { mutableStateOf(initialKategori?.keterangan ?: "") }

    val isBaikChoice = jenisCategoryInput == "Perbuatan Baik"
    val parsedVal = pointInput.toIntOrNull() ?: 0
    val previewVal = if (isBaikChoice) parsedVal else -parsedVal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialKategori != null) "Edit Kategori Aktivitas" else "Tambah Kategori Aktivitas Baru",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBaikChoice) GreenJade else RedCrimson
            )

            // Selector Jenis Kategori
            Column {
                Text("Jenis Kategori Aktivitas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isBaikChoice,
                        onClick = { jenisCategoryInput = "Perbuatan Baik" },
                        label = { Text("🟢 Poin Positif") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenJade.copy(alpha = 0.2f),
                            selectedLabelColor = GreenJade
                        )
                    )
                    FilterChip(
                        selected = !isBaikChoice,
                        onClick = { jenisCategoryInput = "Pelanggaran" },
                        label = { Text("🔴 Pelanggaran") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedCrimson.copy(alpha = 0.2f),
                            selectedLabelColor = RedCrimson
                        )
                    )
                }
            }

            OutlinedTextField(
                value = namaInput,
                onValueChange = { namaInput = it },
                label = { Text("Nama Kategori (e.g. Piket Kebersihan / Terlambat)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pointInput,
                onValueChange = { pointInput = it },
                label = { Text("Bobot Poin (Angka Positif)") },
                placeholder = { Text("Contoh: 10, 15, 25") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = "Preview Efektif: ${if (previewVal > 0) "+$previewVal" else "$previewVal"} Poin",
                        color = if (isBaikChoice) GreenJade else RedCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            OutlinedTextField(
                value = ketInput,
                onValueChange = { ketInput = it },
                label = { Text("Deskripsi Catatan Ringkas (Opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.saveKategori(
                            nama = namaInput,
                            jenis = jenisCategoryInput,
                            nilaiStr = pointInput,
                            keterangan = ketInput
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isBaikChoice) GreenJade else RedCrimson)
                ) {
                    Text("Simpan Changes")
                }
            }
        }
    }
}

// ==========================================
// 10. DROP DOWN SELECTOR COMPONENT (ACCESSIBILITY TABS)
// ==========================================
@Composable
fun SimpleDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BlueSecondary.copy(alpha = 0.08f))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(
                text = selected,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown expand")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 13.sp) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// 11. HADIAH ADMIN MANAGEMENT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadiahAdminScreen(viewModel: MainViewModel) {
    val hadiahs by viewModel.allHadiah.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Hadiah?>(null) }
    
    val nameInput by viewModel.inputNamaHadiah.collectAsState()
    val pointsInput by viewModel.inputPoinHadiah.collectAsState()
    val stockInput by viewModel.inputStokHadiah.collectAsState()
    val descInput by viewModel.inputKeteranganHadiah.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Daftar Hadiah", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentScreen.value = MainViewModel.Screen.PENGATURAN }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.selectHadiahForEdit(null)
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Hadiah", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text("Kupon Hadiah Aktif", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
            Text("Daftar merchandise atau snack yang bisa ditukar murid.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

            if (hadiahs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada hadiah terdaftar", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(hadiahs) { h ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = h.nama_hadiah, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        if (h.keterangan.isNotBlank()) {
                                            Text(text = h.keterangan, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "${h.poin_dibutuhkan} Poin", color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tersedia: ${h.stok_hadiah} unit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (h.stok_hadiah > 0) GreenJade else Color.Red
                                    )
                                    Row {
                                        IconButton(onClick = { 
                                            viewModel.selectHadiahForEdit(h)
                                            showDialog = true 
                                        }) {
                                            Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { showDeleteConfirm = h }) {
                                            Icon(Icons.Default.Delete, "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && pointsInput.isNotBlank()) {
                            viewModel.saveHadiah()
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            },
            title = { Text("Simpan Data Hadiah", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BluePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.inputNamaHadiah.value = it },
                        label = { Text("Nama Hadiah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pointsInput,
                        onValueChange = { viewModel.inputPoinHadiah.value = it },
                        label = { Text("Poin Dibutuhkan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { viewModel.inputStokHadiah.value = it },
                        label = { Text("Ketersediaan Stok") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { viewModel.inputKeteranganHadiah.value = it },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        val deleting = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHadiahRecord(deleting)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Batal")
                }
            },
            title = { Text("Hapus Item Hadiah?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Hapus '${deleting.nama_hadiah}' dari sistem?") }
        )
    }
}

// ==========================================
// 12. HADIAH REQUESTED APPROVALS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadiahRequestsScreen(viewModel: MainViewModel) {
    val requests by viewModel.allPengajuan.collectAsState(initial = emptyList())
    var activeTab by remember { mutableStateOf("MENUNGGU") }
    var rejectReq by remember { mutableStateOf<PengajuanHadiah?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val filteredList = requests.filter { it.status.uppercase() == activeTab }
    val adminSession by viewModel.adminSession.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permintaan Hadiah Murid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentScreen.value = MainViewModel.Screen.PENGATURAN }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "MENUNGGU" -> 0
                    "DISETUJUI" -> 1
                    else -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BluePrimary
            ) {
                Tab(selected = activeTab == "MENUNGGU", onClick = { activeTab = "MENUNGGU" }, text = { Text("Menunggu", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == "DISETUJUI", onClick = { activeTab = "DISETUJUI" }, text = { Text("Disetujui", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == "DITOLAK", onClick = { activeTab = "DITOLAK" }, text = { Text("Ditolak", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data dalam status ini", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                ) {
                    items(filteredList) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = r.nama_siswa, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = r.nama_hadiah, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                    }
                                    val formattedDate = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(r.tanggal_pengajuan))
                                    Text(text = formattedDate, fontSize = 10.sp, color = Color.Gray)
                                }

                                if (r.keterangan.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (r.status.uppercase() == "DITOLAK") "Alasan: ${r.keterangan}" else "Catatan: ${r.keterangan}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(6.dp).fillMaxWidth()
                                    )
                                }

                                if (r.status.uppercase() == "MENUNGGU") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val admin = adminSession?.nama_admin ?: "Guru / Admin"
                                                viewModel.approveStudentRedemption(r.id_pengajuan, admin)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenJade),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Setujui", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                rejectReq = r
                                                rejectReason = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedCrimson),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Tolak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                        TextButton(onClick = { viewModel.deleteStudentRedemption(r) }) {
                                            Text("Hapus Riwayat", fontSize = 11.sp, color = Color.Gray)
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

    if (rejectReq != null) {
        val target = rejectReq!!
        AlertDialog(
            onDismissRequest = { rejectReq = null },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReason.trim().ifBlank { "Ditolak Guru." }
                        viewModel.rejectStudentRedemption(target.id_pengajuan, reason)
                        rejectReq = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tolak")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectReq = null }) {
                    Text("Batal")
                }
            },
            title = { Text("Tolak Pengajuan Hadiah", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tolak pengajuan dari ${target.nama_siswa}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Alasan Penolakan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

// ==========================================
// 13. STUDENT PORTAL SYSTEM SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalScreen(viewModel: MainViewModel) {
    val loggedInSiswa by viewModel.loggedInSiswa.collectAsState()
    val hadiahs by viewModel.allHadiah.collectAsState(initial = emptyList())
    val allPengajuan by viewModel.allPengajuan.collectAsState(initial = emptyList())
    val config by viewModel.pengaturan.collectAsState()
    val allSiswa by viewModel.allSiswa.collectAsState()
    val context = LocalContext.current

    var manualIdInput by remember { mutableStateOf("") }
    var showQrSimulator by remember { mutableStateOf(false) }

    if (loggedInSiswa == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF031B4E), // Deep Blue Primordial
                                Color(0xFF0C101B), // Cosmic Onyx
                                Color(0xFF4A020F)  // Subtle Crimson Accent Red
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val logoUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiSZzd8HFgvU5Nk3-5H6IM0X98Q2Of0MQDQxp8ZON5bAERd6elusg3ir--zNcaNFROzxywcIEuCfHDl6YWrzBTBOVlZxhyFB-pmqROr7cNpaC9oj8SbRn0tsgFz1vXzqxlQCqf3ZbGRvj-WVxPHeDn7TjMptG87hzvaN5mPI0psUUvcS16v1KR-Cawgltvw/s1254/ChatGPT%20Image%20May%2027,%202026,%2008_37_40%20PM.png"
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUrl)
                                    .transformations(TransparentBackgroundTransformation())
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Logo SIKAP",
                            modifier = Modifier.size(110.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "SIKAP",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Portal Murid",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Gunakan ID atau scan QR kartu murid Anda untuk pengecekan poin.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                            onClick = {
                                try {
                                    val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
                                    scanner.startScan()
                                        .addOnSuccessListener { barcode: com.google.mlkit.vision.barcode.common.Barcode ->
                                            val rawValue = barcode.rawValue ?: ""
                                            viewModel.checkStudentQrResult(context, rawValue)
                                        }
                                        .addOnFailureListener { _: Exception ->
                                            Toast.makeText(context, "Batal atau scan gagal.", Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal meluncurkan scanner: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, null, tint = RedCrimson, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("PANGGIL KAMERA SCAN QR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BluePrimary)
                                    Text("Pindai barcode QR kartu murid Anda", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("ATAU MASUK MANUAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = manualIdInput,
                            onValueChange = { manualIdInput = it },
                            label = { Text("Ketik ID Murid", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (manualIdInput.isNotBlank()) viewModel.checkStudentQrResult(context, manualIdInput.trim())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Masuk Portal", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(onClick = { viewModel.exitStudentPortal(context) }) {
                            Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp), tint = RedCrimson)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar ke Login Admin (Guru)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showQrSimulator) {
            AlertDialog(
                onDismissRequest = { showQrSimulator = false },
                confirmButton = { TextButton(onClick = { showQrSimulator = false }) { Text("Batal") } },
                title = { Text("Simulasi Scanner Kamera", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allSiswa) { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.checkStudentQrResult(context, "poinkarakter://siswa/${s.id_siswa}")
                                    showQrSimulator = false
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(s.nama, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Poin: ${s.saldo_poin}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    } else {
        val s = loggedInSiswa!!
        val studentRedemptions = allPengajuan.filter { it.id_siswa == s.id_siswa }
        var activeTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                val logoUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhpdrTOku1VnRYjRzo0QQQ0ufKfHyC7vI3RNg-7vAC7nfSpKE6aVL7P57v_bzf3bRnDlIXPnSRnAZvuaruIhn82v-S_WlMzQFwR9J6LzfO41zdPdzzGJC1l1DW0OyujOz0S-cUzeNZ0c58Ei9YaUZ6T6Vn1xjZ7ePBoMi_37NECWhiGF3hzk6822mIh9zoB/s320/SIKAP%20DENGAN%20DI%20BACKGROUND%20BIRU.png"
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(logoUrl)
                                        .transformations(TransparentBackgroundTransformation())
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Logo SIKAP",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("SIKAP", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("Portal Murid: ${s.nama}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.85f))
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitStudentPortal(context) }) {
                            Icon(Icons.Default.Logout, "Keluar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
                )
            },
            bottomBar = {
                if (s.is_petugas) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Portal") },
                            label = { Text("Portal", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BluePrimary,
                                selectedTextColor = BluePrimary,
                                indicatorColor = BluePrimary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            icon = { Icon(Icons.Default.Assignment, contentDescription = "Catat Poin") },
                            label = { Text("Catat", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BluePrimary,
                                selectedTextColor = BluePrimary,
                                indicatorColor = BluePrimary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                            label = { Text("Riwayat", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BluePrimary,
                                selectedTextColor = BluePrimary,
                                indicatorColor = BluePrimary.copy(alpha = 0.1f)
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Rekap") },
                            label = { Text("Rekap", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BluePrimary,
                                selectedTextColor = BluePrimary,
                                indicatorColor = BluePrimary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeTab == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            val status = viewModel.getCharacterStatus(s.saldo_poin)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SALDO POIN KARAKTER ANDA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("${s.saldo_poin}", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = if (s.saldo_poin >= 70) GreenJade else Color.Red)
                                    Text("Status: $status", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BluePrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Text("Kelas: ${s.kelas}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Gender: ${s.jenis_kelamin}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (s.is_petugas) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                                    border = BorderStroke(1.dp, Color(0xFF5EEAD4))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Assignment, null, tint = Color(0xFF0D9488), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("TUGAS KHUSUS AKTIF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Anda memiliki otoritas khusus untuk mencatat poin perilaku positif dan pelanggaran murid lain.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Klik tab \"Catat\", \"Riwayat\", dan \"Rekap\" di bagian bawah layar untuk mulai bertugas secara realtime.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D9488)
                                        )
                                    }
                                }
                            }
                        }

                if (!config.aktifkan_tukar_hadiah) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                        ) {
                            Text("Fitur penukaran hadiah ditiadakan sementara oleh Guru.", fontSize = 11.sp, color = Color(0xFF92400E), modifier = Modifier.padding(12.dp))
                        }
                    }
                } else {
                    item {
                        Text("Tukar Hadiah", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = BluePrimary)
                    }

                    if (hadiahs.isEmpty()) {
                        item {
                            Text("Belum ada persediaan hadiah di sekolah.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        items(hadiahs) { h ->
                            val isStockOut = h.stok_hadiah <= 0
                            val canAfford = s.saldo_poin >= h.poin_dibutuhkan
                            val btnEnabled = (!isStockOut && canAfford)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(h.nama_hadiah, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${h.poin_dibutuhkan} Poin • Stok: ${if (isStockOut) "Habis" else h.stok_hadiah.toString()}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = { viewModel.submitTukarHadiah(context, h) },
                                        enabled = btnEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isStockOut) "Habis" else "Tukar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Riwayat Pengajuan Poin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }

                if (studentRedemptions.isEmpty()) {
                    item {
                        Text("Belum ada riwayat pengajuan.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    items(studentRedemptions) { r ->
                        val (statusText, statusColor, statusBg) = when (r.status.uppercase()) {
                            "MENUNGGU" -> Triple("Menunggu Persetujuan", Color(0xFFD97706), Color(0xFFFFFBEB))
                            "DISETUJUI" -> Triple("Selesai Ditukar!", Color(0xFF059669), Color(0xFFECFDF5))
                            else -> Triple("Permintaan Ditolak", Color(0xFFDC2626), Color(0xFFFEF2F2))
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.nama_hadiah, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(statusBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(statusText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                    }
                                }
                                if (r.keterangan.isNotBlank() && r.keterangan != "Dicatat via Portal.") {
                                    Text("Ket: ${r.keterangan}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            } else if (activeTab == 1) {
                InputPoinScreen(viewModel = viewModel, isInPortal = true, petugasName = "${s.nama} (Murid Petugas)")
            } else if (activeTab == 2) {
                RiwayatPoinScreen(viewModel = viewModel)
            } else if (activeTab == 3) {
                RekapScreen(viewModel = viewModel)
            }
        }
    }
}
}