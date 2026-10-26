document.querySelector('#cerrarSesion')?.addEventListener('click', async () => {
  await fetch('/api/auth/logout', { method: 'POST' });
  window.location.replace('/InicioSesion.html');
});

document.querySelector('#buscarCita')?.addEventListener('input', (evento) => {
  const texto = evento.target.value.trim().toLocaleLowerCase('es');
  document.querySelectorAll('#tablaCitas tbody tr').forEach((fila) => {
    fila.hidden = !fila.textContent.toLocaleLowerCase('es').includes(texto);
  });
});
