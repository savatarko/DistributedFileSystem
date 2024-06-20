package servent.model;

import java.io.Serializable;

public class DataFile implements Serializable {
    public NetworkFile networkFile;
    public byte[] bytes;

    public DataFile(NetworkFile networkFile, byte[] bytes) {
        this.networkFile = networkFile;
        this.bytes = bytes;
    }
}
