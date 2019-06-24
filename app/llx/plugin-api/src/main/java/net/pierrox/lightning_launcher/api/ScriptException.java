package net.pierrox.lightning_launcher.api;

/**
 * @author lukas
 * @since 11.02.19
 */
public class ScriptException extends RuntimeException {
	public ScriptException() {
	}

	public ScriptException(String message) {
		super(message);
	}

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}

	public ScriptException(Throwable cause) {
		super(cause);
	}
}
