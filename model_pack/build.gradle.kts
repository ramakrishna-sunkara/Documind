plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        // fast-follow: Model downloads automatically after app install
        // This keeps APK size small (~100MB) while model (~529MB) downloads separately
        deliveryType.set("fast-follow")
    }
}
