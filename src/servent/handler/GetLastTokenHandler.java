package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.GetLastTokenMessage;
import servent.message.util.MessageUtil;

import static app.AppConfig.tokenLock;

public class GetLastTokenHandler implements MessageHandler{
    private BasicMessage message;

    public GetLastTokenHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof GetLastTokenMessage){
            GetLastTokenMessage getLastTokenMessage = (GetLastTokenMessage) message;
            if(getLastTokenMessage.getiOwnIt() == null){
                synchronized (tokenLock){
                    if(AppConfig.chordState.token !=null){
                        GetLastTokenMessage response = new GetLastTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.myServentInfo.getIpAddress(),
                                getLastTokenMessage.getSenderPort(),
                                getLastTokenMessage.getSenderIpAddress(),
                                true,
                                getLastTokenMessage.getDead());
                        MessageUtil.sendMessage(response);
                    }
                    else{
                        GetLastTokenMessage response = new GetLastTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.myServentInfo.getIpAddress(),
                                getLastTokenMessage.getSenderPort(),
                                getLastTokenMessage.getSenderIpAddress(),
                                AppConfig.chordState.lastToken,
                                getLastTokenMessage.getDead());
                        MessageUtil.sendMessage(response);
                    }
                }
            }
            else{
                ServentInfo serventInfo = new ServentInfo(getLastTokenMessage.getSenderIpAddress(), getLastTokenMessage.getSenderPort());
                AppConfig.heartbeatManager.tokenHistoryResponse(
                        serventInfo,
                        getLastTokenMessage.getDead(),
                        getLastTokenMessage.getLastToken(),
                        getLastTokenMessage.getiOwnIt()
                );
            }
        }
        else{
            AppConfig.timestampedErrorPrint("GetLastTokenHandler got a message that is not GetLastTokenMessage");
        }
    }
}
