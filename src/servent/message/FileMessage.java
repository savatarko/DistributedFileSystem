package servent.message;

import servent.model.NetworkFile;

public class FileMessage extends BasicMessage{
    public NetworkFile networkFile;
    public byte[] bytes;

//    public FileMessage(NetworkFile networkFile, byte[] bytes) {
//        this.networkFile = networkFile;
//        this.bytes = bytes;
//    }

    public FileMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAdress, NetworkFile networkFile, byte[] bytes){
        super(MessageType.FILE, senderPort, senderIpAdress, receiverPort, receiverIpAdress);
        this.networkFile = networkFile;
        this.bytes = bytes;
    }
}
