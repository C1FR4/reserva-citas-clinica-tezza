const form = document.querySelector('#formularioReserva');
const medico = document.querySelector('#medico');
const fecha = document.querySelector('#fecha');
const horarios = document.querySelector('#horarios-opciones');
const slotsMessage = document.querySelector('#slots-message');
const bookingMessage = document.querySelector('#booking-message');
const button = document.querySelector('#booking-button');

function fechaLocal(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function mostrarError(message) {
  bookingMessage.textContent = message;
  bookingMessage.hidden = !message;
}

async function cargarHorarios() {
  horarios.replaceChildren();
  button.disabled = true;
  mostrarError('');
  if (!medico.value || !fecha.value) {
    slotsMessage.textContent = 'Elige especialista y fecha para ver horarios.';
    return;
  }
  slotsMessage.textContent = 'Consultando horarios...';
  try {
    const params = new URLSearchParams({ idMedico: medico.value, fecha: fecha.value });
    const response = await fetch(`/api/citas/disponibilidad?${params}`);
    if (response.status === 401) return window.location.replace('/InicioSesion.html');
    if (!response.ok) throw new Error('No se pudieron consultar los horarios.');
    const disponibles = await response.json();
    slotsMessage.textContent = disponibles.length ? 'Selecciona una hora:' : 'No hay espacios disponibles para esa fecha.';
    for (const opcion of disponibles) {
      const label = document.createElement('label');
      label.className = 'horario';
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'hora';
      input.value = opcion.hora;
      input.dataset.horario = opcion.idHorario;
      input.addEventListener('change', () => { button.disabled = false; });
      const span = document.createElement('span');
      span.textContent = opcion.hora;
      label.append(input, span);
      horarios.append(label);
    }
  } catch (error) {
    slotsMessage.textContent = error.message;
  }
}

async function iniciar() {
  const session = await fetch('/api/auth/me');
  if (!session.ok) return window.location.replace('/InicioSesion.html');
  const patient = await session.json();
  document.querySelector('#patient-name').textContent = `${patient.nombre} ${patient.apellido}`;
  document.querySelector('#avatar').textContent = patient.nombre.slice(0, 1).toUpperCase();
  fecha.min = fechaLocal(new Date());
  const response = await fetch('/api/citas/medicos');
  if (!response.ok) {
    mostrarError('No se pudieron cargar los especialistas. Actualiza la página.');
    return;
  }
  const medicos = await response.json();
  medico.replaceChildren(new Option('Selecciona un especialista', ''));
  for (const opcion of medicos) {
    medico.add(new Option(`${opcion.nombre} · ${opcion.especialidad}`, opcion.idMedico));
  }
  const medicoSolicitado = new URLSearchParams(window.location.search).get('medico');
  if (medicos.some((opcion) => String(opcion.idMedico) === medicoSolicitado)) {
    medico.value = medicoSolicitado;
  } else if (medicos.length === 1) {
    medico.value = String(medicos[0].idMedico);
  }
  if (medicos.length === 0) mostrarError('Todavía no hay especialistas con horarios disponibles.');
}

medico.addEventListener('change', cargarHorarios);
fecha.addEventListener('change', cargarHorarios);
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const seleccionado = form.querySelector('input[name="hora"]:checked');
  if (!seleccionado) return;
  button.disabled = true;
  mostrarError('');
  try {
    const response = await fetch('/api/citas', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idMedico: Number(medico.value),
        idHorario: Number(seleccionado.dataset.horario),
        fecha: fecha.value,
        hora: seleccionado.value,
        tipoConsulta: document.querySelector('#tipoConsulta').value,
        motivo: document.querySelector('#motivo').value.trim() || null,
      }),
    });
    if (response.status === 401) return window.location.replace('/InicioSesion.html');
    if (!response.ok) {
      const result = await response.json();
      throw new Error(result.error || 'No se pudo reservar la cita.');
    }
    window.location.assign('/MisCitas.html?reservada=1');
  } catch (error) {
    await cargarHorarios();
    mostrarError(error.message);
  }
});

iniciar().catch(() => mostrarError('No se pudo cargar el formulario. Actualiza la página.'));
