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

    private final ReactApplicationContext reactContext;
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
        this.reactContext = reactContext;
        mApplicationContext = getReactApplicationContext();

        wallpaperManager = WallpaperManager.getInstance(mApplicationContext);
        imgView = new ImageView(mApplicationContext);
    }
    @Override
    public String getName() {
        return "WallPaperManager";
    }

    public void sendMessage(String status, String msg, String url){
        if(rctCallback!=null){
            WritableMap map = Arguments.createMap();
            map.putString("status", status);
            map.putString("msg", msg);
            map.putString("url", url);
            rctCallback.invoke(map);
            rctCallback = null;
        }
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
            return;
        }

        rctCallback = callback;
        rctParams = params;

        final SimpleTarget<byte[]> simpleTarget = this.getSimpleTarget(source);
        mCurrentActivity = getCurrentActivity();
        if(mCurrentActivity==null){
            sendMessage("error","CurrentActivity is null",source);
        }
        // final RequestListener listener = this.getRequestListener();

        //handle base64
        if ("data:image/png;base64,".startsWith(source)){
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ThreadUtil.assertMainThread();
                    try{
                        Glide
                            .with(mApplicationContext)
                            .load(Base64.decode(source.replaceAll("data:image\\/.*;base64,", ""), Base64.DEFAULT))
                            .asBitmap()
                            .toBytes()
                            .centerCrop()
                            .into(simpleTarget);
                    }catch (Exception e) {
                        sendMessage("error","Exception in Glide",source);
                    }
                }
            });

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
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ThreadUtil.assertMainThread();
                    try{
                        Glide
                            .with(mApplicationContext)
                            .load(mUri)
                            .asBitmap()
                            .toBytes()
                            .centerCrop()
                            .into(simpleTarget);
                    }catch (Exception e) {
                        sendMessage("error","Exception in Glide",source);
                    }
                }
            });
        } else if (useStorageFile) {
            mCurrentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ThreadUtil.assertMainThread();
                    try{
                        Glide
                            .with(mApplicationContext)
                            .load(mUri)
                            .asBitmap()
                            .toBytes()
                            .centerCrop()
                            .into(simpleTarget);
                    }catch (Exception e) {
                        sendMessage("error","Exception in Glide",source);
                    }
                }
            });
        } else {
            // Handle an http / https address
            final LazyHeaders.Builder lazyHeaders = new LazyHeaders.Builder();

            if(headers != null){
                ReadableMapKeySetIterator it = headers.keySetIterator();
                Log.d("next headers", String.valueOf(it.hasNextKey()));
                while(it.hasNextKey()){
                    String Key = it.nextKey();
                    lazyHeaders.addHeader(Key, headers.getString(Key));
                }
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
                            .into(new SimpleTarget<byte[]>(1080, 1920){
                                @Override
                                public void onResourceReady(byte[] resource, GlideAnimation<? super byte[]> glideAnimation) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(resource, 0, resource.length);
                                    try
                                    {
                                        wallpaperManager.setBitmap(bitmap);
                                    }
                                    catch (Exception e)
                                    {
                                        sendMessage("error","Exception in SimpleTarget",source);
                                        return;
                                    }
                                    sendMessage("success","Set Wallpaper Success",source);
                                }
                                @Override
                                public void onStart(){
                                    sendMessage("success","Set Wallpaper Start",source);
                                }
                            });
                    }catch (Exception e) {
                        sendMessage("error","Exception in Glide",source);
                    }
                }
            });
        }
    }

    private SimpleTarget<byte[]> getSimpleTarget(final String source){
        return new SimpleTarget<byte[]>(1080, 1920){
            @Override
            public void onResourceReady(byte[] resource, GlideAnimation<? super byte[]> glideAnimation) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(resource, 0, resource.length);
                try
                {
                    wallpaperManager.setBitmap(bitmap);
                }
                catch (Exception e)
                {
                    sendMessage("error","Exception in SimpleTarget",source);
                    return;
                }
                sendMessage("success","Set Wallpaper Success",source);
            }
        };
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
