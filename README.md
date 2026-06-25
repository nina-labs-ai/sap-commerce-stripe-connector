# SAP Commerce Stripe Connector

![SAP Commerce payment with Stripe](assets/stripe-sap-commerce-payment.gif)

Stripe payment connector for SAP Commerce Cloud 2211 JDK 21 with OCC APIs,
webhook handling, payment lifecycle services, business-process hooks,
backoffice visibility, and a SAP Composable Storefront integration package.

This project is released under the [MIT License](LICENSE).

This project is not affiliated with, sponsored by, or endorsed by Stripe, Inc.
Stripe is a trademark of Stripe, Inc.

## Compatibility

| Surface | Version validated |
| --- | --- |
| SAP Commerce Cloud | 2211 JDK 21.7 |
| Java | 21 |
| Stripe Java SDK | 33.1.0 |
| SAP Composable Storefront / Spartacus | 2211.31.1 |
| Angular | 17.3 |
| Stripe.js | 5.10.0 |

## Repository Layout

```text
hybris/bin/modules/stripe/          SAP Commerce extensions
installer/recipes/b2c_acc_plus_stripe/
js-storefront/stripe-spartacus-connector/
docs/
assets/stripe-sap-commerce-payment.gif
```

The extension layering keeps payment-provider logic in the Stripe modules while
using SAP Commerce service, facade, OCC, process, and backoffice boundaries:

| Extension | Responsibility |
| --- | --- |
| `stripeservices` | Stripe SDK boundary, payment/session lifecycle, transaction state, webhook service logic |
| `stripefacades` | Commerce-facing orchestration and data mapping |
| `stripeocc` | OCC REST endpoints for hosted Checkout and Payment Elements |
| `stripeevents` | Webhook HTTP endpoint and signature verification |
| `stripefulfilmentprocess` | Post-payment process integration |
| `stripebackoffice` | Operational configuration and backoffice visibility |
| `stripeocctests` | OCC-level validation suite |
| `stripetest` | Shared test/support extension |

`stripespartacussampledata` is intentionally not shipped in this public export:
the validated recipe uses the reusable Spartacus connector package instead of a
deprecated sample-data add-on path.

## Install

1. Copy or clone this repository into a SAP Commerce 2211 JDK21 installation.
2. Ensure these modules are under `hybris/bin/modules/stripe`.
3. Copy the recipe under `installer/recipes/b2c_acc_plus_stripe`.
4. Set Java 21 before running installer or Ant commands:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.2-tem
java -version
```

5. Configure the Stripe properties in `hybris/config/local.properties`:

```properties
stripe.secret.key=insert your secret key
stripe.publishable.key=insert your publishable key
stripe.webhook.secret=insert your webhook signing secret
stripe.checkout.success.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/return?session_id={CHECKOUT_SESSION_ID}
stripe.checkout.cancel.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/cancel?session_id={CHECKOUT_SESSION_ID}
stripe.elements.return.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/elements/return
```

6. Run the recipe:

```bash
cd installer
./install.sh -r b2c_acc_plus_stripe -A local_property:initialpassword.admin=nimda
./install.sh -r b2c_acc_plus_stripe initialize
./install.sh -r b2c_acc_plus_stripe start
```

7. Ensure the OCC OAuth client used by Spartacus exists in SAP Commerce. The
   default Spartacus 2211.31 client is `mobile_android` with client secret
   `secret`:

```impex
INSERT_UPDATE OAuthClientDetails; clientId[unique=true]; resourceIds; scope; authorizedGrantTypes; authorities; clientSecret; registeredRedirectUri
                                ; mobile_android       ; hybris     ; basic; authorization_code,refresh_token,password,client_credentials; ROLE_CLIENT; secret; http://localhost:9001/authorizationserver/oauth2_callback
```

## Storefront

Use `js-storefront/stripe-spartacus-connector` in an SAP Composable Storefront
2211.31 Angular 17 application. The package overrides the CMS
`CheckoutPaymentDetails` component with the Stripe payment step and registers:

- `stripe/checkout/return`
- `stripe/checkout/cancel`
- `stripe/elements/return`

Import `StripeCheckoutModule` from the package and keep guest checkout enabled
in the storefront configuration when the checkout CMS flow contains guest login
components.

Spartacus 2211.31 uses the `mobile_android` OAuth client id and `secret` client
secret by default. If your storefront overrides `authentication.client_id` or
`authentication.client_secret`, create the same OAuth client in SAP Commerce.

## Webhooks

Forward Stripe CLI directly to SAP Commerce. Do not use a local Node forwarder
or `localhost:4242/webhook`.

```bash
stripe listen --forward-to http://127.0.0.1:9001/stripeevents/webhooks/stripe
```

If only HTTPS is available locally:

```bash
stripe listen --skip-verify --forward-to https://127.0.0.1:9002/stripeevents/webhooks/stripe
```

## Validation

Run the deterministic backend checks from `hybris/bin/platform`:

```bash
. ./setantenv.sh
ant build -Dextension.names="stripeservices;stripefacades;stripeocc;stripeevents;stripefulfilmentprocess;stripebackoffice;stripeocctests;stripetest"
ant alltests -Dtestclasses.extensions="stripeservices,stripefacades,stripeocc,stripeevents,stripefulfilmentprocess,stripebackoffice,stripeocctests,stripetest"
ant allwebtests -Dtestclasses.extensions=stripeevents
ant manualtests -Dtestclasses.extensions=stripeocctests -Dtest=com.cci.hybris.stripe.occtests.AllStripeOccSpockTests
```

See:

- [Installation](docs/installation.md)
- [Local E2E](docs/local-e2e.md)
- [OCC contracts](docs/occ-contracts.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Validation notes](docs/validation.md)
