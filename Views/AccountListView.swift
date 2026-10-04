import SwiftUI
import WidgetKit

// MARK: - Stan pobierania dla jednego konta

enum AccountLoansState {
    case idle
    case loading
    case loaded([Loan])
    case error(String)

    var loans: [Loan] {
        if case .loaded(let l) = self { return l }
        return []
    }
}

// MARK: - Główny widok okna

struct SettingsRootView: View {
    @Environment(AccountStore.self) private var store

    @State private var showAddSheet = false
    @State private var editingAccount: LibraryAccount?

    // loansByAccount[account.id] = stan pobierania
    @State private var loansByAccount: [UUID: AccountLoansState] = [:]
    // jwt na czas sesji okna (nie trafia do dysku)
    @State private var jwtByAccount: [UUID: String] = [:]

    @State private var renewStatus: RenewStatus = .idle

    // MARK: - Obliczane

    private var allLoans: [Loan] {
        store.accounts
            .filter(\.isEnabled)
            .flatMap { loansByAccount[$0.id]?.loans ?? [] }
            .sortedByDueDate()
    }

    private var renewableLoans: [Loan] {
        allLoans.filter { $0.renew == true }
    }

    private var isAnyLoading: Bool {
        loansByAccount.values.contains { if case .loading = $0 { return true }; return false }
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            toolbar
            Divider()

            if store.accounts.isEmpty {
                emptyState
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0, pinnedViews: .sectionHeaders) {
                        ForEach(store.accounts) { account in
                            AccountSection(
                                account: account,
                                state: loansByAccount[account.id] ?? .idle,
                                onEdit: { editingAccount = account },
                                onDelete: { store.delete(account: account) },
                                onRenew: { loan in Task { await renew(loan: loan, account: account) } },
                                onToggle: { store.setEnabled($0, for: account) }
                            )
                        }
                    }
                    .padding(.bottom, 8)
                }
            }

            Divider()
            bottomBar
        }
        .frame(minWidth: 560, minHeight: 480)
        .sheet(isPresented: $showAddSheet) {
            AddAccountView().environment(store)
        }
        .sheet(item: $editingAccount) { account in
            EditAccountView(account: account).environment(store)
        }
        .task { await loadAll() }
//        .onChange(of: store.accounts.map(\.id)) { await loadAll() }
    }

    // MARK: - Toolbar

    private var toolbar: some View {
        HStack(spacing: 8) {
            Image(systemName: "books.vertical.fill")
                .foregroundStyle(Color.accentColor)
            Text("Wypożyczenia biblioteczne")
                .font(.headline)
            Spacer()
            if isAnyLoading {
                ProgressView().controlSize(.small)
            }
            refreshButton
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(.bar)
    }

    // MARK: - Pasek dolny

    private var bottomBar: some View {
        HStack {
            // Prolonguj wszystkie
            Button {
                Task { await renewAll() }
            } label: {
                switch renewStatus {
                case .renewingAll:
                    Label("Prolonguję…", systemImage: "arrow.clockwise")
                case .done(let n):
                    Label("Przedłużono \(n)", systemImage: "checkmark.circle.fill")
                default:
                    Label("Prolonguj wszystkie", systemImage: "arrow.clockwise.circle")
                }
            }
            .disabled(renewableLoans.isEmpty || renewStatus == .renewingAll)

            if case .error(let msg) = renewStatus {
                Text(msg).font(.caption).foregroundStyle(.red)
            }

            Spacer()

            Button {
                showAddSheet = true
            } label: {
                Label("Dodaj konto", systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    // MARK: - Odśwież widget

    private var refreshButton: some View {
        Button {
            Task {
                await loadAll()
                WidgetCenter.shared.reloadAllTimelines()
            }
        } label: {
            Label("Odśwież", systemImage: "arrow.clockwise")
                .labelStyle(.iconOnly)
        }
        .buttonStyle(.borderless)
        .help("Odśwież wypożyczenia i wymuś aktualizację widgetu")
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
            Text("Dodaj konto biblioteczne, by wyświetlić wypożyczenia.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 280)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Pobieranie danych

    @MainActor
    private func loadAll() async {
        for account in store.accounts where account.isEnabled {
            await load(account: account)
        }
    }

    @MainActor
    private func load(account: LibraryAccount) async {
        guard account.isEnabled,
              let library = store.library(for: account),
              let password = store.password(for: account)
        else { return }

        loansByAccount[account.id] = .loading

        let client = await PrimoClient(definition: library)
        do {
            try await client.login(username: account.username, password: password)
            let rawLoans = try await client.fetchLoans(type: .active)
            let jwt = await client.currentJWT
            if let jwt { jwtByAccount[account.id] = jwt }

            let loans = rawLoans.compactMap {
                Loan.from(raw: $0, libraryID: library.id, libraryName: library.name, location: library.location)
            }.sortedByDueDate()
            loansByAccount[account.id] = .loaded(loans)
        } catch {
            loansByAccount[account.id] = .error(error.localizedDescription)
        }
    }

    // MARK: - Prolongata

    @MainActor
    private func renew(loan: Loan, account: LibraryAccount) async {
        guard let library = store.library(for: account),
              let jwt = jwtByAccount[account.id]
        else { return }

        let renewer = RenewClient(definition: library, session: URLSession.shared)
        do {
            try await renewer.renew(loanID: loan.id, jwt: jwt)
            // Odśwież listę dla tego konta
            await load(account: account)
        } catch {
            renewStatus = .error(error.localizedDescription)
        }
    }

    @MainActor
    private func renewAll() async {
        renewStatus = .renewingAll
        var count = 0
        for loan in renewableLoans {
            guard let account = store.accounts.first(where: { $0.libraryID == loan.libraryID }) else { continue }
            guard let library = store.library(for: account),
                  let jwt = jwtByAccount[account.id]
            else { continue }
            let renewer = RenewClient(definition: library, session: URLSession.shared)
            do {
                try await renewer.renew(loanID: loan.id, jwt: jwt)
                count += 1
            } catch {
                // kontynuuj mimo błędu pojedynczej prolongaty
            }
        }
        // Odśwież wszystkie konta po zakończeniu
        await loadAll()
        WidgetCenter.shared.reloadAllTimelines()
        renewStatus = .done(count)
        try? await Task.sleep(nanoseconds: 3_000_000_000)
        renewStatus = .idle
    }

    enum RenewStatus: Equatable {
        case idle, renewingAll, done(Int), error(String)
    }
}

// MARK: - Sekcja jednego konta

private struct AccountSection: View {
    @Environment(AccountStore.self) private var store

    let account: LibraryAccount
    let state: AccountLoansState
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onRenew: (Loan) -> Void
    let onToggle: (Bool) -> Void

    var body: some View {
        Section {
            // Lista wypożyczeń (tylko gdy konto aktywne)
            if account.isEnabled {
                switch state {
                case .idle:
                    EmptyView()
                case .loading:
                    HStack {
                        Spacer()
                        ProgressView().controlSize(.small).padding(8)
                        Spacer()
                    }
                case .loaded(let loans):
                    if loans.isEmpty {
                        Text("Brak aktywnych wypożyczeń")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                    } else {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(loans) { loan in
                                AppLoanRow(loan: loan, onRenew: { onRenew(loan) })
                                if loan.id != loans.last?.id {
                                    Divider().padding(.leading, 16)
                                }
                            }
                        }
                        .padding(.bottom, 4)
                    }
                case .error(let msg):
                    Label(msg, systemImage: "exclamationmark.triangle.fill")
                        .font(.caption)
                        .foregroundStyle(.red)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                }
            }
        } header: {
            AccountSectionHeader(
                account: account,
                loanCount: state.loans.count,
                onEdit: onEdit,
                onDelete: onDelete,
                onToggle: onToggle
            )
        }
    }
}

// MARK: - Nagłówek sekcji (sticky)

private struct AccountSectionHeader: View {
    @Environment(AccountStore.self) private var store

    let account: LibraryAccount
    let loanCount: Int
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onToggle: (Bool) -> Void

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "building.columns.fill")
                .foregroundStyle(Color.accentColor)

            VStack(alignment: .leading, spacing: 1) {
                Text(store.library(for: account)?.name ?? account.libraryID)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(account.username)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            if account.isEnabled && loanCount > 0 {
                Text("\(loanCount)")
                    .font(.caption2)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(Color.accentColor.opacity(0.15))
                    .foregroundStyle(Color.accentColor)
                    .clipShape(Capsule())
            }

            Spacer()

            // Menu kontekstowe
            Menu {
                Button("Edytuj…", action: onEdit)
                Button("Usuń", role: .destructive, action: onDelete)
            } label: {
                Image(systemName: "ellipsis.circle")
                    .foregroundStyle(.secondary)
            }
            .menuStyle(.borderlessButton)
            .fixedSize()

            Toggle("", isOn: Binding(
                get: { account.isEnabled },
                set: { onToggle($0) }
            ))
            .labelsHidden()
            .help(account.isEnabled ? "Wyłącz konto" : "Włącz konto")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.bar)
        .opacity(account.isEnabled ? 1 : 0.5)
    }
}

// MARK: - Wiersz wypożyczenia w oknie aplikacji

private struct AppLoanRow: View {
    let loan: Loan
    let onRenew: () -> Void

    @State private var isRenewing = false

    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            // Wskaźnik pilności
            urgencyCircle

            VStack(alignment: .leading, spacing: 2) {
                Text(loan.title)
                    .font(.body)
                    .lineLimit(2)
                HStack(spacing: 6) {
                    if !loan.author.isEmpty {
                        Text(loan.author)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    if let loc = loan.location {
                        Text("·").foregroundStyle(.tertiary).font(.caption)
                        Text(loc)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Spacer()

            // Termin zwrotu
            VStack(alignment: .trailing, spacing: 2) {
                if let due = loan.dueDate {
                    Text(due, style: .date)
                        .font(.callout)
                        .monospacedDigit()
                        .foregroundStyle(loan.isOverdue ? .red : .primary)
                    if let days = loan.daysUntilDue {
                        Text(dueLabelText(days))
                            .font(.caption2)
                            .foregroundStyle(dueForeground(days))
                    }
                }
            }
            .frame(minWidth: 90, alignment: .trailing)

            // Przycisk prolongaty
            Button {
                isRenewing = true
                onRenew()
            } label: {
                if isRenewing {
                    ProgressView().controlSize(.small).frame(width: 24)
                } else {
                    Image(systemName: "arrow.clockwise.circle")
                        .font(.title3)
                }
            }
            .buttonStyle(.borderless)
            .disabled(loan.renew != true || isRenewing)
            .foregroundStyle(loan.renew == true ? Color.accentColor : Color.secondary)
            .help(renewHelp)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var urgencyCircle: some View {
        Group {
            if loan.renew == true {
                Circle()
                    .fill(urgencyColor)
                    .frame(width: 8, height: 8)
            } else if loan.renew == false {
                Rectangle()
                    .fill(urgencyColor)
                    .frame(width: 7, height: 7)
                    .rotationEffect(.degrees(45))
            } else {
                Rectangle()
                    .fill(urgencyColor)
                    .frame(width: 7, height: 7)
            }
        }
    }

    private var urgencyColor: Color {
        guard let days = loan.daysUntilDue else { return .gray }
        if days < 0 { return .red }
        if days <= 3 { return .orange }
        if days <= 7 { return .yellow }
        return .green
    }

    private var renewHelp: String {
        switch loan.renew {
        case true:  return "Prolonguj to wypożyczenie"
        case false: return "Prolongata niedostępna"
        case nil:   return "Status prolongaty nieznany"
        case .some: return ""
        }
    }
}

// MARK: - Helpers

private func dueLabelText(_ days: Int) -> String {
    if days < 0 { return "\(-days) dni po terminie" }
    if days == 0 { return "dziś" }
    if days == 1 { return "jutro" }
    return "za \(days) dni"
}

private func dueForeground(_ days: Int) -> Color {
    if days < 0 { return .red }
    if days <= 3 { return .orange }
    if days <= 7 { return .yellow }
    return .secondary
}
