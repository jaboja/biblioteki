// Loans functionality - for index.html
const today = new Date();
today.setHours(0, 0, 0, 0);
const threeDaysFromNow = new Date(today);
threeDaysFromNow.setDate(threeDaysFromNow.getDate() + 3);

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
    const refreshTimeEl = document.getElementById('refreshTime');
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

// Initialize on page load
window.addEventListener('DOMContentLoaded', loadLoans);