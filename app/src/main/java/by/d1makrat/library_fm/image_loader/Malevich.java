package by.d1makrat.library_fm.image_loader;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import by.d1makrat.library_fm.image_loader.cache.BaseDiskCache;
import by.d1makrat.library_fm.image_loader.cache.DiskCache;
import by.d1makrat.library_fm.image_loader.streams.FileStreamProvider;
import by.d1makrat.library_fm.image_loader.streams.HttpStreamProvider;
import by.d1makrat.library_fm.image_loader.util.IOUtils;

import static by.d1makrat.library_fm.Constants.EMPTY_STRING;

public enum Malevich {

    INSTANCE;

    private Config config;
    DiskCache diskCache;

    public void setConfig(Config config) {
        this.config = config;
        if (config.hasDiskCache()) {
            diskCache = new BaseDiskCache(config.cacheDir);
        }
    }

    public void clearCache() throws IOException {
        if (diskCache != null){
            diskCache.clear();
        }
    }

    public static class Config {
        final File cacheDir;

        public Config(File cacheDir) {
            this.cacheDir = cacheDir;
        }

        boolean hasDiskCache() {
            return cacheDir != null;
        }
    }

    private static final int MAX_MEMORY_FOR_IMAGES = 64 * 1024 * 1024;

    private final BlockingDeque<ImageRequest> queue;
    private final LruCache<String, Bitmap> lruCache;
    private final ExecutorService executorService;
    private final Object lock = new Object();

    Malevich() {
        queue = new LinkedBlockingDeque<>();
        executorService = Executors.newFixedThreadPool(3);
        lruCache = new LruCache<String, Bitmap>(getCacheSize()) {

            @Override
            protected int sizeOf(final String key, final Bitmap value) {
                return key.length() + value.getByteCount();
            }

        };
    }

    public ImageRequest.Builder load(String url) {
        return new ImageRequest.Builder(this).load(url);
    }

    private int getCacheSize() {
        return Math.min((int) (Runtime.getRuntime().maxMemory() / 4), MAX_MEMORY_FOR_IMAGES);
    }

    private void dispatchLoading() {
        new ImageResultAsyncTask().executeOnExecutor(executorService);
    }

    private void processImageResult(ImageResult imageResult) {
        if (imageResult != null) {
            ImageRequest request = imageResult.getRequest();
            ImageView imageView = request.target.get();
            if (imageResult.getBitmap() != null){
                if (imageView != null) {
                    Object tag = imageView.getTag();
                    if (tag != null && tag.equals(request.url)) {
                        imageView.setImageBitmap(imageResult.getBitmap());
                        }
                }
            }
            else if (request.onErrorDrawable != null){
                imageView.setImageDrawable(request.onErrorDrawable);
            }
        }
    }

    void enqueue(ImageRequest request) {
        ImageView imageView = request.target.get();

        if (imageView == null) return;

        imageView.setImageDrawable(request.placeholderDrawable);

        if (request.url != null && !request.url.equals(EMPTY_STRING)) {
            if (imageHasSize(request)) {
                imageView.setTag(request.url);
                queue.addFirst(request);
                dispatchLoading();
                } else {
                deferImageRequest(request);
                }
        }
        else if (request.onErrorDrawable != null){
            imageView.setImageDrawable(request.onErrorDrawable);
        }
    }

    private void deferImageRequest(final ImageRequest request) {
        final ImageView imageView = request.target.get();
        if (imageView == null) return;

        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                ImageView view = request.target.get();
                if (view == null) {
                    return true;
                }


                if (view.getWidth() > 0 && view.getHeight() > 0) {
                    request.width = view.getWidth();
                    request.height = view.getHeight();
                    enqueue(request);
                }
                return true;
            }
        });
    }

    private boolean imageHasSize(ImageRequest request) {
        if (request.width > 0 && request.height > 0) {
            return true;
        }

        ImageView imageView = request.target.get();
        if (imageView != null && imageView.getWidth() > 0 && imageView.getHeight() > 0) {
            request.width = imageView.getWidth();
            request.height = imageView.getHeight();
            return true;
        }

        return false;
    }

    private Bitmap getScaledBitmap(InputStream inputStream, int w, int h) throws IOException {

        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(inputStream.available());
            byte[] chunk = new byte[1 << 16];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateSampleSize(options, w, h);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } finally {
            IOUtils.closeStream(inputStream);
        }
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
                halfHeight /= 2;
                halfWidth /= 2;
            }
        }
        return inSampleSize;
    }

    private static final String TAG = "Malevich";

    @SuppressLint("StaticFieldLeak")//TODO reload
    private class ImageResultAsyncTask extends AsyncTask<Void, Void, ImageResult> {

        @Override
        protected ImageResult doInBackground(Void... voids) {

            ImageResult result = null;

            try {

                ImageRequest request = queue.takeFirst();

                result = new ImageResult(request);

                synchronized (lock) {
                    final Bitmap bitmap = lruCache.get(request.url);
                    if (bitmap != null) {
                        result.setBitmap(bitmap);
                        return result;
                    }
                }

                Bitmap bitmap;
                if (config.hasDiskCache()) {
                    try {
                        File file = diskCache.get(request.url);
                        InputStream fileStream = new FileStreamProvider().get(file);
                        bitmap = getScaledBitmap(fileStream, request.width, request.height);
                        if (bitmap != null) {
                            result.setBitmap(bitmap);
                            return result;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }

                InputStream inputStream = new HttpStreamProvider().get(request.url);

                bitmap = getScaledBitmap(inputStream, request.height, request.width);

                if (bitmap != null) {
                    result.setBitmap(bitmap);
                    cacheBitmap(request, bitmap);
                } else
                    throw new IllegalStateException("Bitmap is null");

                return result;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                if (result != null) {
                    result.setException(e);
                }
                return result;
            }
        }

        @Override
        protected void onPostExecute(ImageResult imageResult) {
            processImageResult(imageResult);
        }

    }

    private void cacheBitmap(ImageRequest request, Bitmap bitmap) {
        synchronized (lock) {
            lruCache.put(request.url, bitmap);
        }

        try {
            if (config.hasDiskCache()) {
                diskCache.save(request.url, bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }
}
