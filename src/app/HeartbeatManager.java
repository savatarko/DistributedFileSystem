package app;

import servent.message.*;
import servent.message.util.MessageUtil;
import servent.model.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static app.ChordState.stopping;
import static app.ChordState.working;

public class HeartbeatManager implements Runnable{
    private List<ServentInfo> nodes = new ArrayList<>();
    private Map<ServentInfo, Long> lastAnswer = new ConcurrentHashMap<>();
    private Map<ServentInfo, Boolean> suspiciousNodes = new ConcurrentHashMap<>();
    private List<ServentInfo> heathyNodes = new ArrayList<>();
    public final Object heartbeatLock = new Object();
    public final Object suspiciousLock = new Object();
    private Map<ServentInfo, List<ServentInfo>> pendingMessages = new ConcurrentHashMap<>();
    public Map<ServentInfo, Map<ServentInfo, Boolean>> tokenWait = new ConcurrentHashMap<>();
    public Map<ServentInfo, Boolean> isMine = new ConcurrentHashMap<>();
    private Map<ServentInfo, Boolean> deadNodes = new ConcurrentHashMap<>();
    Random random = new Random();

    @Override
    public void run() {
        while(!stopping){
            synchronized (heartbeatLock) {
                for (ServentInfo serventInfo : nodes) {
                    lastAnswer.putIfAbsent(serventInfo, System.currentTimeMillis());
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint(e.getMessage());
            }

            for(var node:suspiciousNodes.keySet()){
                if(System.currentTimeMillis() - lastAnswer.get(node) > AppConfig.strongLimit && suspiciousNodes.get(node)){
                    AppConfig.chordState.removeNode(node.getListenerPort(), node.getIpAddress(), false);
                    suspiciousNodes.remove(node);
                    lastAnswer.remove(node);
                    nodes.remove(node);
                    deadNodes.put(node, true);
                    AppConfig.timestampedStandardPrint("Node " + node + " is dead, removing it from system.");
                    //TODO CHECK FOR TOKEN!!!
                    tokenWait.put(node, new ConcurrentHashMap<>());
                    isMine.putIfAbsent(node, true);
                    for(var serventInfo : nodes){
                        tokenWait.get(node).put(serventInfo, false);
                        GetLastTokenMessage getLastTokenMessage = new GetLastTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.myServentInfo.getIpAddress(),
                                serventInfo.getListenerPort(),
                                serventInfo.getIpAddress(),
                                node);
                        AppConfig.timestampedStandardPrint("Sending token history request to " + serventInfo + " for " + node);
                        MessageUtil.sendMessage(getLastTokenMessage);
                    }

                    for(var message : AppConfig.chordState.pendingMessages.get(node).keySet()){
                        BasicMessage message1 = AppConfig.chordState.pendingMessages.get(node).get(message);
                        message1.updateReceiver(AppConfig.chordState.getNextNodePort(), AppConfig.chordState.getNextNodeIp());
                        MessageUtil.sendMessage(message1);
                    }
                }
            }

            for(var node:lastAnswer.keySet()){
                if(System.currentTimeMillis() - lastAnswer.get(node) > AppConfig.weakLimit && !suspiciousNodes.containsKey(node)){
                    suspiciousNodes.putIfAbsent(node, false);
                    synchronized (heartbeatLock) {
                        if(!nodes.contains(node)){
                            continue;
                        }
                        heathyNodes.remove(node);
                        if(!heathyNodes.isEmpty()) {
                            int index = random.nextInt(heathyNodes.size());
                            ServentInfo target = heathyNodes.get(index);
                            NodeCheckMessage nodeCheckMessage = new NodeCheckMessage(AppConfig.myServentInfo.getListenerPort(),
                                    AppConfig.myServentInfo.getIpAddress(),
                                    target.getListenerPort(),
                                    target.getIpAddress(),
                                    node);
                            MessageUtil.sendMessage(nodeCheckMessage);
                            pendingMessages.get(target).add(node);
                        }
                        if(pendingMessages.containsKey(node)){
                            for(var pendingNode:pendingMessages.get(node)){
                                int index = random.nextInt(heathyNodes.size());
                                ServentInfo target = heathyNodes.get(index);
                                NodeCheckMessage nodeCheckMessage = new NodeCheckMessage(AppConfig.myServentInfo.getListenerPort(),
                                        AppConfig.myServentInfo.getIpAddress(),
                                        target.getListenerPort(),
                                        target.getIpAddress(),
                                        pendingNode);
                                MessageUtil.sendMessage(nodeCheckMessage);
                                pendingMessages.get(target).add(pendingNode);
                            }
                        }
                    }
                }
            }
        }
    }

    public void update(String address, int port){
        if(lastAnswer.containsKey(new ServentInfo(address, port))) {
            lastAnswer.put(new ServentInfo(address, port), System.currentTimeMillis());
        }
        suspiciousNodes.remove(new ServentInfo(address, port));
    }

    public void reportBack(String address, int port, Boolean isDead, String senderAddress, int senderPort){
        ServentInfo node = new ServentInfo(address, port);
        if(isDead){
            suspiciousNodes.put(node, true);
        }
        else{
            lastAnswer.put(node, System.currentTimeMillis());
            suspiciousNodes.remove(node);
            heathyNodes.add(node);
        }
        ServentInfo sender = new ServentInfo(senderAddress, senderPort);
        pendingMessages.get(sender).remove(node);
    }

    public boolean isDead(ServentInfo serventInfo){
        if(!nodes.contains(serventInfo)){
            return true;
        }
        return suspiciousNodes.containsKey(serventInfo);
    }

    public List<ServentInfo> getNodes() {
        return nodes;
    }

    public void addNode(ServentInfo serventInfo){
        synchronized (heartbeatLock) {
            if(serventInfo.equals(AppConfig.myServentInfo)){
                return;
            }
            nodes.add(serventInfo);
            heathyNodes.add(serventInfo);
            pendingMessages.put(serventInfo, new ArrayList<>());
            deadNodes.remove(serventInfo);
        }
    }

    public void removeNode(ServentInfo serventInfo){
        synchronized (heartbeatLock) {
            nodes.remove(serventInfo);
            suspiciousNodes.remove(serventInfo);
            lastAnswer.remove(serventInfo);
            heathyNodes.remove(serventInfo);
            deadNodes.put(serventInfo, true);
        }
    }

    public void tokenHistoryResponse(ServentInfo serventInfo, ServentInfo dead, Token tokenToCompare, Boolean isItHis) {
        tokenWait.get(dead).remove(serventInfo);
        if(isItHis){
            isMine.put(dead, false);
            return;
        }
        if(AppConfig.chordState.lastToken == null){
            isMine.put(dead, false);
            return;
        }
        if(!Token.compareTwoTokens(AppConfig.chordState.lastToken, tokenToCompare)){
            isMine.put(dead, false);
            return;
        }
        if(!isMine.get(dead)){
            return;
        }
        if(tokenWait.get(dead).isEmpty()){
            synchronized (AppConfig.tokenLock) {
                if (deadNodes.containsKey(AppConfig.chordState.sentTokenTo) &&
                        AppConfig.chordState.token == null &&
                        AppConfig.chordState.lastToken!=null) {
                    AppConfig.chordState.token = new Token(AppConfig.chordState.lastToken);
                    AppConfig.chordState.token.inUse = false;
                    AppConfig.chordState.lastToken = null;
                    AppConfig.timestampedStandardPrint("I am the new owner of the token.");
//                    AppConfig.chordState.token.versionVector.remove(dead);
                    AppConfig.chordState.token.queue.remove(dead);
                    for(ServentInfo serventInfo1 : AppConfig.chordState.versionVector.keySet()){
                        if(!AppConfig.chordState.token.versionVector.containsKey(serventInfo1)){
                            AppConfig.chordState.token.queue.add(serventInfo1);
                            AppConfig.chordState.token.versionVector.put(serventInfo1, AppConfig.chordState.versionVector.get(serventInfo1));
                        }
                        else{
                            if(AppConfig.chordState.versionVector.get(serventInfo1) == AppConfig.chordState.token.versionVector.get(serventInfo1) + 1){
                                AppConfig.chordState.token.queue.add(serventInfo1);
                            }
                        }
                    }
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
                    }
                }
            }
        }
    }
}
