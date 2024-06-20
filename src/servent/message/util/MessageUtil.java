package servent.message.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.*;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * 
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * in a new thread (non-FIFO) or stores the message in a queue for the {@link FifoSendWorker} (FIFO).
 * 
 * When reading, if we are FIFO, we send an ACK message on the same socket, so the other side
 * knows they can send the next message.
 * @author bmilojkovic
 *
 */
public class MessageUtil {

	/**
	 * Normally this should be true, because it helps with debugging.
	 * Flip this to false to disable printing every message send / receive.
	 */
	public static final boolean MESSAGE_UTIL_PRINTING = true;
	public static final Object messageLock = new Object();
	
	public static BasicMessage readMessage(Socket socket) {
		
		BasicMessage clientMessage = null;
			
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	
			clientMessage = (BasicMessage) ois.readObject();
			
			socket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Error in reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (MESSAGE_UTIL_PRINTING) {
			if(clientMessage.getMessageType() != MessageType.HEARTBEAT && clientMessage.getMessageType() != MessageType.OK)
				AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
				
		return clientMessage;
	}
	
	public static void sendMessage(Message message) {
		if(message instanceof DeleteMessage ||
				message instanceof FileMessage ||
				message instanceof NewFriendMessage ||
				message instanceof StoppedMessage ||
				message instanceof UpdateMessage||
				message instanceof InfoMessage) {
			AppConfig.chordState.pendingMessages.putIfAbsent(new ServentInfo(message.getReceiverIpAddress(), message.getReceiverPort()), new ConcurrentHashMap<>());
			AppConfig.chordState.pendingMessages
					.get(new ServentInfo(message.getReceiverIpAddress(), message.getReceiverPort()))
					.put(message.getMessageId(), (BasicMessage) message);
		}

		Thread delayedSender = new Thread(new DelayedMessageSender(message));
		
		delayedSender.start();
	}
}
