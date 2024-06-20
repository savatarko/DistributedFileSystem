package servent.message;

import servent.model.DataFile;
import servent.model.NetworkFile;

import java.util.List;

public class BackupMessage extends BasicMessage{
    //public List<DataFile> files;
    public NetworkFile networkFile;
    public byte[] bytes;

    public BackupMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAdress, NetworkFile networkFile, byte[] bytes){
        super(MessageType.BACKUP, senderPort, senderIpAdress, receiverPort, receiverIpAdress);
        this.networkFile = networkFile;
        this.bytes = bytes;
    }
}
