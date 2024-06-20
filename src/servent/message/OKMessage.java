package servent.message;

public class OKMessage extends BasicMessage{


    public OKMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, String messageText) {
        super(MessageType.OK, senderPort, senderIpAddress, receiverPort, receiverIpAddress, messageText);
    }
}
