package net.osmand.plus.wikipedia;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.osmand.data.Amenity;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class WikiWebController {

	private static final String TAG = WikiWebController.class.getSimpleName();

	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";

	public WikiArticleSearchTask getArticleSearchTask() {
		return articleSearchTask;
	}

	private WikiArticleSearchTask articleSearchTask;

	public static String getLangFromWebLink(String url) {
		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTP) ? PAGE_PREFIX_HTTP.length() : 0, url.indexOf("."));
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
		}
		return "";
	}

	public static String getArticleNameFromUrl(String url, String lang) {
		String domain = url.contains(WIKIVOAYAGE_DOMAIN) ? WIKIVOAYAGE_DOMAIN : WIKI_DOMAIN;
		String articleName = "";

		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			articleName = url.replace(PAGE_PREFIX_HTTP + lang + domain, "")
					.replaceAll("_", " ");
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			articleName = url.replace(PAGE_PREFIX_HTTPS + lang + domain, "")
					.replaceAll("_", " ");
		}

		try {
			articleName = URLDecoder.decode(articleName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return articleName;
	}

	public void searchAndShowWikiArticle(TravelArticle article,
	                                     String url,
	                                     MapActivity mapActivity,
	                                     boolean nightMode,
	                                     String regionName) {
		List<AmenityIndexRepositoryBinary> indexes = mapActivity.getMyApplication().getResourceManager()
				.getWikiAmenityRepository(article.getLat(), article.getLon());
		if (indexes.isEmpty()) {
			if (Version.isPaidVersion(mapActivity.getMyApplication())) {
				WikivoyageArticleWikiLinkFragment.showInstance(mapActivity.getSupportFragmentManager(), regionName == null ?
						"" : regionName, url);
			} else {
				WikipediaArticleWikiLinkFragment.showInstance(mapActivity.getSupportFragmentManager(), regionName == null ?
						"" : regionName, url);
			}
		} else {
			String lang = getLangFromWebLink(url);
			String name = getArticleNameFromUrl(url, lang);
			WikiArticleSearchTask articleSearchTask = new WikiArticleSearchTask(name, lang, indexes, mapActivity, nightMode, url);
			articleSearchTask.execute();
		}
	}

	public void searchAndShowWikiArticle(Amenity article,
	                                     String url,
	                                     MapActivity mapActivity,
	                                     boolean nightMode,
	                                     String regionName) {

		List<AmenityIndexRepositoryBinary> indexes = mapActivity.getMyApplication().getResourceManager()
				.getWikiAmenityRepository(article.getLocation().getLatitude(), article.getLocation().getLongitude());
		if (indexes.isEmpty()) {
			if (Version.isPaidVersion(mapActivity.getMyApplication())) {
				WikivoyageArticleWikiLinkFragment.showInstance(mapActivity.getSupportFragmentManager(), regionName == null ?
						"" : regionName, url);
			} else {
				WikipediaArticleWikiLinkFragment.showInstance(mapActivity.getSupportFragmentManager(), regionName == null ?
						"" : regionName, url);
			}
		} else {
			String lang = getLangFromWebLink(url);
			String name = getArticleNameFromUrl(url, lang);
			WikiArticleSearchTask articleSearchTask = new WikiArticleSearchTask(name, lang, indexes, mapActivity, nightMode, url);
			articleSearchTask.execute();
		}
	}

	public static class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {
		private ProgressDialog dialog;
		private String name;
		private List<AmenityIndexRepositoryBinary> indexes;
		private WeakReference<MapActivity> weakContext;
		private boolean isNightMode;
		private String url;
		private String lang;

		WikiArticleSearchTask(String articleName, String lang, List<AmenityIndexRepositoryBinary> indexes,
		                      MapActivity context, boolean isNightMode, String url) {
			name = articleName;
			this.lang = lang;
			this.indexes = indexes;
			weakContext = new WeakReference<>(context);
			dialog = createProgressDialog();
			this.isNightMode = isNightMode;
			this.url = url;
		}

		@Override
		protected void onPreExecute() {
			if (dialog != null) {
				dialog.show();
			}
		}

		@Override
		protected List<Amenity> doInBackground(Void... voids) {
			List<Amenity> found = new ArrayList<>();
			for (AmenityIndexRepositoryBinary repo : indexes) {
				if (isCancelled()) {
					break;
				}
				found.addAll(repo.searchAmenitiesByName(0, 0, 0, 0,
						Integer.MAX_VALUE, Integer.MAX_VALUE, name, null));
			}
			return found;
		}

		@Override
		protected void onCancelled() {
			dialog = null;
			indexes.clear();
		}

		@Override
		protected void onPostExecute(List<Amenity> found) {
			MapActivity activity = weakContext.get();
			if (activity != null && !activity.isActivityDestroyed() && dialog != null) {
				dialog.dismiss();
				if (!found.isEmpty()) {
					WikipediaDialogFragment.showInstance(activity, found.get(0), lang);
				} else {
					warnAboutExternalLoad(url, weakContext.get(), isNightMode);
				}
			}
		}

		private ProgressDialog createProgressDialog() {
			MapActivity activity = weakContext.get();
			if (activity != null && !activity.isActivityDestroyed()) {
				ProgressDialog dialog = new ProgressDialog(activity);
				dialog.setCancelable(false);
				dialog.setMessage(activity.getString(R.string.wiki_article_search_text));
				return dialog;
			}
			return null;
		}
	}

	public static void warnAboutExternalLoad(final String url, final Context context, final boolean nightMode) {
		if (context == null) {
			return;
		}
		new AlertDialog.Builder(context)
				.setTitle(url)
				.setMessage(R.string.online_webpage_warning)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						WikipediaDialogFragment.showFullArticle(context, Uri.parse(url), nightMode);
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	public void stopRunningSearchAsyncTask() {
		if (articleSearchTask != null && articleSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			articleSearchTask.cancel(false);
		}
	}
}
