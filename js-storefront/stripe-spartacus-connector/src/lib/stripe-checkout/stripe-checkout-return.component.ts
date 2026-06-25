import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  StripeCheckoutService,
  StripeCheckoutSession,
} from './stripe-checkout.service';
import { StripeCheckoutFlowService } from './stripe-checkout-flow.service';

@Component({
  selector: 'app-stripe-checkout-return',
  templateUrl: './stripe-checkout-return.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StripeCheckoutReturnComponent implements OnInit {
  session?: StripeCheckoutSession;
  sessionId?: string;
  cartId?: string;
  orderCode?: string;
  errorMessage?: string;
  isLoading = true;
  isPlacingOrder = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly stripeCheckoutService: StripeCheckoutService,
    private readonly stripeCheckoutFlowService: StripeCheckoutFlowService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  async ngOnInit(): Promise<void> {
    this.sessionId = this.activatedRoute.snapshot.queryParamMap.get('session_id') ?? undefined;
    this.cartId = this.activatedRoute.snapshot.queryParamMap.get('cartId') ?? undefined;
    this.orderCode = this.activatedRoute.snapshot.queryParamMap.get('orderCode') ?? undefined;
    if (!this.sessionId) {
      this.errorMessage =
        'Stripe did not return a Checkout Session identifier in the storefront return URL.';
      this.isLoading = false;
      return;
    }

    try {
      this.session = await this.stripeCheckoutService.pollCheckoutSession(
        this.sessionId,
        {
          cartId: this.cartId,
          orderCode: this.orderCode,
        }
      );
      if (this.isPaid()) {
        await this.placeOrder();
        return;
      }
    } catch (error) {
      this.errorMessage = this.stripeCheckoutFlowService.resolveErrorMessage(
        error,
        'Stripe Checkout status could not be refreshed.'
      );
    } finally {
      this.isLoading = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  isPaid(): boolean {
    return (
      this.session?.paymentStatus === 'paid' ||
      this.session?.status === 'complete'
    );
  }

  protected async placeOrder(): Promise<void> {
    this.isPlacingOrder = true;
    this.changeDetectorRef.markForCheck();

    try {
      const order = await firstValueFrom(
        this.stripeCheckoutService.finalizeCheckoutSession(
          this.sessionId!,
          this.cartId,
          this.orderCode
        )
      );
      this.stripeCheckoutFlowService.completeOrder(order, this.cartId);
    } catch (error) {
      this.errorMessage = this.stripeCheckoutFlowService.resolveErrorMessage(
        error,
        'Stripe accepted the payment, but SAP Commerce could not place the order.'
      );
    } finally {
      this.isPlacingOrder = false;
    }
  }
}
