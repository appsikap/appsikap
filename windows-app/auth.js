/**
 * SIKAP Auth Module - Handling Supabase Auth (Email, Google OAuth, PIN Murid)
 */

document.addEventListener('DOMContentLoaded', () => {
  setupAuthTabs();
  setupAuthForms();
  checkExistingSession();
});

// UI Helper: Toast Notifications
function showAuthToast(msg) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3500);
}

// Setup Auth Tabs switching (Guru vs Murid)
function setupAuthTabs() {
  const tabBtns = document.querySelectorAll('.auth-tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const targetId = btn.getAttribute('data-target');
      document.querySelectorAll('.auth-form-container').forEach(form => {
        form.classList.remove('active');
      });

      const activeForm = document.getElementById(targetId);
      if (activeForm) activeForm.classList.add('active');
    });
  });

  // Modal Open/Close Event Listeners
  const linkRegister = document.getElementById('link-register');
  if (linkRegister) {
    linkRegister.addEventListener('click', (e) => {
      e.preventDefault();
      openAuthModal('modal-register');
    });
  }

  const linkForgot = document.getElementById('link-forgot-password');
  if (linkForgot) {
    linkForgot.addEventListener('click', (e) => {
      e.preventDefault();
      openAuthModal('modal-forgot');
    });
  }

  document.querySelectorAll('.close-modal').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
    });
  });
}

function openAuthModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.add('active');
}

// Check if user is already logged in
async function checkExistingSession() {
  try {
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (session && session.user) {
      // Check user role from metadata or user_roles table
      const user = session.user;
      const { data: roleData } = await supabaseClient
        .from('user_roles')
        .select('role')
        .eq('user_id', user.id)
        .single();

      const role = roleData ? roleData.role : 'guru';
      if (role === 'super_admin') {
        window.location.href = 'super_admin.html';
      } else {
        window.location.href = 'index.html';
      }
    }
  } catch (err) {
    console.log('No active session:', err);
  }
}

// Setup Auth Form Submission handlers
function setupAuthForms() {
  // 1. GURU LOGIN FORM
  const guruForm = document.getElementById('login-guru-form');
  if (guruForm) {
    guruForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('guru-email').value.trim();
      const password = document.getElementById('guru-password').value;
      const btn = document.getElementById('btn-login-guru');

      btn.disabled = true;
      btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Memproses...';

      try {
        const { data, error } = await supabaseClient.auth.signInWithPassword({
          email: email,
          password: password
        });

        if (error) throw error;

        showAuthToast('Login Berhasil! Mengalihkan...');
        
        // Fetch role
        const { data: roleData } = await supabaseClient
          .from('user_roles')
          .select('role')
          .eq('user_id', data.user.id)
          .single();

        const role = roleData ? roleData.role : 'guru';
        setTimeout(() => {
          if (role === 'super_admin') {
            window.location.href = 'super_admin.html';
          } else {
            window.location.href = 'index.html';
          }
        }, 1000);

      } catch (err) {
        showAuthToast('Gagal Login: ' + (err.message || 'Email atau password salah.'));
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Masuk Sekarang';
      }
    });
  }

  // 2. GOOGLE OAUTH LOGIN
  const btnGoogle = document.getElementById('btn-google-login');
  if (btnGoogle) {
    btnGoogle.addEventListener('click', async () => {
      try {
        const { error } = await supabaseClient.auth.signInWithOAuth({
          provider: 'google',
          options: {
            redirectTo: window.location.origin + window.location.pathname.replace('login.html', 'index.html'),
            queryParams: {
              prompt: 'select_account'
            }
          }
        });
        if (error) throw error;
      } catch (err) {
        showAuthToast('Gagal Login Google: ' + err.message);
      }
    });
  }

  // 3. MURID LOGIN FORM (PIN LOGIN)
  const muridForm = document.getElementById('login-murid-form');
  if (muridForm) {
    muridForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const pin = document.getElementById('murid-pin').value.trim();
      const btn = document.getElementById('btn-login-murid');

      if (pin.length !== 6) {
        showAuthToast('Kode PIN harus 6 digit angka!');
        return;
      }

      btn.disabled = true;
      btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Memeriksa PIN...';

      try {
        const { data, error } = await supabaseClient.rpc('get_student_dashboard', { pin_input: pin });
        if (error) throw error;

        if (!data || data.error) {
          throw new Error(data ? data.error : 'PIN Murid tidak ditemukan.');
        }

        // Parse the RPC result
        const appData = typeof data.data === 'string' ? JSON.parse(data.data) : data.data;
        const studentId = parseInt(data.id_siswa);
        const siswaObj = (appData.siswa || []).find(s => s.id_siswa === studentId);

        if (!siswaObj) {
          throw new Error('Data siswa tidak ditemukan di sistem sekolah.');
        }

        const riwayatSiswa = (appData.riwayatPoin || []).filter(r => r.id_siswa === studentId);

        // Store PIN session locally for murid.html
        sessionStorage.setItem('SIKAP_MURID_SESSION', JSON.stringify({
          pin: pin,
          student: siswaObj,
          riwayat: riwayatSiswa,
          pengaturan: appData.pengaturanSekolah || appData.pengaturan || {},
          hadiah: appData.hadiah || []
        }));

        showAuthToast('Selamat datang, ' + siswaObj.nama + '!');
        setTimeout(() => {
          window.location.href = 'murid.html';
        }, 800);

      } catch (err) {
        showAuthToast('Gagal Login: ' + (err.message || 'PIN tidak valid.'));
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-user-check"></i> Masuk Dashboard Murid';
      }
    });
  }

  // 4. REGISTER GURU FORM
  const regForm = document.getElementById('register-form');
  if (regForm) {
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const nama = document.getElementById('reg-nama').value.trim();
      const sekolah = document.getElementById('reg-sekolah').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const password = document.getElementById('reg-password').value;
      const btn = document.getElementById('btn-submit-register');

      btn.disabled = true;
      btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Mendaftar...';

      try {
        const { data, error } = await supabaseClient.auth.signUp({
          email: email,
          password: password,
          options: {
            data: {
              full_name: nama,
              school_name: sekolah
            }
          }
        });

        if (error) throw error;

        showAuthToast('Pendaftaran Berhasil! Silakan masuk.');
        document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
        document.getElementById('guru-email').value = email;

      } catch (err) {
        showAuthToast('Gagal Mendaftar: ' + err.message);
      } finally {
        btn.disabled = false;
        btn.innerHTML = 'Daftar Akun';
      }
    });
  }

  // 5. FORGOT PASSWORD FORM
  const forgotForm = document.getElementById('forgot-form');
  if (forgotForm) {
    forgotForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('forgot-email').value.trim();
      const btn = document.getElementById('btn-submit-forgot');

      btn.disabled = true;
      btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Mengirim...';

      try {
        const { error } = await supabaseClient.auth.resetPasswordForEmail(email, {
          redirectTo: window.location.origin + '/login.html?reset=true'
        });
        if (error) throw error;

        showAuthToast('Tautan reset kata sandi telah dikirim ke email Anda.');
        document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
      } catch (err) {
        showAuthToast('Gagal: ' + err.message);
      } finally {
        btn.disabled = false;
        btn.innerHTML = 'Kirim Reset Link';
      }
    });
  }
}
