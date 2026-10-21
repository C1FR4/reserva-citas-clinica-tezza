const lista = document.querySelector('#citas-lista');
const notice = document.querySelector('#notice');

function aviso(message) {
  notice.textContent = message;
  notice.hidden = !message;
}

function fechaCita(cita) {
  return new Date(`${cita.fecha}T${cita.hora}:00`);
}

async function cargarCitas() {
  try {
    const response = await fetch('/api/citas/mias');
    if (response.status === 401) return window.location.replace('/InicioSesion.html');
    if (!response.ok) throw new Error('No se pudieron cargar tus citas. Actualiza la página.');
    const citas = await response.json();
    lista.replaceChildren();
    if (citas.length === 0) {
      const vacio = document.createElement('div');
      vacio.className = 'vacio';
      const mensaje = document.createElement('p');
      mensaje.textContent = 'Todavía no tienes citas registradas.';
      const enlace = document.createElement('a');
      enlace.href = 'ReservarCita.html';
      enlace.textContent = 'Reservar mi primera cita →';
      vacio.append(mensaje, enlace);
      lista.append(vacio);
      return;
    }
    for (const cita of citas) {
      const tarjeta = document.createElement('article');
      tarjeta.className = `cita-tarjeta${cita.estado === 'CANCELADA' ? ' cancelada' : ''}`;
      const datos = document.createElement('div');
      const titulo = document.createElement('h2');
      titulo.textContent = cita.especialidad;
      const medico = document.createElement('p');
      medico.textContent = cita.medico;
      const fecha = document.createElement('p');
      fecha.textContent = fechaCita(cita).toLocaleString('es-PE', { dateStyle: 'full', timeStyle: 'short' });
      const tipo = document.createElement('p');
      tipo.textContent = `Consulta ${cita.tipoConsulta.toLowerCase()}`;
      const estado = document.createElement('span');
      estado.className = 'estado';
      estado.textContent = cita.estado;
      datos.append(titulo, medico, fecha, tipo, estado);
      tarjeta.append(datos);
      if (cita.estado === 'RESERVADA' && fechaCita(cita) > new Date()) {
        const cancelar = document.createElement('button');
        cancelar.type = 'button';
        cancelar.className = 'cancelar';
        cancelar.textContent = 'Cancelar cita';
        cancelar.addEventListener('click', async () => {
          if (!window.confirm('¿Cancelar esta cita?')) return;
          cancelar.disabled = true;
          try {
            const result = await fetch(`/api/citas/${cita.idCita}/cancelar`, { method: 'POST' });
            if (!result.ok) throw new Error('No se pudo cancelar la cita.');
            aviso('La cita fue cancelada.');
            await cargarCitas();
          } catch (error) {
            aviso(error.message);
            cancelar.disabled = false;
          }
        });
        tarjeta.append(cancelar);
      }
      lista.append(tarjeta);
    }
  } catch (error) {
    lista.textContent = error.message;
  }
}

async function iniciar() {
  const response = await fetch('/api/auth/me');
  if (!response.ok) return window.location.replace('/InicioSesion.html');
  const patient = await response.json();
  document.querySelector('#patient-name').textContent = `${patient.nombre} ${patient.apellido}`;
  document.querySelector('#avatar').textContent = patient.nombre.slice(0, 1).toUpperCase();
  if (new URLSearchParams(window.location.search).has('reservada')) aviso('Tu cita fue reservada correctamente.');
  await cargarCitas();
}

iniciar().catch(() => { lista.textContent = 'No se pudieron cargar tus citas. Actualiza la página.'; });
