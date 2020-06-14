package dev.oscarreyes.rtmp;

import dev.oscarreyes.rtmp.io.MasterEncoderChannel;

public class RtmpStream {

	private MasterEncoderChannel masterChannel;

	public RtmpStream() {
		this.masterChannel = new MasterEncoderChannel("RTMP");
	}

	public void start() {
		this.masterChannel.start();
	}

	public void stop() {
		this.masterChannel.stop();
	}
}
