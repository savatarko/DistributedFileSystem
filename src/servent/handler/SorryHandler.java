package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.util.MessageUtil;

public class SorryHandler implements MessageHandler {

	private BasicMessage clientMessage;
	
	public SorryHandler(BasicMessage clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SORRY) {
			AppConfig.timestampedStandardPrint("Couldn't enter Chord system because of collision. Change my listener port, please.");
			if(AppConfig.chordState.token!=null){
				TokenMessage tokenMessage = new TokenMessage(AppConfig.myServentInfo.getListenerPort(),
						AppConfig.myServentInfo.getIpAddress(),
						clientMessage.getSenderPort(),
						clientMessage.getSenderIpAddress(),
						AppConfig.chordState.token);
				MessageUtil.sendMessage(tokenMessage);
			}
			System.exit(0);
		} else {
			AppConfig.timestampedErrorPrint("Sorry handler got a message that is not SORRY");
		}

	}

}
