# Stripe OCC Contracts

## Scope

This document is the frontend-facing contract for the Stripe OCC endpoints in `stripeocc`.

Phase 1 supports:

- Stripe Checkout Sessions with hosted Checkout
- Stripe Payment Elements bootstrap through PaymentIntents
- cart-bound Stripe Checkout Session expiration
- cart-bound Stripe PaymentIntent cancellation
- order-bound Stripe refunds

This contract does not cover:

- storefront component code
- webhook HTTP callbacks
- captures or backoffice operations

## Base Path And Auth

All endpoints are rooted under:

```text
/occ/v2/{baseSiteId}/users/{userId}/stripe
```

Expected caller behavior:

- use the same bearer token and OCC session model already used for cart endpoints
- use `current` for `userId` in normal storefront flows
- ensure the authenticated context already has a current cart when calling create endpoints

The Stripe OCC controllers are cart-bound and site-bound. A frontend must not treat returned Stripe ids as globally reusable across users, carts, or base sites.

## Checkout Endpoints

### Get Public Checkout Config

```text
GET /{baseSiteId}/users/{userId}/stripe/checkout/config
```

Response fields:

- `publishableKey`: frontend-safe Stripe publishable key for the current base site
- `paymentOptionId`: SAP payment option identifier. Current value is `stripe-checkout`
- `paymentMethod`: connector payment method identifier. Current value is `card`

Example:

```json
{
  "publishableKey": "site-publishable-key",
  "paymentOptionId": "stripe-checkout",
  "paymentMethod": "card"
}
```

### Create Checkout Session

```text
POST /{baseSiteId}/users/{userId}/stripe/checkout/session
```

Request body:

- none

Required backend state:

- current cart exists
- cart is loadable for the authenticated OCC context
- Stripe site configuration is valid

Response fields:

- `id`: Stripe Checkout Session id such as `cs_test_...`
- `url`: hosted Stripe Checkout URL to redirect the shopper to
- `status`: Stripe Checkout Session status when available
- `paymentStatus`: Stripe payment status when available
- `clientReferenceId`: SAP order or cart reference propagated to Stripe

Example:

```json
{
  "id": "cs_test_a1b2c3",
  "url": "https://checkout.stripe.com/c/pay/cs_test_a1b2c3",
  "status": "open",
  "paymentStatus": "unpaid",
  "clientReferenceId": "00001001"
}
```

Frontend expectation:

- redirect the shopper to `url`
- persist `id` on the frontend only as an opaque reference for status lookup
- do not infer success from the redirect alone; final payment truth comes from webhook processing plus the OCC finalize call after the shopper returns to the storefront

### Expire Checkout Session

```text
POST /{baseSiteId}/users/{userId}/stripe/checkout/session/{sessionId}/expire
```

Request body:

- none

Use this only for the current cart context that originally created the Checkout Session.

Response fields:

- same payload shape as `GET .../checkout/session/{sessionId}`
- `status` is expected to move to `expired`
- `paymentStatus` is expected to be `unpaid`

Frontend expectation:

- call this when the shopper abandons the hosted Checkout redirect and the current cart must discard the active Checkout Session
- after a successful expire call, treat the old session id as closed and create a fresh Checkout Session if the shopper retries

### Retrieve Checkout Session

```text
GET /{baseSiteId}/users/{userId}/stripe/checkout/session/{sessionId}
```

Use this only with a session id previously returned by the create endpoint for the same authenticated cart/order context.

Frontend expectation:

- use it for status refresh, not as a substitute for webhook-driven backend reconciliation
- treat `status` and `paymentStatus` as Stripe-facing display fields, not the sole order-finalization signal

### Finalize Checkout Session

```text
POST /{baseSiteId}/users/{userId}/stripe/checkout/session/{sessionId}/finalize
```

Request body:

- none

Optional query parameters:

- `cartId`: required for anonymous storefront callers so the backend can resolve the paid cart or placed order that owns the Checkout Session. Pass the anonymous cart guid or cart code.
- `orderCode`: optional storefront fallback when the order code is already known from session metadata or prior status refresh.

Response fields:

- standard OCC `OrderWsDTO` payload for the placed or previously placed order

Frontend expectation:

- call this after the hosted Stripe redirect returns to the storefront
- use this instead of treating the browser redirect as order confirmation
- route to Spartacus `/order-confirmation` only after this endpoint succeeds

Example anonymous storefront finalize call:

```text
POST /occ/v2/apparel-uk-spa/users/anonymous/stripe/checkout/session/cs_test_123/finalize?cartId=00012345
```

## Payment Elements Endpoints

### Create PaymentIntent Bootstrap

```text
POST /{baseSiteId}/users/{userId}/stripe/elements/intent
```

Request body:

- none

Optional query parameters:

- `cartId`: required for anonymous storefront callers when the OCC user context cannot restore the current cart by itself. Pass the anonymous cart guid or cart code.

Response fields:

- `id`: Stripe PaymentIntent id such as `pi_...`
- `clientSecret`: client secret for Stripe.js Payment Elements confirmation
- `status`: Stripe PaymentIntent status
- `amount`: amount in minor units
- `currency`: ISO currency code in lowercase Stripe format
- `clientReferenceId`: SAP order or cart reference propagated to Stripe
- `publishableKey`: frontend-safe Stripe publishable key
- `paymentOptionId`: SAP payment option identifier. Current value is `stripe-elements`
- `paymentMethod`: connector payment method identifier. Current value is `payment_element`
- `formattedAmount`: SAP-formatted display amount
- `returnUrl`: absolute frontend return URL configured for Payment Elements confirmation

Example:

```json
{
  "id": "pi_123",
  "clientSecret": "pi_123_secret_abc",
  "status": "requires_payment_method",
  "amount": 1234,
  "currency": "usd",
  "clientReferenceId": "00001001",
  "publishableKey": "site-publishable-key",
  "paymentOptionId": "stripe-elements",
  "paymentMethod": "payment_element",
  "formattedAmount": "$12.34",
  "returnUrl": "https://storefront.example/checkout/stripe/return"
}
```

Frontend expectation:

- initialize Stripe.js with `publishableKey`
- mount Payment Elements with `clientSecret`
- confirm against `returnUrl`
- do not cache `clientSecret` across carts or users

Example anonymous storefront bootstrap call:

```text
POST /occ/v2/apparel-uk-spa/users/anonymous/stripe/elements/intent?cartId=00012345
```

### Retrieve PaymentIntent Bootstrap

```text
GET /{baseSiteId}/users/{userId}/stripe/elements/intent/{paymentIntentId}
```

This endpoint is ownership-protected. The `paymentIntentId` must belong to the current cart context. The controller reloads the current cart before lookup, so a frontend must not use a PaymentIntent id from another cart, order, or shopper session.

Optional query parameters:

- `cartId`: required for anonymous storefront callers when the OCC user context cannot restore the current cart by itself. Pass the anonymous cart guid or cart code.

Use cases:

- refresh frontend bootstrap state after a page reload
- verify that the current cart still maps to the same PaymentIntent

### Cancel PaymentIntent

```text
POST /{baseSiteId}/users/{userId}/stripe/elements/intent/{paymentIntentId}/cancel
```

Request body:

- none

Optional query parameters:

- `cartId`: required for anonymous storefront callers when the OCC user context cannot restore the current cart by itself. Pass the anonymous cart guid or cart code.

This endpoint is current-cart scoped. The `paymentIntentId` must belong to the active cart context.

Response fields:

- same payload shape as `GET .../elements/intent/{paymentIntentId}`
- `status` is expected to move to `canceled`

Frontend expectation:

- call this when the shopper abandons the current Payment Elements attempt and the frontend wants to invalidate the existing PaymentIntent
- after a successful cancel call, discard the old client secret and request a fresh PaymentIntent before trying again

### Finalize Paid PaymentIntent

```text
POST /{baseSiteId}/users/{userId}/stripe/elements/intent/{paymentIntentId}/finalize
```

Request body:

- none

Optional query parameters:

- `cartId`: required for anonymous storefront callers so the backend can resolve the paid cart that owns the PaymentIntent. Pass the anonymous cart guid or cart code.

Response fields:

- standard OCC `OrderWsDTO` payload for the placed or previously placed order

Frontend expectation:

- call this after Stripe confirms a PaymentIntent as `succeeded` or `requires_capture`
- use this instead of the generic OCC `POST .../orders` endpoint for Payment Elements, because the backend must resolve the paid cart/order server-side before placing the order
- treat the response as the single source of truth for order confirmation after a successful Payment Elements payment

Example anonymous storefront finalize call:

```text
POST /occ/v2/apparel-uk-spa/users/anonymous/stripe/elements/intent/pi_123/finalize?cartId=00012345
```

## Storefront Return Model

The current storefront integration uses these rules:

- `apparel-uk-spa` is the validated base site for local browser work
- anonymous Payment Elements and hosted Checkout flows send `cartId` so OCC can restore the guest cart context after returning from Stripe
- the Stripe return route is a transport step only; storefront order confirmation must come from the OCC `finalize` response
- if `finalize` returns an existing order, the storefront should still treat that `OrderWsDTO` as success and route to order confirmation

## Refund Endpoints

### Create Order Refund

```text
POST /{baseSiteId}/users/{userId}/orders/{code}/stripe/refunds
```

Request body:

- `paymentReference`: required Stripe payment reference owned by the order. This can be a `pi_...` PaymentIntent id or a Checkout-backed reference accepted by the backend lifecycle service.
- `amount`: optional decimal refund amount in major currency units. Omit it for a full refund. When provided, it must be positive.

Example request:

```json
{
  "paymentReference": "pi_123",
  "amount": 5.00
}
```

Response fields:

- `id`: Stripe refund id such as `re_...`
- `paymentIntentId`: Stripe PaymentIntent refunded by Stripe
- `status`: Stripe refund status
- `amount`: refund amount in minor units
- `currency`: ISO currency code in lowercase Stripe format
- `formattedAmount`: SAP-formatted refund amount for display
- `orderCode`: SAP order code used for ownership resolution
- `paymentReference`: original payment reference supplied by the caller

Example response:

```json
{
  "id": "re_123",
  "paymentIntentId": "pi_123",
  "status": "succeeded",
  "amount": 500,
  "currency": "usd",
  "formattedAmount": "$5.00",
  "orderCode": "00001001",
  "paymentReference": "pi_123"
}
```

Frontend expectation:

- call this only from an authenticated order-details or support flow where the order already belongs to the active OCC user context
- do not assume every order is refundable; invalid or unowned payment references are rejected through the existing OCC error flow
- after a successful refund, refresh order/payment state from the backend instead of inferring all lifecycle details from the refund response alone

## Error Handling Expectations

Common failure categories:

- `400`: malformed refund requests, missing or invalid cart/payment preconditions, inaccessible owned-order context, or invalid Stripe payment references
- `401`: missing authentication
- current Stripe lookup and ownership failures are surfaced through the existing OCC exception flow, so frontend consumers should currently treat non-auth Stripe retrieval failures as generic request errors instead of relying on a distinct `403` or `404`

Frontend guidance:

- do not retry ownership or configuration failures blindly
- surface payment bootstrap failures as recoverable checkout errors
- if `publishableKey` or `returnUrl` is unexpectedly blank or placeholder-like, stop and treat it as a site configuration defect

## Integration Notes

- Stripe Checkout is the default phase-1 shopper flow.
- Payment Elements support is available at the OCC/backend contract level and can be wired by a frontend that already owns the cart session.
- OPF-facing identifiers are exposed through `paymentOptionId`, so the frontend can distinguish `stripe-checkout` from `stripe-elements` without knowing internal service classes.
- Current cart lifecycle cleanup is available through explicit expire and cancel endpoints, and order-bound refunds are now available through the dedicated refund endpoint.
- Real payment completion still depends on Stripe webhooks updating SAP Commerce state.

## Validation References

These contracts are backed by:

- `stripeocc` unit tests
- live OCC integration tests in `stripeocctests`
- local operator flow in `docs/stripe-local-e2e.md`
