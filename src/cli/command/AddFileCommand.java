package cli.command;

import app.AppConfig;
import app.HashingUtil;
import servent.message.FileMessage;
import servent.message.util.MessageUtil;
import servent.model.NetworkFile;

import java.io.File;
import java.io.FileInputStream;

public class AddFileCommand implements CLICommand {
    @Override
    public String commandName() {
        return "add_file";
    }

    @Override
    public void execute(String args) {
        String[] split = args.split(" ");
        String fileName = split[0];
        File file = new File(fileName);
        if(!file.exists()){
            AppConfig.timestampedErrorPrint("File" + fileName + " does not exist.");
            return;
        }
        byte[] fileBytes = new byte[(int)file.length()];
        try{
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();
        }
        catch (Exception e){
            AppConfig.timestampedErrorPrint("Error reading file: " + e.getMessage());
            return;
        }
        if(split.length == 1){
            NetworkFile networkFile = new NetworkFile(file.getName());
            checkIfMine(fileBytes, networkFile);
        }
        else if(split.length == 2){
            String isFriend = split[1];
            NetworkFile networkFile;
            if(isFriend.equalsIgnoreCase("private")) {
                networkFile = new NetworkFile(file.getName(),
                        HashingUtil.SHA1(AppConfig.myServentInfo.getIpAddress() + ":" + AppConfig.myServentInfo.getListenerPort()));
            }
            else{
                networkFile = new NetworkFile(file.getName());
            }
            checkIfMine(fileBytes, networkFile);
        }
    }

    private void checkIfMine(byte[] fileBytes, NetworkFile networkFile) {
        if(AppConfig.chordState.isKeyMine(networkFile.getHashedName())){
            AppConfig.chordState.insertFile(networkFile, fileBytes, AppConfig.myServentInfo.getListenerPort(), "");
        }
        else{
            FileMessage fileMessage = new FileMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    AppConfig.chordState.getNextNodeForKey(networkFile.getHashedName()).getListenerPort(),
                    AppConfig.chordState.getNextNodeForKey(networkFile.getHashedName()).getIpAddress(),
                    networkFile,
                    fileBytes);
            AppConfig.timestampedStandardPrint("Sending file message to " + fileMessage.getReceiverPort());
            MessageUtil.sendMessage(fileMessage);
        }
    }
}
