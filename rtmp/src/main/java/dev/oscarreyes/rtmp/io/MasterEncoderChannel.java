package dev.oscarreyes.rtmp.io;

import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;

import dev.oscarreyes.rtmp.async.Worker;

/*
* TODO: Move FFmpegMuxer instance to be managed by RtmpStream instead of MasterEncoderChannel
*/

public class MasterEncoderChannel extends Worker {
	private static final String TAG = MasterEncoderChannel.class.getSimpleName();

	private AudioEncoder audioEncoder;
	private FFMpegMuxer ffMpegMuxer;

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
		final Frame audioFrame = this.audioEncoder.getAudioFrame();

		if (audioFrame.data == null) {
			return;
		}

		final ByteBuffer audioBuffer = ByteBuffer.wrap(audioFrame.data);

		// TODO: Fix "Invalid argument error" from ffmpeg muxer, it is thrown only for the first few writes
		try {
			this.ffMpegMuxer.writeAudioSample(audioBuffer, audioFrame.info);
		} catch (IOException ignored) {
		}
	}

	/**
	 * Instantiates and starts all encoders with the worker thread
	 *
	 * @throws IOException
	 */
	public void startEncoder() throws IOException {
		// TODO: Make this use the user selected audio source
		this.audioEncoder = new AudioEncoder(MediaRecorder.AudioSource.DEFAULT);
		this.ffMpegMuxer = new FFMpegMuxer();

		this.ffMpegMuxer.addTrack(this.audioFormat);
		this.ffMpegMuxer.setDestination("rtmp://192.168.0.7/live/STREAM_TEST");

		this.audioEncoder.start();
		this.ffMpegMuxer.start();
		this.start();
	}

	/**
	 * Stops all encoders with the worker thread
	 */
	public void stopEncoder() {
		this.stop();
		this.ffMpegMuxer.stop();
		this.audioEncoder.stop();

		this.ffMpegMuxer = null;
		this.audioEncoder = null;
	}
}
