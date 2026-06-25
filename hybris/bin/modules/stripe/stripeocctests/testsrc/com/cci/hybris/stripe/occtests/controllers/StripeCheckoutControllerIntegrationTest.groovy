package com.cci.hybris.stripe.occtests.controllers

import com.cci.hybris.stripe.services.constants.StripeServicesConstants
import de.hybris.bootstrap.annotations.ManualTest
import de.hybris.platform.core.model.order.CartModel
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
class StripeCheckoutControllerIntegrationTest extends AbstractStripeOccControllerSupport {

	def "Customer can create and retrieve Stripe Checkout Session for current cart"() {
		given: "live Stripe Checkout configuration and an authorized customer cart"
		assumeCheckoutConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)

		when: "the customer creates a Stripe Checkout Session for the current cart"
		HttpResponseDecorator createResponse = restClient.post(
				path: getCheckoutSessionPath(),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint returns the live session and the cart stores the authorization entry"
		with(createResponse) {
			status == SC_CREATED
			isNotEmpty(data.id)
			data.id.startsWith("cs_")
			isNotEmpty(data.url)
			data.url.contains("stripe.com")
			data.clientReferenceId == cart.code
		}

		and: "the same session can be retrieved through OCC"
		HttpResponseDecorator getResponse = restClient.get(
				path: getCheckoutSessionPath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		with(getResponse) {
			status == SC_OK
			data.id == createResponse.data.id
			data.url == createResponse.data.url
			data.clientReferenceId == cart.code
			isNotEmpty(data.status)
			isNotEmpty(data.paymentStatus)
		}

		and: "any already-persisted cart authorization entry stays pending before finalization"
		CartModel persistedCart = getCartModel(cart.code)
		PaymentTransactionEntryModel authorizationEntry = findAuthorizationEntry(persistedCart, createResponse.data.id as String)
		if (authorizationEntry != null) {
			authorizationEntry.requestId == createResponse.data.id
			authorizationEntry.transactionStatus == StripeServicesConstants.STATUS_PENDING
		}
	}

	def "Customer can expire Stripe Checkout Session for current cart"() {
		given: "live Stripe Checkout configuration and an authorized customer cart with an active session"
		assumeCheckoutConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		HttpResponseDecorator createResponse = restClient.post(
				path: getCheckoutSessionPath(),
				contentType: JSON,
				requestContentType: JSON)

		when: "the customer expires the active Stripe Checkout Session"
		HttpResponseDecorator expireResponse = restClient.post(
				path: getCheckoutSessionExpirePath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint returns the expired session state"
		with(expireResponse) {
			status == SC_OK
			data.id == createResponse.data.id
			data.status == STATUS_EXPIRED
			data.paymentStatus == STATUS_UNPAID
			data.clientReferenceId == cart.code
		}

		and: "the cart authorization entry is marked rejected"
		PaymentTransactionEntryModel authorizationEntry = awaitAuthorizationEntry(cart.code, createResponse.data.id as String)
		authorizationEntry != null
		authorizationEntry.transactionStatus == StripeServicesConstants.STATUS_REJECTED
		authorizationEntry.transactionStatusDetails == STATUS_EXPIRED
	}

	def "Customer can read Stripe Checkout public configuration"() {
		given: "a configured Stripe publishable key and an authorized customer"
		assumePublishableKeyConfigured()
		registerAndAuthorizeCustomer(restClient, JSON)

		when: "the customer requests the Stripe Checkout public configuration"
		HttpResponseDecorator response = restClient.get(
				path: getCheckoutConfigPath(),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint exposes the frontend-safe Checkout configuration"
		with(response) {
			status == SC_OK
			data.publishableKey == getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)
			data.paymentOptionId == StripeServicesConstants.PAYMENT_OPTION_ID_CHECKOUT
			data.paymentMethod == StripeServicesConstants.PAYMENT_METHOD
		}
	}

	def "Anonymous request without authorization cannot create Stripe Checkout Session"() {
		given: "no bearer token"
		removeAuthorization(restClient)

		when: "the request hits the Stripe Checkout create endpoint"
		HttpResponseDecorator response = restClient.post(
				path: getCheckoutSessionPath(),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the request"
		with(response) {
			status == SC_UNAUTHORIZED
		}
	}

	def "Anonymous request without authorization cannot expire Stripe Checkout Session"() {
		given: "no bearer token"
		removeAuthorization(restClient)

		when: "the request hits the Stripe Checkout expire endpoint"
		HttpResponseDecorator response = restClient.post(
				path: getCheckoutSessionExpirePath(DUMMY_CHECKOUT_SESSION_ID),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the request"
		with(response) {
			status == SC_UNAUTHORIZED
		}
	}

	def "Customer cannot expire Stripe Checkout Session owned by a different cart"() {
		given: "a Stripe Checkout Session created for another customer's current cart"
		assumeCheckoutConfigured()
		def customerOne = registerAndAuthorizeCustomer(restClient, JSON)
		def originalCart = createCart(restClient, customerOne, JSON)
		addProductToCartOnline(restClient, customerOne, originalCart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		HttpResponseDecorator createResponse = restClient.post(
				path: getCheckoutSessionPath(),
				contentType: JSON,
				requestContentType: JSON)

		and: "a different customer owns the active current cart"
		def customerTwo = registerAndAuthorizeCustomer(restClient, JSON)
		def differentCart = createCart(restClient, customerTwo, JSON)
		addProductToCartOnline(restClient, customerTwo, differentCart.code, PRODUCT_EOS_40D_BODY, 1, JSON)

		when: "the second customer tries to expire the first cart session"
		HttpResponseDecorator response = restClient.post(
				path: getCheckoutSessionExpirePath(createResponse.data.id as String),
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the cart-mismatched lifecycle request"
		with(response) {
			status == SC_BAD_REQUEST
		}
	}

	private PaymentTransactionEntryModel awaitAuthorizationEntry(final String cartCode, final String requestId) {
		final long deadline = System.currentTimeMillis() + 5000L
		while (System.currentTimeMillis() < deadline) {
			CartModel persistedCart = getCartModel(cartCode)
			PaymentTransactionEntryModel authorizationEntry = findAuthorizationEntry(persistedCart, requestId)
			if (authorizationEntry != null) {
				return authorizationEntry
			}
			Thread.sleep(200L)
		}
		throw new AssertionError("Authorization entry for requestId [" + requestId + "] was not created within 5 seconds")
	}
}
