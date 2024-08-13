## Configure for iOS

To use the In-App Purchases API on iOS, you'll need to sign the [Paid Applications Agreement](https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-workflow) and set up your banking and tax information. You also need to enable the [In-App Purchases capability](https://help.apple.com/xcode/mac/current/#/dev88ff319e7) for your app in Xcode.

Next, create an entry for your app in [App Store Connect](https://appstoreconnect.apple.com/login) and configure your in-app purchases, including details (such as name, pricing, and description) that highlight the features and functionality of your in-app products. Make sure each product's status says `Ready to Submit`. Otherwise, it will not be queryable from within your app when you are testing. Be sure to add any necessary metadata to do so including uploading a screenshot (this can be anything when you're testing) and review notes. Your app's status must also say `Ready to Submit` but you do not need to submit your app or its products for review to test purchases in sandbox mode.

Now you can create a [sandbox account](https://developer.apple.com/help/app-store-connect/test-in-app-purchases/create-sandbox-apple-ids/) to test in-app purchases before you make your app available.

For more information, see [Apple's workflow for configuring In-App Purchases](https://developer.apple.com/help/app-store-connect/configure-in-app-purchase-settings/overview-for-configuring-in-app-purchases).