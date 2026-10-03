import { AccountRequest, AccountResponse, LibraryDefinition, AccountFormData, AccountStats } from './types';

let libraries: LibraryDefinition[] = [];
let currentEditId: number | null = null;

export async function loadAccounts(): Promise<void> {
    try {
        await loadLibraries();
        const response = await fetch('/api/accounts');
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data: AccountResponse[] = await response.json();
        displayAccounts(data);
    } catch (error) {
        showError('Nie udało się załadować kont: ' + (error as Error).message);
    }
}

export async function loadLibraries(): Promise<void> {
    try {
        const response = await fetch('/api/accounts/libraries');
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        libraries = await response.json();
        updateLibrarySelect();
    } catch (error) {
        showError('Nie udało się załadować listy bibliotek: ' + (error as Error).message);
    }
}

function updateLibrarySelect(): void {
    const select = document.getElementById('librarySelect') as HTMLSelectElement | null;
    if (!select) return;
    
    select.innerHTML = '<option value="">Wybierz bibliotekę...</option>';
    libraries.forEach(lib => {
        const option = document.createElement('option');
        option.value = lib.id;
        option.textContent = lib.name || lib.id;
        select.appendChild(option);
    });
}

export function displayAccounts(accounts: AccountResponse[]): void {
    // Update refresh time
    const refreshTime = new Date().toLocaleString('pl-PL');
    const refreshTimeEl = document.getElementById('refreshTime');
    if (refreshTimeEl) {
        refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;
    }

    hideError();

    // Save current accounts for editing
    localStorage.setItem('currentAccounts', JSON.stringify(accounts));

    const tbody = document.getElementById('accountsTableBody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (accounts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">Brak kont</td></tr>';
        updateAccountStats(0, 0, 0);
        return;
    }

    // Calculate statistics
    const total = accounts.length;
    const enabled = accounts.filter(a => a.enabled).length;
    const disabled = total - enabled;

    updateAccountStats(total, enabled, disabled);

    // Create table rows
    accounts.forEach(account => {
        const row = document.createElement('tr');
        
        let statusClass = 'renewable';
        let statusText = 'Aktywne';
        if (!account.enabled) {
            statusClass = 'not-renewable';
            statusText = 'Nieaktywne';
        }

        row.innerHTML = `
            <td>${account.id || 'Brak'}</td>
            <td>${account.libraryDisplayName || account.library || 'Brak'}</td>
            <td>${account.username || 'Brak'}</td>
            <td class="${statusClass}">${statusText}</td>
            <td>
                <button onclick="editAccount(${account.id})" style="padding: 5px 10px; margin-right: 5px; background-color: #3498db;" title="Edytuj">Edytuj</button>
                <button onclick="deleteAccount(${account.id})" style="padding: 5px 10px; background-color: #e74c3c;" title="Usuń">Usuń</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function updateAccountStats(total: number, enabled: number, disabled: number): void {
    const totalEl = document.getElementById('totalAccounts');
    const enabledEl = document.getElementById('enabledAccounts');
    const disabledEl = document.getElementById('disabledAccounts');
    
    if (totalEl) totalEl.textContent = total.toString();
    if (enabledEl) enabledEl.textContent = enabled.toString();
    if (disabledEl) disabledEl.textContent = disabled.toString();
}

export function showAddForm(): void {
    currentEditId = null;
    const formTitle = document.getElementById('formTitle');
    if (formTitle) formTitle.textContent = 'Dodaj Nowe Konto';
    
    setFormValues('', '', '', true);
    showForm();
}

export function showEditForm(account: AccountFormData): void {
    currentEditId = account.id;
    const formTitle = document.getElementById('formTitle');
    if (formTitle) formTitle.textContent = 'Edytuj Konto';
    
    setFormValues(account.library, account.username, '', account.enabled);
    showForm();
}

function setFormValues(library: string, username: string, password: string, enabled: boolean): void {
    const librarySelect = document.getElementById('librarySelect') as HTMLSelectElement | null;
    const usernameInput = document.getElementById('usernameInput') as HTMLInputElement | null;
    const passwordInput = document.getElementById('passwordInput') as HTMLInputElement | null;
    const enabledInput = document.getElementById('enabledInput') as HTMLInputElement | null;
    
    if (librarySelect) librarySelect.value = library;
    if (usernameInput) usernameInput.value = username;
    if (passwordInput) passwordInput.value = password;
    if (enabledInput) enabledInput.checked = enabled;
}

function showForm(): void {
    const form = document.getElementById('accountForm');
    if (form) form.style.display = 'block';
}

export function hideForm(): void {
    const form = document.getElementById('accountForm');
    if (form) form.style.display = 'none';
    currentEditId = null;
}

export async function saveAccount(): Promise<void> {
    const librarySelect = document.getElementById('librarySelect') as HTMLSelectElement | null;
    const usernameInput = document.getElementById('usernameInput') as HTMLInputElement | null;
    const passwordInput = document.getElementById('passwordInput') as HTMLInputElement | null;
    const enabledInput = document.getElementById('enabledInput') as HTMLInputElement | null;

    if (!librarySelect || !usernameInput) {
        showError('Proszę uzupełnić wszystkie wymagane pola');
        return;
    }

    const library = librarySelect.value;
    const username = usernameInput.value;
    const password = passwordInput?.value || '';
    const enabled = enabledInput?.checked || false;

    if (!library || !username) {
        showError('Proszę uzupełnić wszystkie wymagane pola');
        return;
    }

    try {
        const requestBody: AccountRequest = { library, username, password, enabled };
        
        let response;
        if (currentEditId) {
            response = await fetch(`/api/accounts/${currentEditId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });
        } else {
            response = await fetch('/api/accounts', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });
        }

        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }

        hideForm();
        await loadAccounts();
        
    } catch (error) {
        showError('Nie udało się zapisać konta: ' + (error as Error).message);
    }
}

export async function deleteAccount(id: number): Promise<void> {
    if (!confirm('Czy na pewno chcesz usunąć to konto?')) {
        return;
    }

    try {
        const response = await fetch(`/api/accounts/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }

        await loadAccounts();
        
    } catch (error) {
        showError('Nie udało się usunąć konta: ' + (error as Error).message);
    }
}

export function editAccount(id: number): void {
    const accountsJson = localStorage.getItem('currentAccounts');
    if (!accountsJson) {
        showError('Nie znaleziono kont do edycji');
        return;
    }
    
    const accounts: AccountResponse[] = JSON.parse(accountsJson);
    const account = accounts.find(a => a.id === id);
    if (account) {
        showEditForm({
            id: account.id,
            library: account.library,
            username: account.username,
            password: '',
            enabled: account.enabled
        });
    } else {
        showError('Nie znaleziono konta do edycji');
    }
}

// Reuse error functions from loans module
export function showError(message: string): void {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

export function hideError(): void {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

// Attach all public functions to window object to survive Vite minification
(window as any).loadAccounts = loadAccounts;
(window as any).loadLibraries = loadLibraries;
(window as any).displayAccounts = displayAccounts;
(window as any).showAddForm = showAddForm;
(window as any).showEditForm = showEditForm;
(window as any).setFormValues = setFormValues;
(window as any).showForm = showForm;
(window as any).hideForm = hideForm;
(window as any).saveAccount = saveAccount;
(window as any).deleteAccount = deleteAccount;
(window as any).editAccount = editAccount;
(window as any).updateLibrarySelect = updateLibrarySelect;
(window as any).updateAccountStats = updateAccountStats;
(window as any).showError = showError;
(window as any).hideError = hideError;

// Initialize on page load
if (window.location.pathname.includes('accounts')) {
    window.addEventListener('DOMContentLoaded', loadAccounts);
}
