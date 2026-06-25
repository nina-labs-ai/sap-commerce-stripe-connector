# Stripe Local E2E Verification

This guide exercises the current Stripe connector locally with real OCC calls and real Stripe test-mode webhooks.

## Scope

- Stripe Checkout Session creation through OCC
- Stripe Payment Elements bootstrap, confirmation, and OCC finalize
- Local webhook delivery into SAP Commerce through Stripe CLI
- Real storefront validation on `apparel-uk-spa`

This guide does not replace the automated servicelayer/OCC/webhook integration suites. It gives you a repeatable operator flow on top of them.

## Prerequisites

- SAP Commerce is built and starts successfully on this repo under JDK 21.
- `stripe.secret.key` and `stripe.webhook.secret` are set in `hybris/config/local.properties`.
- For Payment Elements browser work, set a real `stripe.publishable.key`. The current placeholder is not enough for frontend usage.
- The OCC OAuth client used by Spartacus exists in SAP Commerce. The default
  local client is `mobile_android` with client secret `secret`.
- `stripe` CLI is installed.
- `jq` is available for the sample shell commands below.

## Operator Property Checklist

Before running the browser flow, verify these site-aware properties resolve for `apparel-uk`:

- `stripe.secret.key`
- `stripe.publishable.key`
- `stripe.webhook.secret`
- `stripe.checkout.success.url`
- `stripe.checkout.cancel.url`
- `stripe.elements.return.url`

In this repo, those are the same keys surfaced by the Stripe Backoffice configuration status service. For local validation, the return URLs should point at the `apparel-uk-spa/en/GBP` storefront routes documented below.

## Local URLs

Default local runtime URLs in this repo:

- OCC base: `http://localhost:9001/occ/v2`
- Stripe webhook controller: `http://localhost:9001/stripeevents/webhooks/stripe`
- Storefront base: `http://apparel-uk.local:4200/apparel-uk-spa/en/GBP`
- Stripe Checkout success route in the local storefront: `http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/return`
- Stripe Checkout cancel route in the local storefront: `http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/cancel`
- Stripe Payment Elements return route in the local storefront: `http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/elements/return`

If your local Tomcat ports differ, replace `9001` accordingly. If you only expose HTTPS, switch to `https://localhost:9002` and use `curl -k`.

## Discover Runtime Values

List available base sites:

```bash
curl -s http://localhost:9001/occ/v2/basesites | jq .
```

For the current validated storefront flow, use `apparel-uk-spa`:

```bash
export BASE_SITE=apparel-uk-spa
export OCC_BASE_URL="http://localhost:9001/occ/v2/${BASE_SITE}"
export WEBHOOK_TARGET_URL="http://127.0.0.1:9001/stripeevents/webhooks/stripe"
```

If your local data uses another site, replace `apparel-uk-spa`.

## Align Stripe Return URLs

Before testing the hosted Stripe redirect from the storefront, point the local Stripe runtime properties at the storefront routes:

```properties
apparel-uk.stripe.checkout.success.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/return
apparel-uk.stripe.checkout.cancel.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/cancel
apparel-uk.stripe.elements.return.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/elements/return
```

`DefaultStripeConfigurationService` will append `session_id={CHECKOUT_SESSION_ID}` automatically if the placeholder is not already present.

## Verify OCC OAuth Client

Spartacus 2211.31 requests OCC tokens with the `mobile_android` client id and
the `secret` client secret by default. If `/authorizationserver/oauth/token`
returns `401 invalid_client`, import or update the client in HAC
(`https://localhost:9002` -> Console -> ImpEx Import):

```impex
INSERT_UPDATE OAuthClientDetails; clientId[unique=true]; resourceIds; scope; authorizedGrantTypes; authorities; clientSecret; registeredRedirectUri
                                ; mobile_android       ; hybris     ; basic; authorization_code,refresh_token,password,client_credentials; ROLE_CLIENT; secret; http://localhost:9001/authorizationserver/oauth2_callback
```

If your storefront overrides `authentication.client_id` or
`authentication.client_secret`, use the same values in this ImpEx and in the
token commands below.

## Start SAP Commerce

```bash
cd hybris/bin/platform
./hybrisserver.sh
```

Wait until OCC and the `stripeevents` web module are reachable.

## Start Stripe CLI

```bash
stripe listen --forward-to "${WEBHOOK_TARGET_URL}"
```

The CLI will print the webhook signing secret it is using. Keep `hybris/config/local.properties` aligned with that secret when you validate webhook delivery.

## Create a Customer Token

Register a customer:

```bash
export CUSTOMER_EMAIL="stripe.$(date +%s)@example.com"
export CUSTOMER_PASSWORD="StripePass123!"

curl -s -X POST "${OCC_BASE_URL}/users" \
  --data-urlencode "login=${CUSTOMER_EMAIL}" \
  --data-urlencode "password=${CUSTOMER_PASSWORD}" \
  --data-urlencode "firstName=Stripe" \
  --data-urlencode "lastName=Tester" \
  --data-urlencode "titleCode=mr" | jq .
```

Then request an OAuth token:

```bash
export ACCESS_TOKEN=$(
  curl -s -X POST http://localhost:9001/authorizationserver/oauth/token \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'client_id=mobile_android' \
    --data-urlencode 'client_secret=secret' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "username=${CUSTOMER_EMAIL}" \
    --data-urlencode "password=${CUSTOMER_PASSWORD}" \
  | jq -r '.access_token'
)
```

Sanity check:

```bash
test -n "${ACCESS_TOKEN}" && echo "token ready"
```

## Create a Cart and Add a Product

Create the session cart:

```bash
export CART_CODE=$(
  curl -s -X POST "${OCC_BASE_URL}/users/current/carts" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  | jq -r '.code'
)
```

Choose an in-stock product code from your data set and add it to the cart. Example product discovery:

```bash
curl -s "${OCC_BASE_URL}/products/search?query=:relevance:*&pageSize=5" | jq '.products[] | {code, name}'
```

Then add one product:

```bash
export PRODUCT_CODE='<replace-with-real-product-code>'

curl -s -X POST "${OCC_BASE_URL}/users/current/carts/${CART_CODE}/entries" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"product\":{\"code\":\"${PRODUCT_CODE}\"},\"quantity\":1}" | jq .
```

## OCC Checkout Flow

Create the Checkout Session:

```bash
export CHECKOUT_RESPONSE=$(
  curl -s -X POST "${OCC_BASE_URL}/users/current/stripe/checkout/session" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}"
)

echo "${CHECKOUT_RESPONSE}" | jq .
export CHECKOUT_SESSION_ID=$(echo "${CHECKOUT_RESPONSE}" | jq -r '.id')
export CHECKOUT_URL=$(echo "${CHECKOUT_RESPONSE}" | jq -r '.url')
```

Retrieve the same Checkout Session through OCC:

```bash
curl -s "${OCC_BASE_URL}/users/current/stripe/checkout/session/${CHECKOUT_SESSION_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq .
```

Open `CHECKOUT_URL` in a browser and complete the Stripe test-mode payment.

Expected result:

- OCC returns a `cs_...` session id
- OCC returns a hosted Stripe URL
- Stripe CLI forwards `checkout.session.completed`
- SAP Commerce webhook handling returns HTTP `200`

## OCC Payment Elements Bootstrap And Finalize

This step validates the OCC contract used by the storefront Payment Elements flow.

```bash
curl -s -X POST "${OCC_BASE_URL}/users/current/stripe/elements/intent" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq .
```

Expected result:

- OCC returns a `pi_...` payment intent id
- response includes `clientSecret`
- response includes `publishableKey`
- response includes `returnUrl`

If `publishableKey` is still the placeholder value, stop and fix `stripe.publishable.key` before attempting a frontend Payment Elements flow.

After Stripe confirms the PaymentIntent, the storefront must call the OCC finalize endpoint before showing SAP Commerce order confirmation:

```bash
curl -s -X POST \
  "${OCC_BASE_URL}/users/current/stripe/elements/intent/<payment-intent-id>/finalize" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq .
```

Expected result:

- OCC returns a standard `OrderWsDTO`
- the storefront should route to Spartacus `/order-confirmation`
- order confirmation comes from the OCC finalize response, not from the Stripe redirect alone

## Storefront End-To-End Validation

The currently validated storefront happy path is:

1. Browse to `http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/`
2. Add an in-stock product to cart
3. Continue through guest checkout
4. Complete delivery address and delivery mode
5. On the Stripe payment step, switch to `Payment Elements`
6. Confirm with Stripe test card `4242 4242 4242 4242`, any future expiry, and any CVC
7. Let Stripe return to `stripe/elements/return`
8. Confirm the storefront lands on `/order-confirmation`

What to expect during the browser flow:

- the storefront calls `POST /occ/v2/apparel-uk-spa/users/anonymous/stripe/elements/intent?cartId=...`
- Stripe returns to the configured `stripe/elements/return` route with `payment_intent` and `payment_intent_client_secret`
- the return component refreshes the intent and then calls:

```text
POST /occ/v2/apparel-uk-spa/users/anonymous/stripe/elements/intent/{paymentIntentId}/finalize?cartId={cartGuid}
```

- the finalize response is the trigger for Spartacus order confirmation

## Trigger Webhook Replays

If you completed a Checkout payment in the browser, Stripe will emit the live test-mode event and the CLI will relay it directly into SAP Commerce.

You can also trigger a replay manually:

```bash
stripe events list --limit 10
stripe events resend <event_id> --webhook-endpoint <endpoint_id>
```

Or trigger common local test events with the CLI when applicable:

```bash
stripe trigger checkout.session.completed
stripe trigger payment_intent.succeeded
```

Use these only when the payload metadata matches a Stripe object created by this connector; otherwise SAP Commerce will intentionally ignore the event because the site/payment-flow/order metadata checks will not match.

## What To Check In SAP Commerce

- The OCC create call returns a Stripe id and frontend-safe fields.
- Stripe CLI reports successful delivery to `/stripeevents/webhooks/stripe`.
- Stripe CLI shows delivery success.
- The webhook endpoint returns `200`.
- Payment Elements browser return ends at storefront `/order-confirmation`, not just at the Stripe return route.
- Hosted Checkout browser return also requires the OCC `finalize` call before storefront confirmation.
- Existing automated suites still pass:

```bash
cd hybris/bin/platform
. ./setantenv.sh
ant manualtests -Dtestclasses.extensions=stripeevents -Dtest=com.cci.hybris.stripe.events.controllers.StripeWebhookControllerIntegrationTest
ant manualtests -Dtestclasses.extensions=stripeocctests -Dtest=com.cci.hybris.stripe.occtests.AllStripeOccSpockTests
```
