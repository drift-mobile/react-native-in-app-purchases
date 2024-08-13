package expo.modules.inapppurchases;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import expo.modules.core.Promise;
import expo.modules.core.arguments.MapArguments;
import expo.modules.core.arguments.ReadableArguments;
import expo.modules.core.interfaces.services.EventEmitter;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
  public static final int OK = 0;
  public static final int USER_CANCELED = 1;
  public static final int ERROR = 2;

  public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;
  public static final String PURCHASES_UPDATED_EVENT = "Expo.purchasesUpdated";
  public static final String ACKNOWLEDGING_PURCHASE = "Acknowledging Item";
  private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

  protected static final HashMap<String, Promise> promises = new HashMap<>();
  private final List<Purchase> mPurchases = new ArrayList<>();
  private final HashMap<String, ProductDetails> mProductDetailsMap = new HashMap<>();
  private BillingClient mBillingClient;
  private final EventEmitter mEventEmitter;
  private boolean mIsServiceConnected;
  private final Activity mActivity;
  private final BillingUpdatesListener mBillingUpdatesListener;

  private Set<String> mTokensToBeConsumed;

  /**
   * Listener to the updates that happen when purchases list was updated or consumption of the
   * item was finished
   */
  public interface BillingUpdatesListener {
    void onBillingClientSetupFinished();

    void onConsumeFinished(String token, BillingResult result);

    void onPurchasesUpdated(List<Purchase> purchases);
  }

  public BillingManager(Activity activity, EventEmitter eventEmitter) {
    // Create PendingPurchasesParams
    PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder().build();

    mActivity = activity;
    mEventEmitter = eventEmitter;
    mBillingUpdatesListener = new UpdateListener(eventEmitter);
    mBillingClient =
      BillingClient
        .newBuilder(activity)
        .enablePendingPurchases(pendingPurchasesParams)
        .setListener(this)
        .build();
  }

  public void startConnection(final Promise promise) {
    // Start setup. This is asynchronous and the specified listener will be called
    // once setup completes.
    // It also starts to report all the new purchases through onPurchasesUpdated() callback.
    startServiceConnection(() -> {
      // Notifying the listener that billing client is ready
      mBillingUpdatesListener.onBillingClientSetupFinished();
      promise.resolve(null);
    });
  }

  public void startServiceConnection(final Runnable executeOnSuccess) {
    mBillingClient.startConnection(new BillingClientStateListener() {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        final int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingResponseCode.OK) {
          mIsServiceConnected = true;
          if (executeOnSuccess != null) {
            executeOnSuccess.run();
          }
        }
        mBillingClientResponseCode = responseCode;
      }

      @Override
      public void onBillingServiceDisconnected() {
        mIsServiceConnected = false;
      }
    });
  }

  /**
   * Start a purchase or subscription replace flow
   */
  public void purchaseItemAsync(final String skuId, @Nullable final ReadableArguments details, final Promise promise) {
    ReadableArguments d = details != null ? details : new MapArguments();
    String oldPurchaseToken = d.getString("oldPurchaseToken");
    ReadableArguments accountIdentifiers = d.getArguments("accountIdentifiers");
    // Boolean isVrPurchaseFlow = d.getBoolean("isVrPurchaseFlow");

    Runnable purchaseFlowRequest = () -> {
      ProductDetails productDetails = mProductDetailsMap.get(skuId);

      if (productDetails == null) {
        promise.reject("E_ITEM_NOT_QUERIED", "Must query item from store before calling purchase");
        return;
      }

      // Create a ProductDetailsParams object to use with BillingFlowParams
      List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList;
      List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = productDetails.getSubscriptionOfferDetails();
      BillingFlowParams.ProductDetailsParams.Builder productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
        .setProductDetails(productDetails);

      if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
        // If offer details are available, set the offer token
        String offerToken = offerDetailsList.get(0).getOfferToken();
        productDetailsParamsBuilder.setOfferToken(offerToken);
      }

      // Build the ProductDetailsParams
      productDetailsParamsList = Collections.singletonList(productDetailsParamsBuilder.build());

      // Build the BillingFlowParams with the ProductDetailsParams
      BillingFlowParams.Builder purchaseParamsBuilder = BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(productDetailsParamsList);

      if (oldPurchaseToken != null) {
        purchaseParamsBuilder.setSubscriptionUpdateParams(
          BillingFlowParams.SubscriptionUpdateParams.newBuilder().setOldPurchaseToken(oldPurchaseToken).build()
        );
      }

      /*
        For Android billing to work without a 'Something went wrong on our end. Please try again.'
        error, we must provide BOTH obfuscatedAccountId and obfuscatedProfileId.
       */
      if (accountIdentifiers != null) {
        String obfuscatedAccountId = accountIdentifiers.getString("obfuscatedAccountId");
        String obfuscatedProfileId = accountIdentifiers.getString("obfuscatedProfileId");
        if (obfuscatedAccountId != null && obfuscatedProfileId != null) {
          purchaseParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId);
          purchaseParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId);
        }
      }

      // false will be the default, unless true is passed
      // purchaseParams.setVrPurchaseFlow(isVrPurchaseFlow);

      mBillingClient.launchBillingFlow(mActivity, purchaseParamsBuilder.build());
    };

    executeServiceRequest(purchaseFlowRequest);
  }

  public Context getContext() {
    return mActivity;
  }

  /**
   * Handle a callback that purchases were updated from the Billing library
   */
  @Override
  public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
    if (result.getResponseCode() == BillingResponseCode.OK && purchases != null) {
      for (Purchase purchase : purchases) {
        handlePurchase(purchase);
      }
      mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
    } else {
      Bundle response = formatResponse(result, null);
      mEventEmitter.emit(PURCHASES_UPDATED_EVENT, response);
    }
  }

  public void acknowledgePurchaseAsync(String purchaseToken, final Promise promise) {
    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> {
      Bundle response = formatResponse(billingResult, null);

      promise.resolve(response);
    };

    AcknowledgePurchaseParams acknowledgePurchaseParams =
      AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build();
    mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
  }

  public void consumeAsync(final String purchaseToken, final Promise promise) {
    // If we've already scheduled to consume this token - no action is needed (this could happen
    // if you received the token when querying purchases inside onReceive() and later from
    // onActivityResult()
    if (mTokensToBeConsumed == null) {
      mTokensToBeConsumed = new HashSet<>();
    } else if (mTokensToBeConsumed.contains(purchaseToken)) {
      Bundle response = new Bundle();
      response.putInt("responseCode", BillingClient.BillingResponseCode.OK);
      promise.resolve(response);
      return;
    }

    if (promises.get(ACKNOWLEDGING_PURCHASE) != null) {
      promise.reject("E_UNFINISHED_PROMISE", "Must wait for promise to resolve before recalling function.");
      return;
    }
    promises.put(ACKNOWLEDGING_PURCHASE, promise);
    mTokensToBeConsumed.add(purchaseToken);

    // Generating Consume Response listener
    final ConsumeResponseListener onConsumeListener = (billingResult, purchaseToken1) -> {
      // If billing service was disconnected, we try to reconnect 1 time
      mBillingUpdatesListener.onConsumeFinished(purchaseToken1, billingResult);
    };

    // Creating a runnable from the request to use it inside our connection retry policy below
    Runnable consumeRequest = () -> {
      ConsumeParams consumeParams =
        ConsumeParams.newBuilder()
          .setPurchaseToken(purchaseToken)
          .build();
      // Consume the purchase async
      mBillingClient.consumeAsync(consumeParams, onConsumeListener);
    };

    executeServiceRequest(consumeRequest);
  }

  /**
   * Handles the purchase
   *
   * @param purchase Purchase to be handled
   */
  private void handlePurchase(Purchase purchase) {
    mPurchases.add(purchase);
  }

  /**
   * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
   * client connection response was not received yet.
   */
  public int getBillingClientResponseCode() {
    return mBillingClientResponseCode;
  }

  /**
   * Query both in app purchases and subscriptions and deliver the result in a formalized way
   * through a listener
   */
  public void queryPurchases(final Promise promise) {
    Runnable queryToExecute = () -> {
      final Set<String> ALL_QUERIES = new HashSet<>(Arrays.asList(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS));
      List<Purchase> purchases = new ArrayList<>();
      Set<String> completedQueries = new HashSet<>();
      Set<BillingResult> billingResults = new HashSet<>();

      QueryPurchasesParams inAppPurchasesParams = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP) // For in-app purchases
        .build();

      mBillingClient.queryPurchasesAsync(inAppPurchasesParams, (billingResult, inAppPurchases) -> {
        if (billingResult.getResponseCode() == BillingResponseCode.OK) {
          purchases.addAll(inAppPurchases);
        }
        billingResults.add(billingResult);
        completedQueries.add(BillingClient.ProductType.INAPP);
        if (completedQueries.containsAll(ALL_QUERIES) || !areSubscriptionsSupported()) {
          onQueryPurchasesFinished(aggregateBillingResults(billingResults), purchases, promise);
        }
      });

      if (areSubscriptionsSupported()) {
        QueryPurchasesParams subsPurchasesParams = QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.SUBS) // For subscriptions
          .build();

        mBillingClient.queryPurchasesAsync(subsPurchasesParams, (billingResult, subscriptionPurchases) -> {
          if (billingResult.getResponseCode() == BillingResponseCode.OK) {
            purchases.addAll(subscriptionPurchases);
          }
          billingResults.add(billingResult);
          completedQueries.add(BillingClient.ProductType.SUBS);
          if (completedQueries.containsAll(ALL_QUERIES)) {
            onQueryPurchasesFinished(aggregateBillingResults(billingResults), purchases, promise);
          }
        });
      }
    };

    executeServiceRequest(queryToExecute);
  }

  @NonNull
  private BillingResult aggregateBillingResults(@NonNull Set<BillingResult> billingResults) {
    for (BillingResult result: billingResults) {
      if (result.getResponseCode() != BillingResponseCode.OK) {
        return result;
      }
    }
    return billingResults.iterator().next();
  }

  /**
   * Format the result of a Purchase or Sku Details query depending on the result code
   */
  public static Bundle formatResponse(BillingResult billingResult, ArrayList<? extends Parcelable> results) {
    Bundle response = new Bundle();
    int responseCode = billingResult.getResponseCode();
    if (responseCode == BillingResponseCode.OK) {
      response.putInt("responseCode", OK);
      response.putParcelableArrayList("results", results != null ? results : new ArrayList<>());
    } else if (responseCode == BillingResponseCode.USER_CANCELED) {
      response.putInt("responseCode", USER_CANCELED);
    } else {
      response.putInt("responseCode", ERROR);
      response.putInt("errorCode", errorCodeNativeToJS(responseCode));
    }
    return response;
  }

  /**
   * Convert native error code to match corresponding TS enum
   */
  private static int errorCodeNativeToJS(int responseCode) {
    switch (responseCode) {
      case BillingResponseCode.ERROR:
        return 0;
      case BillingResponseCode.FEATURE_NOT_SUPPORTED:
        return 1;
      case BillingResponseCode.SERVICE_DISCONNECTED:
        return 2;
      case BillingResponseCode.SERVICE_UNAVAILABLE:
        return 3;
      case BillingResponseCode.NETWORK_ERROR:
        return 4;
      case BillingResponseCode.BILLING_UNAVAILABLE:
        return 5;
      case BillingResponseCode.ITEM_UNAVAILABLE:
        return 6;
      case BillingResponseCode.DEVELOPER_ERROR:
        return 7;
      case BillingResponseCode.ITEM_ALREADY_OWNED:
        return 8;
      case BillingResponseCode.ITEM_NOT_OWNED:
        return 9;
    }
    return 0;
  }

  /**
   * Convert native purchase state to match corresponding TS enum
   */
  private static int purchaseStateNativeToJS(int purchaseState) {
    switch (purchaseState) {
      case Purchase.PurchaseState.PENDING:
        return 0;
      case Purchase.PurchaseState.PURCHASED:
        return 1;
      case Purchase.PurchaseState.UNSPECIFIED_STATE:
        return 2;
    }
    return 0;

  }

  public static Bundle purchaseToBundle(Purchase purchase) {
    Bundle bundle = new Bundle();

    bundle.putBoolean("acknowledged", purchase.isAcknowledged());
    bundle.putString("orderId", purchase.getOrderId());

    // Use getProducts() instead of getSkus()
    if (!purchase.getProducts().isEmpty()) {
      bundle.putString("productId", purchase.getProducts().get(0));
    }

    bundle.putInt("purchaseState", purchaseStateNativeToJS(purchase.getPurchaseState()));
    bundle.putLong("purchaseTime", purchase.getPurchaseTime());
    bundle.putString("packageName", purchase.getPackageName());
    bundle.putString("purchaseToken", purchase.getPurchaseToken());

    return bundle;
  }

  /**
   * Handle a result from querying of purchases and report an updated list to the listener
   */
  private void onQueryPurchasesFinished(@NonNull BillingResult billingResult, List<Purchase> purchasesList, final Promise promise) {
    // Have we been disposed of in the meantime? If so, or bad result code, then quit
    if (mBillingClient == null || billingResult.getResponseCode() != BillingResponseCode.OK) {
      promise.reject("E_QUERY_FAILED", "Billing client was null or query was unsuccessful");
      return;
    }

    ArrayList<Bundle> results = new ArrayList<>();
    for (Purchase purchase : purchasesList) {
      results.add(purchaseToBundle(purchase));
    }

    // Update purchases inventory with new list of purchases
    mPurchases.clear();
    onPurchasesUpdated(billingResult, purchasesList);

    final Bundle response = formatResponse(billingResult, results);
    promise.resolve(response);
  }

  public void queryProductDetailsAsync(final List<String> skuList, final ProductDetailsResponseListener listener) {
    // Creating a runnable from the request to use it inside our connection retry policy below
    Runnable queryRequest = () -> {
      // Build a list of ProductDetailsParams for in-app products
      List<QueryProductDetailsParams.Product> inAppProducts = new ArrayList<>();
      for (String sku : skuList) {
        inAppProducts.add(
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(sku)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        );
      }

      QueryProductDetailsParams inAppParams = QueryProductDetailsParams.newBuilder()
        .setProductList(inAppProducts)
        .build();

      mBillingClient.queryProductDetailsAsync(inAppParams, (inAppResult, inAppDetails) -> {
        // Build a list of ProductDetailsParams for subscriptions
        List<QueryProductDetailsParams.Product> subsProducts = new ArrayList<>();
        for (String sku : skuList) {
          subsProducts.add(
            QueryProductDetailsParams.Product.newBuilder()
              .setProductId(sku)
              .setProductType(BillingClient.ProductType.SUBS)
              .build()
          );
        }

        QueryProductDetailsParams subsParams = QueryProductDetailsParams.newBuilder()
          .setProductList(subsProducts)
          .build();

        mBillingClient.queryProductDetailsAsync(subsParams, (subsResult, subsDetails) -> {
          inAppDetails.addAll(subsDetails);
          listener.onProductDetailsResponse(subsResult, inAppDetails);
        });
      });
    };

    // Execute the query request (e.g., on a background thread)
    queryRequest.run();
  }

  public void queryPurchasableItems(List<String> itemList, final Promise promise) {
    queryProductDetailsAsync(itemList, (billingResult, productDetailsList) -> {
      ArrayList<Bundle> results = new ArrayList<>();
      for (ProductDetails productDetails : productDetailsList) {
        mProductDetailsMap.put(productDetails.getProductId(), productDetails);
        results.add(productToBundle(productDetails));
      }
      Bundle response = formatResponse(billingResult, results);
      promise.resolve(response);
    });
  }

  private Bundle productToBundle(ProductDetails productDetails) {
    Bundle bundle = new Bundle();
    bundle.putString("productId", productDetails.getProductId());
    bundle.putString("title", productDetails.getTitle());
    bundle.putString("description", productDetails.getDescription());
    bundle.putString("price", Objects.requireNonNull(productDetails.getOneTimePurchaseOfferDetails()).getFormattedPrice());
    // Add more details as needed
    return bundle;
  }

  private void executeServiceRequest(Runnable runnable) {
    if (mIsServiceConnected) {
      runnable.run();
    } else {
      // If billing service was disconnected, we try to reconnect 1 time.
      startServiceConnection(runnable);
    }
  }

  /**
   * Checks if subscriptions are supported for current client
   */
  public boolean areSubscriptionsSupported() {
    BillingResult billingResult = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
    return billingResult.getResponseCode() == BillingResponseCode.OK;
  }

  /**
   * Clear the resources
   */
  public void destroy() {
    if (mBillingClient != null && mBillingClient.isReady()) {
      mBillingClient.endConnection();
      mBillingClient = null;
    }
  }

}
