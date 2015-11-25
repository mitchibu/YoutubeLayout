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
				Toast.makeText(MainActivity.this, "onGotOut", Toast.LENGTH_SHORT).show();
				cancellation.cancel();
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
