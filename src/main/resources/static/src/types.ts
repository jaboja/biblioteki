// Types for the Biblioteki application

export interface Loan {
    id: string;
    libraryId: string;
    libraryName: string;
    title: string;
    author: string;
    dueDate: string | null;
    location: string | null;
    renewable: boolean | null;
}

export interface LoansResponse {
    fetchedAt: string;
    loans: Loan[];
    errors: string[];
}

export interface LibraryDefinition {
    id: string;
    name: string;
    location: string | null;
}

export interface AccountRequest {
    library: string;
    username: string;
    password: string;
    enabled: boolean;
}

export interface AccountResponse {
    id: number;
    library: string;
    libraryDisplayName: string;
    username: string;
    enabled: boolean;
}

export interface AccountFormData {
    id?: number;
    library: string;
    username: string;
    password: string;
    enabled: boolean;
}

export interface Stats {
    total: number;
    overdue: number;
    dueSoon: number;
    renewable: number;
}

export interface AccountStats {
    total: number;
    enabled: number;
    disabled: number;
}
