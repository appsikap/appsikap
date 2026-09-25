// Konfigurasi Supabase
const SUPABASE_URL = 'https://mixuhkdsejhrcawahmjs.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1peHVoa2RzZWpocmNhd2FobWpzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAyODA0MTksImV4cCI6MjEwNTg1NjQxOX0.DRV-AGabYOmIlSRlEB5Rt72zIqVCVbNDauLzYCwky2o';

// Inisialisasi Supabase Client
const supabase = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
