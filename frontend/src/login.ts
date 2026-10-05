/**
 * Login page functionality
 */

export function initLoginPage(): void {
    const loginForm = document.getElementById('loginForm') as HTMLFormElement | null;
    const errorEl = document.getElementById('error') as HTMLElement | null;

    if (!loginForm) return;

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

            // Redirect to home
            window.location.href = '/';

        } catch (err) {
            if (errorEl) {
                errorEl.style.display = 'block';
            }
        }
    });
}

// Auto-initialize if this module is loaded on the login page
if (window.location.pathname.endsWith('/login.html') || window.location.pathname.endsWith('/login')) {
    document.addEventListener('DOMContentLoaded', initLoginPage);
}
