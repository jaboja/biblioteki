import { Loan, LoansResponse } from './types';
import { checkedFetch, logout } from './utils/auth';

const today = new Date();
today.setHours(0, 0, 0, 0);

async function loadLoans(): Promise<void> {
    try {
        const response = await checkedFetch('/api/loans');
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data: LoansResponse = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się załadować wypożyczeń: ' + (error as Error).message);
    }
}

async function forceRefresh(): Promise<void> {
    try {
        const response = await checkedFetch('/api/loans/refresh', {
            method: 'POST'
        });
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data: LoansResponse = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się wymusić odświeżenia: ' + (error as Error).message);
    }
}

async function renewLoan(loanId: string): Promise<void> {
    try {
        const response = await checkedFetch(`/api/loans/${encodeURIComponent(loanId)}/renew`, {
            method: 'POST'
        });
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        showError('Prolongata udana!');
        // Odśwież listę wypożyczeń po pomyślnej prolongacie
        await loadLoans();
    } catch (error) {
        showError('Nie udało się przedłużyć wypożyczenia: ' + (error as Error).message);
    }
}

export function displayLoans(data: LoansResponse): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Update refresh time
    const refreshTime = new Date(data.fetchedAt).toLocaleString('pl-PL');
    const refreshTimeEl = document.getElementById('refreshTime');
    if (refreshTimeEl) {
        refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;
    }

    // Display errors if any
    if (data.errors && data.errors.length > 0) {
        showError('Wystąpiły błędy: ' + data.errors.join(', '));
    } else {
        hideError();
    }

    const loans = data.loans || [];
    const tbody = document.getElementById('loansTableBody');
    
    if (!tbody) return;
    
    // Clear existing rows
    tbody.innerHTML = '';
    
    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center;">Brak wypożyczeń</td></tr>';
        return;
    }

    // Sort loans by due date (soonest first)
    loans.sort((a: Loan, b: Loan) => {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    });

    // Create table rows
    loans.forEach(loan => {
        const row = document.createElement('tr');
        
        const dueDate = loan.dueDate ? new Date(loan.dueDate + 'T00:00:00') : null;
        let dueDateClass = '';
        let dueDateText = loan.dueDate || 'Brak daty';
        
        if (dueDate) {
            const daysDiff = Math.floor((dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
            if (dueDate < today) { 
                dueDateClass = 'due-overdue';
                dueDateText += ` (${Math.abs(daysDiff)} dni po terminie)`;
            } else if (daysDiff <= 3) { 
                dueDateClass = 'due-soon';
                dueDateText += ` (${daysDiff} dni do terminu)`;
            }
        }

        let renewableClass = 'unknown-renewable';
        if (loan.renewable === true) {
            renewableClass = 'renewable';
        } else if (loan.renewable === false) {
            renewableClass = 'not-renewable';
        }

        let title = loan.title || 'Brak';
        let i = title.indexOf('/');
        if (i >= 0) {
            let subtitle = title.substring(i + 1).trim();
            title = title.substring(0, i).trim();
            title = `<h2>${title}</h2><h3>${subtitle}</h3>`;
        } else {
            title = `<h2>${title}</h2>`;
        }

        let location = loan.libraryName || loan.libraryId;
        location = location ? `<b>${location}</b> ` : '';
        if (loan.location) location += loan.location;

        let renewButton = '';
        if (loan.renewable === true) {
            renewButton = `<button class="renew-btn" data-loan-id="${loan.id}" title="Przedłuż">Przedłuż</button>`;
        }

        row.innerHTML = `
            <td class="due ${dueDateClass} ${renewableClass}">${dueDateText}</td>
            <td>${title}</td>
            <td>${location || 'Brak'}</td>
            <td class="actions">${renewButton}</td>
        `;
        tbody.appendChild(row);
    });
}

// Error handling functions
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

export function initLoansPage(): void {
    // Set up event listeners for buttons
    const loadLoansBtn = document.getElementById('loadLoansBtn');
    const forceRefreshBtn = document.getElementById('forceRefreshBtn');
    const logoutLink = document.getElementById('logoutLink');

    if (loadLoansBtn) {
        loadLoansBtn.addEventListener('click', loadLoans);
    }

    if (forceRefreshBtn) {
        forceRefreshBtn.addEventListener('click', forceRefresh);
    }

    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            logout();
        });
    }

    // Set up event listeners for renew buttons (using event delegation)
    const loansTableBody = document.getElementById('loansTableBody');
    if (loansTableBody) {
        loansTableBody.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            if (target.classList.contains('renew-btn')) {
                const loanId = target.dataset.loanId;
                if (loanId) {
                    renewLoan(loanId);
                }
            }
        });
    }

    // Initialize on page load
    loadLoans();
}

// Auto-initialize if this module is loaded on the loans page
if (window.location.pathname.endsWith('/') || window.location.pathname.endsWith('/index.html')) {
    document.addEventListener('DOMContentLoaded', initLoansPage);
}

export { loadLoans, forceRefresh, renewLoan };