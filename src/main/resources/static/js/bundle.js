// Biblioteki Frontend Bundle
// This file is generated during Maven build from TypeScript sources in /frontend/
// For now, this is a fallback that works with the current HTML structure

const today = new Date();
today.setHours(0, 0, 0, 0);
const threeDaysFromNow = new Date(today);
threeDaysFromNow.setDate(threeDaysFromNow.getDate() + 3);

// ===== LOANS FUNCTIONS =====
async function loadLoans() {
    try {
        const response = await fetch('/api/loans');
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        const data = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się załadować wypożyczeń: ' + error.message);
    }
}

async function forceRefresh() {
    try {
        const response = await fetch('/api/loans/refresh', { method: 'POST' });
        if (!response.ok) throw new Error(`Błąd HTTP! status: ${response.status}`);
        const data = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się wymusić odświeżenia: ' + error.message);
    }
}

function displayLoans(data) {
    const refreshTime = new Date(data.fetchedAt).toLocaleString('pl-PL');
    const refreshTimeEl = document.getElementById('loans-refresh-time');
    if (refreshTimeEl) refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;

    if (data.errors && data.errors.length > 0) {
        showError('Wystąpiły błędy: ' + data.errors.join(', '));
    } else {
        hideError();
    }

    const loans = data.loans || [];
    const tbody = document.getElementById('loansTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    
    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Brak wypożyczeń</td></tr>';
        return;
    }

    loans.sort((a, b) => {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate) - new Date(b.dueDate);
    });

    let total = loans.length, overdue = 0, dueSoon = 0, renewable = 0;
    loans.forEach(loan => {
        if (loan.dueDate) {
            const dueDate = new Date(loan.dueDate + 'T00:00:00');
            if (dueDate < today) overdue++;
            else if (dueDate <= threeDaysFromNow) dueSoon++;
        }
        if (loan.renewable === true) renewable++;
    });

    updateLoansStats(total, overdue, dueSoon, renewable);

    loans.forEach(loan => {
        const row = document.createElement('tr');
        const dueDate = loan.dueDate ? new Date(loan.dueDate + 'T00:00:00') : null;
        let dueDateClass = '', dueDateText = loan.dueDate || 'Brak daty';
        if (dueDate) {
            const daysDiff = Math.floor((dueDate - today) / (1000 * 60 * 60 * 24));
            if (dueDate < today) { dueDateClass = 'due-overdue'; dueDateText += ` (${Math.abs(daysDiff)} dni po terminie)`; }
            else if (daysDiff <= 3) { dueDateClass = 'due-soon'; dueDateText += ` (${daysDiff} dni do terminu)`; }
        }
        let renewableClass = loan.renewable === true ? 'renewable' : loan.renewable === false ? 'not-renewable' : 'unknown-renewable';
        let renewableText = loan.renewable === true ? 'Tak' : loan.renewable === false ? 'Nie' : 'Nieznany';

        row.innerHTML = `
            <td>${loan.libraryName || loan.libraryId || 'Brak'}</td>
            <td>${loan.title || 'Brak'}</td>
            <td>${loan.author || 'Brak'}</td>
            <td class="${dueDateClass}">${dueDateText}</td>
            <td>${loan.location || 'Brak'}</td>
            <td class="${renewableClass}">${renewableText}</td>
        `;
        tbody.appendChild(row);
    });
}

function updateLoansStats(total, overdue, dueSoon, renewable) {
    const el = (id) => document.getElementById(id);
    if (el('totalLoans')) el('totalLoans').textContent = total;
    if (el('overdueLoans')) el('overdueLoans').textContent = overdue;
    if (el('dueSoonLoans')) el('dueSoonLoans').textContent = dueSoon;
    if (el('renewableLoans')) el('renewableLoans').textContent = renewable;
}

// ===== ACCOUNTS FUNCTIONS =====
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
        option.value = lib.id; option.textContent = lib.name || lib.id;
        select.appendChild(option);
    });
}

function displayAccounts(accounts) {
    const refreshTime = new Date().toLocaleString('pl-PL');
    const refreshTimeEl = document.getElementById('accounts-refresh-time');
    if (refreshTimeEl) refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;
    hideError();
    localStorage.setItem('currentAccounts', JSON.stringify(accounts));

    const tbody = document.getElementById('accountsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (accounts.length === 0) { tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">Brak kont</td></tr>'; updateAccountStats(0, 0, 0); return; }

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
            <td><button onclick="editAccount(${account.id})" style="padding: 5px 10px; margin-right: 5px; background-color: #3498db;" title="Edytuj">Edytuj</button>
                <button onclick="deleteAccount(${account.id})" style="padding: 5px 10px; background-color: #e74c3c;" title="Usuń">Usuń</button></td>
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

// ===== ERROR HANDLING =====
function showError(message) {
    [document.getElementById('loans-errors'), document.getElementById('accounts-errors')].forEach(el => { if (el) { el.textContent = message; el.style.display = 'block'; } });
}
function hideError() {
    [document.getElementById('loans-errors'), document.getElementById('accounts-errors')].forEach(el => { if (el) el.style.display = 'none'; });
}

// ===== TAB SWITCHING =====
function switchTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.querySelectorAll('.tab-button').forEach(b => b.classList.remove('active'));
    const content = document.getElementById(tabName + '-content');
    if (content) content.style.display = 'block';
    const button = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
    if (button) button.classList.add('active');
    if (tabName === 'accounts') loadAccounts(); else if (tabName === 'loans') loadLoans();
}

// ===== INITIALIZATION =====
document.addEventListener('DOMContentLoaded', () => {
    const pathname = window.location.pathname;
    if (pathname.endsWith('/') || pathname.endsWith('/index.html')) switchTab('loans');
});

// Global functions
window.loadLoans = loadLoans; window.forceRefresh = forceRefresh;
window.loadAccounts = loadAccounts; window.showAddForm = showAddForm;
window.hideForm = hideForm; window.editAccount = editAccount;
window.deleteAccount = deleteAccount; window.saveAccount = saveAccount;
window.switchTab = switchTab; window.showError = showError; window.hideError = hideError;