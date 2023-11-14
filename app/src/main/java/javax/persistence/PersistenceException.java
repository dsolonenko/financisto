package javax.persistence;

import java.io.Serial;

public class PersistenceException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public PersistenceException() {
		super();
	}

	public PersistenceException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public PersistenceException(String detailMessage) {
		super(detailMessage);
	}

	public PersistenceException(Throwable throwable) {
		super(throwable);
	}
	
}
