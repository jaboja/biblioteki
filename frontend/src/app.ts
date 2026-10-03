// Main application entry point
// This file is the entry point for Vite bundling

// Import all modules
import './main.ts';
import './loans.ts';
import './accounts.ts';

// Ensure all global functions are available
// The functions are attached to window object in main.ts and the respective modules

declare global {
    interface Window {
        // Loans functions
        loadLoans: () => Promise<void>;
        forceRefresh: () => Promise<void>;
        
        // Accounts functions
        loadAccounts: () => Promise<void>;
        showAddForm: () => void;
        hideForm: () => void;
        editAccount: (id: number) => void;
        deleteAccount: (id: number) => Promise<void>;
        saveAccount: () => Promise<void>;
        
        // Error handling
        showError: (message: string) => void;
        hideError: () => void;
    }
}

// Console log to confirm bundle loaded
console.log('Biblioteki frontend bundle loaded');