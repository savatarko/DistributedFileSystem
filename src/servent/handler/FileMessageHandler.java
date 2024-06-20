package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BackupMessage;
import servent.message.FileMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class FileMessageHandler implements MessageHandler{
    private Message fileMessage;

    public FileMessageHandler(Message fileMessage) {
        this.fileMessage = fileMessage;
    }

    @Override
    public void run() {
        if(fileMessage.getMessageType() == MessageType.FILE){
            FileMessage fm = (FileMessage)fileMessage;
            if(AppConfig.chordState.isKeyMine(fm.networkFile.getHashedName())){
                AppConfig.chordState.insertFile(fm.networkFile, fm.bytes, fm.getSenderPort(), fm.getSenderIpAddress());
            }
            else{
                ServentInfo si = AppConfig.chordState.getNextNodeForKey(fm.networkFile.getHashedName());
                Message forwardMessage = new FileMessage(fm.getSenderPort(),
                        fm.getSenderIpAddress(),
                        si.getListenerPort(),
                        si.getIpAddress(),
                        fm.networkFile,
                        fm.bytes);
                AppConfig.timestampedStandardPrint("Forwarding file message for key " + fm.networkFile.getHashedName() + " to " + si.getListenerPort());
                MessageUtil.sendMessage(forwardMessage);
            }
        }
        else {
            AppConfig.timestampedErrorPrint("File message handler got a message that is not FILE_MESSAGE");
        }

    }
}
