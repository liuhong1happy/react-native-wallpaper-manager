 public class MyGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        MemorySizeCalculator calculator = new MemorySizeCalculator(context);
        int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
        int defaultBitmapPoolSize = calculator.getBitmapPoolSize();
        int defaultDiskCacheSize = calculator.getDiskCacheSize();
        
        int customMemoryCacheSize = (int) (10 * defaultMemoryCacheSize);
        int customBitmapPoolSize = (int) (10 * defaultBitmapPoolSize);
        int customDiskCacheSize = (int) (10 * defaultDiskCacheSize);

        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        builder.setMemoryCache(new LruResourceCache(customMemoryCacheSize));
        builder.setBitmapPool(new LruBitmapPool(customBitmapPoolSize));
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, customDiskCacheSize));
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(Model.class, Data.class, new MyModelLoaderFactory());
    }
}