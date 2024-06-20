package servent.message;

public class RequestMyFilesMessage extends BasicMessage{

        public RequestMyFilesMessage(int senderPort, String senderIp, int receiverPort, String receiverIp) {
            super(MessageType.REQUEST_FILES, senderPort, senderIp, receiverPort, receiverIp, "");
        }

        public RequestMyFilesMessage(int senderPort, String senderIp, int receiverPort, String receiverIp, String messageText) {
            super(MessageType.REQUEST_FILES, senderPort, senderIp, receiverPort, receiverIp, messageText);
        }
}
