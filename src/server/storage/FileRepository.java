package server.storage;

import common.exception.StorageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared base of every repository: reads and writes a list of objects to a
 * single file using Java object serialization.
 * <p>
 * The course forbids a database, so the whole persistence layer is this class.
 * The generic parameter {@code T} is bounded by {@link Serializable}, which
 * means the compiler itself refuses a repository of objects that cannot be
 * written to a file - the mistake is caught while writing the code and not at
 * run time.
 * </p>
 * <p>
 * <b>Why a whole list is written at once</b> instead of appending single
 * objects: an {@link ObjectOutputStream} writes a header at the beginning of
 * the file, so a file built by appending several streams cannot be read back by
 * a single {@link ObjectInputStream}. Writing one {@code ArrayList} keeps the
 * file valid and keeps the code short. The data volume of a two branch chain is
 * far too small for this to matter.
 * </p>
 * <p>
 * <b>Releasing resources:</b> every method uses try-with-resources. The stream
 * is declared in the round brackets of the {@code try}, and Java closes it
 * automatically whether the block ended normally or with an exception - it is
 * compiled into exactly the {@code finally} block one would otherwise write by
 * hand, and it cannot be forgotten.
 * </p>
 * <p>
 * <b>Thread safety:</b> a repository object is shared by every client thread,
 * so both methods are marked {@code synchronized}. This is one of the few
 * places where locking the whole method is the right choice, because the file
 * itself is a single resource: two threads must never write it at the same
 * moment, and a thread must not read a file that is half written.
 * </p>
 *
 * @param <T> the type of object kept in this file, which must be serializable
 */
public abstract class FileRepository<T extends Serializable> {

    /** The file this repository reads from and writes to. */
    private final File dataFile;

    /**
     * Creates a repository over one file inside the data folder.
     *
     * @param fileName the name of the file, without any folder
     */
    protected FileRepository(String fileName) {
        StoragePaths.createDirectoriesIfMissing();
        this.dataFile = new File(StoragePaths.DATA_DIRECTORY, fileName);
    }

    /**
     * Creates a repository over one file inside a folder chosen by the caller.
     * <p>
     * This constructor exists for the unit tests, which point the repository at
     * a temporary folder so that running the tests can never damage the real
     * data files of the system.
     * </p>
     *
     * @param directory the folder holding the file
     * @param fileName  the name of the file, without any folder
     */
    protected FileRepository(File directory, String fileName) {
        this.dataFile = new File(directory, fileName);
    }

    /**
     * Reads every object kept in the file.
     * <p>
     * A missing file is not an error: it simply means nothing has been saved
     * yet, so an empty list is returned. That is what lets the system start for
     * the first time on a clean machine.
     * </p>
     *
     * @return a modifiable list of the stored objects, empty when the file does
     *         not exist yet
     * @throws StorageException if the file exists but cannot be read
     */
    @SuppressWarnings("unchecked")
    public synchronized List<T> loadAll() throws StorageException {
        if (!dataFile.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream inputStream =
                     new ObjectInputStream(new FileInputStream(dataFile))) {
            Object storedObject = inputStream.readObject();
            return new ArrayList<>((List<T>) storedObject);
        } catch (ClassNotFoundException missingClass) {
            // The file holds an object whose class is not on the classpath, which
            // happens when a data file is copied from an older version of the project.
            throw new StorageException(dataFile.getPath(),
                    "The data file holds an unknown class", missingClass);
        } catch (IOException readFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to read the data file", readFailure);
        }
    }

    /**
     * Writes every object to the file, replacing whatever it held before.
     *
     * @param items the objects to store, must not be {@code null}
     * @throws StorageException if the file cannot be written
     */
    public synchronized void saveAll(List<T> items) throws StorageException {
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(new FileOutputStream(dataFile))) {
            outputStream.writeObject(new ArrayList<>(items));
        } catch (IOException writeFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to write the data file", writeFailure);
        }
    }

    /**
     * Reads a single object from the file, for repositories that keep exactly
     * one object rather than a list.
     *
     * @return the stored object, or {@code null} when the file does not exist yet
     * @throws StorageException if the file exists but cannot be read
     */
    protected synchronized Object loadSingleObject() throws StorageException {
        if (!dataFile.exists()) {
            return null;
        }
        try (ObjectInputStream inputStream =
                     new ObjectInputStream(new FileInputStream(dataFile))) {
            return inputStream.readObject();
        } catch (ClassNotFoundException missingClass) {
            throw new StorageException(dataFile.getPath(),
                    "The data file holds an unknown class", missingClass);
        } catch (IOException readFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to read the data file", readFailure);
        }
    }

    /**
     * Writes a single object to the file, replacing whatever it held before.
     *
     * @param singleObject the object to store
     * @throws StorageException if the file cannot be written
     */
    protected synchronized void saveSingleObject(Serializable singleObject)
            throws StorageException {
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(new FileOutputStream(dataFile))) {
            outputStream.writeObject(singleObject);
        } catch (IOException writeFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to write the data file", writeFailure);
        }
    }

    /**
     * Returns the file this repository works with, mainly for log messages.
     *
     * @return the data file
     */
    public File getDataFile() {
        return dataFile;
    }
}
