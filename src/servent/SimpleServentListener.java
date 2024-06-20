package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.ChordState;
import servent.handler.*;
import servent.message.*;
import servent.message.util.MessageUtil;
import servent.model.Token;

import static app.ChordState.*;

public class SimpleServentListener implements Runnable, Cancellable {

//	private volatile boolean working = true;
	private List<BasicMessage> stopppedMessages = new ArrayList<>();
	
	public SimpleServentListener() {
		
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				BasicMessage clientMessage;
				
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);

				if(clientMessage instanceof DeleteMessage ||
						clientMessage instanceof FileMessage ||
						clientMessage instanceof NewFriendMessage ||
						clientMessage instanceof StoppedMessage ||
						clientMessage instanceof UpdateMessage||
						clientMessage instanceof InfoMessage) {
					OKMessage okMessage = new OKMessage(AppConfig.myServentInfo.getListenerPort(),
							AppConfig.myServentInfo.getIpAddress(),
							clientMessage.getSenderPort(),
							clientMessage.getSenderIpAddress(),
							String.valueOf(clientMessage.getMessageId()));
					MessageUtil.sendMessage(okMessage);
				}

				if(stopping &&
						clientMessage.getMessageType() != MessageType.STOPPED &&
						clientMessage.getMessageType() != MessageType.TOKEN &&
						clientMessage.getMessageType() != MessageType.TOKEN_REQUEST&&
						clientMessage.getMessageType() != MessageType.NODE_CHECK){
					stopppedMessages.add(clientMessage);
					continue;
					//TODO: posalji OKEJ nazad!
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case NEW_NODE:
					messageHandler = new NewNodeHandler(clientMessage);
					break;
				case WELCOME:
					messageHandler = new WelcomeHandler(clientMessage);
					break;
				case SORRY:
					messageHandler = new SorryHandler(clientMessage);
					break;
				case UPDATE:
					messageHandler = new UpdateHandler(clientMessage);
					break;
				case PUT:
					messageHandler = new PutHandler(clientMessage);
					break;
				case ASK_GET:
					messageHandler = new AskGetHandler(clientMessage);
					break;
				case TELL_GET:
					messageHandler = new TellGetHandler(clientMessage);
					break;
					case FILE:
						messageHandler = new FileMessageHandler(clientMessage);
						break;
					case ERROR:
						messageHandler = new ErrorHandler(clientMessage);
						break;
						case DELETE:
							messageHandler = new DeleteHandler(clientMessage);
							break;
					case INFO:
						messageHandler = new InfoMessageHandler(clientMessage);
						break;
					case NEW_FRIEND:
						messageHandler = new NewFriendMessageHandler(clientMessage);
						break;
					case TOKEN_REQUEST:
						messageHandler = new TokenRequestHandler(clientMessage);
						break;
					case TOKEN:
						messageHandler = new TokenMessageHandler(clientMessage);
						break;
					case STOPPED:
						messageHandler = new StoppedMessageHandler(clientMessage);
						break;
					case REQUEST_FILES:
						messageHandler = new RequestMyFilesHandler(clientMessage);
						break;
					case PING:
						messageHandler = new PingMessageHandler(clientMessage);
						break;
					case PONG:
						messageHandler = new PongMessageHandler(clientMessage);
						break;
					case HEARTBEAT:
						messageHandler = new HeartbeatMessageHandler(clientMessage);
						break;
					case NODE_CHECK:
						messageHandler = new NodeCheckMessageHandler(clientMessage);
						break;
					case BACKUP:
						messageHandler = new BackupMessageHandler(clientMessage);
						break;
					case GET_LAST_TOKEN:
						messageHandler = new GetLastTokenHandler(clientMessage);
						break;
					case REMOVE_FROM_BACKUP:
						messageHandler = new RemoveFromBackupHandler(clientMessage);
						break;
					case OK:
						messageHandler = new OKMessageHandler(clientMessage);
						break;
					case DOUBLE_BACKUP:
						messageHandler = new DoubleBackUpHandler(clientMessage);
						break;

				case POISON:
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		synchronized (AppConfig.tokenLock) {
//			if (AppConfig.chordState.token != null && AppConfig.chordState.token.queue.isEmpty()) {
//				AppConfig.chordState.token.inUse = false;
//				Token tokenCopy = new Token(AppConfig.chordState.token);
//				AppConfig.chordState.lastToken = tokenCopy;
//				TokenMessage tokenMessage = new TokenMessage(AppConfig.myServentInfo.getListenerPort(),
//						AppConfig.myServentInfo.getIpAddress(),
//						AppConfig.chordState.getNextNodePort(),
//						AppConfig.chordState.getNextNodeIp(),
//						tokenCopy);
//				MessageUtil.sendMessage(tokenMessage);
//			}
//		}

		if(AppConfig.chordState.sendFilesToNeighbor()) {
			for (BasicMessage message : stopppedMessages) {
				message.updateReceiver(AppConfig.chordState.getNextNodePort(),
						AppConfig.chordState.getNextNodeIp());
				MessageUtil.sendMessage(message);
			}
		}
		while(AppConfig.chordState.token!=null){
			try{
				Thread.sleep(1000);
			}
			catch (InterruptedException e){
				AppConfig.timestampedErrorPrint(e.getMessage());
			}
		}

		try{
			Thread.sleep(3000);
		}
		catch (InterruptedException e){
			AppConfig.timestampedErrorPrint(e.getMessage());
		}
	}

	@Override
	public void stop() {
		stopping = true;
//		working = false;
	}
}
