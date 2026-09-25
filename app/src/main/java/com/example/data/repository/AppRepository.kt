package com.example.data.repository

import com.example.data.local.AdminDao
import com.example.data.local.KategoriAktivitasDao
import com.example.data.local.PengaturanSekolahDao
import com.example.data.local.RiwayatPoinDao
import com.example.data.local.SiswaDao
import com.example.data.local.HadiahDao
import com.example.data.local.PengajuanHadiahDao
import com.example.data.model.Admin
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import com.example.data.model.Hadiah
import com.example.data.model.PengajuanHadiah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(
    private val adminDao: AdminDao,
    private val siswaDao: SiswaDao,
    private val kategoriDao: KategoriAktivitasDao,
    private val riwayatDao: RiwayatPoinDao,
    private val pengaturanDao: PengaturanSekolahDao,
    private val hadiahDao: HadiahDao,
    private val pengajuanHadiahDao: PengajuanHadiahDao
) {

    // --- SEED DATABASE ---
    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        // 1. Seed Admin if Empty
        if (adminDao.getAdminCount() == 0) {
            adminDao.insertAdmin(
                Admin(
                    username = "admin",
                    password = "admin",
                    nama_admin = "Guru Pengurus"
                )
            )
        }

        // 2. Seed Pengaturan Sekolah if Empty
        if (pengaturanDao.getPengaturan() == null) {
            pengaturanDao.insertOrUpdatePengaturan(
                PengaturanSekolah(
                    nama_sekolah = "SD/SMP/SMA Negeri 1",
                    logo_sekolah = "",
                    tahun_pelajaran = "2025/2026",
                    nama_kepala_sekolah = "Drs. H. Mulyadi, M.Pd.",
                    poin_awal_siswa = 100,
                    nama_guru_ttd = "Achmad, S.Pd.",
                    peran_guru_ttd = "Guru Kelas"
                )
            )
        }

        // 3. Seed Kategori if Empty
        if (kategoriDao.getAllKategoriList().isEmpty()) {
            val defaultKategori = listOf(
                // Perbuatan Baik
                KategoriAktivitas(
                    nama_kategori = "Membantu teman",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 10,
                    keterangan = "Membantu teman yang kesulitan memahami pelajaran atau sedang tertimpa musibah"
                ),
                KategoriAktivitas(
                    nama_kategori = "Menjaga kebersihan kelas",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 10,
                    keterangan = "Melaksanakan piket kelas atau membuang sampah secara sukarela"
                ),
                KategoriAktivitas(
                    nama_kategori = "Menjadi petugas upacara",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 15,
                    keterangan = "Menjadi petugas upacara bendera hari Senin atau hari besar nasional"
                ),
                KategoriAktivitas(
                    nama_kategori = "Jujur mengembalikan barang temuan",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 25,
                    keterangan = "Menyerahkan barang milik orang lain yang ditemukan kepada guru atau piket"
                ),
                KategoriAktivitas(
                    nama_kategori = "Aktif dalam kegiatan sekolah",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 20,
                    keterangan = "Terlibat aktif sebagai pengurus OSIS, panitia acara, atau ekstrakurikuler"
                ),
                KategoriAktivitas(
                    nama_kategori = "Berprestasi di kelas/sekolah",
                    jenis_kategori = "Perbuatan Baik",
                    nilai_poin = 30,
                    keterangan = "Mendapat juara kelas, menjuarai lomba akademik maupun non-akademik"
                ),

                // Pelanggaran
                KategoriAktivitas(
                    nama_kategori = "Terlambat masuk sekolah",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -5,
                    keterangan = "Tiba di sekolah setelah bel masuk berbunyi tanpa alasan yang sah"
                ),
                KategoriAktivitas(
                    nama_kategori = "Tidak memakai atribut lengkap",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -10,
                    keterangan = "Tidak mengenakan dasi, ikat pinggang, topi, atau kaus kaki sesuai ketentuan"
                ),
                KategoriAktivitas(
                    nama_kategori = "Tidak mengerjakan tugas",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -10,
                    keterangan = "Tidak mengumpulkan PR atau tugas sekolah pada waktu yang ditentukan"
                ),
                KategoriAktivitas(
                    nama_kategori = "Membolos",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -25,
                    keterangan = "Meninggalkan jam pelajaran atau lingkungan sekolah tanpa izin guru piket"
                ),
                KategoriAktivitas(
                    nama_kategori = "Berkata kasar",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -20,
                    keterangan = "Mengucapkan kata-kata kotor, merundung (bullying), atau mengejek warga sekolah"
                ),
                KategoriAktivitas(
                    nama_kategori = "Berkelahi",
                    jenis_kategori = "Pelanggaran",
                    nilai_poin = -50,
                    keterangan = "Terlibat perselisihan fisik atau memicu keributan di dalam maupun luar sekolah"
                )
            )
            for (kat in defaultKategori) {
                kategoriDao.insertKategori(kat)
            }
        }

        // 4. Seed Hadiah if Empty
        if (hadiahDao.getAllHadiahList().isEmpty()) {
            val defaultHadiah = listOf(
                Hadiah(nama_hadiah = "Stiker Keren Terpuji", poin_dibutuhkan = 10, stok_hadiah = 50, keterangan = "Stiker hologram motivasi karakter baik"),
                Hadiah(nama_hadiah = "Pensil & Penghapus Karakter", poin_dibutuhkan = 20, stok_hadiah = 30, keterangan = "Satu set alat tulis bertema edukasi mulia"),
                Hadiah(nama_hadiah = "Buku Catatan Diary Pintar", poin_dibutuhkan = 30, stok_hadiah = 25, keterangan = "Buku tulis bergaris tebal untuk merangkum budi pekerti"),
                Hadiah(nama_hadiah = "Botol Minum BPA-Free (Tumbler)", poin_dibutuhkan = 60, stok_hadiah = 15, keterangan = "Botol minum higienis ramah lingkungan"),
                Hadiah(nama_hadiah = "Kotak Bekal Makan Bersekat", poin_dibutuhkan = 80, stok_hadiah = 12, keterangan = "Kotak makan bersekat kuat kancing rapat"),
                Hadiah(nama_hadiah = "Tas Ransel Sekolah Keren", poin_dibutuhkan = 150, stok_hadiah = 5, keterangan = "Tas punggung sekolah bermutu tinggi dan awet")
            )
            for (hd in defaultHadiah) {
                hadiahDao.insertHadiah(hd)
            }
        }
    }

    // --- ADMIN ---
    suspend fun getAdminByUsername(username: String): Admin? = withContext(Dispatchers.IO) {
        adminDao.getAdminByUsername(username)
    }
    suspend fun insertAdmin(admin: Admin): Long = withContext(Dispatchers.IO) {
        adminDao.insertAdmin(admin)
    }
    suspend fun updateAdmin(admin: Admin): Int = withContext(Dispatchers.IO) {
        adminDao.updateAdmin(admin)
    }

    // --- SISWA ---
    val allSiswaFlow: Flow<List<Siswa>> = siswaDao.getAllSiswaFlow()
    val siswaCountFlow: Flow<Int> = siswaDao.getSiswaCountFlow()

    suspend fun getAllSiswaList(): List<Siswa> = withContext(Dispatchers.IO) {
        siswaDao.getAllSiswaList()
    }
    fun getSiswaByIdFlow(id: Int): Flow<Siswa?> = siswaDao.getSiswaByIdFlow(id)
    suspend fun getSiswaById(id: Int): Siswa? = withContext(Dispatchers.IO) {
        siswaDao.getSiswaById(id)
    }
    suspend fun insertSiswa(siswa: Siswa): Long = withContext(Dispatchers.IO) {
        siswaDao.insertSiswa(siswa)
    }
    suspend fun updateSiswa(siswa: Siswa): Int = withContext(Dispatchers.IO) {
        siswaDao.updateSiswa(siswa)
    }
    suspend fun deleteSiswa(siswa: Siswa): Int = withContext(Dispatchers.IO) {
        siswaDao.deleteSiswa(siswa)
    }

    // --- KATEGORI AKTIVITAS ---
    val allKategoriFlow: Flow<List<KategoriAktivitas>> = kategoriDao.getAllKategoriFlow()

    suspend fun getAllKategoriList(): List<KategoriAktivitas> = withContext(Dispatchers.IO) {
        kategoriDao.getAllKategoriList()
    }
    suspend fun getKategoriById(id: Int): KategoriAktivitas? = withContext(Dispatchers.IO) {
        kategoriDao.getKategoriById(id)
    }
    suspend fun insertKategori(kategori: KategoriAktivitas): Long = withContext(Dispatchers.IO) {
        kategoriDao.insertKategori(kategori)
    }
    suspend fun updateKategori(kategori: KategoriAktivitas): Int = withContext(Dispatchers.IO) {
        kategoriDao.updateKategori(kategori)
    }
    suspend fun deleteKategori(kategori: KategoriAktivitas): Int = withContext(Dispatchers.IO) {
        kategoriDao.deleteKategori(kategori)
    }

    // --- RIWAYAT POIN ---
    val allRiwayatFlow: Flow<List<RiwayatPoin>> = riwayatDao.getAllRiwayatFlow()

    suspend fun getAllRiwayatList(): List<RiwayatPoin> = withContext(Dispatchers.IO) {
        riwayatDao.getAllRiwayatList()
    }
    fun getRiwayatForSiswaFlow(idSiswa: Int): Flow<List<RiwayatPoin>> = riwayatDao.getRiwayatForSiswaFlow(idSiswa)
    suspend fun getRiwayatForSiswaList(idSiswa: Int): List<RiwayatPoin> = withContext(Dispatchers.IO) {
        riwayatDao.getRiwayatForSiswaList(idSiswa)
    }

    // Record a points event and update the student's current saldo in transactional-style logic
    suspend fun recordRiwayatAndUpdateSiswa(riwayat: RiwayatPoin): Boolean = withContext(Dispatchers.IO) {
        val siswa = siswaDao.getSiswaById(riwayat.id_siswa) ?: return@withContext false
        riwayatDao.insertRiwayat(riwayat)
        val newSaldo = siswa.saldo_poin + riwayat.nilai_poin
        siswaDao.updateSiswa(siswa.copy(saldo_poin = newSaldo))
        true
    }

    // Delete a points event and restore the student's previous saldo
    suspend fun deleteRiwayatAndRevertSiswa(riwayat: RiwayatPoin): Boolean = withContext(Dispatchers.IO) {
        val siswa = siswaDao.getSiswaById(riwayat.id_siswa) ?: return@withContext false
        riwayatDao.deleteRiwayat(riwayat)
        val newSaldo = siswa.saldo_poin - riwayat.nilai_poin
        siswaDao.updateSiswa(siswa.copy(saldo_poin = newSaldo))
        true
    }

    // --- PENGATURAN SEKOLAH ---
    val pengaturanFlow: Flow<PengaturanSekolah?> = pengaturanDao.getPengaturanFlow()

    suspend fun getPengaturan(): PengaturanSekolah? = withContext(Dispatchers.IO) {
        pengaturanDao.getPengaturan()
    }
    suspend fun saveOrUpdatePengaturan(pengaturan: PengaturanSekolah): Long = withContext(Dispatchers.IO) {
        pengaturanDao.insertOrUpdatePengaturan(pengaturan)
    }

    // --- HADIAH CRUD ---
    val allHadiahFlow: Flow<List<Hadiah>> = hadiahDao.getAllHadiahFlow()

    suspend fun getAllHadiahList(): List<Hadiah> = withContext(Dispatchers.IO) {
        hadiahDao.getAllHadiahList()
    }
    suspend fun getHadiahById(id: Int): Hadiah? = withContext(Dispatchers.IO) {
        hadiahDao.getHadiahById(id)
    }
    suspend fun insertHadiah(hadiah: Hadiah): Long = withContext(Dispatchers.IO) {
        hadiahDao.insertHadiah(hadiah)
    }
    suspend fun updateHadiah(hadiah: Hadiah): Int = withContext(Dispatchers.IO) {
        hadiahDao.updateHadiah(hadiah)
    }
    suspend fun deleteHadiah(hadiah: Hadiah): Int = withContext(Dispatchers.IO) {
        hadiahDao.deleteHadiah(hadiah)
    }

    // --- PENGAJUAN HADIAH ---
    val allPengajuanFlow: Flow<List<PengajuanHadiah>> = pengajuanHadiahDao.getAllPengajuanFlow()
    val pendingPengajuanFlow: Flow<List<PengajuanHadiah>> = pengajuanHadiahDao.getPendingPengajuanFlow()

    fun getPengajuanForSiswaFlow(idSiswa: Int): Flow<List<PengajuanHadiah>> = pengajuanHadiahDao.getPengajuanForSiswaFlow(idSiswa)

    suspend fun insertPengajuan(pengajuan: PengajuanHadiah): Long = withContext(Dispatchers.IO) {
        pengajuanHadiahDao.insertPengajuan(pengajuan)
    }

    suspend fun approvePengajuan(idPengajuan: Int, adminName: String): Boolean = withContext(Dispatchers.IO) {
        val list = pengajuanHadiahDao.getAllPengajuanList()
        val p = list.firstOrNull { it.id_pengajuan == idPengajuan } ?: return@withContext false
        if (p.status != "MENUNGGU") return@withContext false

        val siswa = siswaDao.getSiswaById(p.id_siswa) ?: return@withContext false
        val h = hadiahDao.getHadiahById(p.id_hadiah) ?: return@withContext false

        if (siswa.saldo_poin < h.poin_dibutuhkan) return@withContext false
        if (h.stok_hadiah <= 0) return@withContext false

        // 1. Deduct points and update student
        val newSaldo = siswa.saldo_poin - h.poin_dibutuhkan
        siswaDao.updateSiswa(siswa.copy(saldo_poin = newSaldo))

        // 2. Decrement prize stock
        hadiahDao.updateHadiah(h.copy(stok_hadiah = h.stok_hadiah - 1))

        // 3. Insert into points history for logging
        riwayatDao.insertRiwayat(
            RiwayatPoin(
                id_siswa = p.id_siswa,
                id_kategori = 0, // 0 for special redemption
                jenis_catatan = "Penukaran Hadiah",
                nilai_poin = -h.poin_dibutuhkan,
                tanggal = System.currentTimeMillis(),
                guru_pencatat = adminName,
                keterangan = "Sukses menukar dengan: ${h.nama_hadiah}"
            )
        )

        // 4. Update request status
        pengajuanHadiahDao.updatePengajuan(p.copy(status = "DISETUJUI"))
        true
    }

    suspend fun rejectPengajuan(idPengajuan: Int, keteranganTolak: String): Boolean = withContext(Dispatchers.IO) {
        val list = pengajuanHadiahDao.getAllPengajuanList()
        val p = list.firstOrNull { it.id_pengajuan == idPengajuan } ?: return@withContext false
        if (p.status != "MENUNGGU") return@withContext false

        pengajuanHadiahDao.updatePengajuan(p.copy(status = "DITOLAK", keterangan = keteranganTolak))
        true
    }

    suspend fun deletePengajuan(pengajuan: PengajuanHadiah): Int = withContext(Dispatchers.IO) {
        pengajuanHadiahDao.deletePengajuan(pengajuan)
    }
}
