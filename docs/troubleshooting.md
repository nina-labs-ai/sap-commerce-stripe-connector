# Troubleshooting

## Stripe Checkout returns but no order is confirmed

A browser redirect from Stripe is not enough to place the SAP Commerce order.
The storefront return route must call the matching OCC `finalize` endpoint and
route to `orderConfirmation` from the returned order DTO.

## Webhook forwarding returns 404

Use the `stripeevents` web root:

```bash
stripe listen --forward-to http://127.0.0.1:9001/stripeevents/webhooks/stripe
```

Do not forward to `localhost:4242/webhook`.

## Payment Elements cannot initialize

Check that `stripe.publishable.key` is configured and returned by the OCC
configuration endpoint. The storefront rejects missing publishable keys before
calling Stripe.js.

## Webhook payload deserializes to an empty object

Stripe events can carry an event API version that differs from the Java SDK
model version. The service utility first uses Stripe's safe deserializer and
falls back to the SDK's unsafe deserializer only when the safe object is empty.
