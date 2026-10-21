async function cerrarSesion() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
  } finally {
    window.location.replace('/InicioSesion.html');
  }
}

async function iniciarEspecialistas() {
  const respuestaSesion = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (!respuestaSesion.ok) return window.location.replace('/InicioSesion.html');
  const paciente = await respuestaSesion.json();
  document.querySelectorAll('[data-patient-name]').forEach((nodo) => {
    nodo.textContent = `${paciente.nombre} ${paciente.apellido}`;
  });
  document.querySelectorAll('[data-avatar]').forEach((nodo) => {
    nodo.textContent = paciente.nombre.slice(0, 1).toUpperCase();
  });

  const lista = document.querySelector('#especialistas-lista');
  const respuesta = await fetch('/api/citas/medicos', { credentials: 'same-origin' });
  if (!respuesta.ok) throw new Error('No se pudieron cargar los especialistas. Actualiza la página.');
  const medicos = await respuesta.json();
  lista.replaceChildren();
  if (!medicos.length) {
    const vacio = document.createElement('p');
    vacio.className = 'empty-state';
    vacio.textContent = 'No hay especialistas disponibles por ahora.';
    lista.append(vacio);
  }
  for (const medico of medicos) {
    const tarjeta = document.createElement('article');
    tarjeta.className = 'especialista-card';
    const datos = document.createElement('div');
    const titulo = document.createElement('h3');
    titulo.textContent = medico.nombre;
    const especialidad = document.createElement('p');
    especialidad.textContent = medico.especialidad;
    datos.append(titulo, especialidad);
    const reservar = document.createElement('a');
    reservar.href = `ReservarCita.html?medico=${encodeURIComponent(medico.idMedico)}`;
    reservar.textContent = 'Reservar cita';
    tarjeta.append(datos, reservar);
    lista.append(tarjeta);
  }
}

document.querySelector('#logout-button').addEventListener('click', cerrarSesion);
document.querySelector('#logout-mobile').addEventListener('click', cerrarSesion);
iniciarEspecialistas().catch((error) => {
  document.querySelector('#especialistas-lista').textContent = error.message;
});
