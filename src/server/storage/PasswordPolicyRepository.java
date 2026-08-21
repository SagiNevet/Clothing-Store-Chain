package server.storage;

import common.exception.StorageException;
import common.model.PasswordPolicy;

import java.io.File;

/**
 * Reads and writes the single password policy of the system.
 * <p>
 * Unlike the other repositories this one stores exactly one object rather than
 * a list, which is why it uses the single object helpers of
 * {@link FileRepository} instead of {@code loadAll} and {@code saveAll}.
 * </p>
 */
public class PasswordPolicyRepository extends FileRepository<PasswordPolicy> {

    /**
     * Creates the repository over {@code data/password_policy.dat}.
     */
    public PasswordPolicyRepository() {
        super(StoragePaths.PASSWORD_POLICY_FILE);
    }

    /**
     * Creates the repository over a policy file inside a folder chosen by the
     * caller. Used by the unit tests, so that running them never touches the
     * real data of the system.
     *
     * @param directory the folder holding the policy file
     */
    public PasswordPolicyRepository(File directory) {
        super(directory, StoragePaths.PASSWORD_POLICY_FILE);
    }

    /**
     * Reads the password policy, or builds the default one the first time the
     * server ever runs.
     *
     * @return the stored policy, or {@link PasswordPolicy#createDefault()} when
     *         no policy has been saved yet
     * @throws StorageException if the policy file cannot be read
     */
    public PasswordPolicy loadPolicy() throws StorageException {
        Object storedObject = loadSingleObject();
        if (storedObject == null) {
            return PasswordPolicy.createDefault();
        }
        return (PasswordPolicy) storedObject;
    }

    /**
     * Stores the password policy, replacing the previous one.
     *
     * @param policy the policy defined by the administrator
     * @throws StorageException if the policy file cannot be written
     */
    public void savePolicy(PasswordPolicy policy) throws StorageException {
        saveSingleObject(policy);
    }
}
