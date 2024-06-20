package servent.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;
import servent.message.util.MessageUtil;
import servent.model.NetworkFile;

public class NewNodeHandler implements MessageHandler {

	private BasicMessage clientMessage;
	
	public NewNodeHandler(BasicMessage clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
			int newNodePort = clientMessage.getSenderPort();
			ServentInfo newNodeInfo = new ServentInfo(clientMessage.getSenderIpAddress(), newNodePort);
			
			//check if the new node collides with another existing node.
			synchronized (AppConfig.chordState.nodeLock) {
				try {
					if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
						Message sry = new SorryMessage(AppConfig.myServentInfo.getListenerPort(),
								AppConfig.myServentInfo.getIpAddress(),
								clientMessage.getSenderPort(),
								clientMessage.getSenderIpAddress());
						MessageUtil.sendMessage(sry);
						return;
					}

					//check if he is my predecessor
					boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
					if (isMyPred) { //if yes, prepare and send welcome message
						ServentInfo hisPred = AppConfig.chordState.getPredecessor();
						if (hisPred == null) {
							hisPred = AppConfig.myServentInfo;
						}

//						AppConfig.chordState.setPredecessor(newNodeInfo);

//						AppConfig.timestampedStandardPrint("Sending welcome message to " + newNodePort);
//						WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo.getListenerPort(),
//								AppConfig.myServentInfo.getIpAddress(),
//								newNodePort,
//								clientMessage.getSenderIpAddress(),
//								AppConfig.chordState.versionVector);
//						MessageUtil.sendMessage(wm);
						List<ServentInfo> newNodes = new ArrayList<>();
						ServentInfo serventInfo = new ServentInfo(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());
						newNodes.add(serventInfo);
						AppConfig.chordState.addNodes(newNodes);
						AppConfig.heartbeatManager.addNode(serventInfo);
						String text =AppConfig.myServentInfo.getIpAddress() + ":"  + AppConfig.myServentInfo.getListenerPort();
						AppConfig.chordState.versionVector.putIfAbsent(serventInfo, 1);
						UpdateMessage updateMessage = new UpdateMessage(clientMessage.getSenderPort(),
								clientMessage.getSenderIpAddress(),
								AppConfig.chordState.getNextNodePort(),
								AppConfig.chordState.getNextNodeIp(),
								text,
								AppConfig.chordState.versionVector);
						MessageUtil.sendMessage(updateMessage);
					} else { //if he is not my predecessor, let someone else take care of it
						ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
						NewNodeMessage nnm = new NewNodeMessage(newNodePort,
								clientMessage.getSenderIpAddress(),
								nextNode.getListenerPort(),
								nextNode.getIpAddress());
						MessageUtil.sendMessage(nnm);
					}
				}
				catch (Exception e){
					AppConfig.timestampedErrorPrint(e.getMessage());
				}
			}
			
		} else {
			AppConfig.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
		}

	}

}
