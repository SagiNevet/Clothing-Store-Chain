package server.storage;

import common.exception.StorageException;
import common.model.PasswordPolicy;

import java.io.File;

public class PasswordPolicyRepository extends FileRepository<PasswordPolicy> {

    public PasswordPolicyRepository() {
        super(StoragePaths.PASSWORD_POLICY_FILE);
    }

    public PasswordPolicyRepository(File directory) {
        super(directory, StoragePaths.PASSWORD_POLICY_FILE);
    }

    public PasswordPolicy loadPolicy() throws StorageException {
        Object storedObject = loadSingleObject();
        if (storedObject == null) {
            return PasswordPolicy.createDefault();
        }
        return (PasswordPolicy) storedObject;
    }

    public void savePolicy(PasswordPolicy policy) throws StorageException {
        saveSingleObject(policy);
    }
}
