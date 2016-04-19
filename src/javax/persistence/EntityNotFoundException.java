package javax.persistence;

public class EntityNotFoundException extends PersistenceException {

	private static final long serialVersionUID = 1L;

	private static String getDetailMessage(Class<?> clazz, Object id) {
		return "Unable to find an entity of type "+clazz+" with id "+id;
	}
	
	private final Class<?> clazz;
	private final Object id;

	public EntityNotFoundException(Class<?> clazz, Object id) {
		super(getDetailMessage(clazz, id));
		this.clazz = clazz;
		this.id = id;
	}

	public EntityNotFoundException(Class<?> clazz, Object id, Throwable cause) {
		super(getDetailMessage(clazz, id), cause);
		this.clazz = clazz;
		this.id = id;
	}
 
	public Class<?> getEntiryClass() {
		return clazz;
	}
	
	public Object getId() {
		return id;
	}
	

}
