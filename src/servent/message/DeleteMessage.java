package servent.message;

import servent.model.NetworkFile;

public class DeleteMessage extends BasicMessage{
    private String fileName;
    public DeleteMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAdress, String fileName){
        super(MessageType.DELETE, senderPort, senderIpAdress, receiverPort, receiverIpAdress);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
