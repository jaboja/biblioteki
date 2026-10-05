/**
 * Login page functionality
 */

const loginForm = document.getElementById('loginForm') as HTMLFormElement | null;
const errorEl = document.getElementById('error') as HTMLElement | null;

if (loginForm) {
    loginForm.addEventListener('submit', async (e: Event) => {
        e.preventDefault();

        const usernameInput = document.getElementById('username') as HTMLInputElement | null;
        const passwordInput = document.getElementById('password') as HTMLInputElement | null;

        if (!usernameInput || !passwordInput) return;

        const username = usernameInput.value;
        const password = passwordInput.value;

        if (errorEl) {
            errorEl.style.display = 'none';
        }

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                throw new Error('Login failed');
            }

            const data = await response.json();
            const token = data.token as string;

            // Redirect to home
            window.location.href = '/';

        } catch (err) {
            if (errorEl) {
                errorEl.style.display = 'block';
            }
        }
    });
}
