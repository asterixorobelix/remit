import ComposeApp
import StoreKit

/// StoreKit 2 implementation of the Kotlin `StoreKitBridge` protocol for a non-consumable
/// premium product (`IOS_BILLING_PRODUCT` in Info.plist, sourced from app.properties).
///
/// - Verified transactions are finished after the entitlement is recorded, so StoreKit stops
///   re-delivering them.
/// - `Transaction.updates` is observed for the app's lifetime: Ask to Buy approvals, refunds and
///   purchases made on other devices arrive there, not in `purchase()`.
/// - Pending purchases (Ask to Buy, SCA) are reported as `PurchaseStatus.PENDING`, never success.
///
/// Callbacks are delivered on the main actor for safe Kotlin/Native invocation.
public class StoreKitBridgeImpl: NSObject, StoreKitBridge {
    private let productId = Bundle.main.object(forInfoDictionaryKey: "IOS_BILLING_PRODUCT") as? String
        ?? "com.asterixorobelix.remit.premium.monthly"
    private var updatesTask: _Concurrency.Task<Void, Never>?

    deinit {
        updatesTask?.cancel()
    }

    public func checkEntitlement(callback: @escaping (NSNumber) -> Void) {
        _Concurrency.Task {
            let entitled = await self.hasEntitlement()
            await MainActor.run { callback(NSNumber(value: entitled)) }
        }
    }

    public func purchase(callback: @escaping (NSNumber, String?) -> Void) {
        _Concurrency.Task {
            do {
                guard let product = try await Product.products(for: [self.productId]).first else {
                    await self.report(callback, PurchaseStatus.shared.FAILED, "Product not found")
                    return
                }
                switch try await product.purchase() {
                case .success(.verified(let transaction)):
                    await transaction.finish()
                    await self.report(callback, PurchaseStatus.shared.SUCCESS, nil)
                case .success(.unverified(_, let error)):
                    await self.report(callback, PurchaseStatus.shared.FAILED, error.localizedDescription)
                case .pending:
                    await self.report(callback, PurchaseStatus.shared.PENDING, nil)
                case .userCancelled:
                    await self.report(callback, PurchaseStatus.shared.FAILED, nil)
                @unknown default:
                    await self.report(callback, PurchaseStatus.shared.FAILED, "Unknown purchase result")
                }
            } catch {
                await self.report(callback, PurchaseStatus.shared.FAILED, error.localizedDescription)
            }
        }
    }

    public func restorePurchases(callback: @escaping (NSNumber, String?) -> Void) {
        _Concurrency.Task {
            do {
                try await AppStore.sync()
                let entitled = await self.hasEntitlement()
                await MainActor.run {
                    callback(NSNumber(value: entitled), entitled ? nil : "No previous purchase found")
                }
            } catch {
                await MainActor.run { callback(NSNumber(value: false), error.localizedDescription) }
            }
        }
    }

    public func observeEntitlementChanges(callback: @escaping (NSNumber) -> Void) {
        updatesTask?.cancel()
        updatesTask = _Concurrency.Task.detached { [weak self] in
            for await update in Transaction.updates {
                guard let self else { return }
                if case .verified(let transaction) = update {
                    await transaction.finish()
                }
                let entitled = await self.hasEntitlement()
                await MainActor.run { callback(NSNumber(value: entitled)) }
            }
        }
    }

    private func hasEntitlement() async -> Bool {
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == productId,
               transaction.revocationDate == nil {
                return true
            }
        }
        return false
    }

    private func report(_ callback: @escaping (NSNumber, String?) -> Void, _ status: Int32, _ message: String?) async {
        await MainActor.run { callback(NSNumber(value: status), message) }
    }
}
