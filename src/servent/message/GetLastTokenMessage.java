package servent.message;

import app.ServentInfo;
import servent.model.Token;

public class GetLastTokenMessage extends BasicMessage {

    private Token lastToken;
    private Boolean iOwnIt = null;
    private ServentInfo dead;
    public GetLastTokenMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress) {
        super(MessageType.GET_LAST_TOKEN, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
    }

    public GetLastTokenMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, Token token, ServentInfo dead) {
        super(MessageType.GET_LAST_TOKEN, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.lastToken = token;
        this.iOwnIt = false;
        this.dead = dead;
    }

    public GetLastTokenMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, Boolean mine, ServentInfo dead) {
        super(MessageType.GET_LAST_TOKEN, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.iOwnIt = mine;
        this.dead = dead;
    }

    public GetLastTokenMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, ServentInfo dead) {
        super(MessageType.GET_LAST_TOKEN, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.dead = dead;
    }

    public Token getLastToken() {
        return lastToken;
    }

    public Boolean getiOwnIt() {
        return iOwnIt;
    }

    public ServentInfo getDead() {
        return dead;
    }
}
