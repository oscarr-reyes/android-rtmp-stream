package dev.oscarreyes.rtmp.io;

import dev.oscarreyes.rtmp.async.Worker;

public abstract class Channel extends Worker {
	public Channel(String name) {
		super(name);
	}
}
