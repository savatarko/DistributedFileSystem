package servent.message;

public class NewFriendMessage extends BasicMessage{
    public NewFriendMessage(int senderPort, String senderIp, int receiverPort, String receiverIp) {
        super(MessageType.NEW_FRIEND, senderPort, senderIp, receiverPort, receiverIp);
    }

    public NewFriendMessage(int senderPort, String senderIp, int receiverPort, String receiverIp, String text) {
        super(MessageType.NEW_FRIEND, senderPort, senderIp, receiverPort, receiverIp, text);
    }
}
