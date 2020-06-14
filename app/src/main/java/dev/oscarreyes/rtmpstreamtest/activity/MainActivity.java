package dev.oscarreyes.rtmpstreamtest.activity;

import android.Manifest;
import android.os.Bundle;

import dev.oscarreyes.rtmpstreamtest.R;

public class MainActivity extends BaseActivity {

	private final String[] PERMISSIONS = new String[] {
		Manifest.permission.RECORD_AUDIO
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
}
