const form = document.querySelector('#login-form');
const errorMessage = document.querySelector('#login-error');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorMessage.hidden = true;
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({
        email: form.elements.namedItem('email').value,
        password: form.elements.namedItem('password').value,
      }),
    });
    if (!response.ok) {
      const result = await response.json();
      throw new Error(result.error || 'No se pudo iniciar sesión.');
    }
    const result = await response.json();
    window.location.assign(result.destino);
  } catch (error) {
    errorMessage.textContent = error.message || 'No se pudo conectar con el servidor.';
    errorMessage.hidden = false;
    button.disabled = false;
  }
});
