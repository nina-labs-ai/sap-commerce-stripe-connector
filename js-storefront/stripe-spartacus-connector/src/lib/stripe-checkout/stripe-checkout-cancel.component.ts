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
  selector: 'app-stripe-checkout-cancel',
  templateUrl: './stripe-checkout-cancel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StripeCheckoutCancelComponent implements OnInit {
  session?: StripeCheckoutSession;
  sessionId?: string;
  cartId?: string;
  errorMessage?: string;
  isLoading = true;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly stripeCheckoutService: StripeCheckoutService,
    private readonly stripeCheckoutFlowService: StripeCheckoutFlowService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  async ngOnInit(): Promise<void> {
    this.sessionId = this.activatedRoute.snapshot.queryParamMap.get('session_id') ?? undefined;
    this.cartId = this.activatedRoute.snapshot.queryParamMap.get('cartId') ?? undefined;
    if (!this.sessionId) {
      this.errorMessage =
        'Stripe canceled the redirect without returning a Checkout Session identifier.';
      this.isLoading = false;
      return;
    }

    try {
      const cartId =
        this.cartId ??
        (await this.stripeCheckoutFlowService.getActiveCartIdentifier());
      this.session = await firstValueFrom(
        this.stripeCheckoutService.expireCheckoutSession(this.sessionId, cartId)
      );
    } catch (error) {
      this.errorMessage = this.stripeCheckoutFlowService.resolveErrorMessage(
        error,
        'The Stripe Checkout Session could not be expired.'
      );
    } finally {
      this.isLoading = false;
      this.changeDetectorRef.markForCheck();
    }
  }
}
