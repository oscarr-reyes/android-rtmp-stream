package dev.oscarreyes.rtmp.io;

import android.util.Log;

import dev.oscarreyes.rtmp.async.Worker;

public class MasterEncoderChannel extends Worker {
	private static final String TAG = MasterEncoderChannel.class.getSimpleName();

	public MasterEncoderChannel(String name) {
		super(name);
	}

	@Override
	protected void capture() throws Exception {
		Log.i(TAG, "capture invoked");
	}

	@Override
	protected void process() throws Exception {
		Log.i(TAG, "process invoked");
	}
}
