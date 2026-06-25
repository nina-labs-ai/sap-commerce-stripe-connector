# Architecture

The connector is packaged as eight SAP Commerce extensions plus one reusable SAP
Composable Storefront source package.

## Commerce Extension Layers

| Extension | Role |
| --- | --- |
| `stripeservices` | Stripe Java SDK boundary, configuration, webhook verification, payment lifecycle, and SAP transaction updates. |
| `stripefacades` | Cart/order orchestration and commerce-facing DTO mapping. |
| `stripeocc` | OCC endpoints for Checkout Session, Payment Elements, finalize, cancellation, and refunds. |
| `stripeevents` | Webhook HTTP endpoint at `/stripeevents/webhooks/stripe`. |
| `stripefulfilmentprocess` | Business-process check for Checkout Session payment status. |
| `stripebackoffice` | Backoffice configuration/status surface. |
| `stripeocctests` | Manual OCC validation suite. |
| `stripetest` | Shared test support and integration fixtures. |

## Payment Flows

Hosted Checkout creates a Stripe Checkout Session from the cart, redirects the
shopper to Stripe, then finalizes the order through OCC when the storefront
return route receives the session id.

Payment Elements creates or reuses a Stripe PaymentIntent, confirms the payment
in the browser, then finalizes through OCC when the return route is reached.

Webhook events are signature-verified in `stripeevents` and dispatched by
`stripeservices`. Supported Checkout Session and PaymentIntent events must carry
a deserializable Stripe object; otherwise the service raises an integration
failure so Stripe can retry delivery instead of silently acknowledging a lost
state transition.

## Storefront

`js-storefront/stripe-spartacus-connector` contains the reusable Spartacus
checkout module. It replaces the CMS `CheckoutPaymentDetails` component with the
Stripe payment step and registers the return/cancel routes used by OCC finalize
calls.

`stripespartacussampledata` is not included in the public package. The validated
path is the reusable Spartacus connector package plus the
`b2c_acc_plus_stripe` installer recipe.
