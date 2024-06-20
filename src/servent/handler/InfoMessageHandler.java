package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.ErrorMessage;
import servent.message.InfoMessage;
import servent.message.util.MessageUtil;
import servent.model.NetworkFile;

public class InfoMessageHandler implements MessageHandler{
    private BasicMessage message;

    public InfoMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof InfoMessage){
            InfoMessage infoMessage = (InfoMessage) message;
            if(infoMessage.getFiles()!=null){
                String output = "Files on address "+ infoMessage.getSenderIpAddress() + ":" + infoMessage.getSenderPort() +": ";
                for(NetworkFile networkFile : infoMessage.getFiles()){
                    output += networkFile.getName() + ", ";
                }
                output = output.substring(0, output.length() - 2);
                AppConfig.timestampedStandardPrint(output);
                return;
            }
            if(infoMessage.getReceiverIpAddress().equals(AppConfig.myServentInfo.getIpAddress()) &&
                    infoMessage.getReceiverPort() == AppConfig.myServentInfo.getListenerPort()){
                InfoMessage answer = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.myServentInfo.getIpAddress(),
                        infoMessage.getSenderPort(),
                        infoMessage.getSenderIpAddress(),
                        AppConfig.chordState.getFilesForInfo(infoMessage.getFriends()),
                        infoMessage.getFriends());
                MessageUtil.sendMessage(answer);
            }
            else{
                if(AppConfig.chordState.isKeyMine(infoMessage.getReceiverIpAddress() + ":" +  infoMessage.getReceiverPort())){
                    ErrorMessage errorMessage = new ErrorMessage(infoMessage.getSenderPort(),
                            infoMessage.getSenderIpAddress(),
                            infoMessage.getReceiverPort(),
                            infoMessage.getReceiverIpAddress(),
                            "There is no address in the system: " + infoMessage.getReceiverIpAddress() + ":" +  infoMessage.getReceiverPort());
                    MessageUtil.sendMessage(errorMessage);
                }
                else {
                    InfoMessage forward = new InfoMessage(infoMessage.getSenderPort(),
                            infoMessage.getSenderIpAddress(),
                            AppConfig.chordState.getNextNodeForKey(infoMessage.getReceiverIpAddress() + ":" + infoMessage.getReceiverPort()).getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(infoMessage.getReceiverIpAddress() + ":" + infoMessage.getReceiverPort()).getIpAddress(),
                            infoMessage.getFriends());
                    MessageUtil.sendMessage(forward);
                }
            }
        }
        else{
            AppConfig.timestampedErrorPrint("Info message handler got a message that is not InfoMessage");
        }
    }
}
