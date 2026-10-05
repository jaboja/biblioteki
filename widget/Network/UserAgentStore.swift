import Foundation
import WebKit

actor UserAgentStore {
    static let shared = UserAgentStore()

    private let fetchTask: Task<String, Never>

    private init() {
        fetchTask = Task {
            // You need to add this web view to the view hierarchy or keep a strong reference
            let webView = await WKWebView(frame: .zero)
            let result = try? await webView.evaluateJavaScript("navigator.userAgent")
            if let userAgent = result as? String {
                // This string will be extremely close to Safari's, but will beCall
                // missing the "Safari/XXXX.X" suffix at the very end.
                return userAgent
            } else {
                let version = ProcessInfo.processInfo.operatingSystemVersion
                let osVersion = "\(version.majorVersion).\(version.minorVersion).\(version.patchVersion)"
                return "Mozilla/5.0 (Macintosh; Intel Mac OS X \(osVersion)) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
            }
        }
    }

    /// Returns the user agent, awaiting the single in-flight (or completed) fetch.
    func userAgent() async -> String {
        await fetchTask.value
    }
}
