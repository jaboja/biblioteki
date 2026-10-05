import Foundation
import Security

// MARK: - Predefiniowane biblioteki

struct LibraryDefinition: Identifiable, Hashable, Codable {
    let id: String          // np. "zno"
    let name: String        // np. "Biblioteka ZNO"
    let baseURL: String     // np. "https://omnis-zno.primo.exlibrisgroup.com"
    let vid: String         // np. "48OMNIS_ZNO:ZNO"
    let isNDE: Bool         // Next Discovery Experience (DBP)
    let location: String?

    var instCode: String { vid.components(separatedBy: ":").first ?? vid }

    static let wroclaw: [LibraryDefinition] = [
        LibraryDefinition(
            id: "MBP",
            name: "Miejska Biblioteka Publiczna (Wrocław)",
            baseURL: "https://omnis-mbpwr.primo.exlibrisgroup.com",
            vid: "48OMNIS_MBP:MBP",
            isNDE: false,
            location: nil,
        ),
        LibraryDefinition(
            id: "DBP",
            name: "Dolnośląska Biblioteka Publiczna (Wrocław)",
            baseURL: "https://omnis-dbp.primo.exlibrisgroup.com",
            vid: "48OMNIS_WBP:WBP",
            isNDE: true,
            location: "Rynek",
        ),
        LibraryDefinition(
            id: "ZNO",
            name: "Biblioteka Ossolineum (Wrocław)",
            baseURL: "https://omnis-zno.primo.exlibrisgroup.com",
            vid: "48OMNIS_ZNO:ZNO",
            isNDE: false,
            location: nil,
        ),
    ]
}

// MARK: - Konto użytkownika

/// Dane konta przechowywane w UserDefaults (bez hasła).
struct LibraryAccount: Identifiable, Codable {
    let id: UUID
    let libraryID: String   // odpowiada LibraryDefinition.id
    var username: String
    var isEnabled: Bool

    init(id: UUID = UUID(), libraryID: String, username: String, isEnabled: Bool = true) {
        self.id = id
        self.libraryID = libraryID
        self.username = username
        self.isEnabled = isEnabled
    }

    var keychainKey: String { "primo_password_\(id.uuidString)" }
}

// MARK: - Keychain helper

enum Keychain {
    static func save(key: String, value: String) {
        let data = Data(value.utf8)
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: key,
            kSecValueData: data,
        ]
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    static func load(key: String) -> String? {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: key,
            kSecMatchLimit: kSecMatchLimitOne,
            kSecReturnData: true,
        ]
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func delete(key: String) {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: key,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
