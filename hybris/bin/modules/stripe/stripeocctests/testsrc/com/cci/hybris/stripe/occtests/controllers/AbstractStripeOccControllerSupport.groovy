package com.cci.hybris.stripe.occtests.controllers

import com.cci.hybris.stripe.occtests.util.StripeOccTestConfigurationUtils
import com.cci.hybris.stripe.services.constants.StripeServicesConstants
import com.cci.hybris.stripe.services.data.StripePaymentIntentData
import com.cci.hybris.stripe.services.factory.StripeClientFactory
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService
import de.hybris.bootstrap.annotations.ManualTest
import de.hybris.platform.commercewebservicestests.test.groovy.webservicetests.v2.spock.orders.AbstractOrderTest
import de.hybris.platform.core.Registry
import de.hybris.platform.core.model.order.AbstractOrderModel
import de.hybris.platform.core.model.order.CartModel
import de.hybris.platform.core.model.order.OrderModel
import de.hybris.platform.payment.enums.PaymentTransactionType
import de.hybris.platform.payment.model.PaymentTransactionEntryModel
import de.hybris.platform.servicelayer.config.ConfigurationService
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import de.hybris.platform.servicelayer.search.FlexibleSearchService
import groovyx.net.http.HttpResponseDecorator
import org.apache.commons.lang3.StringUtils
import org.junit.Assume
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams

import static groovyx.net.http.ContentType.JSON
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK

@ManualTest
abstract class AbstractStripeOccControllerSupport extends AbstractOrderTest {

	protected static final String STRIPE_PLACEHOLDER_PREFIX = "insert your"
	protected static final String CURRENT_USER_STRIPE_PREFIX = "/users/current/stripe"
	protected static final String CHECKOUT_SESSION_PATH = CURRENT_USER_STRIPE_PREFIX + "/checkout/session"
	protected static final String CHECKOUT_CONFIG_PATH = CURRENT_USER_STRIPE_PREFIX + "/checkout/config"
	protected static final String ELEMENTS_INTENT_PATH = CURRENT_USER_STRIPE_PREFIX + "/elements/intent"
	protected static final String CURRENT_ORDER_REFUND_PATH_SEGMENT = "/users/current/orders"
	protected static final String REFUND_PATH_SUFFIX = "/stripe/refunds"
	protected static final String CART_QUERY = "SELECT {pk} FROM {Cart} WHERE {code}=?code"
	protected static final String ORDER_QUERY = "SELECT {pk} FROM {Order} WHERE {code}=?code"
	protected static final String ENTRY_BY_ORDER_AND_REQUEST_QUERY = "SELECT {e.pk} " +
			"FROM {PaymentTransactionEntry AS e JOIN PaymentTransaction AS t ON {e.paymentTransaction}={t.pk} " +
			"JOIN AbstractOrder AS o ON {t.order}={o.pk}} " +
			"WHERE {o.code}=?orderCode AND {e.requestId}=?requestId ORDER BY {e.creationtime} DESC"
	protected static final String CHECKOUT_SESSION_PLACEHOLDER = "{CHECKOUT_SESSION_ID}"
	protected static final String DUMMY_CHECKOUT_SESSION_ID = "cs_test_dummy"
	protected static final String DUMMY_PAYMENT_INTENT_ID = "pi_test_dummy"
	protected static final String STATUS_EXPIRED = "expired"
	protected static final String STATUS_UNPAID = "unpaid"
	protected static final String STATUS_CANCELED = "canceled"

	protected void assumeCheckoutConfigured() {
		Assume.assumeTrue("Stripe secret key must be configured for OCC Checkout tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SECRET_KEY)))
		Assume.assumeTrue("Stripe success URL must be configured for OCC Checkout tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL)))
		Assume.assumeTrue("Stripe cancel URL must be configured for OCC Checkout tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_CANCEL_URL)))
	}

	protected void assumePaymentElementsConfigured() {
		Assume.assumeTrue("Stripe secret key must be configured for OCC Payment Elements tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SECRET_KEY)))
		Assume.assumeTrue("Stripe publishable key must be configured for OCC Payment Elements tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)))
		Assume.assumeTrue("Stripe success URL must be configured for OCC Payment Elements tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL)))
	}

	protected void assumePublishableKeyConfigured() {
		Assume.assumeTrue("Stripe publishable key must be configured for OCC Stripe tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY)))
	}

	protected void assumeRefundConfigured() {
		Assume.assumeTrue("Stripe secret key must be configured for OCC refund tests.",
				isConfiguredValue(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SECRET_KEY)))
	}

	protected String getRuntimeProperty(final String key) {
		return getConfigurationService().configuration.getString(key)
	}

	protected String getSiteAwareRuntimeProperty(final String key) {
		def siteUid = getCurrentSiteUid()
		def siteProperty = StringUtils.isBlank(siteUid) ? key : siteUid + "." + key
		return getConfigurationService().configuration.getString(siteProperty, getRuntimeProperty(key))
	}

	protected String getExpectedElementsReturnUrl() {
		String configuredReturnUrl = getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_ELEMENTS_RETURN_URL)
		if (isConfiguredValue(configuredReturnUrl)) {
			return configuredReturnUrl
		}

		return stripCheckoutSessionPlaceholder(getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL))
	}

	protected String getCurrentSiteUid() {
		return StripeOccTestConfigurationUtils.resolveSiteUid(getBasePathWithSite())
	}

	protected String stripCheckoutSessionPlaceholder(final String url) {
		return StripeOccTestConfigurationUtils.stripCheckoutSessionPlaceholder(url, CHECKOUT_SESSION_PLACEHOLDER)
	}

	protected boolean isConfiguredValue(final String value) {
		return StripeOccTestConfigurationUtils.isConfiguredValue(value, STRIPE_PLACEHOLDER_PREFIX)
	}

	protected String getCheckoutSessionPath() {
		return getBasePathWithSite() + CHECKOUT_SESSION_PATH
	}

	protected String getCheckoutSessionPath(final String sessionId) {
		return getCheckoutSessionPath() + "/" + sessionId
	}

	protected String getCheckoutSessionExpirePath(final String sessionId) {
		return getCheckoutSessionPath(sessionId) + "/expire"
	}

	protected String getCheckoutConfigPath() {
		return getBasePathWithSite() + CHECKOUT_CONFIG_PATH
	}

	protected String getElementsIntentPath() {
		return getBasePathWithSite() + ELEMENTS_INTENT_PATH
	}

	protected String getElementsIntentPath(final String paymentIntentId) {
		return getElementsIntentPath() + "/" + paymentIntentId
	}

	protected String getElementsIntentCancelPath(final String paymentIntentId) {
		return getElementsIntentPath(paymentIntentId) + "/cancel"
	}

	protected String getCurrentOrderRefundPath(final String orderCode) {
		return getBasePathWithSite() + CURRENT_ORDER_REFUND_PATH_SEGMENT + "/" + orderCode + REFUND_PATH_SUFFIX
	}

	protected CartModel getCartModel(final String cartCode) {
		FlexibleSearchQuery query = new FlexibleSearchQuery(CART_QUERY)
		query.addQueryParameter("code", cartCode)
		def searchResult = getFlexibleSearchService().search(query)
		return searchResult.result.isEmpty() ? null : (CartModel) searchResult.result.first()
	}

	protected OrderModel getOrderModel(final String orderCode) {
		FlexibleSearchQuery query = new FlexibleSearchQuery(ORDER_QUERY)
		query.addQueryParameter("code", orderCode)
		def searchResult = getFlexibleSearchService().search(query)
		return searchResult.result.isEmpty() ? null : (OrderModel) searchResult.result.first()
	}

	protected PaymentTransactionEntryModel findAuthorizationEntry(final AbstractOrderModel order, final String requestId) {
		return findTransactionEntry(order, PaymentTransactionType.AUTHORIZATION, requestId)
	}

	protected PaymentTransactionEntryModel findTransactionEntry(final AbstractOrderModel order,
			final PaymentTransactionType type,
			final String requestId) {
		if (order == null || StringUtils.isBlank(order.code) || StringUtils.isBlank(requestId)) {
			return null
		}

		FlexibleSearchQuery query = new FlexibleSearchQuery(ENTRY_BY_ORDER_AND_REQUEST_QUERY)
		query.addQueryParameter("orderCode", order.code)
		query.addQueryParameter("requestId", requestId)
		def searchResult = getFlexibleSearchService().search(query)
		return searchResult.result.find { entry ->
			entry.type == type && requestId == entry.requestId
		} as PaymentTransactionEntryModel
	}

	protected def placeRefundableOrder(client, customer, final CartModel cart, format = JSON) {
		def address = createAddress(client, customer, format)
		setDeliveryAddressForCart(client, customer, cart.code, address.id, format)
		setDeliveryModeForCart(client, customer, cart.code, DELIVERY_STANDARD, format)

		def paymentDetails = createPaymentDetailsForCart(client, customer, cart.code, format)
		with(client.put(
				path: getBasePathWithSite() + '/users/' + getUserId(customer) + '/carts/' + cart.code + '/paymentdetails',
				body: ['paymentDetailsId': paymentDetails.id],
				contentType: format,
				requestContentType: groovyx.net.http.ContentType.URLENC)) {
			status == SC_OK
		}

		return placeOrder(client, customer, cart.code, "123", format)
	}

	protected def createPaymentDetailsForCart(client, customer, final String cartCode, format = JSON) {
		HttpResponseDecorator response = client.post(
				path: getBasePathWithSite() + '/users/' + getUserId(customer) + '/carts/' + cartCode + '/paymentdetails',
				body: [
						'accountHolderName' : 'Sven Haiges',
						'cardNumber'        : '4111111111111111',
						'cardType'          : ['code': 'visa'],
						'expiryMonth'       : '01',
						'expiryYear'        : '2030',
						'saved'             : true,
						'defaultPaymentInfo': true,
						'billingAddress'    : [
								'titleCode' : 'mr',
								'firstName' : 'sven',
								'lastName'  : 'haiges',
								'line1'     : 'test1',
								'line2'     : 'test2',
								'postalCode': '12345',
								'town'      : 'somecity',
								'country'   : ['isocode': 'US'],
								'region'    : ['isocode': 'US-NY']
						]
				],
				contentType: format,
				requestContentType: JSON)

		with(response) {
			status == SC_CREATED
		}

		return response.data
	}

	protected PaymentIntent registerCapturedStripePaymentIntent(final String orderCode) {
		OrderModel order = getOrderModel(orderCode)
		String secretKey = getSiteAwareRuntimeProperty(StripeServicesConstants.PROPERTY_SECRET_KEY)
		PaymentIntent paymentIntent = getStripeClientFactory().createPaymentIntent(secretKey, PaymentIntentCreateParams.builder()
				.setAmount(Long.valueOf(1000L))
				.setCurrency("usd")
				.setConfirm(Boolean.TRUE)
				.setErrorOnRequiresAction(Boolean.TRUE)
				.setPaymentMethod("pm_card_visa")
				.addPaymentMethodType("card")
				.putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.code)
				.putMetadata(StripeServicesConstants.METADATA_SITE_UID, order.site.uid)
				.putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName())
				.putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, StripeServicesConstants.PAYMENT_FLOW_ELEMENTS)
				.build())

		StripePaymentIntentData paymentIntentData = new StripePaymentIntentData()
		paymentIntentData.setId(paymentIntent.id)
		paymentIntentData.setClientSecret(paymentIntent.clientSecret)
		paymentIntentData.setStatus(paymentIntent.status)
		paymentIntentData.setAmount(paymentIntent.amount)
		paymentIntentData.setCurrency(paymentIntent.currency)
		paymentIntentData.setClientReferenceId(order.code)

		getStripePaymentTransactionService().registerPaymentIntent(order, paymentIntentData)
		getStripePaymentTransactionService().markPaymentIntentSucceeded(order, paymentIntentData)
		return paymentIntent
	}

	protected FlexibleSearchService getFlexibleSearchService() {
		return Registry.applicationContext.getBean("flexibleSearchService", FlexibleSearchService)
	}

	protected ConfigurationService getConfigurationService() {
		return Registry.applicationContext.getBean("configurationService", ConfigurationService)
	}

	protected StripeClientFactory getStripeClientFactory() {
		return Registry.applicationContext.getBean("stripeClientFactory", StripeClientFactory)
	}

	protected StripePaymentTransactionService getStripePaymentTransactionService() {
		return Registry.applicationContext.getBean("stripePaymentTransactionService", StripePaymentTransactionService)
	}
}
