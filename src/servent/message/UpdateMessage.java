package servent.message;

import app.ServentInfo;

import java.util.Map;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	private Map<ServentInfo, Integer> versionMap;

	public UpdateMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAddress, String text, Map<ServentInfo, Integer> versionMap) {
		super(MessageType.UPDATE, senderPort, senderIpAdress, receiverPort, receiverIpAddress, text);
		this.versionMap = versionMap;
	}

	public Map<ServentInfo, Integer> getVersionMap() {
		return versionMap;
	}
}
