package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Admin
import com.example.data.model.KategoriAktivitas
import com.example.data.model.PengaturanSekolah
import com.example.data.model.RiwayatPoin
import com.example.data.model.Siswa
import com.example.data.model.Hadiah
import com.example.data.model.PengajuanHadiah

@Database(
    entities = [
        Admin::class,
        Siswa::class,
        KategoriAktivitas::class,
        RiwayatPoin::class,
        PengaturanSekolah::class,
        Hadiah::class,
        PengajuanHadiah::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun adminDao(): AdminDao
    abstract fun siswaDao(): SiswaDao
    abstract fun kategoriAktivitasDao(): KategoriAktivitasDao
    abstract fun riwayatPoinDao(): RiwayatPoinDao
    abstract fun pengaturanSekolahDao(): PengaturanSekolahDao
    abstract fun hadiahDao(): HadiahDao
    abstract fun pengajuanHadiahDao(): PengajuanHadiahDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poin_karakter_siswa.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
