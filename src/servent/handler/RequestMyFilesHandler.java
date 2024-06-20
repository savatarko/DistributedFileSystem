package servent.handler;

import app.AppConfig;
import app.HashingUtil;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.RequestMyFilesMessage;

public class RequestMyFilesHandler implements MessageHandler{
    private BasicMessage message;

    public RequestMyFilesHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof RequestMyFilesMessage){
            String hisPredId = message.getMessageText();
            AppConfig.chordState.sendFilesToNode(message.getSenderPort(), message.getSenderIpAddress(), hisPredId);
            AppConfig.chordState.sendBackup(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort()));
        }
        else{
            AppConfig.timestampedErrorPrint("RequestMyFilesHandler got a message that is not REQUEST_FILES");
        }
    }
}
