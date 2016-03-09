package com.kevinhinds.riseandset.widget;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.kevinhinds.riseandset.APIs;
import com.kevinhinds.riseandset.Localized;
import com.kevinhinds.riseandset.MainActivity;
import com.kevinhinds.riseandset.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

/**
 * create the basic MoonWidget widget that will use a service call in a separate thread to update
 * the widget elements
 * 
 * @author khinds
 */
public class MoonWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		// to avoid the ANR "application not responding" error request update for these widgets and
		// launch updater service via a new thread
		appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, MoonWidget.class));
		UpdateService.requestUpdate(appWidgetIds);
		context.startService(new Intent(context, UpdateService.class));
	}

	/**
	 * the UpdateService thread is now ready to update the elements on the widget
	 * 
	 * @param context
	 * @param appWidgetUri
	 * @return
	 */
	public static RemoteViews buildUpdate(Context context, Uri appWidgetUri) {

		// get current date time / location and update the UI with it
		String rightNowDate = (DateFormat.format("MM-dd-yyyy", new java.util.Date()).toString());
		ArrayList<String> astroData = null;

		// determine if there is a valid internet connection to process the widget update
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		boolean isConnected = false;
		if (networkInfo != null && networkInfo.isConnected()) {
			isConnected = true;
		}

		// randomized URL string value
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(1000000);
		String timeStamp = Integer.toString(randomInt);

		// update the widget UI elments based on on the current situation
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.moon_widget);

		// update widget based on how the internet connectivity is
		if (!isConnected) {
			views.setTextViewText(R.id.moonWidgetLastUpdate, "No Connection");
		} else {
			// get the moon live image from the online location
			InputStream is = null;
			try {
				is = fetch("http://space.kevinhinds.net/moon.jpg?" + timeStamp);
			} catch (Exception e) {
			}

			// create a resized image bitmap to show on the widget
			Bitmap bm = BitmapFactory.decodeStream(is);
			Bitmap resizedbitmap = null;
			int widgetImageWidthToPixel = (int) MainActivity.convertDpToPixel(200, context);
			int widgetImageHeightToPixel = (int) MainActivity.convertDpToPixel(200, context);
			resizedbitmap = Bitmap.createScaledBitmap(bm, widgetImageWidthToPixel, widgetImageHeightToPixel, true);
			views.setImageViewBitmap(R.id.moonViewWidget, resizedbitmap);
			views.setTextViewText(R.id.moonWidgetLastUpdate, "LIVE MOON [" + Localized.getCurrentDateTime(context) + "]");

			// set moon current data from location found
			LocationManager locationManager = (LocationManager) context.getSystemService(MainActivity.LOCATION_SERVICE);
			Location currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			astroData = APIs.parseUSNODataByURL(APIs.getUSNOURLByLocation(currentLocation));
			views.setTextViewText(R.id.moonRiseTime, Localized.localizedTime(astroData.get(5), context, rightNowDate));
			views.setTextViewText(R.id.moonTransitTextTime, Localized.localizedTime(astroData.get(6), context, rightNowDate));
			views.setTextViewText(R.id.moonsetTextTime, Localized.localizedTime(astroData.get(7), context, rightNowDate));

			String moonDescription = "";
			try {
				if (astroData.get(8) != null && !astroData.get(8).contains("<a href=")) {
					moonDescription = astroData.get(8);
				}
				if (astroData.get(9) != null && !astroData.get(9).contains("<a href=")) {
					moonDescription = astroData.get(9);
				}
				if (astroData.get(8) != null && !astroData.get(8).contains("<a href=") && astroData.get(9) != null && !astroData.get(9).contains("<a href=")) {
					moonDescription = astroData.get(8) + ". " + astroData.get(9);
				}
				views.setTextViewText(R.id.moonDescription, moonDescription);
			} catch (Exception e) {
			}
		}

		// apply an intent to the widget as a whole to launch the MainActivity
		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.moonWidgetContainer, pendingIntent);
		return views;
	}

	/**
	 * get a URL resource and return contents for further processing
	 * 
	 * @param urlString
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private static InputStream fetch(String urlString) throws MalformedURLException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}