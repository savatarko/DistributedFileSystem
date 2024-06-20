package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.HeartbeatMessage;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class HeartbeatMessageHandler implements MessageHandler{
    private BasicMessage message;

    public HeartbeatMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if (message instanceof HeartbeatMessage) {
//            PongMessage pongMessage = new PongMessage(AppConfig.myServentInfo.getListenerPort(),
//                    AppConfig.myServentInfo.getIpAddress(),
//                    message.getSenderPort(),
//                    message.getSenderIpAddress());
//            MessageUtil.sendMessage(pongMessage);
            AppConfig.heartbeatManager.update(message.getSenderIpAddress(), message.getSenderPort());
        }
        else {
            AppConfig.timestampedErrorPrint("HeartbeatMessageHandler got a message that is not HeartbeatMessage");
        }
    }
}
