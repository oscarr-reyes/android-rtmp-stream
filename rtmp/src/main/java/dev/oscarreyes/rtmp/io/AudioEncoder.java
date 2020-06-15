package dev.oscarreyes.rtmp.io;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder {
	private static final String CODEC = MediaFormat.MIMETYPE_AUDIO_AAC;
	private static final int SAMPLE_RATE = 44100;
	private static final int BITRATE = 128 * 1000; // 128 kbps
	private static final int CHANNEL_STEREO = 2;
	private static final int PCM_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT); // Audio pulse-code modulation waves

	private int audioSource;
	private AudioRecord audioRecord;
	private MediaFormat mediaFormat;
	private MediaCodec encoder;

	private final byte[] pcmBuffer; // Pulse-code modulation buffer data

	/**
	 * Gets the audio media format for AAC
	 * @return Audio media format
	 */
	public static MediaFormat getMediaFormat() {
		MediaFormat mediaFormat = MediaFormat.createAudioFormat(CODEC, SAMPLE_RATE, CHANNEL_STEREO);

		mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
		mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PCM_BUFFER_SIZE);

		return mediaFormat;
	}

	public AudioEncoder(int audioSource) {
		this.audioSource = audioSource;
		this.mediaFormat = getMediaFormat();
		this.pcmBuffer = new byte[PCM_BUFFER_SIZE];
	}

	/**
	 * Gets the codec name for the current audio media format
	 * @return Name of the codec
	 */
	private String getCodecName() {
		final MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

		return codecList.findEncoderForFormat(mediaFormat);
	}

	/**
	 * Starts the audio source and encoder with the configured media format
	 * @throws IOException
	 */
	public void start() throws IOException {
		this.audioRecord = new AudioRecord(
			this.audioSource, SAMPLE_RATE,
			AudioFormat.CHANNEL_IN_STEREO,
			AudioFormat.ENCODING_PCM_16BIT,
			PCM_BUFFER_SIZE
		);

		this.audioRecord.startRecording();

		try {
			this.encoder = MediaCodec.createByCodecName(this.getCodecName());

			this.encoder.configure(this.mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			this.encoder.start();
		} catch (IOException e) {
			throw new IOException("Required audio encoder or configuration is not supported by this device", e);
		}
	}

	/**
	 * Stops the encoder and audio source
	 */
	public void stop() {
		if (this.encoder != null) {
			this.encoder.stop();
			this.encoder.release();

			this.encoder = null;
		}

		if (this.audioRecord != null) {
			this.audioRecord.stop();
			this.audioRecord.setRecordPositionUpdateListener(null);
			this.audioRecord.release();

			this.audioRecord = null;
		}
	}

	/**
	 * Captures and queues the raw audio buffer into the encoder
	 */
	public void captureAudio() {
		final int inputBufferIndex = this.encoder.dequeueInputBuffer(0);

		if (inputBufferIndex >= 0) {
			long timeSystem = System.nanoTime() / 1000; // Publish time system
			int size = this.audioRecord.read(this.pcmBuffer, 0, this.pcmBuffer.length);

			if (size > 0) {
				ByteBuffer byteBuffer = this.encoder.getInputBuffer(inputBufferIndex);

				byteBuffer.put(this.pcmBuffer, 0, size);

				this.encoder.queueInputBuffer(inputBufferIndex, 0, size, timeSystem, 0);
			}
		}
	}

	/**
	 * Dequeues and obtains the newest encoded audio data from the encoder
	 * @return Frame object with data information
	 */
	public Frame getAudioFrame() {
		final Frame frame = new Frame();
		final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		final int outputBufferIndex = this.encoder.dequeueOutputBuffer(bufferInfo, 0);

		if (outputBufferIndex >= 0) {
			final ByteBuffer byteBuffer = this.encoder.getOutputBuffer(outputBufferIndex);

			frame.flags = bufferInfo.flags;
			frame.timestamp = bufferInfo.presentationTimeUs;
			frame.data = new byte[bufferInfo.size];
			frame.info = bufferInfo;

			byteBuffer.get(frame.data);

			this.encoder.releaseOutputBuffer(outputBufferIndex, false);
		}

		return frame;
	}
}
