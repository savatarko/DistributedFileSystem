package servent.message;

import app.ServentInfo;

public class TokenRequestMessage extends BasicMessage{
    private ServentInfo serventInfo;
    private Integer version;

    public TokenRequestMessage(int senderPort, String senderIpAddress, int receiverPort, String receiverIpAddress, ServentInfo serventInfo, Integer version) {
        super(MessageType.TOKEN_REQUEST, senderPort, senderIpAddress, receiverPort, receiverIpAddress);
        this.serventInfo = serventInfo;
        this.version = version;
    }

    public ServentInfo getServentInfo() {
        return serventInfo;
    }

    public Integer getVersion() {
        return version;
    }
}
