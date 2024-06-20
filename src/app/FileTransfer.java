package app;

import servent.message.BackupMessage;
import servent.message.FileMessage;
import servent.message.util.MessageUtil;
import servent.model.NetworkFile;

import java.io.File;
import java.io.FileInputStream;

public class FileTransfer implements Runnable{
    private NetworkFile networkFile;
    private int receiverPort;
    private String receiverIpAddress;
    private Boolean backup;

    public FileTransfer(NetworkFile networkFile, int receiverPort, String receiverIpAddress, Boolean backup) {
        this.networkFile = networkFile;
        this.receiverPort = receiverPort;
        this.receiverIpAddress = receiverIpAddress;
        this.backup = backup;
    }

    @Override
    public void run() {
        File file = new File(AppConfig.rootDirectory + "/" + networkFile.getName());
        if(!file.exists()){
            AppConfig.timestampedErrorPrint("File " + networkFile.getName() + " does not exist.");
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
        if(backup){
            BackupMessage backupMessage = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    receiverPort,
                    receiverIpAddress,
                    networkFile,
                    fileBytes);
            MessageUtil.sendMessage(backupMessage);
            return;
        }
        FileMessage fileMessage = new FileMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.myServentInfo.getIpAddress(),
                receiverPort,
                receiverIpAddress,
                networkFile,
                fileBytes);
        MessageUtil.sendMessage(fileMessage);
//        try{
//            Thread.sleep(1000);
//        }
//        catch (Exception e){
//            AppConfig.timestampedErrorPrint("Error sleeping: " + e.getMessage());
//        }
        AppConfig.chordState.fileUsage.put(networkFile.getHashedName(), AppConfig.chordState.fileUsage.get(networkFile.getHashedName()) - 1);
        if(AppConfig.chordState.fileUsage.get(networkFile.getHashedName()) == 0){
            AppConfig.chordState.fileUsage.remove(networkFile.getHashedName());
            File fileTmp = new File(AppConfig.rootDirectory + "/" + file.getName());
            if(fileTmp.delete()){
                AppConfig.timestampedStandardPrint("File " + fileTmp.getAbsolutePath() + " deleted");
            }
            else{
                AppConfig.timestampedErrorPrint("File " + file.getAbsolutePath() + " could not be deleted");
            }
        }
//        file.delete();
    }
}
