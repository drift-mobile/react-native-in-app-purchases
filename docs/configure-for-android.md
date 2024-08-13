## Configure for Android

On Android, you must first create an entry for your app and upload a release APK in the [Google Play Console](https://developer.android.com/distribute/console/). Then, configure in-app purchases and details under **Monetize > Products > In-app products**.

Then to test your purchases, you must publish your app to a closed or open testing track in Google Play. Note that it may take a few hours for the app to be available for testers. Ensure the testers you invite (including yourself) opt into your app's test. On your test's opt-in URL, your testers will get an explanation of what it means to be a tester and a link to opt-in. At this point, they're all set and can start making purchases once they download your app or build from the source. For more information on testing, follow [instructions from Android's documentation](https://developer.android.com/google/play/billing/test).

#### Associating users and handling out-of-app purchases

Google Play API does not provide information about the purchasing user. To associate such data (for example, to link users in your app's backend to a purchase), you must provide both the `obfuscatedAccountId` and `obfuscatedProfileId` values in the `IAPPurchaseItemOptions` object passed to InAppPurchases.purchaseItemAsync().

If you offer subscriptions, they can be configured for repurchase up to a year after expiration. In this case, your purchase listener callback should handle [out-of-app payments](https://developer.android.com/google/play/billing/integrate#ooap). In addition, these payments can be initiated at any time, even if the app is not installed or inactive. So you must provide a feature for users to claim and activate the purchase. Since there's no way to link an obfuscated account or profile ID with the purchase, your app's backend must manage this scenario appropriately.