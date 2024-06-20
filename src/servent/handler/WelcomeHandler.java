package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class WelcomeHandler implements MessageHandler {

	private Message clientMessage;
	
	public WelcomeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.WELCOME) {
			WelcomeMessage welcomeMsg = (WelcomeMessage)clientMessage;
			
			AppConfig.chordState.init(welcomeMsg);
//			AppConfig.chordState.versionVector = welcomeMsg.getVersionMap();
			//todo: add token lock
			if(AppConfig.chordState.token == null) {
				for(ServentInfo serventInfo : welcomeMsg.getVersionMap().keySet()){
					TokenRequestMessage tokenRequestMessage = new TokenRequestMessage(AppConfig.myServentInfo.getListenerPort(),
							AppConfig.myServentInfo.getIpAddress(),
							serventInfo.getListenerPort(),
							serventInfo.getIpAddress(),
							AppConfig.myServentInfo,
							1);
//					AppConfig.chordState.versionVector.put(serventInfo, 1);
					MessageUtil.sendMessage(tokenRequestMessage);
				}
//				AppConfig.chordState.versionVector.put(AppConfig.myServentInfo, 1);
				while (AppConfig.chordState.token == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				AppConfig.chordState.token.inUse = true;
			}
			AppConfig.chordState.versionVector.putIfAbsent(AppConfig.myServentInfo, 1);
			UpdateMessage um = new UpdateMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					AppConfig.chordState.getNextNodePort(),
					AppConfig.chordState.getNextNodeIp(),
					"",
					AppConfig.chordState.versionVector);
			MessageUtil.sendMessage(um);
			
		} else {
			AppConfig.timestampedErrorPrint("Welcome handler got a message that is not WELCOME");
		}

	}

}
