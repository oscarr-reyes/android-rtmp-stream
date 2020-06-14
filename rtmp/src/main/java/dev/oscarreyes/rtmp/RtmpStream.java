package dev.oscarreyes.rtmp;

import android.util.Log;

import java.io.IOException;

import dev.oscarreyes.rtmp.io.MasterEncoderChannel;

public class RtmpStream {

	private MasterEncoderChannel masterChannel;

	public RtmpStream() {
		this.masterChannel = new MasterEncoderChannel("RTMP");
	}

	/**
	 * Start the stream
	 */
	public void start() {
		try {
			this.masterChannel.startEncoder();
		} catch (IOException e) {
			Log.e("RtmpStream", e.getMessage());
		}
	}

	/**
	 * Stop the stream
	 */
	public void stop() {
		this.masterChannel.stopEncoder();
	}
}
