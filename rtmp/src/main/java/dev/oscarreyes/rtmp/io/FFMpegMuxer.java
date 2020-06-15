package dev.oscarreyes.rtmp.io;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class FFMpegMuxer {
	/**
	 * Supported video codec types
	 */
	private static final int CODEC_H264 = 0;
	private static final int CODEC_H265 = 1;

	/**
	 * Streaming URL
	 */
	private String url;

	/**
	 * Video stream format
	 */
	private MediaFormat video;

	/**
	 * Audio stream format
	 */
	private MediaFormat audio;

	/**
	 * Muxer ID
	 */
	private long id;

	/**
	 * Set destination
	 *
	 * @param url      URL of HTTP server
	 */
	public void setDestination(String url) {
		this.url = url;
	}

	/**
	 * Adds a track with the specified format.
	 *
	 * @param format The media format for the track.
	 */
	public void addTrack(MediaFormat format) {
		if (format.getString(MediaFormat.KEY_MIME).contains("video")) {
			video = format;
		} else {
			audio = format;
		}
	}

	/**
	 * Start the muxer
	 */
	public synchronized void start() throws IOException {
		String format = "mp4";
		if (url.contains("rtmp")) format = "flv";
		if (url.contains("http")) format = "dash";

		id = open(url, format);
		if (id < 0) {
			id = 0;
			throw new SocketException("Cannot not stream to " + url);
		}

		// Add video stream
		if (video != null) {
			int width = video.getInteger(MediaFormat.KEY_WIDTH);
			int height = video.getInteger(MediaFormat.KEY_HEIGHT);
			int fps = video.getInteger(MediaFormat.KEY_FRAME_RATE);
			int gop = video.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL) * fps;
			int bitrate = video.getInteger(MediaFormat.KEY_BIT_RATE);

			String mime = video.getString(MediaFormat.KEY_MIME);
			int type = CODEC_H264;
			if (mime.equals(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
				if (format.equals("flv")) throw new SocketException("H265 video encoding is not supported for RTMP endpoint");
				type = CODEC_H265;
			}

			int ret = addVideoTrack(id, type, width, height, fps, gop, bitrate);
			if (ret < 0) throw new SocketException("Cannot initialize video stream");
		}

		// Add audio stream
		if (audio != null) {
			String mime = audio.getString(MediaFormat.KEY_MIME);
			int sample = audio.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			int bitrate = audio.getInteger(MediaFormat.KEY_BIT_RATE);
			int ret = addAudioTrack(id, 0, sample, bitrate);
			if (ret < 0) throw new SocketException("Cannot initialize audio stream");
		}
	}

	/**
	 * Stop the muxer
	 */
	public synchronized void stop() {
		if (id > 0) close(id);
		id = 0;
	}

	/**
	 * Writes video frames into the output stream
	 *
	 * @param byteBuf    Buffer of video frames
	 * @param bufferInfo Buffer info
	 */
	public void writeVideoSample(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) throws IOException {
		if (id == 0) return;
		String ret = writeVideoSample(id, byteBuf.array(), bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
		if (ret != null) throw new SocketException("Error streaming: " + ret);
	}

	/**
	 * Writes audio frames into the output stream
	 *
	 * @param byteBuf    Buffer of audio frames
	 * @param bufferInfo Buffer info
	 */
	public void writeAudioSample(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) throws IOException {
		if (id == 0) return;
		String ret = writeAudioSample(id, byteBuf.array(), bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
		if (ret != null) throw new SocketException("Error streaming: " + ret);
	}

	// NATIVE CALLS
	private static native long open(String url, String format);

	private static native int addVideoTrack(long id, int type, int width, int height, int fps, int gop, int bitrate);

	private static native int addAudioTrack(long id, int type, int sample, int bitrate);

	private static native String writeVideoSample(long id, byte[] data, int len, long pts, int flags);

	private static native String writeAudioSample(long id, byte[] data, int len, long pts, int flags);

	private static native void close(long id);

	static {
		System.loadLibrary("mobile-ffmpeg-muxer");
	}
}
