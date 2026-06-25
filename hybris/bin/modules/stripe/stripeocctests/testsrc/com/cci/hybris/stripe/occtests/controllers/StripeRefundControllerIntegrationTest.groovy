package com.cci.hybris.stripe.occtests.controllers

import com.cci.hybris.stripe.services.constants.StripeServicesConstants
import de.hybris.bootstrap.annotations.ManualTest
import de.hybris.platform.core.model.order.OrderModel
import de.hybris.platform.payment.enums.PaymentTransactionType
import de.hybris.platform.payment.model.PaymentTransactionEntryModel
import de.hybris.platform.testframework.HybrisSpockRunner
import groovyx.net.http.HttpResponseDecorator
import org.junit.runner.RunWith

import static groovyx.net.http.ContentType.JSON
import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

@ManualTest
@RunWith(HybrisSpockRunner.class)
class StripeRefundControllerIntegrationTest extends AbstractStripeOccControllerSupport {

	def "Customer can create Stripe refund for owned order"() {
		given: "a customer order with a live captured Stripe PaymentIntent"
		assumeRefundConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		def order = placeRefundableOrder(restClient, customer, getCartModel(cart.code), JSON)
		def paymentIntent = registerCapturedStripePaymentIntent(order.code as String)

		when: "the customer requests a partial refund for the owned order"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath(order.code as String),
				body: [
						'paymentReference': paymentIntent.id,
						'amount'          : 5.00
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint returns the created refund details"
		with(response) {
			status == SC_CREATED
			isNotEmpty(data.id)
			data.id.startsWith("re_")
			data.paymentIntentId == paymentIntent.id
			data.orderCode == order.code
			data.paymentReference == paymentIntent.id
			data.amount == 500
			data.currency == "usd"
			isNotEmpty(data.formattedAmount)
		}

		and: "the order payment transaction stores the refund follow-on entry"
		OrderModel persistedOrder = getOrderModel(order.code as String)
		PaymentTransactionEntryModel refundEntry = findTransactionEntry(persistedOrder,
				PaymentTransactionType.REFUND_FOLLOW_ON, response.data.id as String)
		refundEntry != null
		refundEntry.transactionStatus == StripeServicesConstants.STATUS_ACCEPTED
		refundEntry.transactionStatusDetails == response.data.status
	}

	def "Customer cannot refund Stripe order owned by another user"() {
		given: "an order with a captured Stripe payment owned by a different customer"
		assumeRefundConfigured()
		def customerOne = registerAndAuthorizeCustomer(restClient, JSON)
		def originalCart = createCart(restClient, customerOne, JSON)
		addProductToCartOnline(restClient, customerOne, originalCart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		def order = placeRefundableOrder(restClient, customerOne, getCartModel(originalCart.code), JSON)
		def paymentIntent = registerCapturedStripePaymentIntent(order.code as String)

		and: "a different authenticated customer is active"
		registerAndAuthorizeCustomer(restClient, JSON)

		when: "the second customer tries to refund the first customer's order"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath(order.code as String),
				body: [
						'paymentReference': paymentIntent.id,
						'amount'          : 5.00
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the order access outside the current user context"
		with(response) {
			status == SC_BAD_REQUEST
		}
	}

	def "Customer cannot refund owned order with unregistered Stripe reference"() {
		given: "an owned order without the requested Stripe payment reference"
		assumeRefundConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		def order = placeRefundableOrder(restClient, customer, getCartModel(cart.code), JSON)

		when: "the customer submits an unknown payment reference"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath(order.code as String),
				body: [
						'paymentReference': 'pi_unknown_refund',
						'amount'          : 5.00
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the invalid refund reference"
		with(response) {
			status == SC_BAD_REQUEST
		}
	}

	def "Customer cannot create Stripe refund without payment reference"() {
		given: "an owned order but a malformed refund request"
		assumeRefundConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		def order = placeRefundableOrder(restClient, customer, getCartModel(cart.code), JSON)

		when: "the payment reference is omitted"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath(order.code as String),
				body: [
						'amount': 5.00
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the malformed request"
		with(response) {
			status == SC_BAD_REQUEST
		}
	}

	def "Customer cannot create Stripe refund with non positive amount"() {
		given: "an owned order but a malformed refund amount"
		assumeRefundConfigured()
		def customer = registerAndAuthorizeCustomer(restClient, JSON)
		def cart = createCart(restClient, customer, JSON)
		addProductToCartOnline(restClient, customer, cart.code, PRODUCT_EOS_40D_BODY, 1, JSON)
		def order = placeRefundableOrder(restClient, customer, getCartModel(cart.code), JSON)

		when: "the refund amount is zero"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath(order.code as String),
				body: [
						'paymentReference': DUMMY_PAYMENT_INTENT_ID,
						'amount'          : 0
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the malformed request"
		with(response) {
			status == SC_BAD_REQUEST
		}
	}

	def "Anonymous request without authorization cannot create Stripe refund"() {
		given: "no bearer token"
		removeAuthorization(restClient)

		when: "the request hits the Stripe refund endpoint"
		HttpResponseDecorator response = restClient.post(
				path: getCurrentOrderRefundPath("00001001"),
				body: [
						'paymentReference': DUMMY_PAYMENT_INTENT_ID,
						'amount'          : 5.00
				],
				contentType: JSON,
				requestContentType: JSON)

		then: "the OCC endpoint rejects the request"
		with(response) {
			status == SC_UNAUTHORIZED
		}
	}
}
