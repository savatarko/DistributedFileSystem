package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BackupMessage;
import servent.message.BasicMessage;

public class BackupMessageHandler implements MessageHandler{
    private BasicMessage message;

    public BackupMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof BackupMessage){
            AppConfig.chordState.addBackup(new ServentInfo(message.getSenderIpAddress(), message.getSenderPort()),
                    ((BackupMessage) message).networkFile,
                    ((BackupMessage) message).bytes);
        }
        else{
            AppConfig.timestampedErrorPrint("BackupMessageHandler got a message that is not BACKUP");
        }
    }
}
