package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.RemoveFromBackupMessage;

public class RemoveFromBackupHandler implements MessageHandler{
    private BasicMessage message;

    public RemoveFromBackupHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof RemoveFromBackupMessage){
            AppConfig.chordState.removeFromBackup(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort()), ((RemoveFromBackupMessage) message).getNetworkFiles());
        }
        else{
            AppConfig.timestampedErrorPrint("RemoveFromBackupHandler got a message that is not REMOVE_FROM_BACKUP");
        }
    }
}
