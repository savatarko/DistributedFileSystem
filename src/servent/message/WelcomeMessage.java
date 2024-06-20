package servent.message;

import app.ServentInfo;
import servent.model.NetworkFile;

import java.util.List;
import java.util.Map;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<ServentInfo, Integer> versionMap;
	
	public WelcomeMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAddress, Map<ServentInfo, Integer> versionMap) {
		super(MessageType.WELCOME, senderPort, senderIpAdress,  receiverPort, receiverIpAddress);

		this.versionMap = versionMap;
	}


	public Map<ServentInfo, Integer> getVersionMap() {
		return versionMap;
	}
}
