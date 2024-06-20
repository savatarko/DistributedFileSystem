package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.ErrorMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class ErrorHandler implements MessageHandler{
    private Message errorMessage;

    public ErrorHandler(Message errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void run() {
        if(errorMessage instanceof ErrorMessage){
            //TODO: ovo bi trebalo da radi?
            AppConfig.timestampedErrorPrint(errorMessage.getMessageText());
        }
        else {
            AppConfig.timestampedErrorPrint("Error message handler got a message that is not ERROR_MESSAGE");
        }
    }
}
