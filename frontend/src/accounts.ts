import { AccountRequest, AccountResponse, LibraryDefinition, AccountFormData } from './types';
import { checkedFetch } from './utils/auth';

let libraries: LibraryDefinition[] = [];
let currentEditId: number | null | undefined = null;

async function loadLoansWrapper(): Promise<void> {
    try {
        await loadLibraries();
        await loadAccounts();
    } catch (error) {
        showError('Nie udało się załadować danych: ' + (error as Error).message);
    }
}

async function loadAccounts(): Promise<void> {
    try {
        const response = await checkedFetch('/api/accounts');
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data: AccountResponse[] = await response.json();
        displayAccounts(data);
    } catch (error) {
        showError('Nie udało się załadować kont: ' + (error as Error).message);
    }
}

async function loadLibraries(): Promise<void> {
    try {
        const response = await checkedFetch('/api/accounts/libraries');
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
        return;
    }

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
            <td class="actions">
                <button class="edit-btn" data-id="${account.id}" style="background-color: #3498db" title="Edytuj">Edytuj</button>
                <button class="delete-btn" data-id="${account.id}" style="background-color: #e74c3c" title="Usuń">Usuń</button>
            </td>
        `;
        tbody.appendChild(row);
    });

    // Add event listeners to action buttons
    setupActionButtons();
}

function setupActionButtons(): void {
    // Edit buttons
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            const id = parseInt(target.dataset.id || '0');
            if (!isNaN(id)) {
                editAccount(id);
            }
        });
    });

    // Delete buttons
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            const id = parseInt(target.dataset.id || '0');
            if (!isNaN(id)) {
                deleteAccountWrapper(id);
            }
        });
    });
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

async function saveAccountWrapper(): Promise<void> {
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
            response = await checkedFetch(`/api/accounts/${currentEditId}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });
        } else {
            response = await checkedFetch('/api/accounts', {
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

async function deleteAccountWrapper(id: number): Promise<void> {
    if (!confirm('Czy na pewno chcesz usunąć to konto?')) {
        return;
    }

    try {
        const response = await checkedFetch(`/api/accounts/${id}`, {
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

// Reuse error functions
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

export function initAccountsPage(): void {
    // Set up event listeners for buttons
    const loadAccountsBtn = document.getElementById('loadAccountsBtn');
    const showAddFormBtn = document.getElementById('showAddFormBtn');
    const saveAccountBtn = document.getElementById('saveAccountBtn');
    const hideFormBtn = document.getElementById('hideFormBtn');

    if (loadAccountsBtn) {
        loadAccountsBtn.addEventListener('click', loadLoansWrapper);
    }

    if (showAddFormBtn) {
        showAddFormBtn.addEventListener('click', showAddForm);
    }

    if (saveAccountBtn) {
        saveAccountBtn.addEventListener('click', saveAccountWrapper);
    }

    if (hideFormBtn) {
        hideFormBtn.addEventListener('click', hideForm);
    }

    // Initialize on page load
    loadLoansWrapper();
}

// Auto-initialize if this module is loaded on the accounts page
if (window.location.pathname.endsWith('/accounts.html') || window.location.pathname.includes('/accounts')) {
    document.addEventListener('DOMContentLoaded', initAccountsPage);
}
