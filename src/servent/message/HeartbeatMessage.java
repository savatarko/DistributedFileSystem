package servent.message;

public class HeartbeatMessage extends BasicMessage{
    public HeartbeatMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress) {
        super(MessageType.HEARTBEAT, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
    }
}
