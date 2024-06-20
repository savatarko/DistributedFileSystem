package servent.message;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;

	public TellGetMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAddress ,int key, int value) {
		super(MessageType.TELL_GET, senderPort, senderIpAdress, receiverPort,receiverIpAddress, key + ":" + value);
	}
}
