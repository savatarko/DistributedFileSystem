package servent.model;

import app.HashingUtil;

import java.io.Serializable;
import java.util.Objects;

public class NetworkFile implements Serializable {
    private String name;
    private String owner;
    private String hashedName;

    public NetworkFile(String name) {
        this.name = name;
        this.owner = null;
        hashedName = HashingUtil.SHA1(name);
    }

    public NetworkFile(String name, String owner) {
        this.name = name;
        this.owner = owner;
        hashedName = HashingUtil.SHA1(name);
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getHashedName() {
        return hashedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkFile that = (NetworkFile) o;
        return Objects.equals(name, that.name) && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, owner);
    }
}
