import SwiftUI
import WidgetKit

/// Główny widok okna konfiguracyjnego – lista dodanych kont.
struct SettingsRootView: View {
    @Environment(AccountStore.self) private var store
    @State private var showAddSheet = false
    @State private var editingAccount: LibraryAccount?
    @State private var refreshStatus: RefreshStatus = .idle

    var body: some View {
        VStack(spacing: 0) {
            // --- Pasek tytułowy ---
            HStack {
                Image(systemName: "books.vertical.fill")
                    .foregroundStyle(Color.accentColor)
                Text("Wypożyczenia biblioteczne")
                    .font(.headline)
                Spacer()
                refreshButton
            }
            .padding()
            .background(.bar)

            Divider()

            // --- Lista kont ---
            if store.accounts.isEmpty {
                emptyState
            } else {
                List {
                    Section("Konta biblioteczne") {
                        ForEach(store.accounts) { account in
                            AccountRow(account: account)
                                .contextMenu {
                                    Button("Edytuj…") { editingAccount = account }
                                    Button("Usuń", role: .destructive) {
                                        store.delete(account: account)
                                    }
                                }
                        }
                    }
                }
                .listStyle(.inset)
            }

            Divider()

            // --- Pasek dolny ---
            HStack {
                Button(action: { showAddSheet = true }) {
                    Label("Dodaj konto", systemImage: "plus")
                }
                .buttonStyle(.borderedProminent)

                Spacer()

                if case .error(let msg) = refreshStatus {
                    Text(msg)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
                if case .ok = refreshStatus {
                    Label("Odświeżono", systemImage: "checkmark.circle.fill")
                        .font(.caption)
                        .foregroundStyle(.green)
                }
            }
            .padding()
        }
        .sheet(isPresented: $showAddSheet) {
            AddAccountView()
                .environment(store)
        }
        .sheet(item: $editingAccount) { account in
            EditAccountView(account: account)
                .environment(store)
        }
    }

    // MARK: - Odśwież widget

    private var refreshButton: some View {
        Button {
            refreshStatus = .refreshing
            // Wymusza natychmiastowe odświeżenie poza harmonogramem (raz dziennie).
            // WidgetKit wywoła getTimeline, który pobierze nowe dane i ustawi
            // kolejne automatyczne odświeżenie za 24 godziny.
            WidgetCenter.shared.reloadAllTimelines()
            Task {
                // Krótkie opóźnienie, by ProgressView zdążył się pokazać
                try? await Task.sleep(nanoseconds: 800_000_000)
                refreshStatus = .ok
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                refreshStatus = .idle
            }
        } label: {
            if case .refreshing = refreshStatus {
                ProgressView().controlSize(.small)
            } else {
                Label("Odśwież teraz", systemImage: "arrow.clockwise")
                    .labelStyle(.iconOnly)
            }
        }
        .buttonStyle(.borderless)
        .help("Wymuś odświeżenie widgetu (automatycznie: raz dziennie)")
    }

    // MARK: - Pusty stan

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "books.vertical")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("Brak skonfigurowanych kont")
                .font(.title3)
            Text("Dodaj konto biblioteczne, by widget mógł pobierać Twoje wypożyczenia.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 280)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    enum RefreshStatus {
        case idle, refreshing, ok, error(String)
    }
}

// MARK: - Wiersz konta

private struct AccountRow: View {
    @Environment(AccountStore.self) private var store
    let account: LibraryAccount

    var body: some View {
        HStack(spacing: 12) {
            // Ikona biblioteki
            Image(systemName: "building.columns.fill")
                .font(.title2)
                .foregroundStyle(Color.accentColor)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 2) {
                Text(libraryName)
                    .font(.body)
                    .fontWeight(.medium)
                Text(account.username)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Toggle("", isOn: Binding(
                get: { account.isEnabled },
                set: { store.setEnabled($0, for: account) }
            ))
            .labelsHidden()
            .help(account.isEnabled ? "Wyłącz to konto" : "Włącz to konto")
        }
        .padding(.vertical, 4)
        .opacity(account.isEnabled ? 1 : 0.5)
    }

    private var libraryName: String {
        store.library(for: account)?.name ?? account.libraryID
    }
}
