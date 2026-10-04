import Foundation

/// Wysyła żądanie prolongaty jednego wypożyczenia do Primo VE.
/// Endpoint i parametry wywnioskowane z ruchu sieciowego analogicznie
/// do fetchLoans – Primo używa POST na /primaws/rest/priv/myaccount/loans/{loanId}/renew
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
            definition.baseURL + "/primaws/rest/priv/myaccount/loans/\(rawID)/renew"
        )!
        comps.queryItems = [
            .init(name: "lang", value: "pl"),
            .init(name: "vid", value: definition.vid),
            .init(name: "inst_code", value: definition.instCode),
        ]

        var request = URLRequest(url: comps.url!)
        request.httpMethod = "POST"
        request.setValue("Bearer \"\(jwt)\"", forHTTPHeaderField: "Authorization")
        request.setValue("same-origin", forHTTPHeaderField: "Sec-Fetch-Site")
        request.setValue("cors", forHTTPHeaderField: "Sec-Fetch-Mode")
        request.setValue("empty", forHTTPHeaderField: "Sec-Fetch-Dest")
        request.setValue("0", forHTTPHeaderField: "Content-Length")
        if definition.isNDE { request.setValue("true", forHTTPHeaderField: "is-nde") }

        let (_, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode)
        else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw PrimoError.httpError(code, comps.url)
        }
    }
}
