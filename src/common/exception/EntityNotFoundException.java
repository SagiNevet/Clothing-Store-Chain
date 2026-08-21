package common.exception;

/**
 * Thrown when a requested entity - an employee, a customer or a product - does
 * not exist in the files of the system.
 * <p>
 * One exception class serves all three entity kinds. The entity type is a field
 * rather than a separate subclass, because the calling code always reacts in
 * the same way: it shows the user that the item was not found.
 * </p>
 */
public class EntityNotFoundException extends ChainStoreException {

    /** Serialization version, required because exceptions are serializable. */
    private static final long serialVersionUID = 1L;

    /** The kind of entity that was searched, for example "Product". */
    private final String entityType;

    /** The identifier that was searched and not found. */
    private final String entityId;

    /**
     * Creates a not found failure.
     *
     * @param entityType the kind of entity that was searched, for example "Customer"
     * @param entityId   the identifier that was searched and not found
     */
    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " with identifier " + entityId + " was not found");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Returns the kind of entity that was searched.
     *
     * @return the entity type name
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the identifier that was not found.
     *
     * @return the entity identifier
     */
    public String getEntityId() {
        return entityId;
    }
}
