import Foundation

/// Znormalizowane wypożyczenie – niezależne od formatu konkretnej biblioteki.
struct Loan: Identifiable, Codable, Hashable {
    let id: String          // unikalne w kontekście biblioteki (np. loan_id z API)
    let libraryID: String   // skąd pochodzi
    let libraryName: String
    let title: String
    let author: String
    let dueDate: Date?
    let location: String?
    let renew: Bool?

    /// Czy termin zwrotu minął?
    var isOverdue: Bool {
        guard let due = dueDate else { return false }
        return due < Date()
    }

    /// Liczba dni do oddania (ujemna = po terminie).
    var daysUntilDue: Int? {
        guard let due = dueDate else { return nil }
        return Calendar.current.dateComponents([.day], from: .now, to: due).day
    }
}

// MARK: - Parsowanie odpowiedzi Primo

extension Loan {
    /// Parsuje surowy słownik z tablicy `data.loans.loan` zwracanej przez API.
    static func from(
        raw: [String: Any],
        libraryID: String,
        libraryName: String,
        location: String?,
    ) -> Loan? {
        // Primo zwraca pola jako tablice jednoelementowe lub stringi – obsługujemy oba
        func str(_ key: String) -> String? {
            if let arr = raw[key] as? [String] { return arr.first }
            return raw[key] as? String
        }

        let title = str("title") ?? "(brak tytułu)"
        let author = str("author") ?? ""
        let location = stripPrefix(from: location ?? str("mainlocationname"))

        // Termin zwrotu: Primo używa różnych kluczy w zależności od instancji
        let dueDateStr = str("duedate")
        let dueDate = dueDateStr.flatMap { Self.parseDate($0) }

        let renewStr = str("renew")
        let renew = renewStr == "Y" ? true : renewStr == "N" ? false : nil

        let loanID = str("loanid") ?? str("id") ?? UUID().uuidString

        return Loan(
            id: "\(libraryID)_\(loanID)",
            libraryID: libraryID,
            libraryName: libraryName,
            title: title,
            author: author,
            dueDate: dueDate,
            location: location,
            renew: renew,
        )
    }
    
    static func stripPrefix(from location: String?) -> String? {
        if let location = location, location.contains(" - "),
           let separatorRange = location.range(of: " - ")
        {
            return String(location[separatorRange.upperBound...])
        } else {
            return location
        }
    }

    // MARK: - Parsowanie daty

    private static let dateFormatters: [DateFormatter] = {
        // Primo używa różnych formatów w różnych instancjach
        let formats = [
            "yyyyMMdd",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy",
            "yyyy-MM-dd",
        ]
        return formats.map { fmt in
            let f = DateFormatter()
            f.dateFormat = fmt
            f.locale = Locale(identifier: "en_US_POSIX")
            return f
        }
    }()

    static func parseDate(_ string: String) -> Date? {
        for fmt in dateFormatters {
            if let date = fmt.date(from: string) { return date }
        }
        return nil
    }
}

// MARK: - Sortowanie

extension [Loan] {
    /// Sortuje: najpierw po terminie zwrotu (najwcześniejszy pierwszy),
    /// bez daty na końcu; wewnątrz grupy alfabetycznie po tytule.
    func sortedByDueDate() -> [Loan] {
        sorted { a, b in
            switch (a.dueDate, b.dueDate) {
            case let (.some(da), .some(db)): return da < db
            case (.some, .none):             return true
            case (.none, .some):             return false
            case (.none, .none):             return a.title < b.title
            }
        }
    }
}
