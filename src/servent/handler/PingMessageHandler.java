package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.PingMessage;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class PingMessageHandler implements MessageHandler{
    private BasicMessage message;

    public PingMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof PingMessage){
            PongMessage pongMessage = new PongMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    message.getSenderPort(),
                    message.getSenderIpAddress(),
                    AppConfig.chordState.versionVector);
            MessageUtil.sendMessage(pongMessage);
        }
        else {
            AppConfig.timestampedErrorPrint("PingMessageHandler got a message that is not PingMessage");
        }

    }
}
