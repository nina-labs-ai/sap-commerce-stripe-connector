package com.cci.hybris.stripe.occtests.controllers

import com.cci.hybris.stripe.services.constants.StripeServicesConstants
import de.hybris.bootstrap.annotations.ManualTest
import de.hybris.platform.core.model.order.CartModel
import de.hybris.platform.payment.enums.PaymentTransactionType
import de.hybris.platform.payment.model.PaymentTransactionEntryModel
import de.hybris.platform.testframework.HybrisSpockRunner
import groovyx.net.http.HttpResponseDecorator
import org.junit.runner.RunWith

import static groovyx.net.http.ContentType.JSON
import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

@ManualTest
@RunWith(HybrisSpockRunner.class)
class StripePaymentElementControllerIntegrationTest extends AbstractStripeOccControllerSupport {

	def "Customer can create and retrieve Stripe Payment Intent for current cart"() {
		given: "live Stripe Payment Elements configuration and an authorized customer cart"
		assumePaymentElementsConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)

		when: "the customer creates a Stripe PaymentIntent for the current cart"
		HttpResponseDecorator createResponse = restClient.post(
				path: getElementsIntentPath(),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint returns the live payment intent and exposes frontend-safe metadata"
		with(createResponse) {
			status == SC_CREATED
			isNotEmpty(data.id)
			data.id.startsWith("pi_")
			isNotEmpty(data.clientSecret)
			isNotEmpty(data.status)
			isNotEmpty(data.formattedAmount)
			data.clientReferenceId == cart.code
			data.publishableKey == getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)
			data.paymentOptionId == StripeServicesConstants.PAYMENT_OPTION_ID_ELEMENTS
			data.paymentMethod == StripeServicesConstants.PAYMENT_METHOD_ELEMENTS
			data.returnUrl == getExpectedElementsReturnUrl()
		}

		and: "the same payment intent can be retrieved through OCC"
		HttpResponseDecorator getResponse = restClient.get(
				path: getElementsIntentPath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		with(getResponse) {
			status == SC_OK
			data.id == createResponse.data.id
			data.clientSecret == createResponse.data.clientSecret
			data.clientReferenceId == cart.code
			data.publishableKey == getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)
			data.paymentOptionId == StripeServicesConstants.PAYMENT_OPTION_ID_ELEMENTS
			data.paymentMethod == StripeServicesConstants.PAYMENT_METHOD_ELEMENTS
			data.returnUrl == getExpectedElementsReturnUrl()
			isNotEmpty(data.status)
		}

		and: "the cart payment transaction contains the payment intent authorization entry"
		CartModel persistedCart = getCartModel(cart.code)
		PaymentTransactionEntryModel authorizationEntry = findAuthorizationEntry(persistedCart, createResponse.data.id as String)
		authorizationEntry != null
		authorizationEntry.requestId == createResponse.data.id
		authorizationEntry.transactionStatus == StripeServicesConstants.STATUS_PENDING
	}

	def "Anonymous request without authorization cannot create Stripe Payment Intent"() {
		given: "no bearer token"
		removeAuthorization(restClient)

		when: "the request hits the Stripe Payment Elements create endpoint"
		HttpResponseDecorator response = restClient.post(
				path: getElementsIntentPath(),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the request"
		with(response) {
			status == SC_UNAUTHORIZED
		}
	}

	def "Anonymous request without authorization cannot access Stripe Payment Intent lifecycle endpoints"() {
		given: "no bearer token"
		removeAuthorization(restClient)

		when: "the request hits the Stripe PaymentIntent get endpoint"
		HttpResponseDecorator getResponse = restClient.get(
				path: getElementsIntentPath(DUMMY_PAYMENT_INTENT_ID),
				contentType: JSON,
				requestContentType: JSON)

		and: "the request hits the Stripe PaymentIntent cancel endpoint"
		HttpResponseDecorator cancelResponse = restClient.post(
				path: getElementsIntentCancelPath(DUMMY_PAYMENT_INTENT_ID),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoints reject both requests"
		with(getResponse) {
			status == SC_UNAUTHORIZED
		}
		with(cancelResponse) {
			status == SC_UNAUTHORIZED
		}
	}

	def "Customer cannot access Stripe Payment Intent owned by a different cart"() {
		given: "a Stripe PaymentIntent created for another customer's current cart"
		assumePaymentElementsConfigured()
		def customerOne = registerAndAuthorizeCustomer(restClient, JSON)
		def originalCart = createCart(restClient, customerOne, JSON)
		addProductToCartOnline(restClient, customerOne, originalCart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		HttpResponseDecorator createResponse = restClient.post(
				path: getElementsIntentPath(),
				contentType: JSON,
				requestContentType: JSON)

		and: "a different customer owns the active current cart"
		def customerTwo = registerAndAuthorizeCustomer(restClient, JSON)
		def differentCart = createCart(restClient, customerTwo, JSON)
		addProductToCartOnline(restClient, customerTwo, differentCart.code, PRODUCT_EOS_40D_BODY, 1, JSON)

		when: "the second customer tries to read and cancel the first cart payment intent"
		HttpResponseDecorator getResponse = restClient.get(
				path: getElementsIntentPath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)
		HttpResponseDecorator cancelResponse = restClient.post(
				path: getElementsIntentCancelPath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoints reject the cart-mismatched lifecycle requests"
		with(getResponse) {
			status == SC_BAD_REQUEST
		}
		with(cancelResponse) {
			status == SC_BAD_REQUEST
		}
	}

	def "Customer can cancel Stripe Payment Intent for current cart"() {
		given: "live Stripe Payment Elements configuration and an authorized customer cart"
		assumePaymentElementsConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		HttpResponseDecorator createResponse = restClient.post(
				path: getElementsIntentPath(),
				contentType: JSON,
				requestContentType: JSON)

		when: "the customer cancels the Stripe PaymentIntent"
		HttpResponseDecorator cancelResponse = restClient.post(
				path: getElementsIntentCancelPath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint returns the canceled payment intent"
		with(cancelResponse) {
			status == SC_OK
			data.id == createResponse.data.id
			data.status == STATUS_CANCELED
			data.publishableKey == getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)
			data.paymentOptionId == StripeServicesConstants.PAYMENT_OPTION_ID_ELEMENTS
			data.paymentMethod == StripeServicesConstants.PAYMENT_METHOD_ELEMENTS
			data.returnUrl == getExpectedElementsReturnUrl()
		}

		and: "the cart payment transaction stores the rejected authorization and cancel entry"
		CartModel persistedCart = getCartModel(cart.code)
		PaymentTransactionEntryModel authorizationEntry = findTransactionEntry(persistedCart, PaymentTransactionType.AUTHORIZATION,
				createResponse.data.id as String)
		PaymentTransactionEntryModel cancelEntry = findTransactionEntry(persistedCart, PaymentTransactionType.CANCEL,
				createResponse.data.id as String)
		authorizationEntry != null
		authorizationEntry.transactionStatus == StripeServicesConstants.STATUS_REJECTED
		authorizationEntry.transactionStatusDetails == STATUS_CANCELED
		cancelEntry != null
		cancelEntry.transactionStatus == StripeServicesConstants.STATUS_ACCEPTED
		cancelEntry.transactionStatusDetails == STATUS_CANCELED
	}
}
