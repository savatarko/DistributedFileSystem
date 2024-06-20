package servent.message;

public class PingMessage extends BasicMessage{

    public PingMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress) {
        super(MessageType.PING, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
    }
}
