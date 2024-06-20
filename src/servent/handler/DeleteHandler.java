package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class DeleteHandler implements MessageHandler{
    private BasicMessage message;

    public DeleteHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof DeleteMessage){
            DeleteMessage deleteMessage = (DeleteMessage) message;
            if(AppConfig.chordState.isKeyMine(deleteMessage.getFileName())){
                AppConfig.chordState.removeFile(deleteMessage.getFileName(),
                        deleteMessage.getSenderPort(),
                        deleteMessage.getSenderIpAddress());
            }
            else {
                ServentInfo si = AppConfig.chordState.getNextNodeForKey(deleteMessage.getFileName());
                Message forwardMessage = new DeleteMessage(deleteMessage.getSenderPort(),
                        deleteMessage.getSenderIpAddress(),
                        si.getListenerPort(),
                        si.getIpAddress(),
                        deleteMessage.getFileName());
                AppConfig.timestampedStandardPrint("Forwarding file message for key " + deleteMessage.getFileName() + " to " + si.getListenerPort());
                MessageUtil.sendMessage(forwardMessage);
            }
        }
        else {
            AppConfig.timestampedErrorPrint("Delete handler got message that is not DeleteMessage");
        }
    }
}
