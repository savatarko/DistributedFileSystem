package servent.message;

import servent.model.NetworkFile;

public class PutMessage extends BasicMessage {

	private static final long serialVersionUID = 5163039209888734276L;

	private NetworkFile networkFile;

	public PutMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAddress, NetworkFile networkFile) {
		super(MessageType.PUT, senderPort,senderIpAdress,  receiverPort, receiverIpAddress);
		this.networkFile = networkFile;
	}

	public NetworkFile getNetworkFile() {
		return networkFile;
	}
}
