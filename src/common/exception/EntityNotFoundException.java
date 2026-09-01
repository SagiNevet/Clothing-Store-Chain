package common.exception;

public class EntityNotFoundException extends ChainStoreException {

    private static final long serialVersionUID = 1L;

    private final String entityType;

    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " with identifier " + entityId + " was not found");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
