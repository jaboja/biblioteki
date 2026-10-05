import WidgetKit
import SwiftUI

// MARK: - Timeline Entry

struct LoanEntry: TimelineEntry {
    let date: Date
    let loans: [Loan]
    let fetchErrors: [String]   // nazwy bibliotek z błędem

    static var placeholder: LoanEntry {
        LoanEntry(
            date: .now,
            loans: [
                Loan(
                    id: "preview_1",
                    libraryID: "MBP",
                    libraryName: "Miejska Biblioteka Publiczna (Wrocław)",
                    title: "Solaris",
                    author: "Stanisław Lem",
                    dueDate: Calendar.current.date(byAdding: .day, value: 3, to: .now),
                    location: nil,
                    renew: true,
                ),
                Loan(
                    id: "preview_2",
                    libraryID: "DBP",
                    libraryName: "Dolnośląska Biblioteka Publiczna (Wrocław)",
                    title: "Fiasco",
                    author: "Stanisław Lem",
                    dueDate: Calendar.current.date(byAdding: .day, value: 12, to: .now),
                    location: nil,
                    renew: false,
                ),
            ],
            fetchErrors: []
        )
    }
}

// MARK: - Timeline Provider

struct LoanTimelineProvider: TimelineProvider {

    private func makeStore() -> AccountStore { AccountStore() }

    func placeholder(in context: Context) -> LoanEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (LoanEntry) -> Void) {
        if context.isPreview {
            completion(.placeholder)
            return
        }
        Task {
            let entry = await fetchEntry()
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<LoanEntry>) -> Void) {
        Task {
            let entry = await fetchEntry()
            // Odśwież raz dziennie – o tej samej godzinie co teraz, następnego dnia.
            // Użytkownik może wymusić odświeżenie z aplikacji przez WidgetCenter.
            let nextRefresh = Calendar.current.date(byAdding: .day, value: 1, to: .now)!
            let timeline = Timeline(entries: [entry], policy: .after(nextRefresh))
            completion(timeline)
        }
    }

    // MARK: - Pobieranie danych

    private func fetchEntry() async -> LoanEntry {
        let store = makeStore()
        let fetcher = LoanFetcher(store: store)
        let results = await fetcher.fetchAll()

        let loans = results.flatMap(\.loans).sortedByDueDate()
        let errors = results.compactMap { result -> String? in
            guard result.error != nil,
                  let lib = store.library(for: result.account)
            else { return nil }
            return lib.name
        }

        return LoanEntry(date: .now, loans: loans, fetchErrors: errors)
    }
}

// MARK: - Widget konfiguracja

struct LibraryLoanWidget: Widget {
    let kind = "LibraryLoanWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: LoanTimelineProvider()) { entry in
            LoanWidgetView(entry: entry)
                .containerBackground(.background, for: .widget)
        }
        .configurationDisplayName("Wypożyczenia biblioteczne")
        .description("Wyświetla aktualnie wypożyczone książki posortowane według terminu zwrotu.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - Entry point rozszerzenia

@main
struct LibraryWidgetBundle: WidgetBundle {
    var body: some Widget {
        LibraryLoanWidget()
    }
}
