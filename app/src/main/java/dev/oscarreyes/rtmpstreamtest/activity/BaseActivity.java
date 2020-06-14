package dev.oscarreyes.rtmpstreamtest.activity;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		boolean granted = true;

		if (requestCode == permissions.length) {
			for (int grantResult : grantResults) {
				if (grantResult == PackageManager.PERMISSION_DENIED) {
					granted = false;
					break;
				}
			}
		}

		if (granted) {
			onPermissionsGranted();
		} else {
			onPermissionsDenied();
		}
	}

	/**
	 * Verifies if all the provided permissions are granted.
	 * If any permission is not yet granted, then they are automatically requested.
	 * Invokes {@link BaseActivity#onPermissionsGranted()} when all permissions are granted.
	 * Invokes {@link BaseActivity#onPermissionsDenied()} when any permission is denied by the user
	 *
	 * @param permissions The list of permissions to check in the device
	 */
	protected void checkPermissions(String... permissions) {
		List<String> notGranted = new ArrayList<>();

		for (String permission : permissions) {
			if (this.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
				notGranted.add(permission);
			}
		}

		if (!notGranted.isEmpty()) {
			final int permissionCode = notGranted.size();
			final String[] newPermissions = notGranted.toArray(new String[]{});

			this.requestPermissions(newPermissions, permissionCode);
		} else {
			this.onPermissionsGranted();
		}
	}

	/**
	 * Invoked when all permission provided from {@link BaseActivity#checkPermissions(String...)} are granted
	 */
	protected void onPermissionsGranted() {

	}

	/**
	 * Invoked when any permission provided from {@link BaseActivity#checkPermissions(String...)} is denied
	 */
	protected void onPermissionsDenied() {

	}
}
