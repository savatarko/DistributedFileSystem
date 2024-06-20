package servent.message;

import servent.model.NetworkFile;

import java.util.List;

public class InfoMessage extends BasicMessage{
    private List<NetworkFile> files;
    private List<String> friends;

    public InfoMessage(int senderPort, String senderIp, int receiverPort, String receiverIp, List<String> friends) {
        super(MessageType.INFO, senderPort, senderIp, receiverPort, receiverIp);
        this.friends = friends;
    }

    public InfoMessage(int senderPort, String senderIp, int receiverPort, String receiverIp, List<NetworkFile> files, List<String> friends) {
        super(MessageType.INFO, senderPort, senderIp, receiverPort, receiverIp);
        this.files = files;
        this.friends = friends;
    }

    public List<NetworkFile> getFiles() {
        return files;
    }

    public List<String> getFriends() {
        return friends;
    }
}
