//
//  LibraryWidgetApp.swift
//  LibraryWidget
//
//  Created by Jakub Jagiełło on 02/08/2026.
//

import SwiftUI

@main
struct LibraryWidgetApp: App {

    @State private var store = AccountStore()

    var body: some Scene {
        // Okno konfiguracyjne – jedyne okno aplikacji
        Window("Konfiguracja bibliotek", id: "settings") {
            SettingsRootView()
                .environment(store)
                .frame(minWidth: 480, minHeight: 400)
        }
        .windowResizability(.contentMinSize)
        .defaultSize(width: 520, height: 480)
        .commands {
            // Usuń niepotrzebne menu
            CommandGroup(replacing: .newItem) {}
        }
    }
}
