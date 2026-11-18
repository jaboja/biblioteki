/**
 * Authentication utilities shared across all frontend modules.
 *
 * The JWT is stored in an HttpOnly cookie set by the backend
 * (POST /api/auth/login). HttpOnly cookies cannot be read or cleared
 * from JavaScript, so the frontend relies on the browser sending the
 * cookie automatically with same-origin requests. Logout must therefore
 * be done by calling the backend, which clears the cookie server-side
 * via a Set-Cookie header.
 */

/**
 * Redirect to login page.
 */
export function redirectToLogin(): void {
    window.location.href = '/login.html';
}

/**
 * Perform logout by calling the backend, then redirect to the login page.
 */
export async function logout(): Promise<void> {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch {
        // Network errors are ignored - we always redirect to login.
    }
    redirectToLogin();
}

/**
 * Wrapper around fetch that checks for 401/403 and redirects to login.
 * If the response is 401 or 403, it clears the token and redirects.
 */
export async function checkedFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const response = await fetch(input, init);

    if (response.status === 401 || response.status === 403) {
        redirectToLogin();
        throw new Error('Unauthorized - redirected to login');
    }

    return response;
}
