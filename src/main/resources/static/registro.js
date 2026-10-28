const form = document.querySelector('#registro-form');
const error = document.querySelector('#registro-error');
const exito = document.querySelector('#registro-exito');
const boton = form.querySelector('button');
const ayer = new Date();
ayer.setDate(ayer.getDate() - 1);
document.querySelector('#fechaNacimiento').max = `${ayer.getFullYear()}-${String(ayer.getMonth() + 1).padStart(2, '0')}-${String(ayer.getDate()).padStart(2, '0')}`;
form.addEventListener('submit', async (event) => {
  event.preventDefault();
  error.hidden = true;
  exito.hidden = true;
  const datos = Object.fromEntries(new FormData(form));
  if (datos.contrasena !== datos.confirmarContrasena) {
    error.textContent = 'Las contraseñas no coinciden.';
    error.hidden = false;
    return;
  }
  boton.disabled = true;
  try {
    const respuesta = await fetch('/api/auth/registro', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(datos),
    });
    const resultado = await respuesta.json();
    if (!respuesta.ok) throw new Error(resultado.error || 'Revisa los datos del formulario.');
    exito.textContent = resultado.mensaje;
    exito.hidden = false;
    form.querySelectorAll('input, select, button').forEach((campo) => { campo.disabled = true; });
    exito.scrollIntoView({ behavior: 'smooth', block: 'center' });
  } catch (fallo) {
    error.textContent = fallo.message || 'No se pudo conectar con el servidor.';
    error.hidden = false;
    boton.disabled = false;
  }
});
