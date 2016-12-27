package com.cunyutech.hollyliu.reactnative.wallpaper;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;


 public class MyGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        MemorySizeCalculator calculator = new MemorySizeCalculator(context);
        int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
        int defaultBitmapPoolSize = calculator.getBitmapPoolSize();
        int defaultDiskCacheSize = calculator.getMemoryCacheSize();
        
        int customMemoryCacheSize = (int) (10 * defaultMemoryCacheSize);
        int customBitmapPoolSize = (int) (10 * defaultBitmapPoolSize);
        int customDiskCacheSize = (int) (100 * defaultDiskCacheSize);

        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        builder.setMemoryCache(new LruResourceCache(customMemoryCacheSize));
        builder.setBitmapPool(new LruBitmapPool(customBitmapPoolSize));
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, customDiskCacheSize));
    }
     @Override
     public void registerComponents(Context context,Glide glide){

     }
}