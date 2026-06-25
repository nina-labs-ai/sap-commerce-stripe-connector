# Stripe Spartacus Connector

Reusable SAP Composable Storefront module for the SAP Commerce Stripe Connector.

## Install

Copy this package into a Spartacus 2211.31 Angular 17 workspace or publish it to
your internal package registry. Then import the module in your storefront
feature wiring:

```ts
import { StripeCheckoutModule } from '@nina-labs/sap-commerce-stripe-spartacus-connector';

@NgModule({
  imports: [StripeCheckoutModule],
})
export class SpartacusFeaturesModule {}
```

Keep guest checkout enabled when the checkout CMS flow includes guest checkout
login components:

```ts
provideConfig(<CheckoutConfig>{
  checkout: {
    guest: true,
  },
})
```

Keep the SAP Commerce OAuth client aligned with the storefront auth config. The
default Spartacus 2211.31 local config uses `mobile_android` and `secret`.

## Routes

- `stripe/checkout/return`
- `stripe/checkout/cancel`
- `stripe/elements/return`

The return routes call SAP Commerce OCC finalize endpoints before routing to
Spartacus order confirmation.

	## Validate

	```bash
	SPARTACUS_NPM_AUTH=... npm install
	npm run build
	```

The generated package pins the versions validated in the source storefront. A
`package-lock.json` is generated only when `SPARTACUS_NPM_AUTH` is available,
because SAP Spartacus 2211 packages are resolved through SAP's authenticated npm
registry.
