package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.TwoNodeBackUpMessage;

public class DoubleBackUpHandler implements MessageHandler{
    private BasicMessage message;

    public DoubleBackUpHandler(BasicMessage message) {
        this.message = message;
    }


    @Override
    public void run() {
        if(message instanceof TwoNodeBackUpMessage){
            try{
                Thread.sleep(AppConfig.strongLimit + AppConfig.weakLimit/2);
            }
            catch (Exception e){
                AppConfig.timestampedErrorPrint(e.getMessage());
            }
            TwoNodeBackUpMessage twoNodeBackUpMessage = (TwoNodeBackUpMessage) message;
            if(AppConfig.chordState.isKeyMine(twoNodeBackUpMessage.networkFile.getHashedName())){
                AppConfig.chordState.insertFile(twoNodeBackUpMessage.networkFile,
                        twoNodeBackUpMessage.fileBytes,
                        twoNodeBackUpMessage.getSenderPort(),
                        twoNodeBackUpMessage.getSenderIpAddress());
            }
        }
        else{
            System.out.println("DoubleBackUpHandler got a message that is not TwoNodeBackUpMessage");
        }
    }
}
