package dev.oscarreyes.rtmpstreamtest.activity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import dev.oscarreyes.rtmp.RtmpStream;
import dev.oscarreyes.rtmpstreamtest.R;

public class MainActivity extends BaseActivity {
	private boolean isStreaming = false;

	private ImageButton streamButton;
	private RtmpStream rtmpStream;

	private final String[] PERMISSIONS = new String[]{
		Manifest.permission.RECORD_AUDIO
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.streamButton = this.findViewById(R.id.stream_button);
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.checkPermissions(PERMISSIONS);
	}

	@Override
	protected void onPermissionsGranted() {
		this.rtmpStream = new RtmpStream();
	}

	@Override
	protected void onPermissionsDenied() {
		super.onPermissionsDenied();
	}

	private void startStream() {
		this.rtmpStream.start();

		this.isStreaming = true;
		this.streamButton.setBackgroundResource(R.drawable.ic_stop);
	}

	private void stopStream() {
		this.rtmpStream.stop();

		this.isStreaming = false;
		this.streamButton.setBackgroundResource(R.drawable.ic_play_arrow);
	}

	public void onButtonClick(View view) {
		if (this.rtmpStream != null) {
			if (this.isStreaming) {
				this.stopStream();
			} else {
				this.startStream();
			}
		}
	}
}
