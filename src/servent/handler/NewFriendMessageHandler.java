package servent.handler;

import app.AppConfig;
import app.HashingUtil;
import servent.message.BasicMessage;
import servent.message.ErrorMessage;
import servent.message.NewFriendMessage;
import servent.message.util.MessageUtil;

public class NewFriendMessageHandler implements MessageHandler{
    private BasicMessage message;

    public NewFriendMessageHandler(BasicMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        if(message instanceof NewFriendMessage){
            if(message.getMessageText().isEmpty()){
                String addressHashed = HashingUtil.SHA1(message.getSenderIpAddress() + ":" + message.getSenderPort());
                if(message.getReceiverPort() == AppConfig.myServentInfo.getListenerPort() &&
                message.getReceiverIpAddress().equals(AppConfig.myServentInfo.getIpAddress())){
                    AppConfig.chordState.addFriend(addressHashed);
                    String myAdressHashed = HashingUtil.SHA1(AppConfig.myServentInfo.getIpAddress() + ":" + AppConfig.myServentInfo.getListenerPort());
                    NewFriendMessage newFriendMessage = new NewFriendMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.myServentInfo.getIpAddress(),
                            message.getSenderPort(),
                            message.getSenderIpAddress(),
                            myAdressHashed);
                    MessageUtil.sendMessage(newFriendMessage);
                    return;
                }
                if(AppConfig.chordState.isKeyMine(addressHashed)){
                    ErrorMessage errorMessage = new ErrorMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.myServentInfo.getIpAddress(),
                            message.getSenderPort(),
                            message.getSenderIpAddress(),
                            "There is no address in the system: " + message.getReceiverIpAddress() + ":" +  message.getReceiverPort());
                    MessageUtil.sendMessage(errorMessage);
                }
                else{
                    NewFriendMessage forward = new NewFriendMessage(message.getSenderPort(),
                            message.getSenderIpAddress(),
                            AppConfig.chordState.getNextNodeForKey(addressHashed).getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(addressHashed).getIpAddress());
                    MessageUtil.sendMessage(forward);
                }
            }
            else{
                AppConfig.chordState.addFriend(message.getMessageText());
            }
        }
    }
}
