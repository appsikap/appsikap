-- ==========================================
-- SIKAP Supabase Schema V3 (Multi-Role RBAC)
-- ==========================================

-- 1. Membuat tabel profil pengguna untuk menentukan hak akses (Role)
CREATE TABLE IF NOT EXISTS user_roles (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('super_admin', 'guru')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Mengatur RLS agar pengguna hanya bisa membaca profilnya sendiri
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can read own role" ON user_roles
  FOR SELECT USING (auth.uid() = user_id);
-- (Hanya sistem atau Super Admin yang bisa mengubah/menambah role, kita biarkan insert bebas sementara untuk registrasi)
CREATE POLICY "Allow insert during registration" ON user_roles
  FOR INSERT WITH CHECK (auth.uid() = user_id);


-- 2. Memperbarui tabel sikap_datastore
-- Hapus tabel lama (karena arsitekturnya berubah total dari id=1 menjadi user_id)
DROP TABLE IF EXISTS sikap_datastore CASCADE;

CREATE TABLE sikap_datastore (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  data JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE sikap_datastore ENABLE ROW LEVEL SECURITY;
-- Guru bisa membaca, mengubah, dan memasukkan datanya sendiri
CREATE POLICY "Guru can read own datastore" ON sikap_datastore FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Guru can insert own datastore" ON sikap_datastore FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Guru can update own datastore" ON sikap_datastore FOR UPDATE USING (auth.uid() = user_id);


-- 3. Membuat tabel untuk PIN Login Murid
CREATE TABLE IF NOT EXISTS student_access (
  pin TEXT PRIMARY KEY,
  guru_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  id_siswa TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE student_access ENABLE ROW LEVEL SECURITY;
-- Guru bisa mengelola PIN muridnya sendiri
CREATE POLICY "Guru can manage own students pins" ON student_access
  FOR ALL USING (auth.uid() = guru_id);


-- 4. Membuat fungsi aman (Security Definer) untuk login murid menggunakan PIN
-- Fungsi ini mengeksekusi query dengan hak akses admin (Bypass RLS) untuk memberikan 
-- data mentah JSON dari tabel sikap_datastore ke murid, HANYA JIKA PIN tersebut valid.
CREATE OR REPLACE FUNCTION get_student_dashboard(pin_input TEXT)
RETURNS JSONB
SECURITY DEFINER
AS $$
DECLARE
  v_guru_id UUID;
  v_id_siswa TEXT;
  v_data JSONB;
BEGIN
  -- Cari PIN di database
  SELECT guru_id, id_siswa INTO v_guru_id, v_id_siswa FROM student_access WHERE pin = pin_input;
  
  -- Jika PIN tidak ditemukan, kembalikan NULL
  IF v_guru_id IS NULL THEN
    RETURN NULL;
  END IF;
  
  -- Ambil data sekolah dari guru yang bersangkutan
  SELECT data INTO v_data FROM sikap_datastore WHERE user_id = v_guru_id;
  
  -- Kembalikan JSON yang berisi ID Siswa dan seluruh data sekolah agar frontend bisa memfilternya
  RETURN jsonb_build_object(
    'guru_id', v_guru_id,
    'id_siswa', v_id_siswa,
    'data', v_data
  );
END;
$$ LANGUAGE plpgsql;
