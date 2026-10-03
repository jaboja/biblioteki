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

// Tab switching functionality
(window as any).switchTab = function(tabName: string) {
    // Hide all tab contents
    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => {
        (content as HTMLElement).style.display = 'none';
    });
    
    // Remove active class from all tab buttons
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => {
        button.classList.remove('active');
    });
    
    // Show selected tab content
    const selectedContent = document.getElementById(tabName + '-content');
    if (selectedContent) {
        selectedContent.style.display = 'block';
    }
    
    // Add active class to selected tab button
    const selectedButton = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
    if (selectedButton) {
        selectedButton.classList.add('active');
    }
    
    // Load data for the selected tab
    if (tabName === 'accounts') {
        loadAccounts();
    } else if (tabName === 'loans') {
        loadLoans();
    }
};

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    const pathname = window.location.pathname;
    
    // Default to loans tab
    if (pathname.endsWith('/') || pathname.endsWith('/index.html')) {
        (window as any).switchTab('loans');
    }
});