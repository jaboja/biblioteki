import Foundation
import Observation
import WidgetKit

private let kAccountsKey = "library_accounts_v1"

/// Centralny store kont bibliotecznych.
/// Udostępniany przez App Group, by widget mógł go odczytać.
@Observable
final class AccountStore {

    // Klucz App Group – musi być zarejestrowany w Capabilities obu targetów
    static let appGroupID = "group.pl.jaboja.LibraryWidget"

    private var defaults: UserDefaults {
        UserDefaults(suiteName: Self.appGroupID) ?? .standard
    }

    private(set) var accounts: [LibraryAccount] = []

    init() {
        load()
    }

    // MARK: - CRUD

    func add(libraryID: String, username: String, password: String) {
        let account = LibraryAccount(libraryID: libraryID, username: username)
        Keychain.save(key: account.keychainKey, value: password)
        accounts.append(account)
        save()
        reloadWidget()
    }

    func update(account: LibraryAccount, username: String, password: String) {
        guard let idx = accounts.firstIndex(where: { $0.id == account.id }) else { return }
        accounts[idx].username = username
        if !password.isEmpty {
            Keychain.save(key: account.keychainKey, value: password)
        }
        save()
        reloadWidget()
    }

    func delete(account: LibraryAccount) {
        Keychain.delete(key: account.keychainKey)
        accounts.removeAll { $0.id == account.id }
        save()
        reloadWidget()
    }

    func setEnabled(_ enabled: Bool, for account: LibraryAccount) {
        guard let idx = accounts.firstIndex(where: { $0.id == account.id }) else { return }
        accounts[idx].isEnabled = enabled
        save()
        reloadWidget()
    }

    func password(for account: LibraryAccount) -> String? {
        Keychain.load(key: account.keychainKey)
    }

    func library(for account: LibraryAccount) -> LibraryDefinition? {
        LibraryDefinition.wroclaw.first { $0.id == account.libraryID }
    }

    // MARK: - Persystencja

    private func save() {
        guard let data = try? JSONEncoder().encode(accounts) else { return }
        defaults.set(data, forKey: kAccountsKey)
    }

    func load() {
        guard let data = defaults.data(forKey: kAccountsKey),
              let decoded = try? JSONDecoder().decode([LibraryAccount].self, from: data)
        else { return }
        accounts = decoded
    }

    // MARK: - Widget reload

    private func reloadWidget() {
        WidgetCenter.shared.reloadAllTimelines()
    }
}
