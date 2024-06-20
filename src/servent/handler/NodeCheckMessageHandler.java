package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.NodeCheckMessage;
import servent.message.util.MessageUtil;

public class NodeCheckMessageHandler implements MessageHandler{
    private BasicMessage message;;

    public NodeCheckMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof NodeCheckMessage)
        {
            NodeCheckMessage nodeCheckMessage = (NodeCheckMessage) message;
            if(nodeCheckMessage.getDead() == null){
                try{
                    Thread.sleep(AppConfig.weakLimit/2);//nisam siguran koliko vreme da stavim ovde
                }
                catch (InterruptedException e){
                    AppConfig.timestampedErrorPrint(e.getMessage());
                }
                if(AppConfig.heartbeatManager.isDead(nodeCheckMessage.getNodeToCheck())){
                    NodeCheckMessage reply = new NodeCheckMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.myServentInfo.getIpAddress(),
                            nodeCheckMessage.getSenderPort(),
                            nodeCheckMessage.getSenderIpAddress(),
                            nodeCheckMessage.getNodeToCheck(),
                            true);
                    MessageUtil.sendMessage(reply);
                }
                return;
            }
            AppConfig.heartbeatManager.reportBack(nodeCheckMessage.getNodeToCheck().getIpAddress(),
                    nodeCheckMessage.getNodeToCheck().getListenerPort(),
                    nodeCheckMessage.getDead(),
                    nodeCheckMessage.getSenderIpAddress(),
                    nodeCheckMessage.getSenderPort());
        }
        else
        {
            AppConfig.timestampedErrorPrint("Node check handler got a message that is not NodeCheckMessage");
        }
    }
}
