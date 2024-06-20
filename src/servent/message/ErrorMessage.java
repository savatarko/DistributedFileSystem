package servent.message;

public class ErrorMessage extends BasicMessage{

    public ErrorMessage(int senderPort, String senderIpAdress,  int receiverPort, String receiverIpAddress, String message){
        super(MessageType.ERROR, senderPort, senderIpAdress, receiverPort, receiverIpAddress, message);
    }
}
