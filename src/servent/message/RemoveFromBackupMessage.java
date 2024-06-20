package servent.message;

import servent.model.NetworkFile;

import java.util.List;

public class RemoveFromBackupMessage extends BasicMessage{

    private List<NetworkFile> networkFiles;

    public RemoveFromBackupMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, List<NetworkFile> networkFiles) {
        super(MessageType.REMOVE_FROM_BACKUP, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.networkFiles = networkFiles;
    }

    public List<NetworkFile> getNetworkFiles() {
        return networkFiles;
    }
}
