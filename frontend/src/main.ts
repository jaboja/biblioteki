// Main application module
// Sets up global functions and tab switching

import { loadLoans, forceRefreshLoans, showError as showLoansError, hideError as hideLoansError } from './loans';
import { 
    loadAccounts, 
    showAddForm, 
    hideForm,
    editAccount,
    deleteAccount,
    saveAccount,
    showError as showAccountsError, 
    hideError as hideAccountsError 
} from './accounts';

// Make functions globally available for HTML onclick handlers
(window as any).loadLoans = loadLoans;
(window as any).forceRefresh = forceRefreshLoans;

(window as any).loadAccounts = loadAccounts;
(window as any).showAddForm = showAddForm;
(window as any).hideForm = hideForm;
(window as any).editAccount = editAccount;
(window as any).deleteAccount = deleteAccount;
(window as any).saveAccount = saveAccount;

// Global error handling
(window as any).showError = function(message: string) {
    showLoansError(message);
    showAccountsError(message);
};

(window as any).hideError = function() {
    hideLoansError();
    hideAccountsError();
};
