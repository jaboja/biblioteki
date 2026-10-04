import { Loan, LoansResponse } from './types';

const today = new Date();
today.setHours(0, 0, 0, 0);
const threeDaysFromNow = new Date(today);
threeDaysFromNow.setDate(threeDaysFromNow.getDate() + 3);

async function loadLoans(): Promise<void> {
    try {
        const response = await fetch('/api/loans');
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
        const response = await fetch('/api/loans/refresh', {
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

export function displayLoans(data: LoansResponse): void {
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
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Brak wypożyczeń</td></tr>';
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

        row.innerHTML = `
            <td class="due ${dueDateClass} ${renewableClass}">${dueDateText}</td>
            <td>${title}</td>
            <td>${location || 'Brak'}</td>
        `;
        tbody.appendChild(row);
    });
}

// Error handling functions
function showError(message: string): void {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function hideError(): void {
    const errorDiv = document.getElementById('errors');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

// Attach all functions to window object to survive Vite minification
(window as any).loadLoans = loadLoans;
(window as any).forceRefresh = forceRefresh;
(window as any).displayLoans = displayLoans;
(window as any).showError = showError;
(window as any).hideError = hideError;

// Initialize on page load
if (window.location.pathname.endsWith('/') || window.location.pathname.endsWith('/index.html')) {
    window.addEventListener('DOMContentLoaded', loadLoans);
}

export {};