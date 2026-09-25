package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AdminDao
import com.example.data.local.HadiahDao
import com.example.data.local.KategoriAktivitasDao
import com.example.data.local.PengajuanHadiahDao
import com.example.data.local.PengaturanSekolahDao
import com.example.data.local.RiwayatPoinDao
import com.example.data.local.SiswaDao
import com.example.data.model.Admin
import com.example.data.model.Hadiah
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengajuanHadiah
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupRepository(
    private val adminDao: AdminDao,
    private val siswaDao: SiswaDao,
    private val kategoriDao: KategoriAktivitasDao,
    private val riwayatDao: RiwayatPoinDao,
    private val pengaturanDao: PengaturanSekolahDao,
    private val hadiahDao: HadiahDao,
    private val pengajuanHadiahDao: PengajuanHadiahDao
) {

    suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        val rootJson = JSONObject()
        rootJson.put("app", "SIKAP")
        rootJson.put("version", 1)
        rootJson.put("exportTimestamp", System.currentTimeMillis())

        // 1. Siswa
        val siswaList = siswaDao.getAllSiswaList()
        val siswaArray = JSONArray()
        for (s in siswaList) {
            val sj = JSONObject()
            sj.put("id_siswa", s.id_siswa)
            sj.put("nama", s.nama)
            sj.put("kelas", s.kelas)
            sj.put("jenis_kelamin", s.jenis_kelamin)
            sj.put("saldo_poin", s.saldo_poin)
            sj.put("is_petugas", s.is_petugas)
            siswaArray.put(sj)
        }
        rootJson.put("siswas", siswaArray)
        rootJson.put("siswa", siswaArray)

        // 2. Kategori Aktivitas
        val kategoriList = kategoriDao.getAllKategoriList()
        val katArray = JSONArray()
        for (k in kategoriList) {
            val kj = JSONObject()
            kj.put("id_kategori", k.id_kategori)
            kj.put("nama_kategori", k.nama_kategori)
            kj.put("jenis_kategori", k.jenis_kategori)
            kj.put("nilai_poin", k.nilai_poin)
            kj.put("keterangan", k.keterangan)
            katArray.put(kj)
        }
        rootJson.put("kategoris", katArray)
        rootJson.put("kategoriAktivitas", katArray)

        // 3. Riwayat Poin
        val riwayatList = riwayatDao.getAllRiwayatList()
        val riwayatArray = JSONArray()
        for (r in riwayatList) {
            val rj = JSONObject()
            rj.put("id_riwayat", r.id_riwayat)
            rj.put("id_siswa", r.id_siswa)
            rj.put("id_kategori", r.id_kategori)
            rj.put("jenis_catatan", r.jenis_catatan)
            rj.put("nilai_poin", r.nilai_poin)
            rj.put("tanggal", r.tanggal)
            rj.put("guru_pencatat", r.guru_pencatat)
            rj.put("keterangan", r.keterangan)
            riwayatArray.put(rj)
        }
        rootJson.put("riwayats", riwayatArray)
        rootJson.put("riwayatPoin", riwayatArray)

        // 4. Pengaturan Sekolah
        val peng = pengaturanDao.getPengaturan() ?: PengaturanSekolah()
        val pj = JSONObject()
        pj.put("id_pengaturan", peng.id_pengaturan)
        pj.put("nama_sekolah", peng.nama_sekolah)
        pj.put("logo_sekolah", peng.logo_sekolah)
        pj.put("tahun_pelajaran", peng.tahun_pelajaran)
        pj.put("nama_kepala_sekolah", peng.nama_kepala_sekolah)
        pj.put("batas_sangat_baik", peng.batas_sangat_baik)
        pj.put("batas_baik", peng.batas_baik)
        pj.put("batas_pembinaan", peng.batas_pembinaan)
        pj.put("batas_peringatan", peng.batas_peringatan)
        pj.put("aktifkan_tukar_hadiah", peng.aktifkan_tukar_hadiah)
        pj.put("poin_awal_siswa", peng.poin_awal_siswa)
        pj.put("nama_guru_ttd", peng.nama_guru_ttd)
        pj.put("peran_guru_ttd", peng.peran_guru_ttd)
        rootJson.put("pengaturan", pj)
        rootJson.put("pengaturanSekolah", pj)

        // 5. Hadiah
        val hadiahList = hadiahDao.getAllHadiahList()
        val hadiahArray = JSONArray()
        for (h in hadiahList) {
            val hj = JSONObject()
            hj.put("id_hadiah", h.id_hadiah)
            hj.put("nama_hadiah", h.nama_hadiah)
            hj.put("poin_dibutuhkan", h.poin_dibutuhkan)
            hj.put("stok_hadiah", h.stok_hadiah)
            hj.put("keterangan", h.keterangan)
            hadiahArray.put(hj)
        }
        rootJson.put("hadiah", hadiahArray)

        // 6. Pengajuan Hadiah
        val pengajuanList = pengajuanHadiahDao.getAllPengajuanList()
        val pengajuanArray = JSONArray()
        for (p in pengajuanList) {
            val paj = JSONObject()
            paj.put("id_pengajuan", p.id_pengajuan)
            paj.put("id_siswa", p.id_siswa)
            paj.put("id_hadiah", p.id_hadiah)
            paj.put("nama_siswa", p.nama_siswa)
            paj.put("nama_hadiah", p.nama_hadiah)
            paj.put("tanggal_pengajuan", p.tanggal_pengajuan)
            paj.put("status", p.status)
            paj.put("keterangan", p.keterangan)
            pengajuanArray.put(paj)
        }
        rootJson.put("pengajuanHadiah", pengajuanArray)

        rootJson.toString(2)
    }

    suspend fun importFromJsonString(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject(jsonString)

            // 1. Siswa
            val siswaArray = when {
                rootJson.has("siswas") -> rootJson.getJSONArray("siswas")
                rootJson.has("siswa") -> rootJson.getJSONArray("siswa")
                else -> null
            }
            if (siswaArray != null) {
                for (i in 0 until siswaArray.length()) {
                    val obj = siswaArray.getJSONObject(i)
                    val s = Siswa(
                        id_siswa = obj.optInt("id_siswa", 0),
                        nama = obj.optString("nama", ""),
                        kelas = obj.optString("kelas", ""),
                        jenis_kelamin = obj.optString("jenis_kelamin", "Laki-laki"),
                        saldo_poin = obj.optInt("saldo_poin", 100),
                        is_petugas = obj.optBoolean("is_petugas", false)
                    )
                    siswaDao.insertSiswa(s)
                }
            }

            // 2. Kategori Aktivitas
            val katArray = when {
                rootJson.has("kategoris") -> rootJson.getJSONArray("kategoris")
                rootJson.has("kategoriAktivitas") -> rootJson.getJSONArray("kategoriAktivitas")
                else -> null
            }
            if (katArray != null) {
                for (i in 0 until katArray.length()) {
                    val obj = katArray.getJSONObject(i)
                    val k = KategoriAktivitas(
                        id_kategori = obj.optInt("id_kategori", 0),
                        nama_kategori = obj.optString("nama_kategori", ""),
                        jenis_kategori = obj.optString("jenis_kategori", "Perbuatan Baik"),
                        nilai_poin = obj.optInt("nilai_poin", 10),
                        keterangan = obj.optString("keterangan", "")
                    )
                    kategoriDao.insertKategori(k)
                }
            }

            // 3. Riwayat Poin
            val riwayatArray = when {
                rootJson.has("riwayats") -> rootJson.getJSONArray("riwayats")
                rootJson.has("riwayatPoin") -> rootJson.getJSONArray("riwayatPoin")
                else -> null
            }
            if (riwayatArray != null) {
                for (i in 0 until riwayatArray.length()) {
                    val obj = riwayatArray.getJSONObject(i)
                    val r = RiwayatPoin(
                        id_riwayat = obj.optInt("id_riwayat", 0),
                        id_siswa = obj.optInt("id_siswa", 0),
                        id_kategori = obj.optInt("id_kategori", 0),
                        jenis_catatan = obj.optString("jenis_catatan", "Perbuatan Baik"),
                        nilai_poin = obj.optInt("nilai_poin", 0),
                        tanggal = obj.optLong("tanggal", System.currentTimeMillis()),
                        guru_pencatat = obj.optString("guru_pencatat", "Guru"),
                        keterangan = obj.optString("keterangan", "")
                    )
                    riwayatDao.insertRiwayat(r)
                }
            }

            // 4. Pengaturan Sekolah
            val pengObj = when {
                rootJson.has("pengaturan") -> rootJson.optJSONObject("pengaturan")
                rootJson.has("pengaturanSekolah") -> rootJson.optJSONObject("pengaturanSekolah")
                else -> null
            }
            if (pengObj != null) {
                val peng = PengaturanSekolah(
                    id_pengaturan = 1,
                    nama_sekolah = pengObj.optString("nama_sekolah", "SD/SMP/SMA Negeri 1"),
                    logo_sekolah = pengObj.optString("logo_sekolah", ""),
                    tahun_pelajaran = pengObj.optString("tahun_pelajaran", "2025/2026"),
                    nama_kepala_sekolah = pengObj.optString("nama_kepala_sekolah", "Kepala Sekolah, M.Pd."),
                    batas_sangat_baik = pengObj.optInt("batas_sangat_baik", 100),
                    batas_baik = pengObj.optInt("batas_baik", 70),
                    batas_pembinaan = pengObj.optInt("batas_pembinaan", 40),
                    batas_peringatan = pengObj.optInt("batas_peringatan", 0),
                    aktifkan_tukar_hadiah = pengObj.optBoolean("aktifkan_tukar_hadiah", true),
                    poin_awal_siswa = pengObj.optInt("poin_awal_siswa", 100),
                    nama_guru_ttd = pengObj.optString("nama_guru_ttd", "Achmad, S.Pd."),
                    peran_guru_ttd = pengObj.optString("peran_guru_ttd", "Guru Kelas")
                )
                pengaturanDao.insertOrUpdatePengaturan(peng)
            }

            // 5. Hadiah
            if (rootJson.has("hadiah")) {
                val array = rootJson.getJSONArray("hadiah")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val h = Hadiah(
                        id_hadiah = obj.optInt("id_hadiah", 0),
                        nama_hadiah = obj.optString("nama_hadiah", ""),
                        poin_dibutuhkan = obj.optInt("poin_dibutuhkan", 10),
                        stok_hadiah = obj.optInt("stok_hadiah", 10),
                        keterangan = obj.optString("keterangan", "")
                    )
                    hadiahDao.insertHadiah(h)
                }
            }

            // 6. Pengajuan Hadiah
            if (rootJson.has("pengajuanHadiah")) {
                val array = rootJson.getJSONArray("pengajuanHadiah")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val p = PengajuanHadiah(
                        id_pengajuan = obj.optInt("id_pengajuan", 0),
                        id_siswa = obj.optInt("id_siswa", 0),
                        id_hadiah = obj.optInt("id_hadiah", 0),
                        nama_siswa = obj.optString("nama_siswa", ""),
                        nama_hadiah = obj.optString("nama_hadiah", ""),
                        tanggal_pengajuan = obj.optLong("tanggal_pengajuan", System.currentTimeMillis()),
                        status = obj.optString("status", "MENUNGGU"),
                        keterangan = obj.optString("keterangan", "")
                    )
                    pengajuanHadiahDao.insertPengajuan(p)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeToUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = exportToJsonString()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readFromUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { isStream ->
                BufferedReader(InputStreamReader(isStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            importFromJsonString(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
