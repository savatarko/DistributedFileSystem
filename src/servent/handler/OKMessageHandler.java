package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.OKMessage;

public class OKMessageHandler implements MessageHandler{
    private BasicMessage message;

    public OKMessageHandler(BasicMessage message) {
        this.message = message;
    }


    @Override
    public void run() {
        if(message instanceof OKMessage){
            AppConfig.chordState.pendingMessages.get(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort())).remove(Integer.valueOf(message.getMessageText()));
        }
        else{
            AppConfig.timestampedErrorPrint("OK message handler got a message that is not OK message");
        }
    }
}
