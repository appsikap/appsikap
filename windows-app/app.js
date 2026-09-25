/**
 * SIKAP Desktop Edition - Core Application Logic
 * Full JSON Interoperability with Android SIKAP App
 */

// Theme Management
function initTheme() {
  const savedTheme = localStorage.getItem('sikap-theme') || 'dark';
  if (savedTheme === 'light') {
    document.body.classList.add('light-mode');
  }
  updateThemeIcon();
}

function updateThemeIcon() {
  const isLight = document.body.classList.contains('light-mode');
  const icon = document.getElementById('icon-theme');
  if (icon) {
    icon.className = isLight ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
  }
}

function toggleTheme() {
  document.body.classList.toggle('light-mode');
  const isLight = document.body.classList.contains('light-mode');
  localStorage.setItem('sikap-theme', isLight ? 'light' : 'dark');
  updateThemeIcon();
}

// Global State Storage Key
const STORAGE_KEY = 'SIKAP_DESKTOP_DB_V1';

// Default Application State Schema
const defaultState = {
  app: 'SIKAP',
  version: 1,
  exportTimestamp: Date.now(),
  siswa: [
    { id_siswa: 1, nama: 'Ahmad Ridwan', kelas: '5A', jenis_kelamin: 'Laki-laki', saldo_poin: 125, is_petugas: false },
    { id_siswa: 2, nama: 'Siti Aminah', kelas: '5A', jenis_kelamin: 'Perempuan', saldo_poin: 110, is_petugas: false },
    { id_siswa: 3, nama: 'Budi Santoso', kelas: '5B', jenis_kelamin: 'Laki-laki', saldo_poin: 65, is_petugas: false },
    { id_siswa: 4, nama: 'Dewi Lestari', kelas: '6A', jenis_kelamin: 'Perempuan', saldo_poin: 35, is_petugas: false }
  ],
  kategoriAktivitas: [
    { id_kategori: 1, nama_kategori: 'Membantu teman', jenis_kategori: 'Perbuatan Baik', nilai_poin: 10, keterangan: 'Membantu teman yang kesulitan' },
    { id_kategori: 2, nama_kategori: 'Menjaga kebersihan kelas', jenis_kategori: 'Perbuatan Baik', nilai_poin: 10, keterangan: 'Piket atau sapu kelas' },
    { id_kategori: 3, nama_kategori: 'Jujur mengembalikan barang temuan', jenis_kategori: 'Perbuatan Baik', nilai_poin: 25, keterangan: 'Menyerahkan barang hilang ke piket' },
    { id_kategori: 4, nama_kategori: 'Terlambat masuk sekolah', jenis_kategori: 'Pelanggaran', nilai_poin: -5, keterangan: 'Datang lewat dari jam 07:00' },
    { id_kategori: 5, nama_kategori: 'Tidak memakai atribut lengkap', jenis_kategori: 'Pelanggaran', nilai_poin: -10, keterangan: 'Tidak pakai dasi/sabuk' },
    { id_kategori: 6, nama_kategori: 'Membolos jam pelajaran', jenis_kategori: 'Pelanggaran', nilai_poin: -25, keterangan: 'Meninggalkan kelas tanpa izin' }
  ],
  riwayatPoin: [
    { id_riwayat: 1, id_siswa: 1, id_kategori: 3, jenis_catatan: 'Perbuatan Baik', nilai_poin: 25, tanggal: Date.now() - 86400000 * 2, guru_pencatat: 'Achmad, S.Pd.', keterangan: 'Mengembalikan dompet temuan' },
    { id_riwayat: 2, id_siswa: 3, id_kategori: 4, jenis_catatan: 'Pelanggaran', nilai_poin: -5, tanggal: Date.now() - 86400000 * 1, guru_pencatat: 'Guru Piket', keterangan: 'Terlambat 15 menit' },
    { id_riwayat: 3, id_siswa: 4, id_kategori: 6, jenis_catatan: 'Pelanggaran', nilai_poin: -25, tanggal: Date.now() - 3600000 * 4, guru_pencatat: 'Wali Kelas 6A', keterangan: 'Tidak hadir tanpa kabar' }
  ],
  pengaturanSekolah: {
    id_pengaturan: 1,
    nama_sekolah: 'SD Negeri 1 Harapan Bangsa',
    logo_sekolah: '',
    tahun_pelajaran: '2025/2026',
    nama_kepala_sekolah: 'Drs. H. Mulyadi, M.Pd.',
    batas_sangat_baik: 100,
    batas_baik: 70,
    batas_pembinaan: 40,
    batas_peringatan: 0,
    aktifkan_tukar_hadiah: true,
    poin_awal_siswa: 100,
    nama_guru_ttd: 'Achmad, S.Pd.',
    peran_guru_ttd: 'Guru Pembina Karakter'
  },
  hadiah: [
    { id_hadiah: 1, nama_hadiah: 'Stiker Hologram Terpuji', poin_dibutuhkan: 10, stok_hadiah: 50, keterangan: 'Stiker motivasi bintang' },
    { id_hadiah: 2, nama_hadiah: 'Buku Catatan Diary Pintar', poin_dibutuhkan: 30, stok_hadiah: 20, keterangan: 'Buku garis tebal bermutu' },
    { id_hadiah: 3, nama_hadiah: 'Botol Minum BPA Free', poin_dibutuhkan: 60, stok_hadiah: 10, keterangan: 'Tumbler higienis' }
  ],
  pengajuanHadiah: [
    { id_pengajuan: 1, id_siswa: 1, id_hadiah: 2, nama_siswa: 'Ahmad Ridwan', nama_hadiah: 'Buku Catatan Diary Pintar', tanggal_pengajuan: Date.now() - 3600000, status: 'MENUNGGU', keterangan: '' }
  ]
};

// Current Active State
let db = JSON.parse(JSON.stringify(defaultState));

let currentUserId = null;

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
  initTheme();
  setupNavigation();
  setupEventListeners();
  
  // Auth Guard: Check user session
  try {
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (!session || !session.user) {
      window.location.href = 'login.html';
      return;
    }
    currentUserId = session.user.id;

    // Auto-register to user_roles if not exists (for new Google logins)
    supabaseClient.from('user_roles').select('role').eq('user_id', currentUserId).maybeSingle().then(async ({ data }) => {
      if (!data) {
        await supabaseClient.from('user_roles').insert({ user_id: currentUserId, role: 'guru' });
      }
    });
  } catch (e) {
    window.location.href = 'login.html';
    return;
  }

  renderAll();
  
  // Fetch actual data from Supabase for logged-in user
  await loadState();
});

// Load State from Supabase
async function loadState() {
  if (!currentUserId) return;
  try {
    const { data, error } = await supabaseClient
      .from('sikap_datastore')
      .select('data')
      .eq('user_id', currentUserId)
      .maybeSingle();

    if (error) {
      console.error('Error fetching from Supabase:', error);
      const saved = localStorage.getItem(STORAGE_KEY + '_' + currentUserId);
      if (saved) db = normalizeState(JSON.parse(saved));
    } else if (data && data.data && Object.keys(data.data).length > 0) {
      db = normalizeState(data.data);
    } else {
      // First time initialization for new teacher account
      db = JSON.parse(JSON.stringify(defaultState));
      await saveState();
    }
  } catch (e) {
    console.error('Failed to parse state:', e);
    const saved = localStorage.getItem(STORAGE_KEY + '_' + currentUserId);
    if (saved) db = normalizeState(JSON.parse(saved));
  }
  renderAll();
}

// Save State to Supabase
async function saveState() {
  if (!currentUserId) return;
  localStorage.setItem(STORAGE_KEY + '_' + currentUserId, JSON.stringify(db));
  
  try {
    const { error } = await supabaseClient
      .from('sikap_datastore')
      .upsert({ user_id: currentUserId, data: db, updated_at: new Date().toISOString() });
      
    if (error) console.error('Error saving to Supabase:', error);
  } catch (e) {
    console.error('Failed to save state:', e);
  }
  
  renderAll();
}

// Normalize JSON keys for Dual Android/Windows Schema compatibility
function normalizeState(data) {
  return {
    app: data.app || 'SIKAP',
    version: data.version || 1,
    exportTimestamp: data.exportTimestamp || Date.now(),
    siswa: data.siswa || data.siswas || [],
    kategoriAktivitas: data.kategoriAktivitas || data.kategoris || [],
    riwayatPoin: data.riwayatPoin || data.riwayats || [],
    pengaturanSekolah: data.pengaturanSekolah || data.pengaturan || defaultState.pengaturanSekolah,
    hadiah: data.hadiah || [],
    pengajuanHadiah: data.pengajuanHadiah || []
  };
}

// Helper: Calculate Character Status based on thresholds
function getCharacterStatus(poin) {
  const cfg = db.pengaturanSekolah || defaultState.pengaturanSekolah;
  if (poin >= cfg.batas_sangat_baik) return 'Sangat Baik';
  if (poin >= cfg.batas_baik) return 'Baik';
  if (poin >= cfg.batas_pembinaan) return 'Perlu Pembinaan';
  if (poin >= cfg.batas_peringatan) return 'Peringatan';
  return 'Panggilan Ortuk';
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'Sangat Baik': return 'badge-success';
    case 'Baik': return 'badge-info';
    case 'Perlu Pembinaan': return 'badge-warning';
    default: return 'badge-danger';
  }
}

// Formatting Timestamp
function formatDate(ts) {
  if (!ts) return '-';
  const d = new Date(ts);
  return d.toLocaleDateString('id-ID', { day: '2-digit', month: '2-digit', year: 'numeric' }) + ' ' +
         d.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
}

// Render All UI Sections
function renderAll() {
  renderSchoolInfo();
  renderDashboard();
  renderSiswa();
  renderCatatForm();
  renderKategori();
  renderHadiah();
  renderPengaturan();
}

// Render School Header Info
function renderSchoolInfo() {
  const p = db.pengaturanSekolah;
  document.getElementById('display-nama-sekolah').textContent = p.nama_sekolah;
  document.getElementById('display-tahun-pelajaran').textContent = `Tahun: ${p.tahun_pelajaran}`;
}

// Render Dashboard
function renderDashboard() {
  const siswaList = db.siswa || [];
  const riwayatList = db.riwayatPoin || [];
  const cfg = db.pengaturanSekolah;

  // Stats
  document.getElementById('stat-total-siswa').textContent = siswaList.length;

  const baikSum = riwayatList.filter(r => r.jenis_catatan === 'Perbuatan Baik').reduce((acc, r) => acc + Math.abs(r.nilai_poin), 0);
  const burukSum = riwayatList.filter(r => r.jenis_catatan === 'Pelanggaran').reduce((acc, r) => acc + Math.abs(r.nilai_poin), 0);

  document.getElementById('stat-poin-baik').textContent = `+${baikSum}`;
  document.getElementById('stat-poin-buruk').textContent = `-${burukSum}`;

  const sangatBaikCount = siswaList.filter(s => s.saldo_poin >= cfg.batas_sangat_baik).length;
  document.getElementById('stat-sangat-baik').textContent = sangatBaikCount;

  // Leaderboard Top Performers
  const topSiswa = [...siswaList].sort((a, b) => b.saldo_poin - a.saldo_poin).slice(0, 5);
  const topTbody = document.getElementById('top-siswa-tbody');
  if (topSiswa.length === 0) {
    topTbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">Belum ada data murid</td></tr>`;
  } else {
    topTbody.innerHTML = topSiswa.map((s, idx) => `
      <tr>
        <td><strong>#${idx + 1}</strong></td>
        <td><strong>${escapeHtml(s.nama)}</strong></td>
        <td><span class="badge badge-info">${escapeHtml(s.kelas)}</span></td>
        <td><strong class="text-emerald">${s.saldo_poin} Poin</strong></td>
        <td><span class="badge ${getStatusBadgeClass(getCharacterStatus(s.saldo_poin))}">${getCharacterStatus(s.saldo_poin)}</span></td>
      </tr>
    `).join('');
  }

  // Alert Siswa
  const alertSiswa = siswaList.filter(s => s.saldo_poin < cfg.batas_baik);
  const alertTbody = document.getElementById('alert-siswa-tbody');
  if (alertSiswa.length === 0) {
    alertTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted"><i class="fa-solid fa-circle-check text-emerald"></i> Tidak ada murid memerlukan atensi khusus</td></tr>`;
  } else {
    alertTbody.innerHTML = alertSiswa.map(s => `
      <tr>
        <td><strong>${escapeHtml(s.nama)}</strong></td>
        <td>${escapeHtml(s.kelas)}</td>
        <td><strong class="text-rose">${s.saldo_poin} Poin</strong></td>
        <td><span class="badge badge-danger">${getCharacterStatus(s.saldo_poin)}</span></td>
      </tr>
    `).join('');
  }

  // Latest History
  const latestRiwayat = [...riwayatList].sort((a, b) => b.tanggal - a.tanggal).slice(0, 5);
  const latestTbody = document.getElementById('latest-history-tbody');
  if (latestRiwayat.length === 0) {
    latestTbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Belum ada riwayat pencatatan</td></tr>`;
  } else {
    latestTbody.innerHTML = latestRiwayat.map(r => {
      const s = siswaList.find(x => x.id_siswa === r.id_siswa) || { nama: 'Unknown', kelas: '-' };
      const isBaik = r.jenis_catatan === 'Perbuatan Baik';
      return `
        <tr>
          <td><small class="text-muted">${formatDate(r.tanggal)}</small></td>
          <td><strong>${escapeHtml(s.nama)}</strong></td>
          <td>${escapeHtml(s.kelas)}</td>
          <td>${escapeHtml(r.keterangan || r.jenis_catatan)}</td>
          <td><strong class="${isBaik ? 'text-emerald' : 'text-rose'}">${isBaik ? '+' : ''}${r.nilai_poin}</strong></td>
          <td><small>${escapeHtml(r.guru_pencatat)}</small></td>
          <td><small class="text-muted">${escapeHtml(r.keterangan || '-')}</small></td>
        </tr>
      `;
    }).join('');
  }
}

// Render Data Siswa Tab
function renderSiswa() {
  const siswaList = db.siswa || [];
  const search = document.getElementById('siswa-search').value.toLowerCase();
  const filterKelas = document.getElementById('siswa-filter-kelas').value;

  // Update filter options
  const kelasSet = new Set(siswaList.map(s => s.kelas));
  const filterSelect = document.getElementById('siswa-filter-kelas');
  filterSelect.innerHTML = `<option value="Semua">Semua Kelas (${siswaList.length})</option>` +
    Array.from(kelasSet).map(k => `<option value="${escapeHtml(k)}" ${filterKelas === k ? 'selected' : ''}>Kelas ${escapeHtml(k)}</option>`).join('');

  const filtered = siswaList.filter(s => {
    const matchSearch = s.nama.toLowerCase().includes(search) || s.kelas.toLowerCase().includes(search);
    const matchKelas = filterKelas === 'Semua' || s.kelas === filterKelas;
    return matchSearch && matchKelas;
  });

  const tbody = document.getElementById('siswa-table-body');
  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">Data siswa tidak ditemukan</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((s, idx) => `
    <tr>
      <td>${idx + 1}</td>
      <td><strong>${escapeHtml(s.nama)}</strong></td>
      <td><span class="badge badge-info">${escapeHtml(s.kelas)}</span></td>
      <td>${escapeHtml(s.jenis_kelamin)}</td>
      <td><strong class="text-emerald">${s.saldo_poin} Poin</strong></td>
      <td><span class="badge ${getStatusBadgeClass(getCharacterStatus(s.saldo_poin))}">${getCharacterStatus(s.saldo_poin)}</span></td>
      <td>
        <button class="btn btn-sm btn-outline text-amber" onclick="openStudentPinModal(${s.id_siswa})" title="Lihat / Buat PIN Login Murid"><i class="fa-solid fa-key"></i> PIN</button>
        <button class="btn btn-sm btn-outline" onclick="editSiswa(${s.id_siswa})" title="Edit Siswa"><i class="fa-solid fa-pen"></i></button>
        <button class="btn btn-sm btn-danger" onclick="deleteSiswa(${s.id_siswa})" title="Hapus Siswa"><i class="fa-solid fa-trash"></i></button>
      </td>
    </tr>
  `).join('');
}

// Render Form Options for Recording Points
function renderCatatForm() {
  const siswaList = db.siswa || [];
  const katList = db.kategoriAktivitas || [];

  const siswaSelect = document.getElementById('input-catat-siswa');
  siswaSelect.innerHTML = `<option value="">-- Pilih Murid --</option>` +
    siswaList.map(s => `<option value="${s.id_siswa}">${escapeHtml(s.nama)} (${escapeHtml(s.kelas)}) - Saldo: ${s.saldo_poin}</option>`).join('');

  const katSelect = document.getElementById('input-catat-kategori');
  katSelect.innerHTML = `<option value="">-- Pilih Kategori Aktivitas --</option>` +
    katList.map(k => `<option value="${k.id_kategori}">${k.jenis_kategori === 'Perbuatan Baik' ? '🟢' : '🔴'} ${escapeHtml(k.nama_kategori)} (${k.nilai_poin > 0 ? '+' : ''}${k.nilai_poin} Poin)</option>`).join('');

  // Set default date to today
  document.getElementById('input-catat-tanggal').valueAsDate = new Date();

  // Side Riwayat
  const riwayatList = [...(db.riwayatPoin || [])].sort((a, b) => b.tanggal - a.tanggal).slice(0, 10);
  const sideTbody = document.getElementById('side-riwayat-tbody');
  if (riwayatList.length === 0) {
    sideTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">Belum ada riwayat</td></tr>`;
  } else {
    sideTbody.innerHTML = riwayatList.map(r => {
      const s = siswaList.find(x => x.id_siswa === r.id_siswa) || { nama: 'Siswa Dihapus' };
      const isBaik = r.jenis_catatan === 'Perbuatan Baik';
      return `
        <tr>
          <td><small><strong>${escapeHtml(s.nama)}</strong></small></td>
          <td><small>${escapeHtml(r.keterangan || r.jenis_catatan)}</small></td>
          <td><strong class="${isBaik ? 'text-emerald' : 'text-rose'}">${isBaik ? '+' : ''}${r.nilai_poin}</strong></td>
          <td>
            <button class="btn btn-sm btn-outline text-rose" onclick="deleteRiwayat(${r.id_riwayat})"><i class="fa-solid fa-xmark"></i></button>
          </td>
        </tr>
      `;
    }).join('');
  }
}

let currentKategoriFilter = 'Semua';

// Render Kategori Aktivitas Tab
function renderKategori() {
  const katList = db.kategoriAktivitas || [];
  const tbody = document.getElementById('kategori-table-body');
  
  const filtered = katList.filter(k => {
    if (currentKategoriFilter === 'Semua') return true;
    return k.jenis_kategori === currentKategoriFilter;
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">Belum ada kategori aktivitas untuk filter: <strong>${escapeHtml(currentKategoriFilter)}</strong></td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map((k, idx) => {
    const isBaik = k.jenis_kategori === 'Perbuatan Baik';
    return `
      <tr>
        <td>${idx + 1}</td>
        <td><strong>${escapeHtml(k.nama_kategori)}</strong></td>
        <td><span class="badge ${isBaik ? 'badge-success' : 'badge-danger'}">${escapeHtml(k.jenis_kategori)}</span></td>
        <td><strong class="${isBaik ? 'text-emerald' : 'text-rose'}">${isBaik ? '+' : ''}${k.nilai_poin} Poin</strong></td>
        <td><small class="text-muted">${escapeHtml(k.keterangan || '-')}</small></td>
        <td>
          <button class="btn btn-sm btn-outline" onclick="editKategori(${k.id_kategori})" title="Edit Kategori ini"><i class="fa-solid fa-pen"></i> Edit</button>
          <button class="btn btn-sm btn-danger" onclick="deleteKategori(${k.id_kategori})" title="Hapus Kategori"><i class="fa-solid fa-trash"></i></button>
        </td>
      </tr>
    `;
  }).join('');
}

// Render Hadiah & Penukaran Tab
function renderHadiah() {
  const hadiahList = db.hadiah || [];
  const reqList = db.pengajuanHadiah || [];
  const siswaList = db.siswa || [];

  // Table Hadiah
  const hTbody = document.getElementById('hadiah-table-body');
  if (hadiahList.length === 0) {
    hTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">Belum ada katalog hadiah</td></tr>`;
  } else {
    hTbody.innerHTML = hadiahList.map(h => `
      <tr>
        <td><strong>${escapeHtml(h.nama_hadiah)}</strong></td>
        <td><strong class="text-amber">${h.poin_dibutuhkan} Poin</strong></td>
        <td><span class="badge badge-info">${h.stok_hadiah} stok</span></td>
        <td>
          <button class="btn btn-sm btn-danger" onclick="deleteHadiah(${h.id_hadiah})"><i class="fa-solid fa-trash"></i></button>
        </td>
      </tr>
    `).join('');
  }

  // Table Pengajuan
  const reqTbody = document.getElementById('pengajuan-table-body');
  if (reqList.length === 0) {
    reqTbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">Belum ada pengajuan hadiah</td></tr>`;
  } else {
    reqTbody.innerHTML = reqList.map(p => {
      const s = siswaList.find(x => x.id_siswa === p.id_siswa) || { nama: p.nama_siswa };
      return `
        <tr>
          <td><strong>${escapeHtml(s.nama)}</strong></td>
          <td>${escapeHtml(p.nama_hadiah)}</td>
          <td><span class="badge ${p.status === 'MENUNGGU' ? 'badge-warning' : p.status === 'DISETUJUI' ? 'badge-success' : 'badge-danger'}">${p.status}</span></td>
          <td>
            ${p.status === 'MENUNGGU' ? `
              <button class="btn btn-sm btn-success" onclick="approveHadiah(${p.id_pengajuan})"><i class="fa-solid fa-check"></i> Disetujui</button>
              <button class="btn btn-sm btn-danger" onclick="rejectHadiah(${p.id_pengajuan})"><i class="fa-solid fa-xmark"></i> Tolak</button>
            ` : '-'}
          </td>
        </tr>
      `;
    }).join('');
  }
}

// Render Pengaturan Tab
function renderPengaturan() {
  const p = db.pengaturanSekolah;
  document.getElementById('setting-nama-sekolah').value = p.nama_sekolah;
  document.getElementById('setting-tahun-pelajaran').value = p.tahun_pelajaran;
  document.getElementById('setting-kepala-sekolah').value = p.nama_kepala_sekolah;
  document.getElementById('setting-guru-ttd').value = p.nama_guru_ttd;
  document.getElementById('setting-peran-guru').value = p.peran_guru_ttd;

  document.getElementById('setting-batas-sangat-baik').value = p.batas_sangat_baik;
  document.getElementById('setting-batas-baik').value = p.batas_baik;
  document.getElementById('setting-batas-pembinaan').value = p.batas_pembinaan;
  document.getElementById('setting-batas-peringatan').value = p.batas_peringatan;
}

// Setup Event Listeners
function setupEventListeners() {
  // Theme Toggle
  const btnThemeToggle = document.getElementById('btn-theme-toggle');
  if (btnThemeToggle) {
    btnThemeToggle.addEventListener('click', toggleTheme);
  }

  // Search & Filter Siswa
  document.getElementById('siswa-search').addEventListener('input', renderSiswa);
  document.getElementById('siswa-filter-kelas').addEventListener('change', renderSiswa);

  // Form Siswa
  document.getElementById('btn-add-siswa').addEventListener('click', () => {
    document.getElementById('siswa-id').value = '';
    document.getElementById('form-siswa').reset();
    document.getElementById('modal-siswa-title').textContent = 'Tambah Data Siswa';
    openModal('modal-siswa');
  });

  document.getElementById('form-siswa').addEventListener('submit', (e) => {
    e.preventDefault();
    const id = document.getElementById('siswa-id').value;
    const nama = document.getElementById('siswa-nama').value.trim();
    const kelas = document.getElementById('siswa-kelas').value.trim();
    const jk = document.getElementById('siswa-jk').value;
    const poin = parseInt(document.getElementById('siswa-poin').value) || 100;

    if (id) {
      const idx = db.siswa.findIndex(x => x.id_siswa == id);
      if (idx !== -1) {
        db.siswa[idx] = { ...db.siswa[idx], nama, kelas, jenis_kelamin: jk, saldo_poin: poin };
      }
    } else {
      const maxId = db.siswa.reduce((max, x) => Math.max(max, x.id_siswa), 0);
      db.siswa.push({ id_siswa: maxId + 1, nama, kelas, jenis_kelamin: jk, saldo_poin: poin, is_petugas: false });
    }
    saveState();
    closeModal('modal-siswa');
    showToast('Data siswa berhasil disimpan!');
  });

  // Form Catat Poin
  document.getElementById('form-catat-poin').addEventListener('submit', (e) => {
    e.preventDefault();
    const idSiswa = parseInt(document.getElementById('input-catat-siswa').value);
    const idKat = parseInt(document.getElementById('input-catat-kategori').value);
    const dateVal = document.getElementById('input-catat-tanggal').value;
    const pencatat = document.getElementById('input-catat-pencatat').value.trim() || 'Guru Piket';
    const ket = document.getElementById('input-catat-keterangan').value.trim();

    const siswa = db.siswa.find(x => x.id_siswa === idSiswa);
    const kat = db.kategoriAktivitas.find(x => x.id_kategori === idKat);

    if (!siswa || !kat) {
      showToast('Harap pilih murid dan kategori!');
      return;
    }

    const tgl = dateVal ? new Date(dateVal).getTime() : Date.now();
    const maxId = (db.riwayatPoin || []).reduce((max, x) => Math.max(max, x.id_riwayat), 0);

    const riwayat = {
      id_riwayat: maxId + 1,
      id_siswa: idSiswa,
      id_kategori: idKat,
      jenis_catatan: kat.jenis_kategori,
      nilai_poin: kat.nilai_poin,
      tanggal: tgl,
      guru_pencatat: pencatat,
      keterangan: ket || kat.nama_kategori
    };

    db.riwayatPoin.push(riwayat);
    siswa.saldo_poin += kat.nilai_poin;

    saveState();
    document.getElementById('form-catat-poin').reset();
    document.getElementById('input-catat-tanggal').valueAsDate = new Date();
    showToast(`Berhasil mencatat poin (${kat.nilai_poin > 0 ? '+' : ''}${kat.nilai_poin}) untuk ${siswa.nama}`);
  });

  // Quick Manage Kategori from Catat Form
  const btnQuickKat = document.getElementById('btn-quick-manage-kategori');
  if (btnQuickKat) {
    btnQuickKat.addEventListener('click', () => {
      const katTabBtn = document.querySelector('.nav-item[data-tab="kategori"]');
      if (katTabBtn) katTabBtn.click();
    });
  }

  // Kategori Form
  document.getElementById('btn-add-kategori').addEventListener('click', () => {
    document.getElementById('kategori-id').value = '';
    document.getElementById('form-kategori').reset();
    document.getElementById('modal-kategori-title').textContent = 'Tambah Kategori Aktivitas Baru';
    openModal('modal-kategori');
  });

  const btnAddPos = document.getElementById('btn-add-kategori-positif');
  if (btnAddPos) {
    btnAddPos.addEventListener('click', () => {
      document.getElementById('kategori-id').value = '';
      document.getElementById('form-kategori').reset();
      document.getElementById('kategori-jenis').value = 'Perbuatan Baik';
      document.getElementById('modal-kategori-title').textContent = 'Tambah Kategori Poin Positif (Terpuji)';
      openModal('modal-kategori');
    });
  }

  const btnAddNeg = document.getElementById('btn-add-kategori-pelanggaran');
  if (btnAddNeg) {
    btnAddNeg.addEventListener('click', () => {
      document.getElementById('kategori-id').value = '';
      document.getElementById('form-kategori').reset();
      document.getElementById('kategori-jenis').value = 'Pelanggaran';
      document.getElementById('modal-kategori-title').textContent = 'Tambah Kategori Pelanggaran';
      openModal('modal-kategori');
    });
  }

  // Filter Kategori Buttons
  document.querySelectorAll('.kat-filter-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.kat-filter-btn').forEach(b => {
        b.classList.remove('active', 'btn-primary');
        b.classList.add('btn-outline');
      });
      btn.classList.add('active', 'btn-primary');
      btn.classList.remove('btn-outline');
      currentKategoriFilter = btn.getAttribute('data-filter');
      renderKategori();
    });
  });

  document.getElementById('form-kategori').addEventListener('submit', (e) => {
    e.preventDefault();
    const id = document.getElementById('kategori-id').value;
    const nama = document.getElementById('kategori-nama').value.trim();
    const jenis = document.getElementById('kategori-jenis').value;
    let poin = parseInt(document.getElementById('kategori-poin').value) || 0;
    const ket = document.getElementById('kategori-keterangan').value.trim();

    if (jenis === 'Perbuatan Baik') poin = Math.abs(poin);
    else poin = -Math.abs(poin);

    if (id) {
      const idx = db.kategoriAktivitas.findIndex(x => x.id_kategori == id);
      if (idx !== -1) {
        db.kategoriAktivitas[idx] = { ...db.kategoriAktivitas[idx], nama_kategori: nama, jenis_kategori: jenis, nilai_poin: poin, keterangan: ket };
      }
    } else {
      const maxId = db.kategoriAktivitas.reduce((max, x) => Math.max(max, x.id_kategori), 0);
      db.kategoriAktivitas.push({ id_kategori: maxId + 1, nama_kategori: nama, jenis_kategori: jenis, nilai_poin: poin, keterangan: ket });
    }
    saveState();
    closeModal('modal-kategori');
    showToast(id ? `Kategori '${nama}' berhasil diperbarui!` : `Kategori '${nama}' berhasil ditambahkan!`);
  });

  // Hadiah Form
  document.getElementById('btn-add-hadiah').addEventListener('click', () => {
    document.getElementById('hadiah-id').value = '';
    document.getElementById('form-hadiah').reset();
    openModal('modal-hadiah');
  });

  document.getElementById('form-hadiah').addEventListener('submit', (e) => {
    e.preventDefault();
    const nama = document.getElementById('hadiah-nama').value.trim();
    const poin = parseInt(document.getElementById('hadiah-poin').value) || 10;
    const stok = parseInt(document.getElementById('hadiah-stok').value) || 10;
    const ket = document.getElementById('hadiah-keterangan').value.trim();

    const maxId = (db.hadiah || []).reduce((max, x) => Math.max(max, x.id_hadiah), 0);
    db.hadiah.push({ id_hadiah: maxId + 1, nama_hadiah: nama, poin_dibutuhkan: poin, stok_hadiah: stok, keterangan: ket });

    saveState();
    closeModal('modal-hadiah');
    showToast('Hadiah ditambahkan!');
  });

  // Form Pengaturan
  document.getElementById('form-pengaturan').addEventListener('submit', (e) => {
    e.preventDefault();
    db.pengaturanSekolah = {
      ...db.pengaturanSekolah,
      nama_sekolah: document.getElementById('setting-nama-sekolah').value.trim(),
      tahun_pelajaran: document.getElementById('setting-tahun-pelajaran').value.trim(),
      nama_kepala_sekolah: document.getElementById('setting-kepala-sekolah').value.trim(),
      nama_guru_ttd: document.getElementById('setting-guru-ttd').value.trim(),
      peran_guru_ttd: document.getElementById('setting-peran-guru').value.trim(),
      batas_sangat_baik: parseInt(document.getElementById('setting-batas-sangat-baik').value) || 100,
      batas_baik: parseInt(document.getElementById('setting-batas-baik').value) || 70,
      batas_pembinaan: parseInt(document.getElementById('setting-batas-pembinaan').value) || 40,
      batas_peringatan: parseInt(document.getElementById('setting-batas-peringatan').value) || 0
    };
    saveState();
    showToast('Konfigurasi sekolah diperbarui!');
  });

  // JSON Export Buttons
  document.getElementById('btn-quick-export').addEventListener('click', exportBackupJSON);
  document.getElementById('btn-export-json').addEventListener('click', exportBackupJSON);

  // JSON Import Buttons
  document.getElementById('btn-quick-import').addEventListener('click', () => document.getElementById('input-json-file').click());
  document.getElementById('btn-trigger-import-json').addEventListener('click', () => document.getElementById('input-json-file').click());
  document.getElementById('input-json-file').addEventListener('change', handleImportJSON);

  // Import TXT Massal Siswa
  document.getElementById('btn-import-siswa-txt').addEventListener('click', handleImportTXT);

  // Export Excel / CSV
  document.getElementById('btn-export-excel').addEventListener('click', exportCSV);

  // Load Demo Data
  document.getElementById('btn-load-demo').addEventListener('click', () => {
    db = JSON.parse(JSON.stringify(defaultState));
    saveState();
    showToast('Data demo berhasil dimuat!');
  });

  // Reset Database
  document.getElementById('btn-reset-db').addEventListener('click', () => {
    if (confirm('Apakah Anda yakin ingin mengosongkan database di PC ini?')) {
      db = {
        app: 'SIKAP',
        version: 1,
        exportTimestamp: Date.now(),
        siswa: [],
        kategoriAktivitas: defaultState.kategoriAktivitas,
        riwayatPoin: [],
        pengaturanSekolah: defaultState.pengaturanSekolah,
        hadiah: [],
        pengajuanHadiah: []
      };
      saveState();
      showToast('Database PC dikosongkan.');
    }
  });

  // Refresh History
  document.getElementById('btn-refresh-history').addEventListener('click', () => {
    renderDashboard();
    showToast('Riwayat diperbarui.');
  });
}

// Navigation Tabs Handling
function setupNavigation() {
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const tabId = item.getAttribute('data-tab');
      
      navItems.forEach(n => n.classList.remove('active'));
      item.classList.add('active');

      document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
      const activePane = document.getElementById(`tab-${tabId}`);
      if (activePane) activePane.classList.add('active');

      // Update Header Titles
      const pageTitle = document.getElementById('page-title');
      const pageSubtitle = document.getElementById('page-subtitle');

      switch (tabId) {
        case 'dashboard':
          pageTitle.textContent = 'Dashboard Utama';
          pageSubtitle.textContent = 'Ringkasan statistik perkembangan karakter dan saldo poin murid.';
          break;
        case 'siswa':
          pageTitle.textContent = 'Manajemen Data Siswa';
          pageSubtitle.textContent = 'Kelola daftar nama siswa, kelas, dan status saldo poin.';
          break;
        case 'catat-poin':
          pageTitle.textContent = 'Pencatatan Poin & Pelanggaran';
          pageSubtitle.textContent = 'Input perbuatan baik atau pengurangan poin pelanggaran murid.';
          break;
        case 'kategori':
          pageTitle.textContent = 'Kategori Aktivitas';
          pageSubtitle.textContent = 'Atur kriteria perbuatan terpuji dan bobot pelanggaran.';
          break;
        case 'hadiah':
          pageTitle.textContent = 'Katalog Hadiah & Permohonan';
          pageSubtitle.textContent = 'Tukarkan poin prestasi murid dengan hadiah penghargaan.';
          break;
        case 'pengaturan':
          pageTitle.textContent = 'Pengaturan Sekolah';
          pageSubtitle.textContent = 'Konfigurasi nama sekolah, kualifikasi poin, dan tanda tangan laporan.';
          break;
        case 'backup':
          pageTitle.textContent = 'Backup & Restore Center';
          pageSubtitle.textContent = 'Integrasi dan sinkronisasi berkas backup JSON dengan HP Android.';
          break;
      }
    });
  });
}

// Global Actions: Edit/Delete Siswa
window.editSiswa = function(id) {
  const s = db.siswa.find(x => x.id_siswa === id);
  if (s) {
    document.getElementById('siswa-id').value = s.id_siswa;
    document.getElementById('siswa-nama').value = s.nama;
    document.getElementById('siswa-kelas').value = s.kelas;
    document.getElementById('siswa-jk').value = s.jenis_kelamin;
    document.getElementById('siswa-poin').value = s.saldo_poin;
    document.getElementById('modal-siswa-title').textContent = 'Edit Data Siswa';
    openModal('modal-siswa');
  }
};

window.deleteSiswa = function(id) {
  if (confirm('Hapus data siswa ini beserta seluruh riwayatnya?')) {
    db.siswa = db.siswa.filter(x => x.id_siswa !== id);
    db.riwayatPoin = db.riwayatPoin.filter(x => x.id_siswa !== id);
    saveState();
    showToast('Siswa berhasil dihapus!');
  }
};

// Global Actions: Edit/Delete Kategori
window.editKategori = function(id) {
  const k = db.kategoriAktivitas.find(x => x.id_kategori === id);
  if (k) {
    document.getElementById('kategori-id').value = k.id_kategori;
    document.getElementById('kategori-nama').value = k.nama_kategori;
    document.getElementById('kategori-jenis').value = k.jenis_kategori;
    document.getElementById('kategori-poin').value = Math.abs(k.nilai_poin);
    document.getElementById('kategori-keterangan').value = k.keterangan || '';
    document.getElementById('modal-kategori-title').textContent = 'Edit Kategori';
    openModal('modal-kategori');
  }
};

window.deleteKategori = function(id) {
  if (confirm('Hapus kategori aktivitas ini?')) {
    db.kategoriAktivitas = db.kategoriAktivitas.filter(x => x.id_kategori !== id);
    saveState();
    showToast('Kategori dihapus!');
  }
};

// Global Action: Delete History Record & Revert Points
window.deleteRiwayat = function(id) {
  const r = db.riwayatPoin.find(x => x.id_riwayat === id);
  if (r && confirm('Batalkan pencatatan poin ini? Saldo siswa akan dikembalikan.')) {
    const siswa = db.siswa.find(x => x.id_siswa === r.id_siswa);
    if (siswa) {
      siswa.saldo_poin -= r.nilai_poin;
    }
    db.riwayatPoin = db.riwayatPoin.filter(x => x.id_riwayat !== id);
    saveState();
    showToast('Catatan poin dibatalkan!');
  }
};

// Global Action: Delete Hadiah
window.deleteHadiah = function(id) {
  if (confirm('Hapus item hadiah ini?')) {
    db.hadiah = db.hadiah.filter(x => x.id_hadiah !== id);
    saveState();
    showToast('Hadiah dihapus!');
  }
};

// Global Action: Approve/Reject Hadiah
window.approveHadiah = function(id) {
  const p = db.pengajuanHadiah.find(x => x.id_pengajuan === id);
  if (p && p.status === 'MENUNGGU') {
    const s = db.siswa.find(x => x.id_siswa === p.id_siswa);
    const h = db.hadiah.find(x => x.id_hadiah === p.id_hadiah);

    if (!s || !h) {
      showToast('Siswa atau Hadiah tidak ditemukan!');
      return;
    }
    if (s.saldo_poin < h.poin_dibutuhkan) {
      showToast('Poin siswa tidak mencukupi!');
      return;
    }
    if (h.stok_hadiah <= 0) {
      showToast('Stok hadiah habis!');
      return;
    }

    s.saldo_poin -= h.poin_dibutuhkan;
    h.stok_hadiah -= 1;
    p.status = 'DISETUJUI';

    db.riwayatPoin.push({
      id_riwayat: Date.now(),
      id_siswa: s.id_siswa,
      id_kategori: 0,
      jenis_catatan: 'Penukaran Hadiah',
      nilai_poin: -h.poin_dibutuhkan,
      tanggal: Date.now(),
      guru_pencatat: 'Admin PC',
      keterangan: `Menukar hadiah: ${h.nama_hadiah}`
    });

    saveState();
    showToast('Pengajuan hadiah disetujui!');
  }
};

window.rejectHadiah = function(id) {
  const p = db.pengajuanHadiah.find(x => x.id_pengajuan === id);
  if (p && p.status === 'MENUNGGU') {
    p.status = 'DITOLAK';
    saveState();
    showToast('Pengajuan ditolak.');
  }
};

// Export Backup to JSON (Dual Key for Android Compatibility)
function exportBackupJSON() {
  const dataToExport = {
    app: 'SIKAP',
    version: 1,
    exportTimestamp: Date.now(),

    // Dual key format for 100% Android Moshi & JSON parsing compatibility
    siswa: db.siswa,
    siswas: db.siswa,

    kategoriAktivitas: db.kategoriAktivitas,
    kategoris: db.kategoriAktivitas,

    riwayatPoin: db.riwayatPoin,
    riwayats: db.riwayatPoin,

    pengaturanSekolah: db.pengaturanSekolah,
    pengaturan: db.pengaturanSekolah,

    hadiah: db.hadiah,
    pengajuanHadiah: db.pengajuanHadiah,
    admins: [{ id_admin: 1, username: 'admin', password: 'admin', nama_admin: 'Guru Pengurus' }]
  };

  const jsonStr = JSON.stringify(dataToExport, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);

  const dateStr = new Date().toISOString().split('T')[0];
  const a = document.createElement('a');
  a.href = url;
  a.download = `sikap_backup_${dateStr}.json`;
  a.click();
  URL.revokeObjectURL(url);

  showToast('File JSON Backup berhasil diunduh!');
}

// Import Backup JSON from Android
function handleImportJSON(e) {
  const file = e.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(evt) {
    try {
      const parsed = JSON.parse(evt.target.result);
      db = normalizeState(parsed);
      saveState();
      showToast('Pemulihan (Restore) data JSON Android sukses 100%!');
    } catch (err) {
      alert('Gagal memproses file JSON backup. Pastikan file valid!');
    }
  };
  reader.readAsText(file);
  e.target.value = '';
}

// Import Massal Siswa TXT
function handleImportTXT() {
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = '.txt,.csv';
  fileInput.onchange = function(e) {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(evt) {
      const lines = evt.target.result.split('\n');
      let count = 0;
      const defaultPoin = db.pengaturanSekolah.poin_awal_siswa || 100;

      lines.forEach(line => {
        const clean = line.trim();
        if (!clean || clean.startsWith('#')) return;
        const parts = clean.split(',');
        if (parts.length >= 3) {
          const nama = parts[0].trim();
          const kelas = parts[1].trim();
          let jk = parts[2].trim();
          if (jk.toUpperCase().startsWith('L')) jk = 'Laki-laki';
          else if (jk.toUpperCase().startsWith('P')) jk = 'Perempuan';
          else jk = 'Laki-laki';

          if (nama && kelas) {
            const maxId = db.siswa.reduce((max, x) => Math.max(max, x.id_siswa), 0);
            db.siswa.push({
              id_siswa: maxId + 1,
              nama,
              kelas,
              jenis_kelamin: jk,
              saldo_poin: defaultPoin,
              is_petugas: false
            });
            count++;
          }
        }
      });

      if (count > 0) {
        saveState();
        showToast(`Berhasil mengimpor ${count} data siswa!`);
      } else {
        alert('Format file tidak sesuai. Gunakan format: Nama, Kelas, Laki-laki/Perempuan');
      }
    };
    reader.readAsText(file);
  };
  fileInput.click();
}

// Export Report CSV / Excel
function exportCSV() {
  const p = db.pengaturanSekolah;
  let csv = `"${p.nama_sekolah}"\n`;
  csv += `"Tahun Pelajaran: ${p.tahun_pelajaran}"\n`;
  csv += `"Pengurus/TTD: ${p.nama_guru_ttd} (${p.peran_guru_ttd})"\n`;
  csv += `"Tanggal Unduh: ${new Date().toLocaleDateString('id-ID')}"\n\n`;

  csv += `"No","Nama Siswa","Kelas","Jenis Kelamin","Saldo Poin","Status Karakter"\n`;

  db.siswa.forEach((s, idx) => {
    csv += `"${idx + 1}","${s.nama}","${s.kelas}","${s.jenis_kelamin}","${s.saldo_poin}","${getCharacterStatus(s.saldo_poin)}"\n`;
  });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `Rekap_Poin_Siswa_${new Date().toISOString().split('T')[0]}.csv`;
  a.click();
  URL.revokeObjectURL(url);

  showToast('Laporan Rekap CSV/Excel diunduh!');
}

// Modal Helpers
function openModal(id) {
  document.getElementById(id).classList.add('active');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

document.querySelectorAll('.close-modal').forEach(btn => {
  btn.addEventListener('click', (e) => {
    const modal = e.target.closest('.modal');
    if (modal) modal.classList.remove('active');
  });
});

// Toast Helper
function showToast(msg) {
  const toast = document.getElementById('toast');
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3000);
}

// Escape HTML Utility
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Student PIN Access Modal
window.openStudentPinModal = async function(idSiswa) {
  const siswa = db.siswa.find(x => x.id_siswa === idSiswa);
  if (!siswa) return;

  document.getElementById('pin-student-id').value = idSiswa;
  document.getElementById('pin-student-name').textContent = `${siswa.nama} (${siswa.kelas})`;
  document.getElementById('pin-display-box').textContent = 'Memuat...';

  openModal('modal-pin-siswa');

  try {
    const { data } = await supabaseClient
      .from('student_access')
      .select('pin_code')
      .eq('teacher_id', currentUserId)
      .eq('student_id', idSiswa)
      .maybeSingle();

    if (data && data.pin_code) {
      document.getElementById('pin-display-box').textContent = data.pin_code;
    } else {
      document.getElementById('pin-display-box').textContent = 'Belum Ada PIN';
    }
  } catch (err) {
    document.getElementById('pin-display-box').textContent = '------';
  }
};

// Generate Random 6-digit PIN
const btnGenPin = document.getElementById('btn-generate-pin');
if (btnGenPin) {
  btnGenPin.addEventListener('click', async () => {
    const idSiswa = parseInt(document.getElementById('pin-student-id').value);
    const siswa = db.siswa.find(x => x.id_siswa === idSiswa);
    if (!siswa || !currentUserId) return;

    const newPin = Math.floor(100000 + Math.random() * 900000).toString();
    document.getElementById('pin-display-box').textContent = newPin;

    try {
      const { error } = await supabaseClient
        .from('student_access')
        .upsert({
          teacher_id: currentUserId,
          student_id: idSiswa,
          pin_code: newPin
        }, { onConflict: 'teacher_id,student_id' });

      if (error) throw error;
      showToast(`PIN Akses untuk ${siswa.nama} berhasil dibuat: ${newPin}`);
    } catch (err) {
      showToast('Gagal simpan PIN: ' + (err.message || 'Error'));
    }
  });
}

// Logout Guru Handler
const btnLogoutGuru = document.getElementById('btn-logout-guru');
if (btnLogoutGuru) {
  btnLogoutGuru.addEventListener('click', async () => {
    if (confirm('Apakah Anda yakin ingin keluar dari akun?')) {
      await supabaseClient.auth.signOut();
      window.location.href = 'login.html';
    }
  });
}

