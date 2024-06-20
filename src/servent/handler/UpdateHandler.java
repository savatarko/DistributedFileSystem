package servent.handler;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import app.AppConfig;
import app.HashingUtil;
import app.HeartbeatSender;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private BasicMessage clientMessage;
	
	public UpdateHandler(BasicMessage clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				try {
					ServentInfo newNodInfo = new ServentInfo(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());
//					if(clientMessage.getMessageText().equals("")){
//						AppConfig.chordState.sendFilesToNode(clientMessage.getSenderPort(), clientMessage.getSenderIpAddress());
//					}
//					if(!AppConfig.chordState.isKeyMine(newNodInfo.getChordId())) {
//						UpdateMessage updateMessage = new UpdateMessage(clientMessage.getSenderPort(),
//								clientMessage.getSenderIpAddress(),
//								AppConfig.chordState.getPredecessor().getListenerPort(),
//								AppConfig.chordState.getPredecessor().getIpAddress(),
//								clientMessage.getMessageText(),
//								((UpdateMessage) clientMessage).getVersionMap());
//						MessageUtil.sendMessage(updateMessage);
//						return;
//					}
					List<ServentInfo> newNodes = new ArrayList<>();
//					if(clientMessage.getMessageText().equals("")){
//						AppConfig.chordState.setPredecessor(new ServentInfo(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort()));
//					}
					newNodes.add(newNodInfo);
					AppConfig.heartbeatManager.addNode(newNodInfo);

					if(!AppConfig.chordState.containsNode(newNodInfo)) {
						synchronized (AppConfig.chordState.nodeLock) {
							AppConfig.chordState.addNodes(newNodes);
						}
					}
					String newMessageText = "";
					if (clientMessage.getMessageText().equals("")) {
						newMessageText =AppConfig.myServentInfo.getIpAddress() + ":"  + AppConfig.myServentInfo.getListenerPort();
					} else {
						newMessageText = clientMessage.getMessageText() + "," +
								AppConfig.myServentInfo.getIpAddress() + ":"  + AppConfig.myServentInfo.getListenerPort();
					}
					for (ServentInfo node : ((UpdateMessage) clientMessage).getVersionMap().keySet()) {
						AppConfig.chordState.versionVector.putIfAbsent(node, ((UpdateMessage) clientMessage).getVersionMap().get(node));
					}
					Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(),
							clientMessage.getSenderIpAddress(),
							AppConfig.chordState.getNextNodePort(),
							AppConfig.chordState.getNextNodeIp(),
							newMessageText,
							AppConfig.chordState.versionVector);
					MessageUtil.sendMessage(nextUpdate);
				}
				catch (Exception e){
					AppConfig.timestampedErrorPrint(e.getMessage());
				}
			} else {
				String messageText = clientMessage.getMessageText();
				String[] addresses = messageText.split(",");
				
				List<ServentInfo> allNodes = new ArrayList<>();
				for (String address : addresses) {
					String[] split = address.split(":");
					allNodes.add(new ServentInfo(split[0], Integer.parseInt(split[1])));
				}

				synchronized (AppConfig.chordState.nodeLock) {
					AppConfig.chordState.addNodes(allNodes);
				}
				for(ServentInfo node: allNodes)
				{
					if(!node.equals(AppConfig.myServentInfo)) {
						AppConfig.heartbeatManager.addNode(node);
					}
				}
				RequestMyFilesMessage requestMyFilesMessage = new RequestMyFilesMessage(AppConfig.myServentInfo.getListenerPort(),
						AppConfig.myServentInfo.getIpAddress(),
						AppConfig.chordState.getNextNodePort(),
						AppConfig.chordState.getNextNodeIp(),
						AppConfig.chordState.getPredecessor().getChordId());
				MessageUtil.sendMessage(requestMyFilesMessage);
				for(ServentInfo node: ((UpdateMessage)clientMessage).getVersionMap().keySet()){
					AppConfig.chordState.versionVector.putIfAbsent(node, ((UpdateMessage)clientMessage).getVersionMap().get(node));
				}
				if(AppConfig.chordState.token !=null) {
					AppConfig.chordState.token.inUse = false;
				}
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
