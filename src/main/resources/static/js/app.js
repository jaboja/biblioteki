async function loadLoans() {
    try {
        const response = await fetch('/api/loans');
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się załadować wypożyczeń: ' + error.message);
    }
}

async function forceRefresh() {
    try {
        const response = await fetch('/api/loans/refresh', {
            method: 'POST'
        });
        if (!response.ok) {
            throw new Error(`Błąd HTTP! status: ${response.status}`);
        }
        const data = await response.json();
        displayLoans(data);
    } catch (error) {
        showError('Nie udało się wymusić odświeżenia: ' + error.message);
    }
}

function displayLoans(data) {
    // Aktualizuj czas odświeżenia
    const refreshTime = new Date(data.fetchedAt).toLocaleString('pl-PL');
    document.getElementById('refreshTime').textContent = `Ostatnia aktualizacja: ${refreshTime}`;

    // Wyświetl błędy jeśli wystąpiły
    if (data.errors && data.errors.length > 0) {
        showError('Wystąpiły błędy: ' + data.errors.join(', '));
    } else {
        hideError();
    }

    const loans = data.loans || [];
    const tbody = document.getElementById('loansTableBody');
    
    // Wyczyść istniejące wiersze
    tbody.innerHTML = '';
    
    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">Brak wypożyczeń</td></tr>';
        return;
    }

    // Posortuj wypożyczenia według daty zwrotu (najwcześniejsza pierwsza)
    loans.sort((a, b) => {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate) - new Date(b.dueDate);
    });

    // Oblicz statystyki
    let total = loans.length;
    let overdue = 0;
    let dueSoon = 0;
    let renewable = 0;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const threeDaysFromNow = new Date(today);
    threeDaysFromNow.setDate(threeDaysFromNow.getDate() + 3);

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

    // Aktualizuj statystyki
    document.getElementById('totalLoans').textContent = total;
    document.getElementById('overdueLoans').textContent = overdue;
    document.getElementById('dueSoonLoans').textContent = dueSoon;
    document.getElementById('renewableLoans').textContent = renewable;

    // Utwórz wiersze tabeli
    loans.forEach(loan => {
        const row = document.createElement('tr');
        
        const dueDate = loan.dueDate ? new Date(loan.dueDate + 'T00:00:00') : null;
        let dueDateClass = '';
        let dueDateText = loan.dueDate || 'Brak daty';
        
        if (dueDate) {
            const daysDiff = Math.floor((dueDate - today) / (1000 * 60 * 60 * 24));
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

function showError(message) {
    const errorDiv = document.getElementById('errors');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
}

function hideError() {
    const errorDiv = document.getElementById('errors');
    errorDiv.style.display = 'none';
}

// Załaduj wypożyczenia przy ładowaniu strony
window.addEventListener('DOMContentLoaded', loadLoans);