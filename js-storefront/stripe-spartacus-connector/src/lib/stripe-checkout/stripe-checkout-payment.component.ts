import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
  GlobalMessageService,
  GlobalMessageType,
} from '@spartacus/core';
import { CheckoutStepService } from '@spartacus/checkout/base/components';
import { Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';
import { firstValueFrom } from 'rxjs';
import {
  StripePaymentElementIntent,
  StripePaymentElementService,
} from './stripe-payment-element.service';
import {
  StripeCheckoutConfiguration,
  StripeCheckoutService,
} from './stripe-checkout.service';
import { StripeCheckoutFlowService } from './stripe-checkout-flow.service';

type StripePaymentMode = 'checkout' | 'elements';
const SUCCESSFUL_PAYMENT_INTENT_STATUSES = new Set([
  'succeeded',
  'requires_capture',
]);
const FAILED_PAYMENT_INTENT_STATUSES = new Set([
  'requires_payment_method',
  'canceled',
]);

@Component({
  selector: 'app-stripe-checkout-payment',
  templateUrl: './stripe-checkout-payment.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StripeCheckoutPaymentComponent implements OnInit, OnDestroy {
  @ViewChild('paymentElementHost')
  set paymentElementHostSetter(
    value: ElementRef<HTMLDivElement> | undefined
  ) {
    this.paymentElementHost = value;
    this.tryMountPaymentElement();
  }

  checkoutConfiguration?: StripeCheckoutConfiguration;
  paymentElementIntent?: StripePaymentElementIntent;
  errorMessage?: string;
  isLoading = false;
  isPreparingElements = false;
  mode: StripePaymentMode = 'checkout';
  readonly cart$ = this.stripeCheckoutFlowService.cart$;

  protected paymentElementHost?: ElementRef<HTMLDivElement>;
  protected stripe?: Stripe;
  protected elements?: StripeElements;
  protected paymentElement?: StripePaymentElement;

  constructor(
    private readonly stripeCheckoutService: StripeCheckoutService,
    private readonly stripePaymentElementService: StripePaymentElementService,
    private readonly stripeCheckoutFlowService: StripeCheckoutFlowService,
    private readonly checkoutStepService: CheckoutStepService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly globalMessageService: GlobalMessageService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      this.checkoutConfiguration = await firstValueFrom(
        this.stripeCheckoutService.getConfiguration()
      );
    } catch (error) {
      this.errorMessage = this.getErrorMessage(error);
    } finally {
      this.changeDetectorRef.markForCheck();
    }
  }

  ngOnDestroy(): void {
    this.destroyPaymentElement();
  }

  back(): void {
    this.checkoutStepService.back(this.activatedRoute);
  }

  async selectMode(mode: StripePaymentMode): Promise<void> {
    if (this.mode === mode) {
      return;
    }

    this.mode = mode;
    this.errorMessage = undefined;

    if (mode === 'checkout') {
      this.destroyPaymentElement();
      this.changeDetectorRef.markForCheck();
      return;
    }

    await this.ensurePaymentElementsReady();
  }

  async continueWithSelectedMode(): Promise<void> {
    if (this.mode === 'elements') {
      await this.confirmPaymentElements();
      return;
    }

    await this.continueToStripe();
  }

  async continueToStripe(): Promise<void> {
    if (!this.checkoutConfiguration) {
      this.errorMessage =
        'Stripe Checkout is not ready yet. Reload the page and try again.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = undefined;

    try {
      const cartId =
        await this.stripeCheckoutFlowService.getActiveCartIdentifier();
      await this.stripeCheckoutFlowService.assignPaymentOptionIfSupported(
        this.checkoutConfiguration.paymentOptionId,
        cartId
      );
      const session = await firstValueFrom(
        this.stripeCheckoutService.createCheckoutSession(cartId)
      );
      if (!session.url) {
        throw new Error(
          'Stripe Checkout did not return a hosted checkout URL.'
        );
      }
      window.location.assign(session.url);
    } catch (error) {
      this.handleError(error);
    }
  }

  protected async ensurePaymentElementsReady(): Promise<void> {
    if (this.paymentElementIntent && this.stripe) {
      this.tryMountPaymentElement();
      return;
    }

    this.isPreparingElements = true;
    this.isLoading = true;
    this.errorMessage = undefined;
    this.changeDetectorRef.markForCheck();

    try {
      const cartId =
        await this.stripeCheckoutFlowService.getActiveCartIdentifier();
      this.paymentElementIntent = await firstValueFrom(
        this.stripePaymentElementService.createPaymentIntent(cartId)
      );
      this.stripe = await this.stripePaymentElementService.getStripe(
        this.paymentElementIntent.publishableKey
      );
      await this.stripeCheckoutFlowService.assignPaymentOptionIfSupported(
        this.paymentElementIntent.paymentOptionId,
        cartId
      );
      this.tryMountPaymentElement();
    } catch (error) {
      this.handleError(error);
    } finally {
      this.isPreparingElements = false;
      this.isLoading = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  protected async confirmPaymentElements(): Promise<void> {
    if (!this.paymentElementIntent || !this.stripe || !this.elements) {
      await this.ensurePaymentElementsReady();
    }

    if (!this.paymentElementIntent || !this.stripe || !this.elements) {
      this.errorMessage =
        'Stripe Payment Elements is not ready yet. Reload the page and try again.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = undefined;
    this.changeDetectorRef.markForCheck();

    try {
      const cartId =
        await this.stripeCheckoutFlowService.getActiveCartIdentifier();
      await this.stripeCheckoutFlowService.assignPaymentOptionIfSupported(
        this.paymentElementIntent.paymentOptionId,
        cartId
      );

      const result = await this.stripe.confirmPayment({
        elements: this.elements,
        confirmParams: {
          return_url: this.buildPaymentElementsReturnUrl(
            this.paymentElementIntent.returnUrl,
            cartId
          ),
        },
        redirect: 'if_required',
      });

      if (result.error) {
        throw new Error(
          result.error.message ||
            'Stripe Payment Elements could not confirm the payment.'
        );
      }

      if (!result.paymentIntent) {
        throw new Error(
          'Stripe Payment Elements did not return a payment status.'
        );
      }

      await this.handlePaymentIntentStatus(result.paymentIntent.status, cartId);
    } catch (error) {
      this.handleError(error);
    } finally {
      this.isLoading = false;
      this.changeDetectorRef.markForCheck();
    }
  }

  protected async handlePaymentIntentStatus(
    status: string,
    cartId: string
  ): Promise<void> {
    if (SUCCESSFUL_PAYMENT_INTENT_STATUSES.has(status)) {
      await this.finalizePaymentIntent(cartId);
      return;
    }

    if (status === 'processing') {
      throw new Error(
        'Stripe is still processing the Payment Elements payment. Wait for the return page to settle before placing the order.'
      );
    }

    if (FAILED_PAYMENT_INTENT_STATUSES.has(status)) {
      throw new Error(
        'Stripe Payment Elements did not confirm the payment. Try again or switch back to hosted checkout.'
      );
    }

    throw new Error(
      `Stripe Payment Elements returned ${status}. Review the PaymentIntent return page before placing the order.`
    );
  }

  protected async finalizePaymentIntent(cartId: string): Promise<void> {
    if (!this.paymentElementIntent?.id) {
      throw new Error(
        'Stripe Payment Elements did not provide the PaymentIntent needed to finalize the order.'
      );
    }

    const order = await firstValueFrom(
      this.stripePaymentElementService.finalizePaymentIntent(
        this.paymentElementIntent.id,
        cartId
      )
    );
    this.stripeCheckoutFlowService.completeOrder(order, cartId);
  }

  protected tryMountPaymentElement(): void {
    if (
      this.mode !== 'elements' ||
      !this.stripe ||
      !this.paymentElementHost ||
      !this.paymentElementIntent?.clientSecret
    ) {
      return;
    }

    this.destroyPaymentElement();
    this.elements = this.stripe.elements({
      clientSecret: this.paymentElementIntent.clientSecret,
    });
    this.paymentElement = this.elements.create('payment', {
      layout: 'tabs',
    });
    this.paymentElement.mount(this.paymentElementHost.nativeElement);
  }

  protected destroyPaymentElement(): void {
    this.paymentElement?.destroy();
    this.paymentElement = undefined;
    this.elements = undefined;
  }

  protected buildPaymentElementsReturnUrl(
    returnUrl: string,
    cartId: string
  ): string {
    const url = new URL(returnUrl);
    url.searchParams.set('cartId', cartId);
    return url.toString();
  }

  protected handleError(error: unknown): void {
    this.errorMessage = this.getErrorMessage(error);
    this.globalMessageService.add(
      this.errorMessage,
      GlobalMessageType.MSG_TYPE_ERROR
    );
  }

  protected getErrorMessage(error: unknown): string {
    return this.stripeCheckoutFlowService.resolveErrorMessage(
      error,
      'Stripe could not be started. Check the Stripe storefront URLs and OCC session state.'
    );
  }
}
