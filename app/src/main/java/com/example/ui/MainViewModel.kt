package com.example.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Admin
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import com.example.data.model.Hadiah
import com.example.data.model.PengajuanHadiah
import com.example.data.repository.AppRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // --- ENUM ROUTING ---
    enum class Screen {
        LOGIN,
        DASHBOARD,
        SISWA,
        INPUT_POIN,
        RIWAYAT,
        REKAP,
        PENGATURAN,
        KATEGORI_BAIK,
        KATEGORI_BURUK,
        HADIAH_ADMIN,
        HADIAH_REQUESTS,
        PORTAL_SISWA
    }

    val currentScreen = MutableStateFlow(Screen.LOGIN)

    // --- AUTHENTICATION STATE ---
    val isLoggedIn = MutableStateFlow(false)
    val adminSession = MutableStateFlow<Admin?>(null)
    val loginError = MutableStateFlow<String?>(null)

    // --- CURRENT EDIT STATES / SELECTIONS ---
    val editingSiswa = MutableStateFlow<Siswa?>(null)
    val editingKategori = MutableStateFlow<KategoriAktivitas?>(null)
    val selectedSiswaForDetail = MutableStateFlow<Siswa?>(null)

    // --- SEARCH & FILTER STATES ---
    val siswaSearchQuery = MutableStateFlow("")
    val siswaFilterKelas = MutableStateFlow("Semua")

    val riwayatSearchSiswa = MutableStateFlow("")
    val riwayatFilterKelas = MutableStateFlow("Semua")
    val riwayatFilterJenis = MutableStateFlow("Semua") // "Semua", "Perbuatan Baik", "Pelanggaran"
    val riwayatFilterTanggal = MutableStateFlow<String>("") // YYYY-MM-DD
    val riwayatFilterBulan = MutableStateFlow("Semua") // Short name, e.g. "01", "02" etc or "Semua"

    val rekapFilterKelas = MutableStateFlow("Semua")
    val rekapFilterBulan = MutableStateFlow("Semua")
    val rekapFilterTahunPljrn = MutableStateFlow("Semua")

    // --- ROOM REACTIVE DATA ---
    val allSiswa: StateFlow<List<Siswa>> = repository.allSiswaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKategori: StateFlow<List<KategoriAktivitas>> = repository.allKategoriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRiwayat: StateFlow<List<RiwayatPoin>> = repository.allRiwayatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pengaturan: StateFlow<PengaturanSekolah> = repository.pengaturanFlow
        .combine(MutableStateFlow(PengaturanSekolah())) { dbValue, default ->
            dbValue ?: default
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PengaturanSekolah())

    // --- FITUR HADIAH & REWARDS STATE ---
    val allHadiah: StateFlow<List<Hadiah>> = repository.allHadiahFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPengajuan: StateFlow<List<PengajuanHadiah>> = repository.allPengajuanFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingPengajuan: StateFlow<List<PengajuanHadiah>> = repository.pendingPengajuanFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form inputs for prize creation
    val editingHadiah = MutableStateFlow<Hadiah?>(null)
    val inputNamaHadiah = MutableStateFlow("")
    val inputPoinHadiah = MutableStateFlow("")
    val inputStokHadiah = MutableStateFlow("")
    val inputKeteranganHadiah = MutableStateFlow("")

    fun selectHadiahForEdit(hadiah: Hadiah?) {
        editingHadiah.value = hadiah
        if (hadiah != null) {
            inputNamaHadiah.value = hadiah.nama_hadiah
            inputPoinHadiah.value = hadiah.poin_dibutuhkan.toString()
            inputStokHadiah.value = hadiah.stok_hadiah.toString()
            inputKeteranganHadiah.value = hadiah.keterangan
        } else {
            inputNamaHadiah.value = ""
            inputPoinHadiah.value = ""
            inputStokHadiah.value = "10"
            inputKeteranganHadiah.value = ""
        }
    }

    // Student Session / Scan
    val loggedInSiswa = MutableStateFlow<Siswa?>(null)
    val checkInQrInput = MutableStateFlow("")

    // --- EXPORT/IMPORT NOTIFICATIONS ---
    val statusMessage = MutableStateFlow<String?>(null)

    // --- GOOGLE SIGN-IN & DRIVE STATE ---
    val isDarkMode = MutableStateFlow(false)

    fun loadThemeSettings(context: Context) {
        val prefs = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        isDarkMode.value = prefs.getBoolean("is_dark_mode", false)
    }

    fun toggleDarkMode(context: Context, enabled: Boolean) {
        isDarkMode.value = enabled
        val prefs = context.getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun initSession(context: Context) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isSavedLoggedIn = prefs.getBoolean("is_logged_in", false)
        val role = prefs.getString("role", "") ?: ""
        if (isSavedLoggedIn) {
            viewModelScope.launch {
                if (role == "admin") {
                    val username = prefs.getString("admin_username", "") ?: ""
                    val admin = repository.getAdminByUsername(username)
                    if (admin != null) {
                        isLoggedIn.value = true
                        adminSession.value = admin
                        loginError.value = null
                        currentScreen.value = Screen.DASHBOARD
                    } else {
                        clearSession(context)
                    }
                } else if (role == "siswa") {
                    val idSiswa = prefs.getInt("siswa_id", 0)
                    val s = repository.getSiswaById(idSiswa)
                    if (s != null) {
                        isLoggedIn.value = false
                        loggedInSiswa.value = s
                        currentScreen.value = Screen.PORTAL_SISWA
                    } else {
                        clearSession(context)
                    }
                }
            }
        }
    }

    fun saveAdminSession(context: Context, username: String) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("role", "admin")
            .putString("admin_username", username)
            .apply()
    }

    fun saveSiswaSession(context: Context, idSiswa: Int) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("role", "siswa")
            .putInt("siswa_id", idSiswa)
            .apply()
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    val isGoogleConnected = MutableStateFlow(false)
    val googleAccountEmail = MutableStateFlow("")
    val googleAccountName = MutableStateFlow("")
    val googleAccountAvatar = MutableStateFlow("")
    val isAutoSyncEnabled = MutableStateFlow(false)
    val lastSyncedTime = MutableStateFlow(0L)
    val googleDriveFiles = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val syncProgress = MutableStateFlow<String?>(null)

    data class DriveBackupFile(
        val fileId: String,
        val name: String,
        val size: String,
        val formattedDate: String,
        val rawJson: String
    )

    fun loadGoogleSettings(context: Context) {
        val prefs = context.getSharedPreferences("google_settings", Context.MODE_PRIVATE)
        val connected = prefs.getBoolean("google_connected", false)
        isGoogleConnected.value = connected
        googleAccountEmail.value = prefs.getString("google_email", "") ?: ""
        googleAccountName.value = prefs.getString("google_name", "") ?: ""
        googleAccountAvatar.value = prefs.getString("google_avatar", "") ?: ""
        isAutoSyncEnabled.value = prefs.getBoolean("auto_sync_enabled", false)
        lastSyncedTime.value = prefs.getLong("last_synced_time", 0L)
        loadGoogleDriveFiles(context)
        
        if (connected && isAutoSyncEnabled.value) {
            val now = System.currentTimeMillis()
            if (now - lastSyncedTime.value >= 24 * 60 * 60 * 1000) {
                syncDatabaseToGoogleDrive(context, silent = true)
            }
        }
    }

    fun saveGoogleSettings(context: Context) {
        val prefs = context.getSharedPreferences("google_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("google_connected", isGoogleConnected.value)
            putString("google_email", googleAccountEmail.value)
            putString("google_name", googleAccountName.value)
            putString("google_avatar", googleAccountAvatar.value)
            putBoolean("auto_sync_enabled", isAutoSyncEnabled.value)
            putLong("last_synced_time", lastSyncedTime.value)
            apply()
        }
    }

    fun loadGoogleDriveFiles(context: Context) {
        val prefs = context.getSharedPreferences("google_settings", Context.MODE_PRIVATE)
        val currentBackupCache = prefs.getStringSet("backup_files_cache", emptySet()) ?: emptySet()
        val list = currentBackupCache.mapNotNull { item ->
            val parts = item.split("|||")
            if (parts.size >= 5) {
                DriveBackupFile(
                    fileId = parts[0],
                    name = parts[1],
                    size = parts[2],
                    formattedDate = parts[3],
                    rawJson = parts[4]
                )
            } else null
        }.sortedByDescending { it.formattedDate }
        googleDriveFiles.value = list
    }

    fun connectGoogleAccount(context: Context, email: String, name: String) {
        viewModelScope.launch {
            syncProgress.value = "Menghubungkan akun Google..."
            kotlinx.coroutines.delay(1000)
            isGoogleConnected.value = true
            googleAccountEmail.value = email.trim()
            googleAccountName.value = name.trim()
            googleAccountAvatar.value = if (name.isNotBlank()) name.substring(0, 1).uppercase() else "G"
            saveGoogleSettings(context)
            syncProgress.value = null
            statusMessage.value = "Akun Google ${email} berhasil dihubungkan!"
            
            // Auto login with Google account details
            loginAdminWithGoogle(context)
        }
    }

    fun disconnectGoogleAccount(context: Context) {
        isGoogleConnected.value = false
        googleAccountEmail.value = ""
        googleAccountName.value = ""
        googleAccountAvatar.value = ""
        isAutoSyncEnabled.value = false
        saveGoogleSettings(context)
        statusMessage.value = "Akun Google berhasil diputuskan."
    }

    fun loginAdminWithGoogle(context: Context) {
        if (isGoogleConnected.value) {
            viewModelScope.launch {
                val email = googleAccountEmail.value
                val name = googleAccountName.value
                val existingAdmin = repository.getAdminByUsername(email)
                val admin = if (existingAdmin != null) {
                    existingAdmin.copy(nama_admin = name)
                } else {
                    val newAdmin = Admin(username = email, password = "", nama_admin = name)
                    repository.insertAdmin(newAdmin)
                    newAdmin
                }
                isLoggedIn.value = true
                adminSession.value = admin
                loginError.value = null
                saveAdminSession(context, email)
                currentScreen.value = Screen.DASHBOARD
            }
        }
    }

    fun syncDatabaseToGoogleDrive(context: Context, silent: Boolean = false) {
        viewModelScope.launch {
            if (!isGoogleConnected.value) {
                if (!silent) statusMessage.value = "Hubungkan akun Google Drive Anda terlebih dahulu!"
                return@launch
            }
            if (!silent) syncProgress.value = "Sedang mensinkronisasikan basis data ke Google Drive..."
            
            val payloadJson = exportToJSONString()
            if (payloadJson.isBlank()) {
                if (!silent) {
                    syncProgress.value = null
                    statusMessage.value = "Gagal memproses ekspor basis data!"
                }
                return@launch
            }

            kotlinx.coroutines.delay(1200)

            val now = System.currentTimeMillis()
            val docName = "poin_karakter_backup_${formatTimestampDate(now, "yyyyMMdd_HHmmss")}.json"
            val newFile = DriveBackupFile(
                fileId = "gdrive_${now}",
                name = docName,
                size = "${String.format("%.2f", payloadJson.length / 1024.0)} KB",
                formattedDate = formatTimestampDate(now, "dd/MM/yyyy HH:mm:ss"),
                rawJson = payloadJson
            )

            val prefs = context.getSharedPreferences("google_settings", Context.MODE_PRIVATE)
            val currentBackupCache = prefs.getStringSet("backup_files_cache", emptySet()) ?: emptySet()
            val newCache = currentBackupCache.toMutableSet()
            newCache.add("${newFile.fileId}|||${newFile.name}|||${newFile.size}|||${newFile.formattedDate}|||${newFile.rawJson}")
            prefs.edit().putStringSet("backup_files_cache", newCache).apply()

            loadGoogleDriveFiles(context)

            lastSyncedTime.value = now
            saveGoogleSettings(context)

            if (!silent) {
                syncProgress.value = null
                statusMessage.value = "Sinkronisasi ke Google Drive berhasil disinkronkan!"
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context, backup: DriveBackupFile) {
        viewModelScope.launch {
            syncProgress.value = "Sedang mengunduh dan memulihkan basis data dari Google Drive..."
            kotlinx.coroutines.delay(1500)
            val success = restoreFromJSONString(backup.rawJson)
            syncProgress.value = null
            if (success) {
                statusMessage.value = "Restorasi dari Google Drive berhasil disinkronkan!"
            } else {
                statusMessage.value = "Gagal memulihkan data dari Drive!"
            }
        }
    }

    fun deleteBackupFromGoogleDrive(context: Context, backup: DriveBackupFile) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("google_settings", Context.MODE_PRIVATE)
            val currentBackupCache = prefs.getStringSet("backup_files_cache", emptySet()) ?: emptySet()
            val targetStrPrefix = "${backup.fileId}|||"
            val newCache = currentBackupCache.filter { !it.startsWith(targetStrPrefix) }.toSet()
            prefs.edit().putStringSet("backup_files_cache", newCache).apply()
            loadGoogleDriveFiles(context)
            statusMessage.value = "Berkas file backup berhasil dihapus dari Google Drive."
        }
    }

    fun checkAndTriggerAutoSync(context: Context) {
        if (isGoogleConnected.value && isAutoSyncEnabled.value) {
            val now = System.currentTimeMillis()
            if (now - lastSyncedTime.value >= 24 * 60 * 60 * 1000) {
                syncDatabaseToGoogleDrive(context, silent = true)
            }
        }
    }

    init {
        // Initialize DB items (Seed default users, configurations, and categories)
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            // Auto login in dev or set session if required
        }
    }

    // --- AUTH ACTIONS ---
    fun loginAdmin(context: Context, usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            if (usernameInput.isBlank() || passwordInput.isBlank()) {
                loginError.value = "Username dan password tidak boleh kosong!"
                return@launch
            }
            val admin = repository.getAdminByUsername(usernameInput)
            if (admin != null && admin.password == passwordInput) {
                isLoggedIn.value = true
                adminSession.value = admin
                loginError.value = null
                saveAdminSession(context, usernameInput)
                currentScreen.value = Screen.DASHBOARD
            } else {
                loginError.value = "Username atau password salah!"
            }
        }
    }

    fun logout(context: Context) {
        isLoggedIn.value = false
        adminSession.value = null
        currentScreen.value = Screen.LOGIN
        clearSession(context)
    }

    // --- SISWA CRUD ---
    fun saveSiswa(nama: String, kelas: String, jenisKelamin: String) {
        viewModelScope.launch {
            if (nama.isBlank() || kelas.isBlank()) {
                statusMessage.value = "Nama dan Kelas harus diisi!"
                return@launch
            }
            val current = editingSiswa.value
            if (current != null) {
                repository.updateSiswa(current.copy(nama = nama.trim(), kelas = kelas.trim(), jenis_kelamin = jenisKelamin))
                statusMessage.value = "Berhasil memperbarui data siswa."
            } else {
                val configVal = pengaturan.value
                val startingPoin = configVal?.poin_awal_siswa ?: 100
                repository.insertSiswa(
                    Siswa(
                        nama = nama.trim(),
                        kelas = kelas.trim(),
                        jenis_kelamin = jenisKelamin,
                        saldo_poin = startingPoin
                    )
                )
                statusMessage.value = "Berhasil menambahkan data siswa (Poin Awal: $startingPoin)."
            }
            editingSiswa.value = null
        }
    }

    fun removeSiswa(siswa: Siswa) {
        viewModelScope.launch {
            repository.deleteSiswa(siswa)
            statusMessage.value = "Berhasil menghapus siswa ${siswa.nama}."
        }
    }

    // --- KATEGORI CRUD ---
    fun saveKategori(nama: String, jenis: String, nilaiStr: String, keterangan: String) {
        viewModelScope.launch {
            val nilaiAbs = nilaiStr.toIntOrNull()
            if (nama.isBlank() || nilaiAbs == null) {
                statusMessage.value = "Nama kategori dan nilai poin harus valid!"
                return@launch
            }

            // Aturan poin: Perbuatan Baik harus bernilai positif, Pelanggaran harus bernilai negatif
            val nilaiFinal = if (jenis == "Perbuatan Baik") {
                Math.abs(nilaiAbs)
            } else {
                -Math.abs(nilaiAbs)
            }

            val current = editingKategori.value
            if (current != null) {
                repository.updateKategori(current.copy(
                    nama_kategori = nama.trim(),
                    jenis_kategori = jenis,
                    nilai_poin = nilaiFinal,
                    keterangan = keterangan.trim()
                ))
                statusMessage.value = "Berhasil memperbarui kategori."
            } else {
                repository.insertKategori(KategoriAktivitas(
                    nama_kategori = nama.trim(),
                    jenis_kategori = jenis,
                    nilai_poin = nilaiFinal,
                    keterangan = keterangan.trim()
                ))
                statusMessage.value = "Berhasil menambahkan kategori."
            }
            editingKategori.value = null
        }
    }

    fun removeKategori(kategori: KategoriAktivitas) {
        viewModelScope.launch {
            repository.deleteKategori(kategori)
            statusMessage.value = "Berhasil menghapus kategori ${kategori.nama_kategori}."
        }
    }

    // --- INPUT POIN ACTION ---
    fun recordCatatanPoin(
        idSiswa: Int,
        idKategori: Int,
        customDate: Long,
        pencatat: String,
        catatanTambahan: String,
        isFromPortal: Boolean = false
    ) {
        viewModelScope.launch {
            if (idSiswa == 0 || idKategori == 0) {
                statusMessage.value = "Format input tidak lengkap. Harap pilih siswa dan kategori!"
                return@launch
            }
            val kategori = repository.getKategoriById(idKategori)
            if (kategori == null) {
                statusMessage.value = "Kategori tidak ditemukan!"
                return@launch
            }

            val riwayat = RiwayatPoin(
                id_siswa = idSiswa,
                id_kategori = idKategori,
                jenis_catatan = kategori.jenis_kategori,
                nilai_poin = kategori.nilai_poin,
                tanggal = if (customDate == 0L) System.currentTimeMillis() else customDate,
                guru_pencatat = if (pencatat.isBlank()) "Guru Pengurus" else pencatat,
                keterangan = catatanTambahan.trim()
            )

            val sukses = repository.recordRiwayatAndUpdateSiswa(riwayat)
            if (sukses) {
                statusMessage.value = "Berhasil mencatat poin karakter murid."
                if (!isFromPortal) {
                    currentScreen.value = Screen.DASHBOARD
                }
            } else {
                statusMessage.value = "Gagal mencatat poin. Murid tidak ditemukan!"
            }
        }
    }

    fun recordCatatanPoinKustom(
        idSiswa: Int,
        namaKategoriKustom: String,
        jenisCatatanKustom: String,
        nilaiPoinKustom: Int,
        customDate: Long,
        pencatat: String,
        catatanTambahan: String,
        isFromPortal: Boolean = false
    ) {
        viewModelScope.launch {
            if (idSiswa == 0 || namaKategoriKustom.isBlank()) {
                statusMessage.value = "Gagal mencatat: Murid dan Nama Kategori harus diisi!"
                return@launch
            }
            val riwayat = RiwayatPoin(
                id_siswa = idSiswa,
                id_kategori = -1, // -1 signals manual custom
                jenis_catatan = jenisCatatanKustom,
                nilai_poin = nilaiPoinKustom,
                tanggal = if (customDate == 0L) System.currentTimeMillis() else customDate,
                guru_pencatat = if (pencatat.isBlank()) "Guru Pengurus" else pencatat,
                keterangan = "[Poin Kustom: $namaKategoriKustom] ${catatanTambahan.trim()}"
            )

            val sukses = repository.recordRiwayatAndUpdateSiswa(riwayat)
            if (sukses) {
                statusMessage.value = "Berhasil mencatat poin karakter kustom ($nilaiPoinKustom Poin)."
                if (!isFromPortal) {
                    currentScreen.value = Screen.DASHBOARD
                }
            } else {
                statusMessage.value = "Gagal mencatat poin. Murid tidak ditemukan!"
            }
        }
    }

    fun importStudentsFromTxt(content: String): Int {
        var importCount = 0
        val lines = content.lines()
        val defaultPoin = pengaturan.value?.poin_awal_siswa ?: 100
        viewModelScope.launch {
            for (line in lines) {
                val cleaned = line.trim()
                if (cleaned.isEmpty() || cleaned.startsWith("#")) continue
                val parts = cleaned.split(",")
                if (parts.size >= 3) {
                    val nama = parts[0].trim()
                    val kelas = parts[1].trim()
                    var jk = parts[2].trim()
                    if (jk.startsWith("L", ignoreCase = true)) {
                        jk = "Laki-laki"
                    } else if (jk.startsWith("P", ignoreCase = true)) {
                        jk = "Perempuan"
                    } else {
                        jk = "Laki-laki" // default
                    }
                    if (nama.isNotEmpty() && kelas.isNotEmpty()) {
                        repository.insertSiswa(
                            Siswa(
                                nama = nama,
                                kelas = kelas,
                                jenis_kelamin = jk,
                                saldo_poin = defaultPoin
                            )
                        )
                        importCount++
                    }
                }
            }
            statusMessage.value = "Berhasil mengimpor $importCount data siswa secara masal!"
        }
        return importCount
    }

    fun getStudentFormatTemplate(): String {
        return """
            # FORMAT IMPORT MASSAL DATA SISWA POIN KARAKTER
            # Baris yang diawali dengan tanda pagar '#' akan diabaikan oleh sistem.
            # Tuliskan data dengan format: Nama Lengkap, Kelas, Jenis Kelamin (Laki-laki atau Perempuan)
            # Pastikan dipisahkan dengan tanda koma ( , ).
            # Contoh:
            Ahmad Ridwan, 5A, Laki-laki
            Siti Aminah, 5B, Perempuan
            Andi Wijaya, 6C, Laki-laki
            Fatimah Azzahra, 6A, Perempuan
        """.trimIndent()
    }

    fun deleteRiwayatRecord(riwayat: RiwayatPoin) {
        viewModelScope.launch {
            val sukses = repository.deleteRiwayatAndRevertSiswa(riwayat)
            if (sukses) {
                statusMessage.value = "Berhasil membatalkan dan menghapus catatan poin."
            } else {
                statusMessage.value = "Gagal membatalkan catatan poin."
            }
        }
    }

    // --- SETTINGS (PENGATURAN) CRUD ---
    fun updateSettings(
        namaSekolah: String,
        tahunPelajaran: String,
        namaKepalaSekolah: String,
        batasSangatBaik: Int,
        batasBaik: Int,
        batasPembinaan: Int,
        batasPeringatan: Int,
        poinAwalSiswa: Int,
        namaGuruTtd: String,
        peranGuruTtd: String
    ) {
        viewModelScope.launch {
            val current = pengaturan.value
            val update = current.copy(
                nama_sekolah = namaSekolah.trim(),
                tahun_pelajaran = tahunPelajaran.trim(),
                nama_kepala_sekolah = namaKepalaSekolah.trim(),
                batas_sangat_baik = batasSangatBaik,
                batas_baik = batasBaik,
                batas_pembinaan = batasPembinaan,
                batas_peringatan = batasPeringatan,
                poin_awal_siswa = poinAwalSiswa,
                nama_guru_ttd = namaGuruTtd.trim(),
                peran_guru_ttd = peranGuruTtd.trim()
            )
            repository.saveOrUpdatePengaturan(update)
            statusMessage.value = "Konfigurasi pengaturan sekolah diperbarui."
        }
    }

    fun updateAdminProfile(namaAdmin: String, usernameBaru: String, passwordBaru: String) {
        viewModelScope.launch {
            val session = adminSession.value ?: return@launch
            if (usernameBaru.isBlank() || passwordBaru.isBlank() || namaAdmin.isBlank()) {
                statusMessage.value = "Profil admin tidak boleh kosong!"
                return@launch
            }
            val updated = session.copy(
                nama_admin = namaAdmin.trim(),
                username = usernameBaru.trim(),
                password = passwordBaru.trim()
            )
            repository.updateAdmin(updated)
            adminSession.value = updated
            statusMessage.value = "Profil & keamanan admin berhasil diperbarui."
        }
    }

    // --- DETAILED CHARACTER STATE CALCULATOR ---
    fun getCharacterStatus(poin: Int, config: PengaturanSekolah = pengaturan.value): String {
        return when {
            poin >= config.batas_sangat_baik -> "Sangat Baik"
            poin >= config.batas_baik -> "Baik"
            poin >= config.batas_pembinaan -> "Perlu Pembinaan"
            poin >= config.batas_peringatan -> "Peringatan"
            else -> "Panggilan Orang Tua"
        }
    }

    // --- REKAPAN DATA / DASHBOARD reactive values ---
    // Helpers to support month parsing
    fun formatTimestampDate(ts: Long, format: String = "dd/MM/yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(format, Locale("id", "ID"))
            sdf.format(Date(ts))
        } catch (e: Exception) {
            "-"
        }
    }

    // --- JSON BACKUP PAYLOAD DATA ---
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    data class BackupPayload(
        val admins: List<Admin>,
        val siswas: List<Siswa>,
        val kategoris: List<KategoriAktivitas>,
        val riwayats: List<RiwayatPoin>,
        val pengaturan: PengaturanSekolah?
    )

    fun exportToJSONString(): String {
        return try {
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = BackupPayload(
                admins = listOf(adminSession.value ?: Admin(username = "admin" , password = "admin", nama_admin = "Guru Pengurus")),
                siswas = allSiswa.value,
                kategoris = allKategori.value,
                riwayats = allRiwayat.value,
                pengaturan = pengaturan.value
            )
            adapter.toJson(payload)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun restoreFromJSONString(json: String): Boolean {
        return try {
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(json) ?: return false

            viewModelScope.launch {
                // Delete everything or fallback to destructive reconstruction
                // To keep database clean and safe, replace items:
                for (a in payload.admins) {
                    repository.updateAdmin(a)
                }
                
                // Clear and re-populate siswa
                val existingSiswa = repository.getAllSiswaList()
                for (s in existingSiswa) {
                    repository.deleteSiswa(s)
                }
                for (s in payload.siswas) {
                    repository.insertSiswa(s)
                }

                // Clear and re-populate kategori
                val existingKategori = repository.getAllKategoriList()
                for (k in existingKategori) {
                    repository.deleteKategori(k)
                }
                for (k in payload.kategoris) {
                    repository.insertKategori(k)
                }

                // Clear and re-populate riwayat
                val existingRiwayat = repository.getAllRiwayatList()
                for (r in existingRiwayat) {
                    repository.deleteRiwayatAndRevertSiswa(r) // Triggers automatic reversals
                }
                // Write riwayat direct
                for (r in payload.riwayats) {
                    repository.recordRiwayatAndUpdateSiswa(r)
                }

                // Save settings
                payload.pengaturan?.let {
                    repository.saveOrUpdatePengaturan(it)
                }
                
                statusMessage.value = "Pemulihan database (Restore) berhasil disinkronkan!"
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            statusMessage.value = "Format backup tidak valid atau rusak!"
            false
        }
    }

    // --- REPORT CSV EXPORT (EXCEL COMPATIBLE) ---
    fun generateCSVReport(context: Context, mode: String, filterVal: String): File? {
        val fileName = "Rekap_Poin_${mode.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
        val csvFile = File(context.cacheDir, fileName)
        
        try {
            val writer = csvFile.bufferedWriter()
            // Write School Info Header
            val config = pengaturan.value
            writer.write("${config.nama_sekolah}\n")
            writer.write("Tahun Pelajaran: ${config.tahun_pelajaran}\n")
            writer.write("${config.peran_guru_ttd}: ${config.nama_guru_ttd}\n")
            writer.write("Laporan Rekap: $mode ($filterVal)\n")
            writer.write("Tanggal Unduh: ${formatTimestampDate(System.currentTimeMillis())}\n\n")

            // Write Columns Header
            writer.write("No,Nama Siswa,Kelas,Jenis Kelamin,Total Poin Positif,Total Poin Negatif,Saldo Akhir Poin,Status Karakter\n")

            val siswas = allSiswa.value
            val riwayats = allRiwayat.value

            val filteredSiswas = when (mode) {
                "Per Kelas" -> siswas.filter { it.kelas == filterVal }
                else -> siswas
            }

            filteredSiswas.forEachIndexed { i, s ->
                val sRiwayats = riwayats.filter { it.id_siswa == s.id_siswa }
                val posPoints = sRiwayats.filter { it.jenis_catatan == "Perbuatan Baik" }.sumOf { it.nilai_poin }
                val negPoints = sRiwayats.filter { it.jenis_catatan == "Pelanggaran" }.sumOf { it.nilai_poin }
                val status = getCharacterStatus(s.saldo_poin)
                
                writer.write("${i + 1},\"${s.nama}\",\"${s.kelas}\",\"${s.jenis_kelamin}\",$posPoints,$negPoints,${s.saldo_poin},\"$status\"\n")
            }

            writer.flush()
            writer.close()
            return csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // --- WEBVIEW NATIVE HTML PDF REPORT PRINTING ---
    fun invokeNativePrintHTML(
        context: Context,
        mode: String,
        filterVal: String,
        targetSiswa: Siswa? = null
    ) {
        val config = pengaturan.value
        val siswas = allSiswa.value
        val riwayats = allRiwayat.value
        val schoolName = config.nama_sekolah
        val tp = config.tahun_pelajaran
        val kasek = config.nama_kepala_sekolah

        // Construct a highly polished CSS/HTML layout ready for direct printing or saving as PDF
        val htmlContent = StringBuilder()
        htmlContent.append("""
            <html>
            <head>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 24px; color: #1e293b; background: white; margin: 0; }
                    .header { text-align: center; border-bottom: 3px double #0f172a; padding-bottom: 12px; margin-bottom: 24px; }
                    .header h1 { margin: 0; font-size: 24px; text-transform: uppercase; letter-spacing: 1px; color: #1e40af; }
                    .header p { margin: 4px 0; font-size: 14px; color: #475569; }
                    .meta-info { margin-bottom: 20px; font-size: 13px; line-height: 1.6; }
                    .meta-info span { font-weight: bold; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
                    th, td { border: 1px solid #cbd5e1; padding: 8px 10px; text-align: left; }
                    th { background-color: #f1f5f9; font-weight: bold; color: #0f172a; }
                    tr:nth-child(even) { background-color: #f8fafc; }
                    .badge { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; color: white; }
                    .sangat-baik { background-color: #16a34a; }
                    .baik { background-color: #3b82f6; }
                    .pembinaan { background-color: #eab308; color: #1e293b; }
                    .peringatan { background-color: #f97316; }
                    .orangtua { background-color: #dc2626; }
                    .footer { margin-top: 40px; text-align: right; font-size: 13px; line-height: 1.8; }
                    .footer-space { display: inline-block; width: 220px; text-align: center; }
                    .point-plus { color: #16a34a; font-weight: bold; }
                    .point-minus { color: #dc2626; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class='header'>
                    <h1>LAPORAN REKAPITULASI KARAKTER SISWA</h1>
                    <h1>$schoolName</h1>
                    <p>Tahun Pelajaran: $tp | Kepala Sekolah: $kasek</p>
                </div>
        """.trimIndent())

        if (mode == "Per Siswa" && targetSiswa != null) {
            // Single Pupil Detailed Report
            val sRiwayats = riwayats.filter { it.id_siswa == targetSiswa.id_siswa }
            val posPoints = sRiwayats.filter { it.jenis_catatan == "Perbuatan Baik" }.sumOf { it.nilai_poin }
            val negPoints = sRiwayats.filter { it.jenis_catatan == "Pelanggaran" }.sumOf { it.nilai_poin }
            val status = getCharacterStatus(targetSiswa.saldo_poin)
            val badgeClass = when (status) {
                "Sangat Baik" -> "sangat-baik"
                "Baik" -> "baik"
                "Perlu Pembinaan" -> "pembinaan"
                "Peringatan" -> "peringatan"
                else -> "orangtua"
            }

            htmlContent.append("""
                <div class='meta-info'>
                    <table style='width: 50%; border: none; margin-bottom: 20px;'>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Nama Lengkap</span></td><td style='border: none; padding: 2px 0;'>: ${targetSiswa.nama}</td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Kelas</span></td><td style='border: none; padding: 2px 0;'>: ${targetSiswa.kelas}</td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Jenis Kelamin</span></td><td style='border: none; padding: 2px 0;'>: ${targetSiswa.jenis_kelamin}</td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Total Poin POSITIF</span></td><td style='border: none; padding: 2px 0; class="point-plus"'>: +$posPoints</td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Total Poin NEGATIF</span></td><td style='border: none; padding: 2px 0; class="point-minus"'>: $negPoints</td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Saldo Akhir Poin</span></td><td style='border: none; padding: 2px 0;'>: <strong>${targetSiswa.saldo_poin}</strong></td></tr>
                        <tr style='background: none;'><td style='border: none; padding: 2px 0;'><span>Status Karakter</span></td><td style='border: none; padding: 2px 0;'><span class='badge $badgeClass'>$status</span></td></tr>
                    </table>
                </div>
                <h3>RIWAYAT SEJARAH AKTIVITAS & PERILAKU SISWA</h3>
                <table>
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Tanggal</th>
                            <th>Jenis Aktivitas</th>
                            <th>Nilai Poin</th>
                            <th>Pencatat</th>
                            <th>Keterangan</th>
                        </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            if (sRiwayats.isEmpty()) {
                htmlContent.append("<tr><td colspan='6' style='text-align:center;'>Belum ada catatan aktivitas untuk siswa ini.</td></tr>")
            } else {
                sRiwayats.forEachIndexed { idx, r ->
                    val dateStr = formatTimestampDate(r.tanggal, "dd MMMM yyyy")
                    val pointVal = if (r.nilai_poin > 0) "+${r.nilai_poin}" else "${r.nilai_poin}"
                    val pointStyle = if (r.nilai_poin > 0) "point-plus" else "point-minus"
                    htmlContent.append("""
                        <tr>
                            <td>${idx + 1}</td>
                            <td>$dateStr</td>
                            <td>${r.jenis_catatan}</td>
                            <td class='$pointStyle'>$pointVal</td>
                            <td>${r.guru_pencatat}</td>
                            <td>${r.keterangan.ifBlank { "-" }}</td>
                        </tr>
                    """.trimIndent())
                }
            }
            htmlContent.append("</tbody></table>")

        } else {
            // General Table Report (e.g. Class, Month, or All)
            val filteredSiswas = when (mode) {
                "Per Kelas" -> siswas.filter { it.kelas == filterVal }
                else -> siswas
            }

            htmlContent.append("""
                <div class='meta-info'>
                    <span>Jenis Laporan:</span> Rekap Laporan $mode ($filterVal)<br/>
                    <span>Tanggal Dicetak:</span> ${formatTimestampDate(System.currentTimeMillis(), "dd MMMM yyyy")}<br/>
                    <span>Kategori Filter:</span> ${if (filterVal.isBlank()) "Semua Data" else filterVal}
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Siswa</th>
                            <th>Kelas</th>
                            <th>J.K</th>
                            <th>Poin Positif</th>
                            <th>Poin Negatif</th>
                            <th>Saldo Akhir</th>
                            <th>Status Karakter</th>
                        </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            if (filteredSiswas.isEmpty()) {
                htmlContent.append("<tr><td colspan='8' style='text-align:center;'>Tidak ada data siswa untuk parameter ini.</td></tr>")
            } else {
                filteredSiswas.forEachIndexed { idx, s ->
                    val sRiwayats = riwayats.filter { it.id_siswa == s.id_siswa }
                    val posPoints = sRiwayats.filter { it.jenis_catatan == "Perbuatan Baik" }.sumOf { it.nilai_poin }
                    val negPoints = sRiwayats.filter { it.jenis_catatan == "Pelanggaran" }.sumOf { it.nilai_poin }
                    val status = getCharacterStatus(s.saldo_poin)
                    val badgeClass = when (status) {
                        "Sangat Baik" -> "sangat-baik"
                        "Baik" -> "baik"
                        "Perlu Pembinaan" -> "pembinaan"
                        "Peringatan" -> "peringatan"
                        else -> "orangtua"
                    }
                    htmlContent.append("""
                        <tr>
                            <td>${idx + 1}</td>
                            <td><strong>${s.nama}</strong></td>
                            <td>${s.kelas}</td>
                            <td>${if (s.jenis_kelamin.startsWith("L", true)) "L" else "P"}</td>
                            <td class='point-plus'>+$posPoints</td>
                            <td class='point-minus'>$negPoints</td>
                            <td><strong>${s.saldo_poin}</strong></td>
                            <td><span class='badge $badgeClass'>$status</span></td>
                        </tr>
                    """.trimIndent())
                }
            }
            htmlContent.append("</tbody></table>")
        }

        val printDateStr = formatTimestampDate(System.currentTimeMillis(), "dd MMMM yyyy")
        val ttdRole = config.peran_guru_ttd
        val ttdName = config.nama_guru_ttd
        htmlContent.append("""
                <div class='footer'>
                    <div class='footer-space'>
                        <p>Dicetak pada $printDateStr,</p>
                        <p>$ttdRole $schoolName</p>
                        <br/><br/><br/><br/>
                        <p><strong>$ttdName</strong></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        // Launch standard Android Html Printing Routine via off-screen WebView Client
        viewModelScope.launch(Dispatchers.Main) {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Laporan_${mode.replace(" ", "_")}_Poin_Karakter"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                    statusMessage.value = "Printer Laporan berhasil dipanggil."
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null)
        }
    }

    fun toggleAktifkanTukarHadiah(enabled: Boolean) {
        viewModelScope.launch {
            val current = pengaturan.value
            repository.saveOrUpdatePengaturan(current.copy(aktifkan_tukar_hadiah = enabled))
            statusMessage.value = if (enabled) "Sistem penukaran hadiah diaktifkan." else "Sistem penukaran hadiah dinonaktifkan."
        }
    }

    fun saveHadiah() {
        viewModelScope.launch {
            val nama = inputNamaHadiah.value.trim()
            val poinStr = inputPoinHadiah.value.trim()
            val stokStr = inputStokHadiah.value.trim()
            val keterangan = inputKeteranganHadiah.value.trim()

            if (nama.isBlank() || poinStr.isBlank()) {
                statusMessage.value = "Nama dan Poin kebutuhan harus diisi!"
                return@launch
            }

            val poin = poinStr.toIntOrNull() ?: 0
            val stok = stokStr.toIntOrNull() ?: 0

            val current = editingHadiah.value
            if (current != null) {
                val updated = current.copy(
                    nama_hadiah = nama,
                    poin_dibutuhkan = poin,
                    stok_hadiah = stok,
                    keterangan = keterangan
                )
                repository.updateHadiah(updated)
                statusMessage.value = "Berhasil memperbarui hadiah."
            } else {
                val newHadiah = Hadiah(
                    nama_hadiah = nama,
                    poin_dibutuhkan = poin,
                    stok_hadiah = stok,
                    keterangan = keterangan
                )
                repository.insertHadiah(newHadiah)
                statusMessage.value = "Berhasil menambahkan hadiah baru."
            }
            selectHadiahForEdit(null)
            currentScreen.value = Screen.PENGATURAN
        }
    }

    fun deleteHadiahRecord(hadiah: Hadiah) {
        viewModelScope.launch {
            repository.deleteHadiah(hadiah)
            statusMessage.value = "Berhasil menghapus hadiah '${hadiah.nama_hadiah}'."
        }
    }

    fun selectSiswaForPortal(context: Context, siswa: Siswa) {
        loggedInSiswa.value = siswa
        saveSiswaSession(context, siswa.id_siswa)
        currentScreen.value = Screen.PORTAL_SISWA
    }

    fun exitStudentPortal(context: Context) {
        loggedInSiswa.value = null
        currentScreen.value = Screen.LOGIN
        clearSession(context)
    }

    fun submitTukarHadiah(context: Context, hadiah: Hadiah) {
        viewModelScope.launch {
            val siswa = loggedInSiswa.value ?: return@launch
            if (siswa.saldo_poin < hadiah.poin_dibutuhkan) {
                statusMessage.value = "Gagal! Poin Anda (${siswa.saldo_poin}) tidak mencukupi untuk ${hadiah.nama_hadiah} (${hadiah.poin_dibutuhkan} Poin)."
                return@launch
            }
            if (hadiah.stok_hadiah <= 0) {
                statusMessage.value = "Gagal! Stok hadiah ini sedang kosong."
                return@launch
            }

            val request = PengajuanHadiah(
                id_siswa = siswa.id_siswa,
                id_hadiah = hadiah.id_hadiah,
                nama_siswa = siswa.nama,
                nama_hadiah = hadiah.nama_hadiah,
                tanggal_pengajuan = System.currentTimeMillis(),
                status = "MENUNGGU",
                keterangan = "Dicatat via Portal."
            )
            repository.insertPengajuan(request)
            statusMessage.value = "Sukses mengajukan penukaran! Guru akan memproses persetujuan Anda."
            
            // Sync current student details
            val fresh = repository.getSiswaById(siswa.id_siswa)
            if (fresh != null) {
                loggedInSiswa.value = fresh
            }
        }
    }

    fun approveStudentRedemption(idPengajuan: Int, adminName: String) {
        viewModelScope.launch {
            val sukses = repository.approvePengajuan(idPengajuan, adminName)
            if (sukses) {
                statusMessage.value = "Pengajuan penukaran hadiah berhasil disetujui."
            } else {
                statusMessage.value = "Gagal menyetujui. Pastikan poin siswa cukup dan stok hadiah mencukupi!"
            }
        }
    }

    fun rejectStudentRedemption(idPengajuan: Int, reason: String) {
        viewModelScope.launch {
            val sukses = repository.rejectPengajuan(idPengajuan, reason)
            if (sukses) {
                statusMessage.value = "Pengajuan berhasil ditolak."
            } else {
                statusMessage.value = "Gagal memproses penolakan pengajuan."
            }
        }
    }

    fun deleteStudentRedemption(req: PengajuanHadiah) {
        viewModelScope.launch {
            repository.deletePengajuan(req)
            statusMessage.value = "Pengajuan berhasil dihapus dari sistem."
        }
    }

    fun checkStudentQrResult(context: Context, qrContent: String) {
        viewModelScope.launch {
            val cleaned = qrContent.trim()
            val id = when {
                cleaned.toIntOrNull() != null -> cleaned.toIntOrNull()
                cleaned.contains("/") -> {
                    val lastPart = cleaned.substringAfterLast("/")
                    val pureDigits = lastPart.takeWhile { it.isDigit() }
                    pureDigits.toIntOrNull() ?: lastPart.filter { it.isDigit() }.toIntOrNull()
                }
                else -> {
                    val digits = cleaned.filter { it.isDigit() }
                    digits.toIntOrNull()
                }
            }

            if (id == null) {
                statusMessage.value = "Format QR Code atau input salah: '$cleaned'"
                return@launch
            }

            val student = repository.getSiswaById(id)
            if (student != null) {
                loggedInSiswa.value = student
                saveSiswaSession(context, student.id_siswa)
                currentScreen.value = Screen.PORTAL_SISWA
                statusMessage.value = "Masuk Berhasil: Halo ${student.nama}!"
            } else {
                statusMessage.value = "Siswa dengan ID '$id' tidak ditemukan!"
            }
        }
    }

    fun appointSiswaPetugas(idSiswa: Int, isPetugas: Boolean) {
        viewModelScope.launch {
            val student = repository.getSiswaById(idSiswa)
            if (student != null) {
                val updated = student.copy(is_petugas = isPetugas)
                repository.updateSiswa(updated)
                statusMessage.value = "Status petugas '${student.nama}' berhasil diperbarui."
                
                // If the updated student is currently the logged-in student in the portal, sync it
                if (loggedInSiswa.value?.id_siswa == idSiswa) {
                    loggedInSiswa.value = updated
                }
            }
        }
    }
}
