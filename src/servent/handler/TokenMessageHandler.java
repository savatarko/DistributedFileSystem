package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.TokenMessage;
import servent.message.util.MessageUtil;
import servent.model.Token;

import static app.ChordState.working;

public class TokenMessageHandler implements MessageHandler{
    private BasicMessage message;

    public TokenMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof TokenMessage){
            TokenMessage tokenMessage = (TokenMessage) message;
//            tokenMessage.token.inUse = true;
            AppConfig.chordState.token = tokenMessage.token;
            while(AppConfig.chordState.token.inUse){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for(ServentInfo serventInfo : AppConfig.chordState.versionVector.keySet()){
                if(!AppConfig.chordState.token.versionVector.containsKey(serventInfo)){
                    AppConfig.chordState.token.queue.add(serventInfo);
                    AppConfig.chordState.token.versionVector.put(serventInfo, AppConfig.chordState.versionVector.get(serventInfo));
                }
                else{
                    if(AppConfig.chordState.versionVector.get(serventInfo) == AppConfig.chordState.token.versionVector.get(serventInfo) + 1){
                        AppConfig.chordState.token.queue.add(serventInfo);
                    }
                }
//                AppConfig.chordState.token.versionVector.put(serventInfo,
//                        Math.max(
//                                AppConfig.chordState.versionVector.get(serventInfo),
//                                AppConfig.chordState.token.versionVector.get(serventInfo)));
//                AppConfig.chordState.token.versionVector.put(serventInfo, AppConfig.chordState.versionVector.get(serventInfo));
//                AppConfig.timestampedStandardPrint("PUTTING INTO TOKEN " + serventInfo.toString() + " " + AppConfig.chordState.versionVector.get(serventInfo) + " " + AppConfig.chordState.token.versionVector.get(serventInfo));

            }
            AppConfig.chordState.token.versionVector.put(AppConfig.myServentInfo, AppConfig.chordState.versionVector.get(AppConfig.myServentInfo));
            if(AppConfig.chordState.token.queue.peek() != null){
                AppConfig.chordState.token.inUse = true;
                ServentInfo nextServent = AppConfig.chordState.token.queue.poll();
                Token tokenCopy = new Token(AppConfig.chordState.token);
                AppConfig.chordState.lastToken = tokenCopy;
                AppConfig.chordState.sentTokenTo = nextServent;
                TokenMessage newTokenMessage = new TokenMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.myServentInfo.getIpAddress(),
                        nextServent.getListenerPort(),
                        nextServent.getIpAddress(),
                        tokenCopy);
                AppConfig.chordState.token = null;
                MessageUtil.sendMessage(newTokenMessage);
                return;
            }
            if (!working){
                AppConfig.chordState.token.inUse = false;
//                AppConfig.chordState.token.versionVector.remove(AppConfig.myServentInfo);
                Token tokenCopy = new Token(AppConfig.chordState.token);
                AppConfig.chordState.lastToken = tokenCopy;
                TokenMessage tokenMessage1 = new TokenMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.myServentInfo.getIpAddress(),
                        AppConfig.chordState.getNextNodePort(),
                        AppConfig.chordState.getNextNodeIp(),
                        tokenCopy);
                MessageUtil.sendMessage(tokenMessage1);
                AppConfig.chordState.token = null;
            }
        }
        else{
            AppConfig.timestampedErrorPrint("Token message handler got a message that is not TOKEN");
        }
    }
}
