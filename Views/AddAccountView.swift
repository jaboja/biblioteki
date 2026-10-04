import SwiftUI

// MARK: - Dodawanie konta

struct AddAccountView: View {
    @Environment(AccountStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var selectedLibrary: LibraryDefinition = LibraryDefinition.wroclaw[0]
    @State private var username = ""
    @State private var password = ""
    @State private var isValidating = false
    @State private var validationError: String?

    private var canSubmit: Bool {
        !username.trimmingCharacters(in: .whitespaces).isEmpty &&
        !password.isEmpty && !isValidating
    }

    var body: some View {
        formContent(title: "Dodaj konto biblioteczne", submitLabel: "Dodaj") {
            store.add(
                libraryID: selectedLibrary.id,
                username: username.trimmingCharacters(in: .whitespaces),
                password: password
            )
        }
    }

    // Wspólny layout formularza (reused przez EditAccountView)
    @ViewBuilder
    func formContent(
        title: String,
        submitLabel: String,
        onSubmit: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            // Nagłówek
            Text(title)
                .font(.headline)
                .padding()

            Divider()

            // Formularz
            Form {
                Section("Biblioteka") {
                    Picker("System biblioteczny", selection: $selectedLibrary) {
                        ForEach(LibraryDefinition.wroclaw, id: \.id) { lib in
                            Text(lib.name).tag(lib)
                        }
                    }
                    .pickerStyle(.radioGroup)
                }

                Section("Dane logowania") {
                    TextField("Numer karty lub e-mail", text: $username)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.username)
                        .multilineTextAlignment(.leading)
                    SecureField("Hasło", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.password)
                        .multilineTextAlignment(.leading)
                }

                if let error = validationError {
                    Section {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }
            }
            .formStyle(.grouped)
            .padding(.horizontal)

            Divider()

            // Przyciski
            HStack {
                Spacer()
                Button("Anuluj") { dismiss() }
                    .keyboardShortcut(.cancelAction)

                Button(submitLabel) {
                    Task { await validate(onSuccess: onSubmit) }
                }
                .keyboardShortcut(.defaultAction)
                .disabled(!canSubmit)

                if isValidating {
                    ProgressView().controlSize(.small)
                }
            }
            .padding()
        }
        .frame(minWidth: 350, minHeight: 480)
    }

    // MARK: - Walidacja przez próbne logowanie

    func validate(onSuccess: () -> Void) async {
        guard let library = LibraryDefinition.wroclaw.first(where: { $0.id == selectedLibrary.id })
        else { return }

        isValidating = true
        validationError = nil

        let client = await PrimoClient(definition: library)
        do {
            try await client.login(
                username: username.trimmingCharacters(in: .whitespaces),
                password: password
            )
            await client.logout()
            onSuccess()
            dismiss()
        } catch {
            validationError = error.localizedDescription
        }
        isValidating = false
    }
}

// MARK: - Edycja konta

struct EditAccountView: View {
    @Environment(AccountStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    let account: LibraryAccount

    @State private var username: String
    @State private var password = ""
    @State private var isValidating = false
    @State private var validationError: String?

    init(account: LibraryAccount) {
        self.account = account
        _username = State(initialValue: account.username)
    }

    private var library: LibraryDefinition? { store.library(for: account) }

    private var canSubmit: Bool {
        !username.trimmingCharacters(in: .whitespaces).isEmpty && !isValidating
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Edytuj konto – \(library?.name ?? account.libraryID)")
                .font(.headline)
                .padding()

            Divider()

            Form {
                Section("Dane logowania") {
                    TextField("Numer karty lub e-mail", text: $username)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.username)
                        .multilineTextAlignment(.leading)
                    SecureField("Hasło", text: $password)
                        .textFieldStyle(.roundedBorder)
                        .textContentType(.password)
                        .multilineTextAlignment(.leading)
                }

                if let error = validationError {
                    Section {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }
            }
            .formStyle(.grouped)
            .padding(.horizontal)

            Divider()

            HStack {
                Spacer()
                Button("Anuluj") { dismiss() }
                    .keyboardShortcut(.cancelAction)
                Button("Zapisz") {
                    Task { await validate() }
                }
                .keyboardShortcut(.defaultAction)
                .disabled(!canSubmit)
                if isValidating { ProgressView().controlSize(.small) }
            }
            .padding()
        }
        .frame(minWidth: 400, minHeight: 240)
    }

    private func validate() async {
        // Jeśli hasło puste – zapisz bez sprawdzania
        if password.isEmpty {
            store.update(account: account, username: username.trimmingCharacters(in: .whitespaces), password: "")
            dismiss()
            return
        }

        guard let lib = library else { return }
        isValidating = true
        validationError = nil

        let client = await PrimoClient(definition: lib)
        do {
            try await client.login(
                username: username.trimmingCharacters(in: .whitespaces),
                password: password
            )
            await client.logout()
            store.update(
                account: account,
                username: username.trimmingCharacters(in: .whitespaces),
                password: password
            )
            dismiss()
        } catch {
            validationError = error.localizedDescription
        }
        isValidating = false
    }
}
