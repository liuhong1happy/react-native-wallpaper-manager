package com.cunyutech.hollyliu.reactnative.wallpaper;

import android.graphics.RectF;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.util.Base64;
import android.util.SparseArray;
import android.view.View;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.Map;

public class WallPaperModule extends ReactContextBaseJavaModule {

    private WallpaperManager wallpaperManager;
    private Callback rctCallback = null;
    private ReadableMap rctParams;
    private ResourceDrawableIdHelper mResourceDrawableIdHelper;
    private boolean mInitialized = false;

    public WallPaperModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wallpaperManager = WallpaperManager.getInstance(getReactApplicationContext());
    }
    @Override
    public String getName() {
        return "WallPaperManager";
    }

    @ReactMethod
    public void setWallpaper(final ReadableMap params, Callback callback){

        String source = params.hasKey("uri") ? params.getString("uri") : null;
        ReadableMap headers = params.hasKey("headers") ? params.getMap("headers") : null;

        if(rctCallback==null){
            WritableMap map = Arguments.createMap();
            map.putInt("status", 'error');
            map.putString("msg", 'busy');
            map.putString("url",source);
            rctCallback.invoke(response);
        }

        rctCallback = callback;
        rctParams = params;



        RequestListener listener = this.getRequestListener();

        //handle base64
        if (source.startsWith("data:image/png;base64,")){
            Glide
                    .with(this.getContext())
                    .load(Base64.decode(source.replaceAll("data:image\\/.*;base64,", ""), Base64.DEFAULT))
                    .into(listener)
            ;
            return;
        }

        boolean useStorageFile = false ;

        // handle bundled app resources
        try {
            mUri = Uri.parse(source);
            // Verify scheme is set, so that relative uri (used by static resources) are not handled.
            if (mUri.getScheme() == null) {
                mUri = null;
            } else if(
                    !mUri.getScheme().equals("http") &&
                            !mUri.getScheme().equals("https")
                    ){
                useStorageFile = true ;

                if (!mInitialized) {
                    this.setImageURI(mUri);
                }
            }
        } catch (Exception e) {
            // ignore malformed uri, then attempt to extract resource ID.
        }


        if (mUri == null) {
            mUri = mResourceDrawableIdHelper.getResourceDrawableUri(
                    this.getContext(),
                    source
            );
            Glide
                    .with(this.getContext())
                    .load(mUri)
                    .into(listener);
        } else if (useStorageFile) {
            Glide
                    .with(this.getContext())
                    .load(mUri)
                    .into(listener);
        } else {
            // Handle an http / https address

            LazyHeaders.Builder lazyHeaders = new LazyHeaders.Builder();
            Log.d("null headers", String.valueOf(headers != null));
            if(headers != null){
                ReadableMapKeySetIterator it = headers.keySetIterator();
                Log.d("next headers", String.valueOf(it.hasNextKey()));
                while(it.hasNextKey()){
                    String Key = it.nextKey();
                    lazyHeaders.addHeader(Key, headers.getString(Key));
                }
            }

            Log.d("thing", mUri.toString());
            Glide
                    .with(this.getContext())
                    .load(new GlideUrl(mUri.toString(), lazyHeaders.build()))
                    .listener(listener);;
        }
    }

    private RequestListener getRequestListener() {

        return new RequestListener<GlideUrl, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, GlideUrl model,
                                       Target<GlideDrawable> target,
                                       boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable drawable, GlideUrl model,
                                           Target<GlideDrawable> target,
                                           boolean isFromMemoryCache,
                                           boolean isFirstResource) {

                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                drawable.draw(canvas);

                try
                {
                    manager.setBitmap(bitmap);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                WritableMap map = Arguments.createMap();
                map.putInt("status", 'success');
                map.putString("url", model);
                map.putBoolean("isFromMemoryCache", isFromMemoryCache);
                map.putBoolean("isFirstResource", isFirstResource);

                rctCallback.invoke(response);
                rctCallback = null;
                return false;
            }
        };
    }

}