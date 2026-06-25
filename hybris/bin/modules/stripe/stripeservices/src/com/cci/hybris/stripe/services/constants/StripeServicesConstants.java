package com.cci.hybris.stripe.services.constants;

/**
 * Stripe services extension constants.
 */
public final class StripeServicesConstants extends GeneratedStripeServicesConstants {

    public static final String EXTENSIONNAME = "stripeservices";
    public static final String PLATFORM_LOGO_CODE = "stripeservicesPlatformLogo";

    public static final String PAYMENT_PROVIDER = "Stripe";
    public static final String PAYMENT_METHOD = "card";
    public static final String PAYMENT_METHOD_ELEMENTS = "payment_element";
    public static final String PAYMENT_OPTION_ID_CHECKOUT = "stripe-checkout";
    public static final String PAYMENT_OPTION_ID_ELEMENTS = "stripe-elements";
    public static final String PAYMENT_OPTION_ID = PAYMENT_OPTION_ID_CHECKOUT;

    public static final String PROPERTY_SECRET_KEY = "stripe.secret.key";
    public static final String PROPERTY_PUBLISHABLE_KEY = "stripe.publishable.key";
    public static final String PROPERTY_WEBHOOK_SECRET = "stripe.webhook.secret";
    public static final String PROPERTY_SUCCESS_URL = "stripe.checkout.success.url";
    public static final String PROPERTY_CANCEL_URL = "stripe.checkout.cancel.url";
    public static final String PROPERTY_ELEMENTS_RETURN_URL = "stripe.elements.return.url";
    public static final String SITE_PROPERTY_SEPARATOR = ".";

    public static final String METADATA_ORDER_CODE = "orderCode";
    public static final String METADATA_SITE_UID = "siteUid";
    public static final String METADATA_ORDER_TYPE = "orderType";
    public static final String METADATA_PAYMENT_FLOW = "paymentFlow";

    public static final String PAYMENT_FLOW_CHECKOUT = "checkout";
    public static final String PAYMENT_FLOW_ELEMENTS = "elements";

    public static final String CHECKOUT_SESSION_PREFIX = "cs_";
    public static final String PAYMENT_INTENT_PREFIX = "pi_";
    public static final String ORDER_DESCRIPTION_PREFIX = "Order ";

    public static final String CHECKOUT_SESSION_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";
    public static final String CHECKOUT_SESSION_PLACEHOLDER_VALIDATION_VALUE = "stripe-session-id";
    public static final String PLACEHOLDER_PREFIX = "insert your";
    public static final String QUERY_PARAM_SESSION_ID = "session_id";
    public static final String QUERY_PARAM_CART_ID = "cartId";
    public static final String QUERY_PARAM_ORDER_CODE = "orderCode";
    public static final String URL_QUERY_SEPARATOR = "?";
    public static final String URL_AMPERSAND = "&";
    public static final String URL_EQUALS = "=";
    public static final String URL_SCHEME_HTTP = "http";
    public static final String URL_SCHEME_HTTPS = "https";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
    public static final String STATUS_PENDING_LOWER = "pending";

    public static final String STRIPE_STATUS_REQUIRES_PAYMENT_METHOD = "requires_payment_method";
    public static final String STRIPE_STATUS_REQUIRES_CONFIRMATION = "requires_confirmation";
    public static final String STRIPE_STATUS_SUCCEEDED = "succeeded";
    public static final String STRIPE_STATUS_CANCELED = "canceled";
    public static final String STRIPE_STATUS_REQUIRES_CAPTURE = "requires_capture";
    public static final String STRIPE_STATUS_COMPLETE = "complete";
    public static final String STRIPE_STATUS_EXPIRED = "expired";
    public static final String STRIPE_PAYMENT_STATUS_PAID = "paid";
    public static final String STRIPE_PAYMENT_STATUS_UNPAID = "unpaid";
    public static final String EVENT_CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    public static final String EVENT_CHECKOUT_SESSION_EXPIRED = "checkout.session.expired";
    public static final String EVENT_PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String EVENT_PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    public static final String EVENT_PAYMENT_INTENT_CANCELED = "payment_intent.canceled";

    private StripeServicesConstants() {
    }
}
