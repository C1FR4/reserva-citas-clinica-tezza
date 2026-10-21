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
    await loadAppointments();
  } catch {
    window.location.replace('/InicioSesion.html');
  }
}

async function loadAppointments() {
  const container = document.querySelector('#upcoming-list');
  try {
    const response = await fetch('/api/citas/mias', { credentials: 'same-origin' });
    if (!response.ok) throw new Error('No se pudieron cargar las citas.');
    const citas = await response.json();
    document.querySelector('#completed-count').textContent = String(citas.length);
    const ahora = new Date();
    const proximas = citas.filter((cita) => cita.estado !== 'CANCELADA' &&
      new Date(`${cita.fecha}T${cita.hora}:00`) >= ahora)
      .sort((a, b) => `${a.fecha}T${a.hora}`.localeCompare(`${b.fecha}T${b.hora}`));
    container.replaceChildren();
    if (proximas.length === 0) {
      const vacio = document.createElement('p');
      vacio.className = 'empty-state';
      vacio.textContent = 'Aún no tienes citas próximas.';
      container.append(vacio);
      return;
    }
    const siguiente = proximas[0];
    const fecha = new Date(`${siguiente.fecha}T12:00:00`);
    document.querySelector('#next-date').textContent = String(fecha.getDate());
    document.querySelector('#next-month').textContent = fecha.toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    document.querySelector('#next-specialty').textContent = siguiente.especialidad;
    document.querySelector('#next-doctor').textContent = siguiente.medico;
    for (const cita of proximas.slice(0, 3)) {
      const item = document.createElement('div');
      item.className = 'cita';
      const bloqueFecha = document.createElement('div');
      bloqueFecha.className = 'fecha';
      const dia = document.createElement('strong');
      const mes = document.createElement('span');
      const fechaCita = new Date(`${cita.fecha}T12:00:00`);
      dia.textContent = String(fechaCita.getDate());
      mes.textContent = fechaCita.toLocaleDateString('es-PE', { month: 'short' }).toUpperCase();
      bloqueFecha.append(dia, mes);
      const datos = document.createElement('div');
      datos.className = 'datos-cita';
      const titulo = document.createElement('h3');
      titulo.textContent = cita.medico;
      const especialidad = document.createElement('p');
      especialidad.textContent = cita.especialidad;
      const hora = document.createElement('span');
      hora.textContent = cita.hora;
      datos.append(titulo, especialidad, hora);
      const estado = document.createElement('span');
      estado.className = 'estado';
      estado.textContent = cita.estado;
      item.append(bloqueFecha, datos, estado);
      container.append(item);
    }
  } catch {
    container.textContent = 'No se pudieron cargar las citas. Actualiza la página.';
  }
}

async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  } finally {
    window.location.replace('/InicioSesion.html');
  }
}

document.querySelector('#logout-button').addEventListener('click', logout);
document.querySelector('#logout-mobile').addEventListener('click', logout);

loadPatient();
