document.querySelector('#cerrarSesion').addEventListener('click', async () => {
  await fetch('/api/auth/logout', { method: 'POST' });
  window.location.replace('/InicioSesion.html');
});

const busqueda = document.querySelector('#buscarMedico');
busqueda.addEventListener('input', () => {
  const texto = busqueda.value.trim().toLocaleLowerCase('es');
  document.querySelectorAll('#tablaMedicos tbody tr').forEach((fila) => {
    fila.hidden = !fila.textContent.toLocaleLowerCase('es').includes(texto);
  });
});

document.querySelectorAll('[data-confirmar]').forEach((boton) => {
  boton.addEventListener('click', (evento) => {
    if (!window.confirm(boton.dataset.confirmar)) evento.preventDefault();
  });
});
