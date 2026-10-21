async function loadPatient() {
  try {
    const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
    if (!response.ok) throw new Error('Sesión vencida');
    const patient = await response.json();
    document.querySelectorAll('[data-patient-name]').forEach((element) => {
      element.textContent = `${patient.nombre} ${patient.apellido}`;
    });
    document.querySelectorAll('[data-avatar]').forEach((element) => {
      element.textContent = patient.nombre.slice(0, 1).toUpperCase();
    });
    document.querySelector('#welcome-name').textContent = `¡Hola, ${patient.nombre}!`;
  } catch {
    window.location.replace('/InicioSesion.html');
  }
}

document.querySelector('#logout-button').addEventListener('click', async () => {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  } finally {
    window.location.replace('/InicioSesion.html');
  }
});

loadPatient();
