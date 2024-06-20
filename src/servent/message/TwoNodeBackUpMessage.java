package servent.message;

import servent.model.NetworkFile;

public class TwoNodeBackUpMessage extends BasicMessage{
    public NetworkFile networkFile;
    public byte[] fileBytes;
    public TwoNodeBackUpMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, NetworkFile networkFile, byte[] fileBytes) {
        super(MessageType.DOUBLE_BACKUP, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.networkFile = networkFile;
        this.fileBytes = fileBytes;
    }


}
