package servent.message;

import app.ServentInfo;

import java.util.Map;

public class PongMessage extends BasicMessage{

    private Map<ServentInfo, Integer> versionMap;

    public PongMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, Map<ServentInfo, Integer> versionMap) {
        super(MessageType.PONG, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.versionMap = versionMap;
    }

    public Map<ServentInfo, Integer> getVersionMap() {
        return versionMap;
    }
}
