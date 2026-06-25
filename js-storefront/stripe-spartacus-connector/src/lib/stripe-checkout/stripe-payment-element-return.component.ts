import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PaymentIntent } from '@stripe/stripe-js';
import { firstValueFrom } from 'rxjs';
import {
  StripePaymentElementIntent,
  StripePaymentElementService,
} from './stripe-payment-element.service';
import { StripeCheckoutFlowService } from './stripe-checkout-flow.service';

@Component({
  selector: 'app-stripe-payment-element-return',
  templateUrl: './stripe-payment-element-return.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StripePaymentElementReturnComponent implements OnInit {
  paymentIntentId?: string;
  clientSecret?: string;
  cartId?: string;
  intent?: StripePaymentElementIntent;
  stripePaymentIntent?: PaymentIntent | null;
  errorMessage?: string;
  isLoading = true;
  isPlacingOrder = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly stripePaymentElementService: StripePaymentElementService,
    private readonly stripeCheckoutFlowService: StripeCheckoutFlowService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  async ngOnInit(): Promise<void> {
    this.paymentIntentId =
      this.activatedRoute.snapshot.queryParamMap.get('payment_intent') ??
      undefined;
    this.clientSecret =
      this.activatedRoute.snapshot.queryParamMap.get(
        'payment_intent_client_secret'
      ) ?? undefined;
    this.cartId =
      this.activatedRoute.snapshot.queryParamMap.get('cartId') ?? undefined;

    if (!this.paymentIntentId || !this.clientSecret) {
      this.errorMessage =
        'Stripe did not return the Payment Elements intent details needed to finish checkout.';
      this.isLoading = false;
      return;
    }

    try {
      await this.stripeCheckoutFlowService.restoreCartContext(this.cartId);
      this.intent = await firstValueFrom(
        this.stripePaymentElementService.getPaymentIntent(
          this.paymentIntentId,
          this.cartId
        )
      );
      this.stripePaymentIntent =
        await this.stripePaymentElementService.retrievePaymentIntent(
          this.intent.publishableKey,
          this.clientSecret
        );

      if (this.isSuccessfulStatus()) {
        await this.placeOrder();
        return;
      }
    } catch (error) {
      this.errorMessage = this.stripeCheckoutFlowService.resolveErrorMessage(
        error,
        'Stripe Payment Elements status could not be refreshed.'
      );
    } finally {
      this.isLoading = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  isSuccessfulStatus(): boolean {
    return this.matchesStatus('succeeded', 'requires_capture');
  }

  isPendingStatus(): boolean {
    return this.matchesStatus(
      'processing',
      'requires_action',
      'requires_confirmation'
    );
  }

  isFailedStatus(): boolean {
    return this.matchesStatus('requires_payment_method', 'canceled');
  }

  protected matchesStatus(...statuses: string[]): boolean {
    return Boolean(
      this.stripePaymentIntent?.status &&
        statuses.includes(this.stripePaymentIntent.status)
    );
  }

  protected async placeOrder(): Promise<void> {
    this.isPlacingOrder = true;
    this.changeDetectorRef.markForCheck();

    try {
      const order = await firstValueFrom(
        this.stripePaymentElementService.finalizePaymentIntent(
          this.paymentIntentId!,
          this.cartId
        )
      );
      this.stripeCheckoutFlowService.completeOrder(order, this.cartId);
    } catch (error) {
      this.errorMessage = this.stripeCheckoutFlowService.resolveErrorMessage(
        error,
        'Stripe confirmed the payment, but SAP Commerce could not place the order.'
      );
    } finally {
      this.isPlacingOrder = false;
    }
  }
}
