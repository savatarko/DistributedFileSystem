package servent.model;

import app.AppConfig;
import app.ServentInfo;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Token implements Serializable {
    public Map<ServentInfo, Integer> versionVector;
    public Queue<ServentInfo> queue;
    public Boolean inUse;

    public Token(ServentInfo firstNode){
        queue = new LinkedList<>();
        versionVector = new HashMap<>();
        versionVector.put(firstNode, 1);
        inUse = false;
    }

    public Token(Token token) {
        this.versionVector = new HashMap<>(token.versionVector);
        this.queue = new LinkedList<>(token.queue);
        this.inUse = token.inUse;
    }

    public static void requestToken(){
        AppConfig.chordState.versionVector.put(AppConfig.myServentInfo, AppConfig.chordState.versionVector.get(AppConfig.myServentInfo) + 1);
        for (ServentInfo serventInfo : AppConfig.chordState.versionVector.keySet()) {
            if(serventInfo.equals(AppConfig.myServentInfo))
                continue;
            TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    serventInfo.getListenerPort(),
                    serventInfo.getIpAddress(),
                    AppConfig.myServentInfo,
                    AppConfig.chordState.versionVector.get(AppConfig.myServentInfo));
            MessageUtil.sendMessage(tokenRequestMessage);
        }
    }

    public static Boolean compareTwoTokens(Token x, Token y){
        if(x.versionVector.keySet().size() > y.versionVector.keySet().size()){
            return true;
        }
        if(x.versionVector.keySet().size() < y.versionVector.keySet().size()){
            return false;
        }
        for(ServentInfo serventInfo : x.versionVector.keySet()) {
            if(!y.versionVector.containsKey(serventInfo)){
                return true;
            }
            if(x.versionVector.get(serventInfo) > y.versionVector.get(serventInfo)){
                return true;
            }
            if(x.versionVector.get(serventInfo) < y.versionVector.get(serventInfo)){
                return false;
            }
        }
        return x.queue.size() < y.queue.size();
    }
}
