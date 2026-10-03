// Accounts functionality - for accounts.html
let libraries = [];
let currentEditId = null;

async function loadAccounts() {
    try {
        await loadLibraries();
        const response = await fetch('/api/accounts');
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        const data = await response.json();
        displayAccounts(data);
    } catch (error) {
        showError('Nie udało się załadować kont: ' + error.message);
    }
}

async function loadLibraries() {
    try {
        const response = await fetch('/api/accounts/libraries');
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        libraries = await response.json();
        updateLibrarySelect();
    } catch (error) {
        showError('Nie udało się załadować listy bibliotek: ' + error.message);
    }
}

function updateLibrarySelect() {
    const select = document.getElementById('librarySelect');
    if (!select) return;
    select.innerHTML = '<option value="">Wybierz bibliotekę...</option>';
    libraries.forEach(lib => {
        const option = document.createElement('option');
        option.value = lib.id;
        option.textContent = lib.name || lib.id;
        select.appendChild(option);
    });
}

function displayAccounts(accounts) {
    const refreshTime = new Date().toLocaleString('pl-PL');
    const refreshTimeEl = document.getElementById('refreshTime');
    if (refreshTimeEl) refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;
    hideError();
    localStorage.setItem('currentAccounts', JSON.stringify(accounts));

    const tbody = document.getElementById('accountsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (accounts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">Brak kont</td></tr>';
        updateAccountStats(0, 0, 0);
        return;
    }

    const total = accounts.length, enabled = accounts.filter(a => a.enabled).length, disabled = total - enabled;
    updateAccountStats(total, enabled, disabled);

    accounts.forEach(account => {
        const row = document.createElement('tr');
        let statusClass = account.enabled ? 'renewable' : 'not-renewable';
        let statusText = account.enabled ? 'Aktywne' : 'Nieaktywne';
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

function updateAccountStats(total, enabled, disabled) {
    const el = (id) => document.getElementById(id);
    if (el('totalAccounts')) el('totalAccounts').textContent = total;
    if (el('enabledAccounts')) el('enabledAccounts').textContent = enabled;
    if (el('disabledAccounts')) el('disabledAccounts').textContent = disabled;
}

function showAddForm() {
    currentEditId = null;
    const formTitle = document.getElementById('formTitle');
    if (formTitle) formTitle.textContent = 'Dodaj Nowe Konto';
    setFormValues('', '', '', true);
    showForm();
}

function showEditForm(account) {
    currentEditId = account.id;
    const formTitle = document.getElementById('formTitle');
    if (formTitle) formTitle.textContent = 'Edytuj Konto';
    setFormValues(account.library, account.username, '', account.enabled);
    showForm();
}

function setFormValues(library, username, password, enabled) {
    const el = (id) => document.getElementById(id);
    if (el('librarySelect')) el('librarySelect').value = library;
    if (el('usernameInput')) el('usernameInput').value = username;
    if (el('passwordInput')) el('passwordInput').value = password;
    if (el('enabledInput')) el('enabledInput').checked = enabled;
}

function showForm() { const form = document.getElementById('accountForm'); if (form) form.style.display = 'block'; }
function hideForm() { const form = document.getElementById('accountForm'); if (form) form.style.display = 'none'; currentEditId = null; }

async function saveAccount() {
    const el = (id) => document.getElementById(id);
    const library = el('librarySelect')?.value, username = el('usernameInput')?.value;
    const password = el('passwordInput')?.value || '', enabled = el('enabledInput')?.checked || false;
    if (!library || !username) { showError('Proszę uzupełnić wszystkie wymagane pola'); return; }
    try {
        const requestBody = { library, username, password, enabled };
        let response;
        if (currentEditId) response = await fetch(`/api/accounts/${currentEditId}`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestBody) });
        else response = await fetch('/api/accounts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestBody) });
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        hideForm(); await loadAccounts();
    } catch (error) { showError('Nie udało się zapisać konta: ' + error.message); }
}

async function deleteAccount(id) {
    if (!confirm('Czy na pewno chcesz usunąć to konto?')) return;
    try {
        const response = await fetch(`/api/accounts/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        await loadAccounts();
    } catch (error) { showError('Nie udało się usunąć konta: ' + error.message); }
}

function editAccount(id) {
    const accountsJson = localStorage.getItem('currentAccounts');
    if (!accountsJson) { showError('Nie znaleziono kont do edycji'); return; }
    const accounts = JSON.parse(accountsJson);
    const account = accounts.find(a => a.id === id);
    if (account) showEditForm({ id: account.id, library: account.library, username: account.username, password: '', enabled: account.enabled });
    else showError('Nie znaleziono konta do edycji');
}

function showError(message) {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function hideError() {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) errorDiv.style.display = 'none';
}

// Tab switching functions
function switchTab(tabName) {
    const contents = document.querySelectorAll('.tab-content');
    const buttons = document.querySelectorAll('.tab-button');
    
    contents.forEach(c => c.style.display = 'none');
    buttons.forEach(b => b.classList.remove('active'));
    
    const content = document.getElementById(tabName + '-content');
    if (content) content.style.display = 'block';
    
    const button = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
    if (button) button.classList.add('active');
}

// Initialize on page load
window.addEventListener('DOMContentLoaded', () => {
    const pathname = window.location.pathname;
    if (pathname.includes('accounts')) {
        loadAccounts();
    } else {
        loadLoans();
    }
});

// Make functions globally available
window.loadAccounts = loadAccounts;
window.showAddForm = showAddForm;
window.hideForm = hideForm;
window.editAccount = editAccount;
window.deleteAccount = deleteAccount;
window.saveAccount = saveAccount;
window.switchTab = switchTab;
window.showError = showError;
window.hideError = hideError;