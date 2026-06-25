import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Order } from '@spartacus/order/root';
import {
  PaymentIntent,
  Stripe,
  loadStripe,
} from '@stripe/stripe-js';
import { Observable } from 'rxjs';
import { StripeOccEndpointService } from './stripe-occ-endpoint.service';

export interface StripePaymentElementIntent {
  id: string;
  clientSecret: string;
  status?: string;
  amount?: number;
  currency?: string;
  clientReferenceId?: string;
  publishableKey: string;
  paymentOptionId: string;
  paymentMethod: string;
  formattedAmount?: string;
  returnUrl: string;
}

@Injectable({
  providedIn: 'root',
})
export class StripePaymentElementService {
  private stripeCache = new Map<string, Promise<Stripe | null>>();

  constructor(
    private readonly http: HttpClient,
    private readonly stripeOccEndpointService: StripeOccEndpointService
  ) {}

  createPaymentIntent(cartId?: string): Observable<StripePaymentElementIntent> {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<StripePaymentElementIntent>(
        this.stripeOccEndpointService.userUrl(
          context,
          'stripe/elements/intent'
        ),
        {},
        this.stripeOccEndpointService.cartOptions(cartId)
      )
    );
  }

  getPaymentIntent(
    paymentIntentId: string,
    cartId?: string
  ): Observable<StripePaymentElementIntent> {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.get<StripePaymentElementIntent>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/elements/intent/${paymentIntentId}`
        ),
        this.stripeOccEndpointService.cartOptions(cartId)
      )
    );
  }

  cancelPaymentIntent(
    paymentIntentId: string,
    cartId?: string
  ): Observable<StripePaymentElementIntent> {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<StripePaymentElementIntent>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/elements/intent/${paymentIntentId}/cancel`
        ),
        {},
        this.stripeOccEndpointService.cartOptions(cartId)
      )
    );
  }

  finalizePaymentIntent(
    paymentIntentId: string,
    cartId?: string
  ): Observable<Order> {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<Order>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/elements/intent/${paymentIntentId}/finalize`
        ),
        {},
        this.stripeOccEndpointService.orderOptions(cartId)
      )
    );
  }

  async getStripe(publishableKey: string): Promise<Stripe> {
    if (!publishableKey) {
      throw new Error('Stripe publishable key is missing for Payment Elements.');
    }

    let stripePromise = this.stripeCache.get(publishableKey);
    if (!stripePromise) {
      stripePromise = loadStripe(publishableKey);
      this.stripeCache.set(publishableKey, stripePromise);
    }

    const stripe = await stripePromise;
    if (!stripe) {
      throw new Error('Stripe.js could not be initialized for Payment Elements.');
    }

    return stripe;
  }

  async retrievePaymentIntent(
    publishableKey: string,
    clientSecret: string
  ): Promise<PaymentIntent | null> {
    const stripe = await this.getStripe(publishableKey);
    const result = await stripe.retrievePaymentIntent(clientSecret);

    if (result.error) {
      throw new Error(
        result.error.message ||
          'Stripe could not verify the Payment Elements payment.'
      );
    }

    return result.paymentIntent ?? null;
  }
}
