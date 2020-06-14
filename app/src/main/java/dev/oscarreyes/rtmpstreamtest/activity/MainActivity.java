package dev.oscarreyes.rtmpstreamtest.activity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import dev.oscarreyes.rtmpstreamtest.R;

public class MainActivity extends BaseActivity {
	private boolean isStreaming = false;

	private ImageButton streamButton;

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
		super.onPermissionsGranted();
	}

	@Override
	protected void onPermissionsDenied() {
		super.onPermissionsDenied();
	}

	public void onButtonClick(View view) {
		this.isStreaming = !this.isStreaming;

		if (this.isStreaming) {
			this.streamButton.setBackgroundResource(R.drawable.ic_stop);
		} else {
			this.streamButton.setBackgroundResource(R.drawable.ic_play_arrow);
		}
	}
}
