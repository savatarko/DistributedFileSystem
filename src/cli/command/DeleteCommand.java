package cli.command;

import app.AppConfig;
import app.HashingUtil;
import servent.message.DeleteMessage;
import servent.message.util.MessageUtil;

public class DeleteCommand implements CLICommand{
    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if(splitArgs.length == 1){
            String fileName = splitArgs[0];
            String fileHashed = HashingUtil.SHA1(fileName);
            if(AppConfig.chordState.isKeyMine(fileHashed)){
                AppConfig.chordState.removeFile(fileHashed, AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getIpAddress());
            }
            else {
                DeleteMessage deleteMessage = new DeleteMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.myServentInfo.getIpAddress(),
                        AppConfig.chordState.getNextNodeForKey(fileHashed).getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(fileHashed).getIpAddress(),
                        fileHashed);
                AppConfig.timestampedStandardPrint("Forwarding file message for key " + fileHashed + " to " + AppConfig.chordState.getNextNodeForKey(fileHashed).getListenerPort());
                MessageUtil.sendMessage(deleteMessage);
            }
        }
        else {
            AppConfig.timestampedErrorPrint("Invalid argument for remove_file: " + args + ". Should be file name.");
        }
    }
}
