package dev.oscarreyes.rtmp.io;

import android.media.MediaCodec;

public class Frame {
	public int flags;
	public int size;
	public long timestamp;
	public byte[] data;
	public MediaCodec.BufferInfo info;
}
