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

public abstract class FileRepository<T extends Serializable> {

    private final File dataFile;

    protected FileRepository(String fileName) {
        StoragePaths.createDirectoriesIfMissing();
        this.dataFile = new File(StoragePaths.DATA_DIRECTORY, fileName);
    }

    protected FileRepository(File directory, String fileName) {
        this.dataFile = new File(directory, fileName);
    }

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

            throw new StorageException(dataFile.getPath(),
                    "The data file holds an unknown class", missingClass);
        } catch (IOException readFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to read the data file", readFailure);
        }
    }

    public synchronized void saveAll(List<T> items) throws StorageException {
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(new FileOutputStream(dataFile))) {
            outputStream.writeObject(new ArrayList<>(items));
        } catch (IOException writeFailure) {
            throw new StorageException(dataFile.getPath(),
                    "Failed to write the data file", writeFailure);
        }
    }

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

    public File getDataFile() {
        return dataFile;
    }
}
