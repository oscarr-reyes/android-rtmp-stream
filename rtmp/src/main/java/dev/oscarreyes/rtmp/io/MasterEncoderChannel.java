package dev.oscarreyes.rtmp.io;

import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.IOException;

import dev.oscarreyes.rtmp.async.Worker;

public class MasterEncoderChannel extends Worker {
	private static final String TAG = MasterEncoderChannel.class.getSimpleName();

	private AudioEncoder audioEncoder;

	private final MediaFormat audioFormat;

	public MasterEncoderChannel(String name) {
		super(name);

		this.audioFormat = AudioEncoder.getMediaFormat();
	}

	@Override
	protected void capture() throws Exception {
		this.audioEncoder.captureAudio();
	}

	@Override
	protected void process() throws Exception {
		this.audioEncoder.getAudioFrame();
	}

	/**
	 * Instantiates and starts all encoders with the worker thread
	 * @throws IOException
	 */
	public void startEncoder() throws IOException {
		// TODO: Make this use the user selected audio source
		this.audioEncoder = new AudioEncoder(MediaRecorder.AudioSource.DEFAULT);

		this.audioEncoder.start();
		this.start();
	}

	/**
	 * Stops all encoders with the worker thread
	 */
	public void stopEncoder() {
		this.stop();
		this.audioEncoder.stop();

		this.audioEncoder = null;
	}
}
