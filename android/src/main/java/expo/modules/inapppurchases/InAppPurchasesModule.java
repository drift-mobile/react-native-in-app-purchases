package expo.modules.inapppurchases;

import java.util.List;

import android.content.Context;
import android.app.Activity;

import expo.modules.core.ExportedModule;
import expo.modules.core.ModuleRegistry;
import expo.modules.core.Promise;
import expo.modules.core.arguments.ReadableArguments;
import expo.modules.core.interfaces.ExpoMethod;
import expo.modules.core.interfaces.ActivityProvider;
import expo.modules.core.interfaces.RegistryLifecycleListener;
import expo.modules.core.interfaces.services.EventEmitter;

public class InAppPurchasesModule extends ExportedModule implements RegistryLifecycleListener {
  private static final String NAME = "ExpoInAppPurchases";

  private BillingManager mBillingManager;
  private ModuleRegistry mModuleRegistry;

  public InAppPurchasesModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mModuleRegistry = moduleRegistry;
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void connectAsync(final Promise promise) {
    Activity activity = getCurrentActivity();
    if (activity == null) {
      promise.reject("E_ACTIVITY_UNAVAILABLE", "Activity is not available");
    }
    EventEmitter mEventEmitter = mModuleRegistry.getModule(EventEmitter.class);
    mBillingManager = new BillingManager(activity, mEventEmitter);
    mBillingManager.startConnection(promise);
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void getProductsAsync(List<String> itemList, final Promise promise) {
    mBillingManager.queryPurchasableItems(itemList, promise);
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void getPurchaseHistoryAsync(final ReadableArguments options, final Promise promise) {
    String USE_GOOGLE_PLAY_CACHE_KEY = "useGooglePlayCache";
    if (options.getBoolean(USE_GOOGLE_PLAY_CACHE_KEY, true)) {
      mBillingManager.queryPurchases(promise);
    }
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void purchaseItemAsync(String skuId, ReadableArguments details, final Promise promise) {
    mBillingManager.purchaseItemAsync(skuId, details, promise);
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void getBillingResponseCodeAsync(final Promise promise) {
    promise.resolve(mBillingManager.getBillingClientResponseCode());
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void finishTransactionAsync(String purchaseToken, Boolean consume, final Promise promise) {
    if (consume != null && consume) {
      mBillingManager.consumeAsync(purchaseToken, promise);
    } else {
      mBillingManager.acknowledgePurchaseAsync(purchaseToken, promise);
    }
  }

  // Suppressing "unused" warning because this method is exposed to React Native
  @SuppressWarnings("unused")
  @ExpoMethod
  public void disconnectAsync(final Promise promise) {
    if (mBillingManager != null) {
      mBillingManager.destroy();
      mBillingManager = null;
    }
    promise.resolve(null);
  }

  private Activity getCurrentActivity() {
    ActivityProvider activityProvider = mModuleRegistry.getModule(ActivityProvider.class);
    return activityProvider != null ? activityProvider.getCurrentActivity() : null;
  }
}
