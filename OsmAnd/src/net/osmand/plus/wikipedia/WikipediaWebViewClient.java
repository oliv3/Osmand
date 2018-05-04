package net.osmand.plus.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WikipediaWebViewClient extends WebViewClient {

	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";

	private Context context;
	private boolean nightMode;

	public WikipediaWebViewClient(FragmentActivity context, boolean nightMode) {
		this.context = context;
		this.nightMode = nightMode;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS)) {
			WikiWebController.warnAboutExternalLoad(url, context, nightMode);
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(i);
		}
		return true;
	}
}
