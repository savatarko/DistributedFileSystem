package servent.message;

import servent.model.Token;

public class TokenMessage extends BasicMessage{
    public Token token;

    public TokenMessage(int senderPort, String senderIpAdress, int receiverPort, String receiverIpAddress, Token token) {
        super(MessageType.TOKEN, senderPort, senderIpAdress, receiverPort, receiverIpAddress);
        this.token = token;
    }
}
