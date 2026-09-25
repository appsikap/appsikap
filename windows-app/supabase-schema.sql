-- Skema Database untuk SIKAP Windows (Opsi B: JSON Tunggal)
-- Buka dashboard Supabase Anda -> SQL Editor -> Tempel kueri ini dan jalankan (Run).

CREATE TABLE IF NOT EXISTS sikap_datastore (
  id INT PRIMARY KEY,
  data JSONB NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Masukkan baris pertama sebagai inisialisasi (Data kosong default)
INSERT INTO sikap_datastore (id, data)
VALUES (1, '{}')
ON CONFLICT (id) DO NOTHING;

-- Mengatur kebijakan keamanan (RLS) agar aplikasi bisa membaca dan menulis bebas 
-- (Untuk kemudahan fase awal. Nanti bisa diamankan lebih lanjut jika aplikasi dipakai secara publik).
ALTER TABLE sikap_datastore ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access"
ON sikap_datastore FOR SELECT
USING (true);

CREATE POLICY "Allow public update access"
ON sikap_datastore FOR UPDATE
USING (true);
