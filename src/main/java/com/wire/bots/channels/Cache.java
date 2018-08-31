package com.wire.bots.channels;

import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.tools.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

class Cache {
    private static final ConcurrentHashMap<String, Picture> pictures = new ConcurrentHashMap<>();//<Url, Picture>

    @Nullable
    static Picture getPicture(WireClient client, String url) {

        return pictures.computeIfAbsent(url, k -> {
            try {
                return upload(client, k);
            } catch (Exception e) {
                Logger.warning("getPicture: url: %s, error: %s", url, e.getMessage());
                return null;
            }
        });
    }

    private static Picture upload(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);
        preview.setRetention("eternal");

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }
}
