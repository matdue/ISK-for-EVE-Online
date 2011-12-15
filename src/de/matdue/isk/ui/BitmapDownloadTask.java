package de.matdue.isk.ui;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapDownloadTask extends AsyncTask<String, Void, Bitmap> {

	private WeakReference<ImageView> imageViewReference;

	private CacheManager cacheManager;
	private BitmapDownloadManager bitmapDownloadManager;
	private Map<String, Bitmap> memoryCache;

	public BitmapDownloadTask(ImageView imageView, CacheManager cacheManager,
			BitmapDownloadManager bitmapDownloadManager, Map<String, Bitmap> memoryCache) {
		imageViewReference = new WeakReference<ImageView>(imageView);
		this.cacheManager = cacheManager;
		this.bitmapDownloadManager = bitmapDownloadManager;
		this.memoryCache = memoryCache;

		DownloadingDrawable downloadingDrawable = new DownloadingDrawable(this);
		imageView.setImageDrawable(downloadingDrawable);
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		InputStream cachedImage = cacheManager.getStream(params[0]);
		if (cachedImage != null) {
			Bitmap bitmap = BitmapFactory.decodeStream(cachedImage);
			if (memoryCache != null) {
				memoryCache.put(params[0], bitmap);
			}
			try {
				cachedImage.close();
			} catch (IOException e) {
				// Ignore errors
			}
			return bitmap;
		}
		
		Bitmap bitmap = bitmapDownloadManager.downloadBitmap(params[0]);
		if (memoryCache != null) {
			memoryCache.put(params[0], bitmap);
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (isCancelled()) {
			result = null;
		}

		if (imageViewReference != null) {
			ImageView imageView = imageViewReference.get();
			if (imageView != null) {
				Drawable current = imageView.getDrawable();
				if (current instanceof DownloadingDrawable) {
					DownloadingDrawable currentDownloadingDrawable = (DownloadingDrawable) current;
					if (currentDownloadingDrawable.getDownloadingTask() == this) {
						imageView.setImageBitmap(result);
					}
				}
			}
		}
	}

	static class DownloadingDrawable extends ColorDrawable {

		private final WeakReference<BitmapDownloadTask> bitmapDownloadTaskReference;

		public DownloadingDrawable(BitmapDownloadTask downloadingTask) {
			super(Color.BLACK);
			bitmapDownloadTaskReference = new WeakReference<BitmapDownloadTask>(
					downloadingTask);
		}

		public BitmapDownloadTask getDownloadingTask() {
			return bitmapDownloadTaskReference.get();
		}
	}

}
