package com.alexey_sel.elpisremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.VideoView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
	String URL;
	TextView songAndArtist;
	Button play;
	Button next;
	Button like;
	Button dislike;
	public AsyncResponse handler;
	boolean forceUpdate = false;
	boolean connected = false;
	ProgressBar progressBar;
	String ip;
	VideoView mVideoView;
	MediaPlayer player;
	Context t;
	boolean listen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		t = this;
		setContentView(R.layout.activity_remote_control);
		play = (Button) findViewById(R.id.buttonPlay);
		next = (Button) findViewById(R.id.buttonNext);
		like = (Button) findViewById(R.id.buttonLike);
		dislike = (Button) findViewById(R.id.buttonDislike);
		currentSongAmazonID = "";
		ipport = (EditText) findViewById(R.id.ipandportvalue);
		songAndArtist = (TextView) findViewById(R.id.songAndArtist);
		progressBar = (ProgressBar) findViewById(R.id.ProgressBar);
		handler = this;
		ip = ipport.getText().toString();
		listen = false;
		// mVideoView = (VideoView) findViewById(R.id.videoView1);
		// mVideoView.setMediaController(new MediaController(this));
		player = new MediaPlayer();
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (play.getText().equals("Playing")) {
					new RequestTask().execute("pause", new String());
					play.setText(R.string.pause_button_string);
					pausePlaying();
				} else {
					new RequestTask().execute("play", new String());
					play.setText(R.string.play_button_string);
					continuePlaying();
				}
			}
		});
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showProgressBar();
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
		ipport.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == 123) {
					ip = ipport.getText().toString();
					Log.d("Setting ip", ip);
				}
				return false;
			}
		});
		ScheduledExecutorService scheduler = Executors
				.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				getCurrentSong();
			}
		}, 0, 10, TimeUnit.SECONDS);

		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (!connected) {
					showProgressBar();
					connect();
				} else {
					hideProgressBar();
				}
			}
		}, 0, 2, TimeUnit.SECONDS);

	}

	public void pausePlaying() {
		if (listen){
			player.pause();
		}
	}

	public void continuePlaying() {
		if (listen){
			player.start();
		}
	}

	public void stopSong(){
		player.stop();
	}
	
	public void playSong() {
		// this plays the song on a videoview. bam!
		/*
		 * Uri video = Uri.parse(URL); mVideoView.setVideoURI(video);
		 * mVideoView.start();
		 */
		if (listen) {
			try {
				player.stop();
				player.release();
				player = new MediaPlayer();
				player.setDataSource(URL);
				player.prepare();
				player.start();
			} catch (IllegalArgumentException | SecurityException
					| IllegalStateException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/*
		 * This is the code to run VLC to play the song. Intent intent = new
		 * Intent(Intent.ACTION_VIEW);
		 * intent.setPackage("org.videolan.vlc.betav7neon");
		 * intent.setDataAndType(Uri.parse(URL), "application/mp4");
		 * startActivity(intent);
		 */
	}

	public void showProgressBar() {
		if (progressBar.getVisibility() != View.VISIBLE)
			progressBar.setVisibility(View.VISIBLE);
	}

	public void hideProgressBar() {
		if (progressBar.getVisibility() == View.VISIBLE)
			progressBar.setVisibility(View.GONE);
	}

	public void connect() {
		new RequestTask().execute("connect", new String());
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

	public String getIPPort() {
		String address = Constants.PREFIX + ip + Constants.IP_PORT_SEPARATOR
				+ Constants.PORT;
		// Log.d("address", address);
		return address;
	}

	class RequestTask extends AsyncTask<String, String, String> {
		public String command = null;

		@Override
		protected String doInBackground(String... uri) {
			this.command = uri[0];
			if (connected || command.equals("connect")) {
				Log.d("Command:ip", command + ":" + ip);
				HttpParams httpParams = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, 100);
				HttpClient httpclient = new DefaultHttpClient(httpParams);
				HttpResponse response;

				String responseString = uri[1];
				try {
					response = httpclient.execute(new HttpGet(getIPPort() + "/"
							+ uri[0]));
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
					return null;
				} catch (IOException e) {
					return null;
				}
				return responseString;
			} else {
				showProgressBar();
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result == null)
				result = "";
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
		case R.id.listen:
			listen = !item.isChecked();
			item.setChecked(!item.isChecked());
			if (listen)
				playSong();
			else
				stopSong();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void processFinish(String output, String command) {
		if (connected || command.equals("connect")) {
			switch (command) {
			case "currentsong": {
				try {
					JSONObject jsonObj = new JSONObject(output);
					JSONObject station = jsonObj.getJSONObject("Station");
					if (station.getBoolean("SkipLimitReached")) {
						new AlertDialog.Builder(this)
								.setTitle("The Box is Open")
								.setMessage(
										"Ups, the Skip Limit Was Reached...You're screwed.")
								.setPositiveButton(android.R.string.ok,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
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
							songAndArtist.setText(jsonObj
									.getString("SongTitle")
									+ " by "
									+ jsonObj.getString("Artist"));
							currentSongAmazonID = jsonObj
									.getString("AmazonTrackID");
							boolean liked = jsonObj.getBoolean("Loved");
							if (liked) {
								like.setText(R.string.liked_button_string);
							} else {
								like.setText(R.string.like_button_string);
							}
							URL = jsonObj.getString("AudioUrl");
							playSong();
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			}
			case "next": {
				Toast.makeText(getApplicationContext(), output,
						Toast.LENGTH_LONG).show();
				getCurrentSong();
				break;
			}
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
			case "connect": {
				if (output.equals("true")) {
					connected = true;
				}
				break;
			}
			default: {
				Toast.makeText(getApplicationContext(), output,
						Toast.LENGTH_LONG).show();
				break;
			}
			}
		}
	}
}
