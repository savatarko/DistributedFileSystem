package servent.message;

import app.ServentInfo;

public class NodeCheckMessage extends BasicMessage{
    private Boolean isDead;
    private ServentInfo nodeToCheck;

    public NodeCheckMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, ServentInfo nodeToCheck) {
        super(MessageType.NODE_CHECK, senderPort, senderIpAddress, receiverPort, receiverIpAddress, nodeToCheck.toString());
        this.nodeToCheck = nodeToCheck;
        isDead = null;
    }

    public NodeCheckMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, ServentInfo nodeToCheck, Boolean isDead) {
        super(MessageType.NODE_CHECK, senderPort, senderIpAddress, receiverPort, receiverIpAddress, nodeToCheck.toString());
        this.isDead = isDead;
        this.nodeToCheck = nodeToCheck;
    }

    public Boolean getDead() {
        return isDead;
    }

    public ServentInfo getNodeToCheck() {
        return nodeToCheck;
    }
}
