package com.asterixorobelix.remit.shared.di

import com.asterixorobelix.remit.shared.data.PaymentRepositoryImpl
import com.asterixorobelix.remit.shared.data.remote.KtorPaymentsApi
import com.asterixorobelix.remit.shared.data.remote.PaymentRequestConfigurer
import com.asterixorobelix.remit.shared.data.remote.PaymentsApi
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.usecase.IdempotencyKeys
import com.asterixorobelix.remit.shared.domain.usecase.ObservePaymentHistoryUseCase
import com.asterixorobelix.remit.shared.domain.usecase.SendPaymentUseCase
import com.asterixorobelix.remit.shared.domain.usecase.ValidatePaymentUseCase
import com.asterixorobelix.remit.shared.domain.validation.FilterAmountInputUseCase
import com.asterixorobelix.remit.shared.domain.validation.FilterRecipientInputUseCase
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The payments feature for any host app. The host provides an `HttpClient`, an `ApiEndpoint`, a
 * `PendingPaymentStore` (durable storage for the payment in flight), and the backend: an
 * `AuthSession` and a `PaymentHistorySource` (`firebaseBackendModule`, or another provider's), and
 * optionally a `PaymentRequestConfigurer`. Everything here depends only on those interfaces.
 */
fun paymentsModule(): Module = module {
    single<PaymentsApi> {
        KtorPaymentsApi(get(), get(), getOrNull<PaymentRequestConfigurer>() ?: PaymentRequestConfigurer { })
    }
    singleOf(::PaymentRepositoryImpl) bind PaymentRepository::class
    factoryOf(::PaymentValidator)
    factory { IdempotencyKeys(get(), ::newIdempotencyKey) }
    factoryOf(::SendPaymentUseCase)
    factoryOf(::ValidatePaymentUseCase)
    factoryOf(::FilterAmountInputUseCase)
    factoryOf(::FilterRecipientInputUseCase)
    factoryOf(::ObservePaymentHistoryUseCase)
}

@OptIn(ExperimentalUuidApi::class)
internal fun newIdempotencyKey(): String = Uuid.random().toString()
