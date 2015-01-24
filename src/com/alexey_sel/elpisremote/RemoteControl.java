package com.alexey_sel.elpisremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteControl extends Activity implements AsyncResponse {
	EditText ipport;
	String currentSongAmazonID;
	TextView songAndArtist;
	Button play;
	Button next;
	Button like;
	Button dislike;
	public AsyncResponse handler;
	boolean forceUpdate = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_control);
		play = (Button) findViewById(R.id.buttonPlay);
		next = (Button) findViewById(R.id.buttonNext);
		like = (Button) findViewById(R.id.buttonLike);
		dislike = (Button) findViewById(R.id.buttonDislike);
		currentSongAmazonID = "";
		ipport = (EditText) findViewById(R.id.ipandportvalue);
		songAndArtist = (TextView) findViewById(R.id.songAndArtist);
		handler = this;
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (play.getText().equals("Playing")) {
					new RequestTask().execute("pause", new String());
					play.setText(R.string.pause_button_string);
				} else {
					new RequestTask().execute("play", new String());
					play.setText(R.string.play_button_string);
				}
			}
		});
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new RequestTask().execute("next", new String());
			}
		});
		like.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new RequestTask().execute("like", new String());
			}
		});
		dislike.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new RequestTask().execute("dislike", new String());
			}
		});
		getCurrentSong();
		ScheduledExecutorService scheduler = Executors
				.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				getCurrentSong();
			}
		}, 0, 10, TimeUnit.SECONDS);

	}

	public void getCurrentSong() {
		new RequestTask().execute("currentsong", new String());
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}

	class RequestTask extends AsyncTask<String, String, String> {
		public String command = null;

		@Override
		protected String doInBackground(String... uri) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			this.command = uri[0];
			String responseString = uri[1];
			try {
				response = httpclient.execute(new HttpGet(ipport.getText()
						.toString() + "/" + uri[0]));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					responseString = out.toString();
					out.close();
				} else {
					// Closes the connection.
					response.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (ClientProtocolException e) {
				// TODO Handle problems..
			} catch (IOException e) {
				// TODO Handle problems..
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			handler.processFinish(result, this.command);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.remote_control, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.updateSongInfo:
			forceUpdate = true;
			getActionBar();
			return true;
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void processFinish(String output, String command) {
		switch (command) {
		case "currentsong": {
			try {
				JSONObject jsonObj = new JSONObject(output);
				JSONObject station = jsonObj.getJSONObject("Station");
				if (station.getBoolean("SkipLimitReached")) {
					new AlertDialog.Builder(this)
				    .setTitle("The Box is Open")
				    .setMessage("Ups, the Skip Limit Was Reached...You're screwed.")
				    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) { 
				        	dialog.cancel();
				        }
				     })
				    .setIcon(android.R.drawable.ic_dialog_alert)
				     .show();
				} else {
					if (!jsonObj.getString("AmazonTrackID").equals(
							currentSongAmazonID)
							|| forceUpdate) {
						forceUpdate = false;
						new DownloadImageTask(
								(ImageView) findViewById(R.id.coverImage))
								.execute(jsonObj.getString("AlbumArtUrl"));
						songAndArtist.setText(jsonObj.getString("SongTitle")
								+ " by " + jsonObj.getString("Artist"));
						currentSongAmazonID = jsonObj
								.getString("AmazonTrackID");
						boolean liked = jsonObj.getBoolean("Loved");
						if (liked) {
							like.setText(R.string.liked_button_string);
						} else {
							like.setText(R.string.like_button_string);
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;
		}
		case "next":
		case "dislike": {
			getCurrentSong();
			break;
		}
		case "like": {
			if (output.equals("Like")) {
				like.setText(R.string.liked_button_string);
			} else {
				like.setText(R.string.like_button_string);
			}
			break;
		}

		default: {
			Toast.makeText(getApplicationContext(), output, Toast.LENGTH_LONG)
					.show();
			break;
		}
		}
	}
}
