import { Loan, LoansResponse } from './types';

const today = new Date();
today.setHours(0, 0, 0, 0);
const threeDaysFromNow = new Date(today);
threeDaysFromNow.setDate(threeDaysFromNow.getDate() + 3);

export async function loadLoans(): Promise<void> {
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

export async function forceRefreshLoans(): Promise<void> {
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

    // Calculate statistics
    let total = loans.length;
    let overdue = 0;
    let dueSoon = 0;
    let renewable = 0;

    loans.forEach(loan => {
        if (loan.dueDate) {
            const dueDate = new Date(loan.dueDate + 'T00:00:00');
            if (dueDate < today) {
                overdue++;
            } else if (dueDate <= threeDaysFromNow) {
                dueSoon++;
            }
        }
        if (loan.renewable === true) {
            renewable++;
        }
    });

    // Update statistics
    updateLoansStats(total, overdue, dueSoon, renewable);

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
        let renewableText = 'Nieznany';
        if (loan.renewable === true) {
            renewableClass = 'renewable';
            renewableText = 'Tak';
        } else if (loan.renewable === false) {
            renewableClass = 'not-renewable';
            renewableText = 'Nie';
        }

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

function updateLoansStats(total: number, overdue: number, dueSoon: number, renewable: number): void {
    const totalEl = document.getElementById('totalLoans');
    const overdueEl = document.getElementById('overdueLoans');
    const dueSoonEl = document.getElementById('dueSoonLoans');
    const renewableEl = document.getElementById('renewableLoans');
    
    if (totalEl) totalEl.textContent = total.toString();
    if (overdueEl) overdueEl.textContent = overdue.toString();
    if (dueSoonEl) dueSoonEl.textContent = dueSoon.toString();
    if (renewableEl) renewableEl.textContent = renewable.toString();
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

// Initialize on page load
if (window.location.pathname.endsWith('/') || window.location.pathname.endsWith('/index.html')) {
    window.addEventListener('DOMContentLoaded', loadLoans);
}
