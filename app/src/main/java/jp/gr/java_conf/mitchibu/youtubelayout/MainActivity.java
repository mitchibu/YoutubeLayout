package jp.gr.java_conf.mitchibu.youtubelayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import jp.gr.java_conf.mitchibu.lib.youtubelayout.YoutubeLayout;

public class MainActivity extends AppCompatActivity {
	private YoutubeLayout yt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		yt = (YoutubeLayout)findViewById(R.id.yt);
		yt.setOnGotOutListener(new YoutubeLayout.OnGotOutListener() {
			@Override
			public void onGotOut(YoutubeLayout yt, YoutubeLayout.Cancellation cancellation) {
				android.util.Log.v("test", "onGotOut");
				Toast.makeText(MainActivity.this, "onGotOut", Toast.LENGTH_SHORT).show();
				cancellation.cancel();
			}
		});
		yt.setOnMaximizedListener(new YoutubeLayout.OnMaximizedListener() {
			@Override
			public void onMaximized(YoutubeLayout yt) {
				android.util.Log.v("test", "onMaximized");
			}
		});
		yt.setOnMinimizedListener(new YoutubeLayout.OnMinimizedListener() {
			@Override
			public void onMinimized(YoutubeLayout yt) {
				android.util.Log.v("test", "onMinimized");
			}
		});
	}

	@Override
	public void onBackPressed() {
		if(yt.isMaximized()) {
			yt.minimize(true);
		} else {
			super.onBackPressed();
		}
	}
}
