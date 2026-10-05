import Foundation

// MARK: - Błędy

enum PrimoError: LocalizedError {
    case httpError(Int, URL?)
    case authFailed(String)
    case missingJWT
    case notLoggedIn
    case decodingError

    var errorDescription: String? {
        switch self {
        case .httpError(let code, let url): return "HTTP \(code) dla \(url?.absoluteString ?? "?")"
        case .authFailed(let msg):          return "Błąd logowania: \(msg)"
        case .missingJWT:                   return "Brak tokenu JWT w odpowiedzi"
        case .notLoggedIn:                  return "Wymagane logowanie"
        case .decodingError:                return "Błąd parsowania odpowiedzi"
        }
    }
}

class RedirectCatchingSessionDelegate: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession,
                    task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        debugPrint("Intercepted redirect with status: \(response.statusCode)")
        debugPrint("Request headers: \(request.allHTTPHeaderFields ?? [:])")
        debugPrint("Headers: \(response.allHeaderFields)")
        completionHandler(nil)
    }
}

// MARK: - Klient

/// Klient HTTP dla Ex Libris Primo VE.
/// Odpowiednik klasy PrimoClient z primo_client.py.
actor PrimoClient {

    let definition: LibraryDefinition

    private var jwt: String?
    private var jwtAcquiredAt: Date?
    private let jwtTTL: TimeInterval = 14 * 60   // 14 minut (z marginesem)

    private let session: URLSession

    init(definition: LibraryDefinition) async {
        self.definition = definition
        let config = URLSessionConfiguration.ephemeral
        let ua: String = await UserAgentStore.shared.userAgent()
        config.httpAdditionalHeaders = [
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "pl-PL,pl;q=0.9",
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Pragma": "no-cache",
            "Priority": "u=3, i",
            "User-Agent": ua,
        ]
        self.session = URLSession(configuration: config, delegate: RedirectCatchingSessionDelegate(), delegateQueue: nil)
    }

    // MARK: - Logowanie

    var isLoggedIn: Bool {
        guard let acquiredAt = jwtAcquiredAt else { return false }
        return Date().timeIntervalSince(acquiredAt) < jwtTTL
    }

    var currentJWT: String? {
        jwt
    }

    func login(username: String, password: String) async throws {
        // Init session cookies
        let initUrl = URL(string: definition.baseURL + "/nde/login?vid=\(definition.vid)&lang=pl")!
        _ = try await session.data(from: initUrl)

        var components = URLComponents()
        components.queryItems = [
            .init(name: "authenticationProfile", value: "Alma"),
            .init(name: "username", value: username),
            .init(name: "password", value: password),
            .init(name: "view", value: definition.vid),
            .init(name: "institution", value: definition.instCode),
            .init(name: "targetUrl", value: nil),
        ]
        let body = components.percentEncodedQuery.map { Data($0.utf8) } ?? Data()

        let url = URL(string: definition.baseURL + "/primaws/suprimaLogin")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue(definition.baseURL, forHTTPHeaderField: "Origin")
        request.setValue("\(initUrl.absoluteString)", forHTTPHeaderField: "Referer")
        request.setValue("same-origin", forHTTPHeaderField: "Sec-Fetch-Site")
        request.setValue("cors", forHTTPHeaderField: "Sec-Fetch-Mode")
        request.setValue("empty", forHTTPHeaderField: "Sec-Fetch-Dest")
        if definition.isNDE { request.setValue("true", forHTTPHeaderField: "is-nde") }
        request.httpBody = body

        let (data, response) = try await session.data(for: request)
        try assertHTTPOK(response, url: url)

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let rawJWT = json["jwtData"] as? String
        else { throw PrimoError.missingJWT }

        // Serwer zwraca token owinięty cudzysłowami – stripujemy je
        jwt = rawJWT.trimmingCharacters(in: CharacterSet(charactersIn: "\""))
        jwtAcquiredAt = Date()
    }

    func logout() {
        jwt = nil
        jwtAcquiredAt = nil
    }

    // MARK: - Wypożyczenia

    /// Pobiera wypożyczenia i zwraca surową tablicę loan[] z odpowiedzi.
    func fetchLoans(type: LoanType = .active, patronID: String? = nil) async throws -> [[String: Any]] {
        guard isLoggedIn, let token = jwt else { throw PrimoError.notLoggedIn }

        var comps = URLComponents(string: definition.baseURL + "/primaws/rest/priv/myaccount/loans")!
        var queryItems: [URLQueryItem] = [
            .init(name: "lang", value: "pl"),
            .init(name: "bulk", value: "50"),
            .init(name: "offset", value: "1"),
            .init(name: "type", value: type.rawValue),
        ]
        if let pid = patronID { queryItems.append(.init(name: "patron_id", value: pid)) }
        comps.queryItems = queryItems

        var request = URLRequest(url: comps.url!)
        // Primo oczekuje tokenu w cudzysłowach w nagłówku Authorization
        request.setValue("Bearer \"\(token)\"", forHTTPHeaderField: "Authorization")
        request.setValue("same-origin", forHTTPHeaderField: "Sec-Fetch-Site")
        request.setValue("cors", forHTTPHeaderField: "Sec-Fetch-Mode")
        request.setValue("empty", forHTTPHeaderField: "Sec-Fetch-Dest")
        if definition.isNDE { request.setValue("true", forHTTPHeaderField: "is-nde") }

        let (data, response) = try await session.data(for: request)

        if let http = response as? HTTPURLResponse, http.statusCode == 401 {
            throw PrimoError.authFailed("Token wygasł (HTTP 401)")
        }
        try assertHTTPOK(response, url: comps.url!)

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let loansData = (json["data"] as? [String: Any])?["loans"] as? [String: Any],
              let loanArray = loansData["loan"] as? [[String: Any]]
        else { return [] }

        return loanArray
    }

    // MARK: - Helpers

    private func assertHTTPOK(_ response: URLResponse, url: URL) throws {
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw PrimoError.httpError(code, url)
        }
    }
}

// MARK: - Typy pomocnicze

enum LoanType: String {
    case active  = "active"
    case history = "history"
}
