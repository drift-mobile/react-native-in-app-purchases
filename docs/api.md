# API

```javascript
import * as InAppPurchases from 'react-native-in-app-purchases';
```

## Table of Contents

- [Methods](#methods)
  - [InAppPurchases.connectAsync()](#inapppurchasesconnectasync)
  - [InAppPurchases.disconnectAsync()](#inapppurchasesdisconnectasync)
  - [InAppPurchases.finishTransactionAsync(purchase, consumeItem)](#inapppurchasesfinishtransactionasyncpurchase-consumeitem)
  - [InAppPurchases.getBillingResponseCodeAsync()](#inapppurchasesgetbillingresponsecodeasync)
  - [InAppPurchases.getProductsAsync(itemList)](#inapppurchasesgetproductsasyncitemlist)
  - [InAppPurchases.getPurchaseHistoryAsync(options)](#inapppurchasesgetpurchasehistoryasyncoptions)
  - [InAppPurchases.purchaseItemAsync(itemId, details)](#inapppurchasespurchaseitemasyncitemid-details)
  - [InAppPurchases.setPurchaseListener(callback)](#inapppurchasessetpurchaselistenercallback)
- [Interfaces](#interfaces)
  - [IAPItemDetails](#iapitemdetails)
  - [IAPPurchaseItemOptions](#iappurchaseitemoptions-android-only)
  - [IAPQueryResponse](#iapqueryresponse)
  - [InAppPurchase](#inapppurchase)
- [Types](#types)
  - [IAPPurchaseHistoryOptions](#iappurchasehistoryoptions)
  - [QueryResult](#queryresult)
- [Enums](#enums)
  - [IAPErrorCode](#iaperrorcode)
  - [IAPItemType](#iapitemtype)
  - [IAPResponseCode](#iapresponsecode)
  - [InAppPurchaseState](#inapppurchasestate)

## Methods

### `InAppPurchases.connectAsync()`

Connects to the app store and performs all of the necessary initialization to prepare the module to accept payments. This method must be called before anything else, otherwise an error will be thrown.

  Returns: `Promise<void>`
  Returns a Promise that fulfills when connection is established.

### `InAppPurchases.disconnectAsync()`

Disconnects from the app store and cleans up memory internally. Call this when you are done using the In-App Purchases API in your app.

No other methods can be used until the next time you call connectAsync.

  Returns: `Promise<void>`
  Returns a Promise that fulfils when disconnecting process is finished.

### `InAppPurchases.finishTransactionAsync(purchase, consumeItem)`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **purchase** | `[InAppPurchase](https://docs.expo.dev/versions/v49.0.0/sdk/in-app-purchases/#inapppurchase)` | The purchase you want to mark as completed. |
| **consumeItem** | `boolean` | **Android Only.** A boolean indicating whether or not the item is a consumable. |

Marks a transaction as completed. This must be called on successful purchases only after you have verified the transaction and unlocked the functionality purchased by the user.

On Android, this will either "acknowledge" or "consume" the purchase depending on the value of consumeItem. Acknowledging indicates that this is a one time purchase (e.g. premium upgrade), whereas consuming a purchase allows it to be bought more than once. You cannot buy an item again until it's consumed. Both consuming and acknowledging let Google know that you are done processing the transaction. If you do not acknowledge or consume a purchase within three days, the user automatically receives a refund, and Google Play revokes the purchase.

On iOS, this will [mark the transaction as finished](https://developer.apple.com/documentation/storekit/skpaymentqueue/1506003-finishtransaction) and prevent it from reappearing in the purchase listener callback. It will also let the user know their purchase was successful.

consumeItem is ignored on iOS because you must specify whether an item is a consumable or non-consumable in its product entry in App Store Connect, whereas on Android you indicate an item is consumable at runtime.

> Make sure that you verify each purchase to prevent faulty transactions and protect against fraud before you call `finishTransactionAsync`. On iOS, you can validate the purchase's transactionReceipt with the App Store as described [here](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/validating_receipts_with_the_app_store?language=objc). On Android, you can verify your purchase using the Google Play Developer API as described [here](https://developer.android.com/google/play/billing/security#validating-purchase).

  Returns: `Promise<void>`

#### Example

```javascript
if (!purchase.acknowledged) {
  await finishTransactionAsync(purchase, false); // or true for consumables
}
```

### `InAppPurchases.getBillingResponseCodeAsync()`

Returns the last response code. This is more descriptive on Android since there is native support for retrieving the billing response code.

On Android, this will return `IAPResponseCode.ERROR` if you are not connected or one of the billing response codes found [here](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode) if you are.

On iOS, this will return `IAPResponseCode.OK` if you are connected or `IAPResponseCode.ERROR` if you are not. Therefore, it's a good way to test whether or not you are connected and it's safe to use the other methods.


Returns: `Promise<IAPResponseCode>`
Returns a Promise that fulfils with an number representing the IAPResponseCode.

#### Example

```javascript
const responseCode = await getBillingResponseCodeAsync();
 if (responseCode !== IAPResponseCode.OK) {
  // Either we're not connected or the last response returned an error (Android)
}
```

### `InAppPurchases.getProductsAsync(itemList)`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **itemList** | `string[]` | The list of product IDs whose details you want to query from the app store. |

Retrieves the product details (price, description, title, etc) for each item that you inputted in the Google Play Console and App Store Connect. These products are associated with your app's specific Application/Bundle ID and cannot be retrieved from other apps. This queries both in-app products and subscriptions so there's no need to pass those in separately.

You must retrieve an item's details before you attempt to purchase it via `purchaseItemAsync`. This is a prerequisite to buying a product even if you have the item details bundled in your app or on your own servers.

If any of the product IDs passed in are invalid and don't exist, you will not receive an [IAPItemDetails](#iapitemdetails) object corresponding to that ID. For example, if you pass in four product IDs in but one of them has a typo, you will only get three response objects back.

Returns: `Promise<IAPQueryResponse<IAPItemDetails>>`
Returns a Promise that resolves with an `IAPQueryResponse` containing [IAPItemDetails](#iapitemdetails) objects in the `results` array.

#### Example

```javascript
// These product IDs must match the item entries you created in the App Store Connect and Google Play Console.
// If you want to add more or edit their attributes you can do so there.

const items = Platform.select({
  ios: [
    'dev.products.gas',
    'dev.products.premium',
    'dev.products.gold_monthly',
    'dev.products.gold_yearly',
  ],
  android: ['gas', 'premium', 'gold_monthly', 'gold_yearly'],
});

 // Retrieve product details
const { responseCode, results } = await getProductsAsync(items);
if (responseCode === IAPResponseCode.OK) {
  this.setState({ items: results });
}
```

### `InAppPurchases.getPurchaseHistoryAsync(options)`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **options** | `IAPPurchaseHistoryOptions` | An optional PurchaseHistoryOptions object. |

Retrieves the user's purchase history.

Please note that on iOS, StoreKit actually creates a new transaction object every time you restore completed transactions, therefore the `purchaseTime` and `orderId` may be inaccurate if it's a restored purchase. If you need the original transaction's information you can use `originalPurchaseTime` and `originalOrderId`, but those will be 0 and an empty string respectively if it is the original transaction.

You should not call this method on launch because restoring purchases on iOS prompts for the user’s App Store credentials, which could interrupt the flow of your app.

You should not call this method on launch because restoring purchases on iOS prompts for the user’s App Store credentials, which could interrupt the flow of your app.

Returns: `Promise<IAPQueryResponse<InAppPurchase>>`
Returns a Promise that fulfills with an IAPQueryResponse that contains an array of InAppPurchase objects.

### `InAppPurchases.purchaseItemAsync(itemId, details)`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **itemId** | `string` | The product ID of the item you want to buy. |
| **details** | `(optional) IAPPurchaseItemOptions` | **Android Only.** Details for billing flow. |

Initiates the purchase flow to buy the item associated with this `productId`. This will display a prompt to the user that will allow them to either buy the item or cancel the purchase. When the purchase completes, the result must be handled in the callback that you passed in to `setPurchaseListener`.

Remember, you have to query an item's details via `getProductsAsync` and set the purchase listener before you attempt to buy an item.

[Apple](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers) and [Google](https://developer.android.com/google/play/billing/subscriptions) both have their own workflows for dealing with subscriptions. In general, you can deal with them in the same way you do one-time purchases but there are caveats including if a user decides to cancel before the expiration date. To check the status of a subscription, you can use the [Google Play Developer API](https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptions/get) on Android and the [Status Update Notifications service](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers/enabling_app_store_server_notifications) on iOS.

Returns: `Promise<void>`
Returns a Promise that resolves when the purchase is done processing. To get the actual result of the purchase, you must handle purchase events inside the setPurchaseListener callback.

### `InAppPurchases.setPurchaseListener(callback)`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **result** | `IAPQueryResponse<InAppPurchase>` | An IAPQueryResponse containing an array of InAppPurchase objects. |

Sets a callback that handles incoming purchases. This must be done before any calls to `purchaseItemAsync` are made, otherwise those transactions will be lost. **You should set the purchase listener globally**, and not inside a specific screen, to ensure that you receive incomplete transactions, subscriptions, and deferred transactions.

Purchases can either be instantiated by the user (via `purchaseItemAsync`) or they can come from subscription renewals or unfinished transactions on iOS (e.g. if your app exits before `finishTransactionAsync` was called).

Note that on iOS, the results array will only contain one item: the one that was just purchased. On Android, it will return both finished and unfinished purchases, hence the array return type. This is because the Google Play Billing API detects purchase updates but doesn't differentiate which item was just purchased, therefore there's no good way to tell but in general it will be whichever purchase has `acknowledged` set to `false`, so those are the ones that you have to handle in the response. Consumed items will not be returned however, so if you consume an item that record will be gone and no longer appear in the results array when a new purchase is made.

Returns: `void`

## Interfaces

### `IAPItemDetails`

Details about the purchasable item that you inputted in App Store Connect and Google Play Console.

| Name | Type | Description | Example |
| ---  | ---  | ----------- | ------- |
| **description** | `string` | User facing description about the item. | Example: Currency used to trade for items in the game |
| **price** | `string` | The price formatted with the local currency symbol. Use this to display the price, not to make calculations. | Example: `$1.99` |
| **priceAmountMicros** | `number` | The price in micro-units, where 1,000,000 micro-units equal one unit of the currency. Use this for calculations. | Example: `1990000` |
| **priceCurrencyCode** | `string` | The local currency code from the ISO 4217 code list. | Example: `USD`, `CAN`, `RUB` |
| **productId** | `string` | The product ID representing an item inputted in App Store Connect and Google Play Console. | Example: `gold` |
| **subscriptionPeriod** | `(optional) string` | The length of a subscription period specified in ISO 8601 format. In-app purchases return `P0D`. On iOS, non-renewable subscriptions also return `P0D`. | Example: `P0D`, `P6W`, `P3M`, `P6M`, `P1Y` |
| **title** | `string` | The title of the purchasable item. This should be displayed to the user and may be different from the `productId`. | Example: `Gold Coin` |
| **type** | `IAPItemType` | The type of the purchase. Note that this is not very accurate on iOS as this data is only available on iOS 11.2 and higher and non-renewable subscriptions always return `IAPItemType.PURCHASE`. |

### `IAPPurchaseItemOptions` (Android Only)

The purchaseItemAsync billing context on Android.

| Name | Type | Description |
| ---  | ---  | ----------- |
| **accountIdentifiers** | `(optional) { obfuscatedAccountId: string, obfuscatedProfileId: string }` | Account identifiers, both need to be provided to work with Google Play Store. |
| **isVrPurchaseFlow** | `(optional) boolean` | Whether the purchase is happening in a VR context. |
| **oldPurchaseToken** | `(optional) string` | The `purchaseToken` of the purchase that the user is upgrading or downgrading from. This is mandatory for replacing an old subscription such as when a user upgrades from a monthly subscription to a yearly one that provides the same content. You can get the purchase token from [getPurchaseHistoryAsync](#inapppurchasesgetpurchasehistoryasyncoptions). |

### `IAPQueryResponse`

The response type for queries and purchases.

| Name | Type | Description |
| ---  | ---  | ----------- |
| **errorCode** | `(optional) IAPErrorCode` | `IAPErrorCode` that provides more detail on why an error occurred. `null` unless `responseCode` is `IAPResponseCode.ERROR`. |
| **responseCode** | `IAPResponseCode` | The response code from a query or purchase. |
| **results** | `(optional) QueryResult[]` | The array containing the `InAppPurchase` or `IAPItemDetails` objects requested depending on the method. |

### `InAppPurchase`

| Name | Type | Description | Platform |
| ---  | ---  | ----------- | -------- |
| **acknowledged** | `boolean` | Boolean indicating whether this item has been "acknowledged" via `finishTransactionAsync`. | Both |
| **orderId** | `string` | A string that uniquely identifies a successful payment transaction. | Both |
| **originalOrderId** | `(optional) string` | Represents the original order ID for restored purchases. | iOS |
| **originalPurchaseTime** | `(optional) string` | Represents the original purchase time for restored purchases. | iOS |
| **packageName** | `(optional) string` | The application package from which the purchase originated. | Android |
| **productId** | `string` | The product ID representing an item inputted in Google Play Console and App Store Connect. | Both |
| **purchaseState** | `InAppPurchaseState` | The state of the purchase. | Both |
| **purchaseTime** | `number` | The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970). | Both |
| **purchaseToken** | `(optional) string` | A token that uniquely identifies a purchase for a given item and user pair. | Android |
| **transactionReceipt** | `(optional) string` | The App Store receipt found in the main bundle encoded as a Base64 String. | iOS |

## Types

### `IAPPurchaseHistoryOptions`

| Name | Type | Description |
| ---  | ---  | ----------- |
| **useGooglePlayCache** | `(optional) boolean` | A boolean that indicates whether or not you want to make a network request to sync expired/consumed purchases and those on other devices. If set to `true`, this method returns purchase details only for the user's currently owned items (active subscriptions and non-consumed one-time purchases). If set to false, it will make a network request and return the most recent purchase made by the user for each product, even if that purchase is expired, canceled, or consumed. The return type if this is false is actually a subset of when it's true. This is because Android returns a PurchaseHistoryRecord which only contains the purchase time, purchase token, and product ID, rather than all of the attributes found in the InAppPurchase type. Default: true |

### `QueryResult`

Acceptable values are: [InAppPurchase](#inapppurchase) | [IAPItemDetails](#iapitemdetails)

## Enums

  ### `IAPErrorCode`

  Abstracts over the Android [Billing Response Codes](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode) and iOS [SKErrorCodes](https://developer.apple.com/documentation/storekit/skerrorcode?language=objc).

  #### UNKNOWN

  `IAPErrorCode.UNKNOWN ＝ 0`
  An unknown or unexpected error occurred. See SKErrorUnknown on iOS, ERROR on Android.

  #### PAYMENT_INVALID

  `IAPErrorCode.PAYMENT_INVALID ＝ 1`
  The feature is not allowed on the current device, or the user is not authorized to make payments. See SKErrorClientInvalid, SKErrorPaymentInvalid, and SKErrorPaymentNotAllowed on iOS, FEATURE_NOT_SUPPORTED on Android.

  #### SERVICE_DISCONNECTED

  `IAPErrorCode.SERVICE_DISCONNECTED ＝ 2`
  Play Store service is not connected now. See SERVICE_DISCONNECTED on Android.

  #### SERVICE_UNAVAILABLE

  `IAPErrorCode.SERVICE_UNAVAILABLE ＝ 3`
  Network connection is down. See SERVICE_UNAVAILABLE on Android.

  #### SERVICE_TIMEOUT

  `IAPErrorCode.SERVICE_TIMEOUT ＝ 4`
  The request has reached the maximum timeout before Google Play responds. See SERVICE_TIMEOUT on Android.

  #### BILLING_UNAVAILABLE

  `IAPErrorCode.BILLING_UNAVAILABLE ＝ 5`
  Billing API version is not supported for the type requested. See BILLING_UNAVAILABLE on Android.

  #### ITEM_UNAVAILABLE

  `IAPErrorCode.ITEM_UNAVAILABLE ＝ 6`
  Requested product is not available for purchase. See SKErrorStoreProductNotAvailable on iOS, ITEM_UNAVAILABLE on Android.

  ### DEVELOPER_ERROR

  `IAPErrorCode.DEVELOPER_ERROR ＝ 7`
  Invalid arguments provided to the API. This error can also indicate that the application was not correctly signed or properly set up for In-app Billing in Google Play. See DEVELOPER_ERROR on Android.

  #### ITEM_ALREADY_OWNED

  `IAPErrorCode.ITEM_ALREADY_OWNED ＝ 8`
  Failure to purchase since item is already owned. See ITEM_ALREADY_OWNED on Android.

  #### ITEM_NOT_OWNED

  `IAPErrorCode.ITEM_NOT_OWNED ＝ 9`
  Failure to consume since item is not owned. See ITEM_NOT_OWNED on Android.

  #### CLOUD_SERVICE

  `IAPErrorCode.CLOUD_SERVICE ＝ 10`
  Apple Cloud Service connection failed or invalid permissions. See SKErrorCloudServicePermissionDenied, SKErrorCloudServiceNetworkConnectionFailed and SKErrorCloudServiceRevoked on iOS.

  #### PRIVACY_UNACKNOWLEDGED

  `IAPErrorCode.PRIVACY_UNACKNOWLEDGED ＝ 11`
  The user has not yet acknowledged Apple’s privacy policy for Apple Music. See SKErrorPrivacyAcknowledgementRequired on iOS.

  #### UNAUTHORIZED_REQUEST

  `IAPErrorCode.UNAUTHORIZED_REQUEST ＝ 12`
  The app is attempting to use a property for which it does not have the required entitlement. See SKErrorUnauthorizedRequestData on iOS.

  #### INVALID_IDENTIFIER

  `IAPErrorCode.INVALID_IDENTIFIER ＝ 13`
  The offer identifier or price specified in App Store Connect is no longer valid. See SKErrorInvalidSignature, SKErrorInvalidOfferPrice, SKErrorInvalidOfferIdentifier on iOS.

  #### MISSING_PARAMS

  `IAPErrorCode.MISSING_PARAMS ＝ 14`
  Parameters are missing in a payment discount. See SKErrorMissingOfferParams on iOS.

### `IAPItemType`

  #### PURCHASE

  `IAPItemType.PURCHASE ＝ 0`
  One time purchase or consumable.

  #### SUBSCRIPTION

  `IAPItemType.SUBSCRIPTION ＝ 1`
  Subscription.

### `IAPResponseCode`

  #### OK

  `IAPResponseCode.OK ＝ 0`
  Response returned successfully.

  #### USER_CANCELED

  `IAPResponseCode.USER_CANCELED ＝ 1`
  User canceled the purchase.

  #### ERROR

  `IAPResponseCode.ERROR ＝ 2`
  An error occurred. Check the errorCode for additional details.

  #### DEFERRED

  `IAPResponseCode.DEFERRED ＝ 3`
  Purchase was deferred.

### `InAppPurchaseState`

  #### PURCHASING

  `InAppPurchaseState.PURCHASING ＝ 0`
  The transaction is being processed.

  #### PURCHASED

  `InAppPurchaseState.PURCHASED ＝ 1`
  The App Store successfully processed payment.

  #### FAILED

  `InAppPurchaseState.FAILED ＝ 2`
  The transaction failed.

  #### RESTORED

  `InAppPurchaseState.RESTORED ＝ 3`
  This transaction restores content previously purchased by the user. Read the originalTransaction properties to obtain information about the original purchase.

  #### DEFERRED

  `InAppPurchaseState.DEFERRED ＝ 4`
  The transaction has been received, but its final status is pending external action such as the Ask to Buy feature where a child initiates a new purchase and has to wait for the family organizer's approval. Update your UI to show the deferred state, and wait for another callback that indicates the final status.
  