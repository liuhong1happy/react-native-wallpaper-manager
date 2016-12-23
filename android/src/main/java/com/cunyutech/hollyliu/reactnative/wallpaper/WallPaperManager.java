package com.cunyutech.hollyliu.reactnative.wallpaper;

import android.net.Uri;
import android.util.Log;
import android.util.Base64;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.BitmapFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.SimpleTarget;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper;

import java.util.Map;

public class WallPaperManager extends ReactContextBaseJavaModule {

    private WallpaperManager wallpaperManager;
    private Callback rctCallback = null;
    private ReadableMap rctParams;
    private ResourceDrawableIdHelper mResourceDrawableIdHelper;
    private Uri mUri;
    private ImageView imgView;
    private ReactApplicationContext mApplicationContext;
    private Activity mCurrentActivity;

    public WallPaperManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mApplicationContext = getReactApplicationContext();

        wallpaperManager = WallpaperManager.getInstance(mApplicationContext);
        imgView = new ImageView(mApplicationContext);
    }
    @Override
    public String getName() {
        return "WallPaperManager";
    }

    @ReactMethod
    public void setWallpaper(final ReadableMap params, Callback callback){

        final String source = params.hasKey("uri") ? params.getString("uri") : null;
        ReadableMap headers = params.hasKey("headers") ? params.getMap("headers") : null;

        if(rctCallback!=null){
            WritableMap map = Arguments.createMap();

            map.putString("status", "error");
            map.putString("msg", "busy");
            map.putString("url",source);
            callback.invoke(map);
        }

        rctCallback = callback;
        rctParams = params;

        final RequestListener listener = this.getRequestListener();

        //handle base64
        if (source.startsWith("data:image/png;base64,")){
            Glide
                    .with(this.getReactApplicationContext())
                    .load(Base64.decode(source.replaceAll("data:image\\/.*;base64,", ""), Base64.DEFAULT))
                    .listener(listener)
                    .into(imgView)
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
                useStorageFile = true;
            }
        } catch (Exception e) {
            // ignore malformed uri, then attempt to extract resource ID.
        }


        if (mUri == null) {
            mUri = mResourceDrawableIdHelper.getResourceDrawableUri(
                    this.getReactApplicationContext(),
                    source
            );
            Glide
                    .with(this.getReactApplicationContext())
                    .load(mUri)
                    .listener(listener)
                    .into(imgView);
        } else if (useStorageFile) {
            Glide
                    .with(this.getReactApplicationContext())
                    .load(mUri)
                    .listener(listener)
                    .into(imgView);
        } else {
            // Handle an http / https address
            final LazyHeaders.Builder lazyHeaders = new LazyHeaders.Builder();
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
            mCurrentActivity = getCurrentActivity();
            if(mCurrentActivity==null){
                WritableMap map = Arguments.createMap();
                map.putString("status", "error");
                map.putString("msg", "CurrentActivity");
                map.putString("url",source);
                rctCallback.invoke(map);
            }
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ThreadUtil.assertMainThread();
                    try{
                        Glide
                                .with(mApplicationContext)
                                .load(new GlideUrl(mUri.toString(), lazyHeaders.build()))
                                .asBitmap()
                                .toBytes()
                                .centerCrop()
                                .into(new SimpleTarget<byte[]>(1080, 1920) {
                                    @Override
                                    public void onResourceReady(byte[] resource, GlideAnimation<? super byte[]> glideAnimation) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(resource, 0, resource.length);
                                        try
                                        {
                                            wallpaperManager.setBitmap(bitmap);
                                        }
                                        catch (Exception e)
                                        {
                                            WritableMap map = Arguments.createMap();
                                            map.putString("status", "error");
                                            map.putString("url", source);
                                            map.putBoolean("isFromMemoryCache", false);
                                            map.putBoolean("isFirstResource", true);

                                            rctCallback.invoke(map);
                                            return;
                                        }

                                        WritableMap map = Arguments.createMap();
                                        map.putString("status", "success");
                                        map.putString("url", source);
                                        map.putBoolean("isFromMemoryCache", false);
                                        map.putBoolean("isFirstResource", true);

                                        rctCallback.invoke(map);
                                    }
                                });
                    }catch (Exception e) {
                        WritableMap map = Arguments.createMap();
                        map.putString("status", "error");
                        map.putString("url",source);
                        map.putString("msg", "onException");
                        rctCallback.invoke(map);
                    }
                }
            });
        }
    }

    private RequestListener getRequestListener() {

        return new RequestListener<GlideUrl, GlideDrawable>() {

            @Override
            public boolean onException(Exception e, GlideUrl model,
                                       Target<GlideDrawable> target,
                                       boolean isFirstResource) {
                WritableMap map = Arguments.createMap();
                map.putString("status", "error");
                map.putString("url", model.toString());
                map.putBoolean("isFirstResource", isFirstResource);
                map.putString("msg", "onException");
                rctCallback.invoke(map);
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
                    wallpaperManager.setBitmap(bitmap);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                WritableMap map = Arguments.createMap();
                map.putString("status", "success");
                map.putString("url", model.toString());
                map.putBoolean("isFromMemoryCache", isFromMemoryCache);
                map.putBoolean("isFirstResource", isFirstResource);

                rctCallback.invoke(map);
                rctCallback = null;
                return false;
            }
        };
    }
};
