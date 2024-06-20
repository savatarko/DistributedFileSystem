package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class PongMessageHandler implements MessageHandler{
    private BasicMessage message;

    public PongMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof PongMessage){
            //TODO: handle it
            PongMessage pongMessage = (PongMessage)message;
            AppConfig.connection = true;
            AppConfig.chordState.init(pongMessage);
//			AppConfig.chordState.versionVector = welcomeMsg.getVersionMap();
            //todo: add token lock
            int version = 1;
            if(pongMessage.getVersionMap().containsKey(AppConfig.myServentInfo)){
                version = pongMessage.getVersionMap().get(AppConfig.myServentInfo) + 1;
            }
            if(AppConfig.chordState.token == null) {
                for(ServentInfo serventInfo : pongMessage.getVersionMap().keySet()){
                    TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.myServentInfo.getIpAddress(),
                            serventInfo.getListenerPort(),
                            serventInfo.getIpAddress(),
                            AppConfig.myServentInfo,
                            version);
//					AppConfig.chordState.versionVector.put(serventInfo, 1);
                    MessageUtil.sendMessage(tokenRequestMessage);
//                    AppConfig.chordState.versionVector.putIfAbsent(serventInfo, pongMessage.getVersionMap().get(serventInfo));
                }
                AppConfig.chordState.versionVector.put(AppConfig.myServentInfo, 1);
                while (AppConfig.chordState.token == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                AppConfig.chordState.token.inUse = true;
            }
            AppConfig.chordState.versionVector.putIfAbsent(AppConfig.myServentInfo, 1);
            NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    pongMessage.getSenderPort(),
                    pongMessage.getSenderIpAddress());
            MessageUtil.sendMessage(nnm);

        }
        else{
            AppConfig.timestampedErrorPrint("PongMessageHandler got a message that is not PongMessage");
        }
    }
}
