# Installation

## Prerequisites

- SAP Commerce Cloud 2211 JDK21 installation.
- Java 21 selected before `ant`, `install.sh`, or server startup.
- Stripe CLI for local webhook forwarding.
- Node.js/npm for SAP Composable Storefront.
- Local hosts entry: `127.0.0.1 apparel-uk.local`.

## Copy the Connector

Copy the `hybris/bin/modules/stripe` directory into the same path in your SAP
Commerce installation. Copy `installer/recipes/b2c_acc_plus_stripe` into the
Commerce installer recipes directory.

The public export intentionally ships one validated recipe. A separate OCC-only
recipe is not included because the connector has been validated against the
combined B2C accelerator plus OCC/composable setup used by the local runtime.

## Configure Stripe

Use placeholder properties only in version control. Set real values in
`hybris/config/local.properties` or a deployment secret mechanism.

```properties
stripe.secret.key=insert your secret key
stripe.publishable.key=insert your publishable key
stripe.webhook.secret=insert your webhook signing secret
stripe.checkout.success.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/return?session_id={CHECKOUT_SESSION_ID}
stripe.checkout.cancel.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/checkout/cancel?session_id={CHECKOUT_SESSION_ID}
stripe.elements.return.url=http://apparel-uk.local:4200/apparel-uk-spa/en/GBP/stripe/elements/return
```

## Build and Start

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.2-tem
cd installer
./install.sh -r b2c_acc_plus_stripe -A local_property:initialpassword.admin=nimda
./install.sh -r b2c_acc_plus_stripe initialize
./install.sh -r b2c_acc_plus_stripe start
```

## Storefront OAuth Client

Spartacus 2211.31 uses `mobile_android` and `secret` for local OCC token
requests by default. Import or update that OAuth client before browser checkout
validation:

```impex
INSERT_UPDATE OAuthClientDetails; clientId[unique=true]; resourceIds; scope; authorizedGrantTypes; authorities; clientSecret; registeredRedirectUri
                                ; mobile_android       ; hybris     ; basic; authorization_code,refresh_token,password,client_credentials; ROLE_CLIENT; secret; http://localhost:9001/authorizationserver/oauth2_callback
```

If your storefront uses different `authentication.client_id` or
`authentication.client_secret` values, keep the storefront config and SAP
Commerce OAuth client in sync.
