package net.osmand.plus.wikivoyage;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikipedia.WikiWebController;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;


interface RegionCallback {
	void onRegionFound(String s);
}
/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient implements RegionCallback {

	private static final String TAG = WikivoyageWebViewClient.class.getSimpleName();

	private OsmandApplication app;
	private FragmentManager fragmentManager;
	private Context context;
	private TravelArticle article;
	private boolean nightMode;
	private String regionName;

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";
	private FetchWikiRegion fetchRegionTask;
	private WikiWebController wikiWebController;


	public WikivoyageWebViewClient(FragmentActivity context, FragmentManager fm, boolean nightMode) {
		app = (OsmandApplication) context.getApplication();
		fragmentManager = fm;
		this.context = context;
		this.nightMode = nightMode;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		boolean isWebPage = url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS);
		wikiWebController = new WikiWebController();
		if (url.contains(WIKIVOAYAGE_DOMAIN) && isWebPage) {
			String lang = getLang(url);
			String articleName = getArticleNameFromUrl(url, lang);
			long articleId = app.getTravelDbHelper().getArticleId(articleName, lang);
			if (articleId != 0) {
				WikivoyageArticleDialogFragment.showInstance(app, fragmentManager, articleId, lang);
			} else {
				WikiWebController.warnAboutExternalLoad(url, context, nightMode);
			}
			return true;
		} else if (url.contains(WIKI_DOMAIN) && isWebPage) {
			wikiWebController.searchAndShowWikiArticle(article, url, (MapActivity) context, nightMode, regionName);
		} else if (isWebPage) {
			WikiWebController.warnAboutExternalLoad(url, context, nightMode);
		} else if (url.startsWith(PREFIX_GEO)) {
			if (article != null) {
				List<GPXUtilities.WptPt> points = article.getGpxFile().getPoints();
				GPXUtilities.WptPt gpxPoint = null;
				String coordinates = url.replace(PREFIX_GEO, "");
				double lat;
				double lon;
				try {
					lat = Double.valueOf(coordinates.substring(0, coordinates.indexOf(",")));
					lon = Double.valueOf(coordinates.substring(coordinates.indexOf(",") + 1,
							coordinates.length()));
				} catch (NumberFormatException e) {
					Log.w(TAG, e.getMessage(), e);
					return true;
				}
				for (GPXUtilities.WptPt point : points) {
					if (point.getLatitude() == lat && point.getLongitude() == lon) {
						gpxPoint = point;
						break;
					}
				}
				if (gpxPoint != null) {
					final OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(lat, lon,	settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxPoint.name),
							false,
							gpxPoint);
					fragmentManager.popBackStackImmediate(WikivoyageExploreDialogFragment.TAG,
							POP_BACK_STACK_INCLUSIVE);

					File path = app.getTravelDbHelper().createGpxFile(article);
					GPXUtilities.GPXFile gpxFile = article.getGpxFile();
					gpxFile.path = path.getAbsolutePath();
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
					MapActivity.launchMapActivityMoveToTop(context);
				}
			}
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(i);
		}
		return true;
	}

	@NonNull
	private String getLang(String url) {
		return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
	}

	private String getArticleNameFromUrl(String url, String lang) {
		String domain = url.contains(WIKIVOAYAGE_DOMAIN) ? WIKIVOAYAGE_DOMAIN : WIKI_DOMAIN;
		String articleName = url.replace(PAGE_PREFIX_HTTPS + lang + domain, "")
				.replaceAll("_", " ");
		try {
			articleName = URLDecoder.decode(articleName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return articleName;
	}

	public void setArticle(TravelArticle article) {
		this.article = article;
		if (this.article != null && app != null) {
			fetchRegionTask = new FetchWikiRegion(this, app.getRegions(), article.getLat(), article.getLon());
			fetchRegionTask.execute();
		}
	}

	@Override
	public void onRegionFound(String s) {
		regionName = s;
	}

	public void stopRunningAsyncTasks() {
		wikiWebController.stopRunningSearchAsyncTask();
		if (fetchRegionTask != null && fetchRegionTask.getStatus() == AsyncTask.Status.RUNNING) {
			fetchRegionTask.cancel(false);
		}
	}

	private static class FetchWikiRegion extends AsyncTask<Void, Void, String> {

		private RegionCallback callback;
		private OsmandRegions osmandRegions;
		private double lat;
		private double lon;

		FetchWikiRegion(RegionCallback callback, OsmandRegions osmandRegions, double lat, double lon) {
			this.callback = callback;
			this.osmandRegions = osmandRegions;
			this.lat = lat;
			this.lon = lon;
		}

		@Override
		protected String doInBackground(Void... voids) {
			if (osmandRegions != null) {
				int x31 = MapUtils.get31TileNumberX(lon);
				int y31 = MapUtils.get31TileNumberY(lat);
				List<BinaryMapDataObject> dataObjects = null;
				try {
					if (isCancelled()) {
						return null;
					}
					dataObjects = osmandRegions.query(x31, y31);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (dataObjects != null) {
					for (BinaryMapDataObject b : dataObjects) {
						if (isCancelled()) {
							break;
						}
						if(osmandRegions.contain(b, x31, y31)) {
							String downloadName = osmandRegions.getDownloadName(b);
							if (downloadName == null) {
								return "";
							}
							return osmandRegions.getLocaleName(downloadName, false);
						}
					}
				}
			}
			return "";
		}

		@Override
		protected void onCancelled(){
			callback = null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (callback != null) {
				callback.onRegionFound(result);
			}
		}
	}
}
