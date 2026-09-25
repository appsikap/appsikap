package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin")
data class Admin(
    @PrimaryKey(autoGenerate = true) val id_admin: Int = 0,
    val username: String,
    val password: String,
    val nama_admin: String
)

@Entity(tableName = "siswa")
data class Siswa(
    @PrimaryKey(autoGenerate = true) val id_siswa: Int = 0,
    val nama: String,
    val kelas: String,
    val jenis_kelamin: String, // "Laki-laki" atau "Perempuan"
    val saldo_poin: Int = 100, // Default saldo poin awal adalah 100 (Sangat Baik)
    val is_petugas: Boolean = false
)

@Entity(tableName = "kategori_aktivitas")
data class KategoriAktivitas(
    @PrimaryKey(autoGenerate = true) val id_kategori: Int = 0,
    val nama_kategori: String,
    val jenis_kategori: String, // "Perbuatan Baik" atau "Pelanggaran"
    val nilai_poin: Int, // nilai positif untuk perbuatan baik, nilai negatif untuk pelanggaran (atau simpan nilai positif absolut dan sesuaikan saat hitung)
    val keterangan: String = ""
)

@Entity(tableName = "riwayat_poin")
data class RiwayatPoin(
    @PrimaryKey(autoGenerate = true) val id_riwayat: Int = 0,
    val id_siswa: Int,
    val id_kategori: Int,
    val jenis_catatan: String, // "Perbuatan Baik" atau "Pelanggaran"
    val nilai_poin: Int, // e.g. +10 atau -5
    val tanggal: Long, // timestamp
    val guru_pencatat: String,
    val keterangan: String = ""
)

@Entity(tableName = "pengaturan_sekolah")
data class PengaturanSekolah(
    @PrimaryKey val id_pengaturan: Int = 1, // Hanya ada 1 record pengaturan
    val nama_sekolah: String = "SD/SMP/SMA Negeri 1",
    val logo_sekolah: String = "",
    val tahun_pelajaran: String = "2025/2026",
    val nama_kepala_sekolah: String = "Kepala Sekolah, M.Pd.",
    val batas_sangat_baik: Int = 100,
    val batas_baik: Int = 70,
    val batas_pembinaan: Int = 40,
    val batas_peringatan: Int = 0,
    val aktifkan_tukar_hadiah: Boolean = true,
    val poin_awal_siswa: Int = 100,
    val nama_guru_ttd: String = "Achmad, S.Pd.",
    val peran_guru_ttd: String = "Guru Kelas"
)

@Entity(tableName = "hadiah")
data class Hadiah(
    @PrimaryKey(autoGenerate = true) val id_hadiah: Int = 0,
    val nama_hadiah: String,
    val poin_dibutuhkan: Int,
    val stok_hadiah: Int = 10,
    val keterangan: String = ""
)

@Entity(tableName = "pengajuan_hadiah")
data class PengajuanHadiah(
    @PrimaryKey(autoGenerate = true) val id_pengajuan: Int = 0,
    val id_siswa: Int,
    val id_hadiah: Int,
    val nama_siswa: String,
    val nama_hadiah: String,
    val tanggal_pengajuan: Long,
    val status: String, // "MENUNGGU", "DISETUJUI", "DITOLAK"
    val keterangan: String = ""
)
