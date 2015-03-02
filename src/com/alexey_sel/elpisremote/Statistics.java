package com.alexey_sel.elpisremote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class Statistics {
	static boolean OFF = true;
	public static boolean sendAnonymousStatistics(Context c) {
		if (OFF)
			return true;
		String id = UUID.randomUUID().toString();
		String androidVersion = android.os.Build.VERSION.RELEASE;
		Long tsLong = System.currentTimeMillis() / 1000;
		String dateTime = tsLong.toString();
		String remoteControlVersion = "";
		try {
			remoteControlVersion = c.getPackageManager().getPackageInfo(
					c.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		final String finalData = dateTime + ";" + androidVersion + ";" + id + ";"
				+ remoteControlVersion;
		final AtomicInteger finalStatus = new AtomicInteger(-1);

		Thread t = new Thread(new Runnable() {
	        @Override
	        public void run() {
	        	HttpClient httpclient = new DefaultHttpClient();
	    		HttpPost httppost = new HttpPost(
	    				"");

	    		try {
	    			// Add your data
	    			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	    			nameValuePairs.add(new BasicNameValuePair("data", finalData));
	    			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	    			// Execute HTTP Post Request
	    			HttpResponse response = httpclient.execute(httppost);
	    			finalStatus.set(response.getStatusLine().getStatusCode());

	    		} catch (ClientProtocolException e) {
	    			// TODO Auto-generated catch block
	    		} catch (IOException e) {
	    			// TODO Auto-generated catch block
	    		}
	        }
	    });
	    t.start();
		try {
			t.join(1000);
			if (finalStatus.get() == HttpStatus.SC_OK)
				return true;
			return false;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
}
