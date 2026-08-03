import Foundation

/// Wynik pobrania dla jednego konta.
struct FetchResult {
    let account: LibraryAccount
    let loans: [Loan]
    let error: Error?
}

/// Pobiera wypożyczenia ze wszystkich włączonych kont równolegle.
struct LoanFetcher {

    let store: AccountStore

    func fetchAll() async -> [FetchResult] {
        let enabled = store.accounts.filter(\.isEnabled)
        return await withTaskGroup(of: FetchResult.self) { group in
            for account in enabled {
                group.addTask { await fetch(account: account) }
            }
            var results: [FetchResult] = []
            for await result in group { results.append(result) }
            return results
        }
    }

    /// Pobiera wszystkie aktywne wypożyczenia dla jednego konta.
    func fetch(account: LibraryAccount) async -> FetchResult {
        guard let library = store.library(for: account),
              let password = store.password(for: account)
        else {
            return FetchResult(account: account, loans: [], error: nil)
        }

        let client = PrimoClient(definition: library)
        do {
            try await client.login(username: account.username, password: password)
            let rawLoans = try await client.fetchLoans(type: .active)
            await client.logout()

            let loans = rawLoans.compactMap {
                Loan.from(raw: $0, libraryID: library.id, libraryName: library.name, location: library.location)
            }
            return FetchResult(account: account, loans: loans, error: nil)
        } catch {
            return FetchResult(account: account, loans: [], error: error)
        }
    }

    /// Zwraca posortowaną listę wszystkich wypożyczeń ze wszystkich kont.
    func fetchAllLoans() async -> [Loan] {
        let results = await fetchAll()
        return results.flatMap(\.loans).sortedByDueDate()
    }
}
