import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CartNotEmptyGuard, CheckoutAuthGuard } from '@spartacus/checkout/base/components';
import { CmsConfig, I18nModule, provideConfig } from '@spartacus/core';
import { StripeCheckoutCancelComponent } from './stripe-checkout-cancel.component';
import { StripeCheckoutPaymentComponent } from './stripe-checkout-payment.component';
import { StripePaymentElementReturnComponent } from './stripe-payment-element-return.component';
import { StripeCheckoutReturnComponent } from './stripe-checkout-return.component';

const stripeCheckoutRoutes: Routes = [
  {
    path: 'stripe/checkout/return',
    component: StripeCheckoutReturnComponent,
  },
  {
    path: 'stripe/checkout/cancel',
    component: StripeCheckoutCancelComponent,
  },
  {
    path: 'stripe/elements/return',
    component: StripePaymentElementReturnComponent,
  },
];

@NgModule({
  declarations: [
    StripeCheckoutPaymentComponent,
    StripeCheckoutReturnComponent,
    StripeCheckoutCancelComponent,
    StripePaymentElementReturnComponent,
  ],
  imports: [CommonModule, RouterModule.forChild(stripeCheckoutRoutes), I18nModule],
  providers: [
    provideConfig(<CmsConfig>{
      cmsComponents: {
        CheckoutPaymentDetails: {
          component: StripeCheckoutPaymentComponent,
          guards: [CheckoutAuthGuard, CartNotEmptyGuard],
        },
      },
    }),
  ],
})
export class StripeCheckoutModule {}
