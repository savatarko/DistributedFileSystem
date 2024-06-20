package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.TokenMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;
import servent.model.Token;

public class TokenRequestHandler implements MessageHandler{
    private BasicMessage message;
//    private final Object lock = new Object();

    public TokenRequestHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof TokenRequestMessage){
            synchronized (AppConfig.tokenLock) {
                TokenRequestMessage tokenRequestMessage = (TokenRequestMessage) message;
                AppConfig.chordState.versionVector.putIfAbsent(tokenRequestMessage.getServentInfo(), 0);
                AppConfig.chordState.versionVector.put(
                        tokenRequestMessage.getServentInfo(),
                        Math.max(
                                AppConfig.chordState.versionVector.get(tokenRequestMessage.getServentInfo()),
                                tokenRequestMessage.getVersion()
                        ));
                if (AppConfig.chordState.token != null) {
                    if (!AppConfig.chordState.token.inUse) {
                        AppConfig.chordState.token.inUse = true;
                        AppConfig.chordState.token.versionVector.put(
                                tokenRequestMessage.getServentInfo(),
                                AppConfig.chordState.versionVector.get(tokenRequestMessage.getServentInfo())
                        );
                        Token tokenCopy = new Token(AppConfig.chordState.token);
                        AppConfig.chordState.lastToken = tokenCopy;
                        AppConfig.chordState.sentTokenTo = tokenRequestMessage.getServentInfo();

                        TokenMessage tokenMessage = new TokenMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.myServentInfo.getIpAddress(),
                                tokenRequestMessage.getSenderPort(),
                                tokenRequestMessage.getSenderIpAddress(),
                                tokenCopy);
                        AppConfig.chordState.token = null;
                        MessageUtil.sendMessage(tokenMessage);
                    }
                }
            }
        }
        else{
            AppConfig.timestampedErrorPrint("TokenRequestHandler got a message that is not TokenRequestMessage");
        }

    }
}
