package servent.message;

public class StoppedMessage extends BasicMessage{

    public StoppedMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress) {
        super(MessageType.STOPPED, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
    }
}
