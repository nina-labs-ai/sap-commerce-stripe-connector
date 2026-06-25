# Validation Notes

This export is generated from the source repository after upgrading
`stripe-java` to `33.1.0`.

Source validation evidence captured during export preparation:

- `ant build -Dextension.names=stripeservices`
- `ant alltests -Dtestclasses.extensions=stripeservices`
- `ant integrationtests -Dtestclasses.extensions=stripeservices`
- JUnit XML scans reported no failures or errors for the SDK compatibility pass.

Run the full public-release validation matrix before publishing a new release:

- all Stripe extension build and alltests targets
- `stripeevents` allwebtests
- `stripeocctests` manual OCC Spock suite
- Spartacus storefront build
- real Stripe CLI direct webhook forwarding
- hosted Checkout success and cancel flows
- Payment Elements success and declined-card flows
- invalid/missing webhook signature negative paths
- duplicate and late webhook replay negative paths
- public export hygiene scan
