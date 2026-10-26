const form = document.querySelector('#formularioReserva');
const medico = document.querySelector('#medico');
const fecha = document.querySelector('#fecha');
const horarios = document.querySelector('#horarios-opciones');
const slotsMessage = document.querySelector('#slots-message');
const bookingMessage = document.querySelector('#booking-message');
const button = document.querySelector('#booking-button');
const resumen = document.querySelector('#resumen-reserva');
const dias = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
const nombresDias = { MIERCOLES: 'miércoles', SABADO: 'sábado' };
let especialistas = [];
let consultaActual = 0;
let citaEditando = null;

function fechaLocal(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function mostrarError(message) {
  bookingMessage.textContent = message;
  bookingMessage.hidden = !message;
}

function especialistaActual() {
  return especialistas.find((opcion) => String(opcion.idMedico) === medico.value);
}

function actualizarEspecialista() {
  const seleccionado = especialistaActual();
  const descripcion = document.querySelector('#doctor-description');
  if (!seleccionado) {
    descripcion.textContent = 'Selecciona un especialista para consultar sus días y horas de atención.';
    return;
  }
  const turnos = seleccionado.horarios.map((turno) =>
    `${nombresDias[turno.dia] || turno.dia.toLowerCase()} ${turno.inicio}–${turno.fin}`);
  descripcion.textContent = `${seleccionado.nombre} · ${seleccionado.especialidad}. Atención: ${turnos.join(', ')}.`;
}

function actualizarResumen() {
  const seleccionado = form.querySelector('input[name="hora"]:checked');
  const doctor = especialistaActual();
  resumen.hidden = !(seleccionado && doctor && fecha.value);
  button.disabled = resumen.hidden;
  if (resumen.hidden) return;
  document.querySelector('#resumen-medico').textContent = `${doctor.nombre} · ${doctor.especialidad}`;
  const fechaLegible = new Date(`${fecha.value}T12:00:00`).toLocaleDateString('es-PE', { dateStyle: 'full' });
  document.querySelector('#resumen-fecha').textContent = `${fechaLegible} a las ${seleccionado.value}`;
  const tipo = document.querySelector('#tipoConsulta').selectedOptions[0].textContent;
  document.querySelector('#resumen-tipo').textContent = `Consulta ${tipo.toLowerCase()}`;
}

async function cargarHorarios() {
  const consulta = ++consultaActual;
  horarios.replaceChildren();
  actualizarResumen();
  mostrarError('');
  if (!medico.value || !fecha.value) {
    slotsMessage.textContent = 'Elige especialista y fecha para ver horarios.';
    return;
  }
  if (fecha.value < fechaLocal(new Date())) {
    slotsMessage.textContent = 'La fecha ya pasó. Elige hoy o una fecha futura.';
    return;
  }
  slotsMessage.textContent = 'Consultando horarios...';
  try {
    const params = new URLSearchParams({ idMedico: medico.value, fecha: fecha.value });
    const response = await fetch(`/api/citas/disponibilidad?${params}`);
    if (response.status === 401) return window.location.replace('/InicioSesion.html');
    const disponibles = await response.json();
    if (consulta !== consultaActual) return;
    if (!response.ok) throw new Error(disponibles.error || 'No se pudieron consultar los horarios.');
    slotsMessage.textContent = disponibles.length
      ? 'Selecciona una hora disponible:'
      : 'Este especialista no tiene espacios libres en esa fecha. Elige otro día.';
    for (const opcion of disponibles) {
      const label = document.createElement('label');
      label.className = 'horario';
      const input = document.createElement('input');
      input.type = 'radio';
      input.name = 'hora';
      input.value = opcion.hora;
      input.dataset.horario = opcion.idHorario;
      input.addEventListener('change', actualizarResumen);
      const span = document.createElement('span');
      span.textContent = opcion.hora;
      label.append(input, span);
      horarios.append(label);
    }
  } catch (error) {
    if (consulta === consultaActual) slotsMessage.textContent = error.message;
  }
}

function proximaFechaConAtencion(doctor) {
  const hoy = new Date();
  for (let offset = 1; offset <= 7; offset += 1) {
    const candidata = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + offset);
    if (doctor.horarios.some((turno) => turno.dia === dias[candidata.getDay()])) {
      return fechaLocal(candidata);
    }
  }
  return '';
}

async function iniciar() {
  const session = await fetch('/api/auth/me');
  if (!session.ok) return window.location.replace('/InicioSesion.html');
  const patient = await session.json();
  document.querySelector('#patient-name').textContent = `${patient.nombre} ${patient.apellido}`;
  document.querySelector('#avatar').textContent = patient.nombre.slice(0, 1).toUpperCase();
  fecha.min = fechaLocal(new Date());
  const response = await fetch('/api/citas/medicos');
  if (!response.ok) throw new Error('No se pudieron cargar los especialistas. Actualiza la página.');
  especialistas = await response.json();
  medico.replaceChildren(new Option('Selecciona un especialista', ''));
  for (const opcion of especialistas) {
    medico.add(new Option(`${opcion.nombre} · ${opcion.especialidad}`, opcion.idMedico));
  }
  const parametros = new URLSearchParams(window.location.search);
  const editar = parametros.get('editar');
  if (editar) {
    const detalle = await fetch(`/api/citas/${encodeURIComponent(editar)}`);
    if (!detalle.ok) throw new Error('No encontramos la cita que quieres modificar.');
    citaEditando = await detalle.json();
    if (citaEditando.estado !== 'RESERVADA' || citaEditando.numeroModificaciones >= 2 ||
        new Date(`${citaEditando.fecha}T${citaEditando.hora}:00`).getTime() - Date.now() < 86400000) {
      throw new Error('Esta cita ya no se puede modificar.');
    }
    document.title = 'Modificar cita | Clínica Tezza';
    document.querySelector('#booking-button').firstChild.textContent = 'Guardar cambios ';
  }
  const medicoSolicitado = citaEditando?.idMedico || parametros.get('medico');
  const elegido = especialistas.find((opcion) => String(opcion.idMedico) === String(medicoSolicitado)) || especialistas[0];
  if (!elegido) return mostrarError('Todavía no hay especialistas con horarios disponibles.');
  medico.value = String(elegido.idMedico);
  actualizarEspecialista();
  fecha.value = citaEditando?.fecha || proximaFechaConAtencion(elegido);
  if (citaEditando) {
    document.querySelector('#tipoConsulta').value = citaEditando.tipoConsulta;
    document.querySelector('#motivo').value = citaEditando.motivo || '';
  }
  await cargarHorarios();
}

medico.addEventListener('change', () => { actualizarEspecialista(); cargarHorarios(); });
fecha.addEventListener('change', cargarHorarios);
document.querySelector('#tipoConsulta').addEventListener('change', actualizarResumen);
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const seleccionado = form.querySelector('input[name="hora"]:checked');
  if (!seleccionado || !form.reportValidity()) return;
  button.disabled = true;
  mostrarError('');
  try {
    const response = await fetch(citaEditando ? `/api/citas/${citaEditando.idCita}` : '/api/citas', {
      method: citaEditando ? 'PUT' : 'POST',
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
      throw new Error(result.error || 'No se pudo guardar la cita.');
    }
    if (citaEditando) return window.location.assign('/MisCitas.html?modificada=1');
    const result = await response.json();
    window.location.assign(`/MisCitas.html?reservada=${result.idCita}`);
  } catch (error) {
    await cargarHorarios();
    mostrarError(error.message);
  }
});

iniciar().catch((error) => mostrarError(error.message || 'No se pudo cargar el formulario.'));
