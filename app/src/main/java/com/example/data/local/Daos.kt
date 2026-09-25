package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Admin
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import com.example.data.model.Hadiah
import com.example.data.model.PengajuanHadiah
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admin WHERE username = :username LIMIT 1")
    suspend fun getAdminByUsername(username: String): Admin?

    @Query("SELECT COUNT(*) FROM admin")
    suspend fun getAdminCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: Admin): Long

    @Update
    suspend fun updateAdmin(admin: Admin): Int
}

@Dao
interface SiswaDao {
    @Query("SELECT * FROM siswa ORDER BY nama ASC")
    fun getAllSiswaFlow(): Flow<List<Siswa>>

    @Query("SELECT * FROM siswa ORDER BY nama ASC")
    suspend fun getAllSiswaList(): List<Siswa>

    @Query("SELECT * FROM siswa WHERE id_siswa = :id LIMIT 1")
    fun getSiswaByIdFlow(id: Int): Flow<Siswa?>

    @Query("SELECT * FROM siswa WHERE id_siswa = :id LIMIT 1")
    suspend fun getSiswaById(id: Int): Siswa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiswa(siswa: Siswa): Long

    @Update
    suspend fun updateSiswa(siswa: Siswa): Int

    @Delete
    suspend fun deleteSiswa(siswa: Siswa): Int

    @Query("SELECT COUNT(*) FROM siswa")
    fun getSiswaCountFlow(): Flow<Int>
}

@Dao
interface KategoriAktivitasDao {
    @Query("SELECT * FROM kategori_aktivitas ORDER BY nama_kategori ASC")
    fun getAllKategoriFlow(): Flow<List<KategoriAktivitas>>

    @Query("SELECT * FROM kategori_aktivitas ORDER BY nama_kategori ASC")
    suspend fun getAllKategoriList(): List<KategoriAktivitas>

    @Query("SELECT * FROM kategori_aktivitas WHERE id_kategori = :id LIMIT 1")
    suspend fun getKategoriById(id: Int): KategoriAktivitas?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKategori(kategori: KategoriAktivitas): Long

    @Update
    suspend fun updateKategori(kategori: KategoriAktivitas): Int

    @Delete
    suspend fun deleteKategori(kategori: KategoriAktivitas): Int
}

@Dao
interface RiwayatPoinDao {
    @Query("SELECT * FROM riwayat_poin ORDER BY tanggal DESC")
    fun getAllRiwayatFlow(): Flow<List<RiwayatPoin>>

    @Query("SELECT * FROM riwayat_poin ORDER BY tanggal DESC")
    suspend fun getAllRiwayatList(): List<RiwayatPoin>

    @Query("SELECT * FROM riwayat_poin WHERE id_siswa = :idSiswa ORDER BY tanggal DESC")
    fun getRiwayatForSiswaFlow(idSiswa: Int): Flow<List<RiwayatPoin>>

    @Query("SELECT * FROM riwayat_poin WHERE id_siswa = :idSiswa ORDER BY tanggal DESC")
    suspend fun getRiwayatForSiswaList(idSiswa: Int): List<RiwayatPoin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiwayat(riwayat: RiwayatPoin): Long

    @Update
    suspend fun updateRiwayat(riwayat: RiwayatPoin): Int

    @Delete
    suspend fun deleteRiwayat(riwayat: RiwayatPoin): Int
}

@Dao
interface PengaturanSekolahDao {
    @Query("SELECT * FROM pengaturan_sekolah WHERE id_pengaturan = 1 LIMIT 1")
    fun getPengaturanFlow(): Flow<PengaturanSekolah?>

    @Query("SELECT * FROM pengaturan_sekolah WHERE id_pengaturan = 1 LIMIT 1")
    suspend fun getPengaturan(): PengaturanSekolah?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePengaturan(pengaturan: PengaturanSekolah): Long
}

@Dao
interface HadiahDao {
    @Query("SELECT * FROM hadiah ORDER BY nama_hadiah ASC")
    fun getAllHadiahFlow(): Flow<List<Hadiah>>

    @Query("SELECT * FROM hadiah ORDER BY nama_hadiah ASC")
    suspend fun getAllHadiahList(): List<Hadiah>

    @Query("SELECT * FROM hadiah WHERE id_hadiah = :id LIMIT 1")
    suspend fun getHadiahById(id: Int): Hadiah?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiah(hadiah: Hadiah): Long

    @Update
    suspend fun updateHadiah(hadiah: Hadiah): Int

    @Delete
    suspend fun deleteHadiah(hadiah: Hadiah): Int
}

@Dao
interface PengajuanHadiahDao {
    @Query("SELECT * FROM pengajuan_hadiah ORDER BY tanggal_pengajuan DESC")
    fun getAllPengajuanFlow(): Flow<List<PengajuanHadiah>>

    @Query("SELECT * FROM pengajuan_hadiah ORDER BY tanggal_pengajuan DESC")
    suspend fun getAllPengajuanList(): List<PengajuanHadiah>

    @Query("SELECT * FROM pengajuan_hadiah WHERE id_siswa = :idSiswa ORDER BY tanggal_pengajuan DESC")
    fun getPengajuanForSiswaFlow(idSiswa: Int): Flow<List<PengajuanHadiah>>

    @Query("SELECT * FROM pengajuan_hadiah WHERE status = 'MENUNGGU' ORDER BY tanggal_pengajuan DESC")
    fun getPendingPengajuanFlow(): Flow<List<PengajuanHadiah>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengajuan(pengajuan: PengajuanHadiah): Long

    @Update
    suspend fun updatePengajuan(pengajuan: PengajuanHadiah): Int

    @Delete
    suspend fun deletePengajuan(pengajuan: PengajuanHadiah): Int
}
