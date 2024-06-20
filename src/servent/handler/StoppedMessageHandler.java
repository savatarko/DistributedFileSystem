package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.StoppedMessage;
import servent.message.util.MessageUtil;

import static app.ChordState.stopping;
import static app.ChordState.working;

public class StoppedMessageHandler implements MessageHandler{
    private BasicMessage message;

    public StoppedMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof StoppedMessage){
            if(message.getSenderIpAddress().equals(message.getReceiverIpAddress()) && message.getSenderPort() == message.getReceiverPort()){
                working = false;
                if(AppConfig.chordState.token != null){
                    AppConfig.chordState.token.inUse = false;
                }
            }
            else{
                try {
                    StoppedMessage stoppedMessage = new StoppedMessage(message.getSenderPort(),
                            message.getSenderIpAddress(),
                            AppConfig.chordState.getNextNodePort(),
                            AppConfig.chordState.getNextNodeIp());
                    MessageUtil.sendMessage(stoppedMessage);
                    AppConfig.chordState.removeNode(message.getSenderPort(), message.getSenderIpAddress(), true);
                    AppConfig.heartbeatManager.removeNode(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort()));
//                    AppConfig.chordState.versionVector.remove(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort()));
                }
                catch (Exception e){
                    AppConfig.timestampedErrorPrint(e.getMessage());
                }
            }
        }
        else{
            AppConfig.timestampedErrorPrint("StoppedMessageHandler got a message that is not StoppedMessage");
        }

    }
}
