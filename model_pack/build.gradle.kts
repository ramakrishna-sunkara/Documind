plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        // fast-follow: Model downloads automatically after app install
        deliveryType.set("fast-follow")
    }
}
