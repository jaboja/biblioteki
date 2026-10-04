import Foundation

/// Wysyła żądanie prolongaty jednego wypożyczenia do Primo VE.
/// Endpoint i parametry wywnioskowane z ruchu sieciowego – Primo używa POST na /primaws/rest/priv/myaccount/renew_loans
actor RenewClient {

    let definition: LibraryDefinition
    private let session: URLSession

    init(definition: LibraryDefinition, session: URLSession) {
        self.definition = definition
        self.session = session
    }

    func renew(loanID: String, jwt: String) async throws {
        // loanID w Loan.id ma postać "LIBRARYID_rawLoanId" – wyodrębnij rawLoanId
        let rawID = loanID.components(separatedBy: "_").dropFirst().joined(separator: "_")

        var comps = URLComponents(string:
            definition.baseURL + "/primaws/rest/priv/myaccount/renew_loans"
        )!
        comps.queryItems = [
            .init(name: "lang", value: "pl"),
        ]

        var request = URLRequest(url: comps.url!)
        request.httpMethod = "POST"
        request.setValue("Bearer \"\(jwt)\"", forHTTPHeaderField: "Authorization")
        request.setValue("application/json;charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.setValue("no-cache", forHTTPHeaderField: "Pragma")
        request.setValue("same-origin", forHTTPHeaderField: "Sec-Fetch-Site")
        request.setValue("cors", forHTTPHeaderField: "Sec-Fetch-Mode")
        request.setValue("follow", forHTTPHeaderField: "Sec-Fetch-Redirect")
        request.setValue(NetworkConstants.userAgent, forHTTPHeaderField: "User-Agent")
        if definition.isNDE { request.setValue("true", forHTTPHeaderField: "is-nde") }

        let body: [String: Any] = ["id": rawID]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (_, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode)
        else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw PrimoError.httpError(code, comps.url)
        }
    }
}
