import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Pass StoreKitBridgeImpl() to enable in-app purchases.
        // Remove the argument (or pass nil) if your app does not use IAP.
        MainViewControllerKt.startKoinIos(storeKitBridge: StoreKitBridgeImpl())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
