/**
 * Authentication utilities shared across all frontend modules.
 */

/**
 * Get JWT token from localStorage or cookies.
 */
export function getToken(): string | null {
    // Check cookies
    const cookies = document.cookie.split(';');
    for (let i = 0; i < cookies.length; i++) {
        const cookie = cookies[i].trim();
        if (cookie.startsWith('jwt=')) {
            return cookie.substring(4);
        }
    }
    
    return null;
}

/**
 * Clear authentication token from storage.
 */
export function clearToken(): void {
    document.cookie = 'jwt=; Max-Age=0; path=/;';
}

/**
 * Redirect to login page.
 */
export function redirectToLogin(): void {
    window.location.href = '/login.html';
}

/**
 * Wrapper around fetch that checks for 401/403 and redirects to login.
 * If the response is 401 or 403, it clears the token and redirects.
 */
export async function checkedFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const response = await fetch(input, {
        ...init,
        headers: {
            ...init?.headers,
            'Authorization': `Bearer ${getToken() || ''}`
        }
    });
    
    if (response.status === 401 || response.status === 403) {
        clearToken();
        redirectToLogin();
        // This will throw since we're redirecting, but we need to return something
        // The redirect happens before this, so the throw won't be reached
        throw new Error('Unauthorized - redirected to login');
    }
    
    return response;
}
