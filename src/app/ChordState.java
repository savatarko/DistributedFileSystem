package app;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import servent.message.*;
import servent.message.util.MessageUtil;
import servent.model.DataFile;
import servent.model.NetworkFile;
import servent.model.Token;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * 
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState {

//	public static int CHORD_SIZE;
	public static String chordHash(String value) {
//		return 61 * value % CHORD_SIZE;
		return HashingUtil.SHA1(value);
	}
	
	private int chordLevel; //log_2(CHORD_SIZE)
	
	private ServentInfo[] successorTable;
	private ServentInfo[] backupTable;
	private ServentInfo predecessorInfo;
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;
	
	private List<NetworkFile> myFiles;
	private Map<ServentInfo, List<NetworkFile>> backup;
	public Map<String, Integer> fileUsage;
	private final Object fileLock = new Object();
	private List<String> friends;
	public Token token;
	public Token lastToken;
	public ServentInfo sentTokenTo;
	public Map<ServentInfo, Integer> versionVector;
	public static volatile Boolean stopping = false;
	public static volatile Boolean working = true;
	//kljuc id poruke, vrednost poruka
	public Map<ServentInfo, Map<Integer, BasicMessage>> pendingMessages;
	public final Object nodeLock = new Object();
	
	public ChordState() {
		this.chordLevel = 1;
////		int tmp = CHORD_SIZE;
//		while (tmp != 2) {
//			if (tmp % 2 != 0) { //not a power of 2
//				throw new NumberFormatException();
//			}
//			tmp /= 2;
//			this.chordLevel++;
//		}

		successorTable = new ServentInfo[chordLevel];
		backupTable = new ServentInfo[2];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}

		predecessorInfo = null;
		myFiles = new ArrayList<>();
		allNodeInfo = new ArrayList<>();
		friends = new ArrayList<>();
		friends.add(HashingUtil.SHA1(AppConfig.myServentInfo.getIpAddress() + ":" + AppConfig.myServentInfo.getListenerPort()));
		versionVector = new ConcurrentHashMap<>();
		versionVector.put(AppConfig.myServentInfo, 1);
		pendingMessages = new ConcurrentHashMap<>();
		backup = new ConcurrentHashMap<>();
		fileUsage = new ConcurrentHashMap<>();
	}
	
	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(BasicMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
		successorTable[0] = new ServentInfo(welcomeMsg.getSenderIpAddress(), welcomeMsg.getSenderPort());
//		this.myFiles = welcomeMsg.getValues();
		
		//tell bootstrap this node is not a collider
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n" + AppConfig.myServentInfo.getIpAddress() + "\n");
			
			bsWriter.flush();
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getChordLevel() {
		return chordLevel;
	}
	
	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}
	
	public int getNextNodePort() {
		if(successorTable[0] != null)
			return successorTable[0].getListenerPort();
		return predecessorInfo.getListenerPort();
	}

	public String getNextNodeIp() {
		if(successorTable[0] != null)
			return successorTable[0].getIpAddress();
		return predecessorInfo.getIpAddress();
	}
	
	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}
	
	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public List<NetworkFile> getMyFiles() {
		return myFiles;
	}

	public List<NetworkFile> getFilesForInfo(List<String> friends){
		List<NetworkFile> output = new ArrayList<>();
		for(NetworkFile file:myFiles){
			if(file.getOwner() == null){
				output.add(file);
			}
			else{
				for(String friend:friends){
					if(friend.equals(file.getOwner())){
						output.add(file);
						break;
					}
				}
			}
		}
		return output;
	}

	public void setMyFiles(List<NetworkFile> myFiles) {
		this.myFiles = myFiles;
	}

	public boolean isCollision(String chordId) {
		if (chordId.equals(AppConfig.myServentInfo.getChordId())) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (Objects.equals(serventInfo.getChordId(), chordId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(String key) {
		if (predecessorInfo == null) {
			return true;
		}
		
		String predecessorChordId = predecessorInfo.getChordId();
		String myChordId = AppConfig.myServentInfo.getChordId();
		
		if (predecessorChordId.compareTo(myChordId) < 0) { //no overflow
			if (key.compareTo(myChordId) <= 0 && key.compareTo(predecessorChordId) > 0) {
				return true;
			}
		} else { //overflow
			if (key.compareTo(myChordId) <= 0 || key.compareTo(predecessorChordId) > 0) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required node when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(String key) {
		synchronized (AppConfig.chordState.nodeLock) {
			if (isKeyMine(key)) {
				return AppConfig.myServentInfo;
			}

			//normally we start the search from our first successor
			int startInd = 0;

			//if the key is smaller than us, and we are not the owner,
			//then all nodes up to CHORD_SIZE will never be the owner,
			//so we start the search from the first item in our table after CHORD_SIZE
			//we know that such a node must exist, because otherwise we would own this key
			if (key.compareTo(AppConfig.myServentInfo.getChordId()) < 0) {
				int skip = 1;
				while (skip < successorTable.length && successorTable[skip].getChordId().compareTo(successorTable[startInd].getChordId()) > 0) {
					startInd++;
					skip++;
				}
			}

			String previousId = successorTable[startInd].getChordId();

			for (int i = startInd + 1; i < successorTable.length; i++) {
				if (successorTable[i] == null) {
					AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
					break;
				}

				String successorId = successorTable[i].getChordId();

				if (successorId.compareTo(key) >= 0) {
					return successorTable[i - 1];
				}
				if (key.compareTo(previousId) > 0 && successorId.compareTo(previousId) < 0) { //overflow
					return successorTable[i - 1];
				}
				previousId = successorId;
			}
			//if we have only one node in all slots in the table, we might get here
			//then we can return any item
			return successorTable[0];
		}
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]

		//adding part for 2 nodes before for backup
		predecessorInfo = allNodeInfo.get(allNodeInfo.size()-1);
		if(allNodeInfo.size() > 1){
			backupTable[0] = allNodeInfo.get(allNodeInfo.size()-1);
			if(allNodeInfo.size() > 2){
				backupTable[1] = allNodeInfo.get(allNodeInfo.size()-2);
			}
		}

		this.chordLevel = 1;

		int tmp = allNodeInfo.size();
		//OVO JE MOZDA POGRESNO!!
		while (tmp !=1) {
			tmp /= 2;
			this.chordLevel++;
		}

		successorTable = new ServentInfo[this.chordLevel];
		
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;
		
		int currentIncrement = 1;
		
		ServentInfo previousNode = AppConfig.myServentInfo;
		
		//i is successorTable index
		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			if(currentIncrement >= allNodeInfo.size()) {
				break;
			}
			successorTable[i] = allNodeInfo.get(currentIncrement);
		}

	}

	public void removeUnnecessaryBackup(){
		for(var servantInfo: backup.keySet()){
			if(!servantInfo.equals(successorTable[0]) && !servantInfo.equals(predecessorInfo)){
				for(NetworkFile networkFile: backup.get(servantInfo)){
					File file = new File(AppConfig.rootDirectory + "/" + networkFile.getName());
					if(file.delete()){
						AppConfig.timestampedStandardPrint("File " + file.getName() + " deleted");
					}
					else{
						AppConfig.timestampedErrorPrint("File " + file.getName() + " could not be deleted");
					}
				}
				backup.remove(servantInfo);
			}
		}
	}

	public void takeBackupFiles(ServentInfo serventInfo){
		synchronized (fileLock) {
//			if (isKeyMine(serventInfo.getChordId())) {
				AppConfig.timestampedStandardPrint("Taking backup files from " + serventInfo);
//				sendBackup(new ServentInfo(getNextNodeIp(), getNextNodePort()));
//				sendBackup(predecessorInfo);
			if(!backup.containsKey(serventInfo)){
				return;
			}
				for(var file :backup.get(serventInfo)){
					myFiles.add(file);
//					fileUsage.putIfAbsent(file.getHashedName(), 0);
//					fileUsage.put(file.getHashedName(), fileUsage.get(file.getHashedName()) + 1);
				}
				backup.remove(serventInfo);
//				sendMyFilesBackup(predecessorInfo, backup.remove(serventInfo));
//			}
		}
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId().compareTo(o2.getChordId());
			}
			
		});
		
		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();
		
		String myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId().compareTo(myId) < 0) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
//			if(!versionVector.containsKey(serventInfo)){
//				versionVector.put(serventInfo, 0);
//			}
		}
		
		allNodeInfo.clear();
		newList.sort(new Comparator<ServentInfo>() {

			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId().compareTo(o2.getChordId());
			}

		});
		newList2.sort(new Comparator<ServentInfo>() {

			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId().compareTo(o2.getChordId());
			}

		});
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
//		if (newList2.size() > 0) {
//			predecessorInfo = newList2.get(newList2.size()-1);
//		} else {
//			predecessorInfo = newList.get(newList.size()-1);
//		}
		
		updateSuccessorTable();
		removeUnnecessaryBackup();
		synchronized (fileLock) {
//			sendBackup(successorTable[0]);
//			if(!predecessorInfo.equals(successorTable[0])) {
//				sendBackup(predecessorInfo);
//			}
			if(newNodes.size() == 1){
				ServentInfo newNode = newNodes.get(0);
				if(newNode.equals(successorTable[0])){
					sendBackup(newNode);
				}
			}
			else{
//				sendBackup(successorTable[0]);
				if(!predecessorInfo.equals(successorTable[0])) {
					sendBackup(predecessorInfo);
				}
			}
		}
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(NetworkFile networkFile) {
		if (isKeyMine(networkFile.getHashedName())) {
			myFiles.add(networkFile);
//			fileUsage.putIfAbsent(networkFile, 1);
		} else {
			ServentInfo nextNode = getNextNodeForKey(networkFile.getHashedName());
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					nextNode.getListenerPort(),
					nextNode.getIpAddress(),
					networkFile);
			MessageUtil.sendMessage(pm);
		}
	}

	public void insertFile(NetworkFile networkFile, byte[] bytes, int originalSenderPort, String originalSenderIp){
		synchronized (fileLock){
			if(myFiles.contains(networkFile)){
				AppConfig.timestampedErrorPrint("File with name" + networkFile.getName() + " already exists");
				if(originalSenderPort != AppConfig.myServentInfo.getListenerPort()){
					ErrorMessage errorMessage = new ErrorMessage(AppConfig.myServentInfo.getListenerPort(),AppConfig.myServentInfo.getIpAddress(),
							originalSenderPort, originalSenderIp,
							"File with name" + networkFile.getName() + " already exists");
					MessageUtil.sendMessage(errorMessage);
				}
				return;
			}
			myFiles.add(networkFile);
			fileUsage.putIfAbsent(networkFile.getHashedName(), 0);
			fileUsage.put(networkFile.getHashedName(), fileUsage.get(networkFile.getHashedName()) + 1);
		}
		String filePath = AppConfig.rootDirectory+ "/" + networkFile.getName();
		File file = new File(filePath);
		try{
			OutputStream outputStream = new FileOutputStream(file);
			outputStream.write(bytes);
			outputStream.close();
		}
		catch (Exception e){
			AppConfig.timestampedErrorPrint(e.getMessage());
			AppConfig.timestampedErrorPrint(filePath);
		}
		if(predecessorInfo == null){
			return;
		}
		BackupMessage backupMessage1 = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
				AppConfig.myServentInfo.getIpAddress(),
				AppConfig.chordState.getNextNodePort(),
				AppConfig.chordState.getNextNodeIp(),
				networkFile,
				bytes);
		MessageUtil.sendMessage(backupMessage1);
		if(!AppConfig.chordState.getPredecessor().equals(AppConfig.chordState.getSuccessorTable()[0])) {
			BackupMessage backupMessage2 = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					AppConfig.chordState.getPredecessor().getListenerPort(),
					AppConfig.chordState.getPredecessor().getIpAddress(),
					networkFile,
					bytes);
			MessageUtil.sendMessage(backupMessage2);
		}
	}

	public void removeFile(String fileName, int originalSenderPort, String originalSenderIp){
		synchronized (fileLock){
			NetworkFile fileToRemove = null;
			for(NetworkFile file : myFiles){
				if(file.getHashedName().equals(fileName)){
					fileToRemove = file;
					break;
				}
			}
			if(fileToRemove == null){
				AppConfig.timestampedErrorPrint("File with name" + fileName + " does not exist");
				if(originalSenderPort != AppConfig.myServentInfo.getListenerPort()){
					ErrorMessage errorMessage = new ErrorMessage(AppConfig.myServentInfo.getListenerPort(),AppConfig.myServentInfo.getIpAddress(),
							originalSenderPort, originalSenderIp,
							"File with name" + fileName + " does not exist");
					MessageUtil.sendMessage(errorMessage);
				}
				return;
			}
			myFiles.remove(fileToRemove);

			RemoveFromBackupMessage removeFromBackupMessage1 = new RemoveFromBackupMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					successorTable[0].getListenerPort(),
					successorTable[0].getIpAddress(),
					List.of(fileToRemove));
			MessageUtil.sendMessage(removeFromBackupMessage1);

			if(!predecessorInfo.equals(successorTable[0])) {
				RemoveFromBackupMessage removeFromBackupMessage2 = new RemoveFromBackupMessage(AppConfig.myServentInfo.getListenerPort(),
						AppConfig.myServentInfo.getIpAddress(),
						predecessorInfo.getListenerPort(),
						predecessorInfo.getIpAddress(),
						List.of(fileToRemove));
				MessageUtil.sendMessage(removeFromBackupMessage2);
			}

			fileUsage.put(fileToRemove.getHashedName(), fileUsage.get(fileToRemove.getHashedName()) - 1);
			if(fileUsage.get(fileToRemove.getHashedName()) == 0) {
				fileUsage.remove(fileToRemove.getHashedName());
				String filePath = AppConfig.rootDirectory + "/" + fileToRemove.getName();
				File file = new File(filePath);
				if (file.delete()) {
					AppConfig.timestampedStandardPrint("File " + fileToRemove.getName() + " deleted");
				} else {
					AppConfig.timestampedErrorPrint("File " + fileToRemove.getName() + " could not be deleted");
				}
			}
		}
	}
	
	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
//	public NetworkFile getValue(String key) {
//		if (isKeyMine(key)) {
//			if (valueMap.containsKey(key)) {
//				return valueMap.get(key);
//			} else {
//				return -1;
//			}
//		}
//
//		ServentInfo nextNode = getNextNodeForKey(key);
//		AskGetMessage agm = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), String.valueOf(key));
//		MessageUtil.sendMessage(agm);
//
//		return -2;
//	}


	public List<String> getFriends() {
		return friends;
	}

	public void addFriend(String friend){
		friends.add(friend);
	}

	public void removeNode(int port, String address, Boolean graceful){
		ServentInfo nodeToRemove = null;
		for(ServentInfo node:allNodeInfo){
			if(node.getIpAddress().equals(address) && node.getListenerPort() == port){
				nodeToRemove = node;
				break;
			}
		}
		if(nodeToRemove != null){
			synchronized (AppConfig.chordState.nodeLock) {
				if(nodeToRemove.equals(predecessorInfo)){
					takeBackupFiles(nodeToRemove);
				}
//				boolean shouldISendBackup = false;
//				if(getNextNodePort() == port && getNextNodeIp().equals(address)) {
//					shouldISendBackup = true;
//				}
				Boolean shouldIDoDouble = false;
				if(predecessorInfo.equals(nodeToRemove) && allNodeInfo.size() >3){
					shouldIDoDouble = true;
				}
				allNodeInfo.remove(nodeToRemove);
				updateSuccessorTable();
				sendBackup(predecessorInfo);
				if(!predecessorInfo.equals(successorTable[0])){
					sendBackup(successorTable[0]);
				}
				if(!graceful && shouldIDoDouble){
					if(backup.get(nodeToRemove)!=null){
						for(NetworkFile file:backup.get(nodeToRemove)){
							File fileTmp = new File(AppConfig.rootDirectory + "/" + file.getName());
							byte[] bytes;
							try{
								bytes = new byte[(int) fileTmp.length()];
								FileInputStream fileInputStream = new FileInputStream(fileTmp);
								fileInputStream.read(bytes);
								fileInputStream.close();
								TwoNodeBackUpMessage twoNodeBackUpMessage = new TwoNodeBackUpMessage(AppConfig.myServentInfo.getListenerPort(),
										AppConfig.myServentInfo.getIpAddress(),
										allNodeInfo.get(1).getListenerPort(),
										allNodeInfo.get(1).getIpAddress(),
										file,
										bytes);
								MessageUtil.sendMessage(twoNodeBackUpMessage);
							}
							catch (Exception e){
								AppConfig.timestampedErrorPrint(e.getMessage());
							}
						}
						backup.remove(nodeToRemove);
					}
				}
//				if(shouldISendBackup) {
//				takeBackupFiles(nodeToRemove);
//				}
			}
		}
	}

	public void sendFilesToNode(int port, String address, String hisPredId){
		String myId = AppConfig.myServentInfo.getChordId();
		ServentInfo hisPred = AppConfig.chordState.getPredecessor();
		if (hisPred == null) {
			hisPred = AppConfig.myServentInfo;
		}
		List<NetworkFile> myValues = AppConfig.chordState.getMyFiles();
		List<NetworkFile> hisValues = new ArrayList<>();
//		String hisPredId = hisPred.getChordId();
		String newNodeId = HashingUtil.SHA1(address + ":" + port);

		for (var valueEntry : myValues) {
			if (hisPredId.equals(myId)) { //i am first and he is second
				if (myId.compareTo(newNodeId) < 0) {
					if (valueEntry.getHashedName().compareTo(newNodeId) <= 0 && valueEntry.getHashedName().compareTo(myId) > 0) {
						hisValues.add(valueEntry);
					}
				} else {
					if (valueEntry.getHashedName().compareTo(newNodeId) <= 0 || valueEntry.getHashedName().compareTo(myId) > 0) {
						hisValues.add(valueEntry);
					}
				}
			}
			if (hisPredId.compareTo(myId) < 0) { //my old predecesor was before me
				if (valueEntry.getHashedName().compareTo(newNodeId) <= 0) {
					hisValues.add(valueEntry);
				}
			} else { //my old predecesor was after me
				if (hisPredId.compareTo(newNodeId) > 0) { //new node overflow
					if (valueEntry.getHashedName().compareTo(newNodeId) <= 0 || valueEntry.getHashedName().compareTo(hisPredId) > 0) {
						hisValues.add(valueEntry);
					}
				} else { //no new node overflow
					if (valueEntry.getHashedName().compareTo(newNodeId) <= 0 && valueEntry.getHashedName().compareTo(hisPredId) > 0) {
						hisValues.add(valueEntry);
					}
				}

			}

		}
		for (var key : hisValues) { //remove his values from my map
			myValues.remove(key);
		}
		if(!predecessorInfo.equals(successorTable[0])) {
			RemoveFromBackupMessage removeMessage = new RemoveFromBackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getIpAddress(),
					getNextNodePort(), getNextNodeIp(), hisValues);
			MessageUtil.sendMessage(removeMessage);
		}
//		if(!predecessorInfo.equals(successorTable[0])){
//			RemoveFromBackupMessage removeMessage1 = new RemoveFromBackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getIpAddress(),
//					predecessorInfo.getListenerPort(), predecessorInfo.getIpAddress(), hisValues);
//			MessageUtil.sendMessage(removeMessage1);
//		}

		AppConfig.chordState.setMyFiles(myValues);
		synchronized (fileLock) {
			for (NetworkFile file : hisValues) {
				Thread thread = new Thread(new FileTransfer(file, port, address, false));
				thread.start();
				AppConfig.chordState.myFiles.remove(file);
//				fileUsage.put(file.getHashedName(), fileUsage.get(file.getHashedName()) - 1);
//				if(fileUsage.get(file.getHashedName()) == 0){
//					fileUsage.remove(file.getHashedName());
//					File fileTmp = new File(AppConfig.rootDirectory + "/" + file.getName());
//					if(fileTmp.delete()){
//						AppConfig.timestampedStandardPrint("File " + file.getName() + " deleted");
//					}
//					else{
//						AppConfig.timestampedErrorPrint("File " + file.getName() + " could not be deleted");
//					}
//				}
			}
		}
	}

	public void sendBackup(ServentInfo serventInfo){
//		Thread thread = new Thread(() -> {
//            List<DataFile> files = new ArrayList<>();
//				for (var file : myFiles) {
//					try {
//						File f = new File(AppConfig.rootDirectory + "/" + file.getName());
//						byte[] fileBytes = new byte[(int) f.length()];
//						FileInputStream fis = new FileInputStream(f);
//						fis.read(fileBytes);
//						fis.close();
//						files.add(new DataFile(file, fileBytes));
//						AppConfig.timestampedStandardPrint("Adding file " + file.getName() + " to backup (sending end)");
//					} catch (Exception e) {
//						AppConfig.timestampedErrorPrint(e.getMessage());
//					}
//				}
//			BackupMessage backupMessage = new BackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getIpAddress(),
//					serventInfo.getListenerPort(), serventInfo.getIpAddress(), files);
//			MessageUtil.sendMessage(backupMessage);
//        });
//		thread.start();
		synchronized (fileLock){
			for(var file : myFiles){
				FileTransfer fileTransfer = new FileTransfer(file, serventInfo.getListenerPort(), serventInfo.getIpAddress(), true);
				Thread thread = new Thread(fileTransfer);
				thread.start();
			}
		}
	}

	public void sendMyFilesBackup(ServentInfo serventInfo, List<NetworkFile> backupFiles){
//		Thread thread = new Thread(() -> {
//			List<DataFile> files = new ArrayList<>();
//			for (var file : backupFiles) {
//				try {
//					File f = new File(AppConfig.rootDirectory + "/" + file.getName());
//					byte[] fileBytes = new byte[(int) f.length()];
//					FileInputStream fis = new FileInputStream(f);
//					fis.read(fileBytes);
//					fis.close();
//					files.add(new DataFile(file, fileBytes));
//				} catch (Exception e) {
//					AppConfig.timestampedErrorPrint(e.getMessage());
//				}
//			}
//			BackupMessage backupMessage = new BackupMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getIpAddress(),
//					serventInfo.getListenerPort(), serventInfo.getIpAddress(), files);
//			MessageUtil.sendMessage(backupMessage);
//		});
//		thread.start();
	}

	public boolean sendFilesToNeighbor(){
		if(successorTable[0] == null || successorTable[0].equals(AppConfig.myServentInfo)){
			return false;
		}
		for(NetworkFile file:myFiles){
			Thread thread = new Thread(new FileTransfer(file, successorTable[0].getListenerPort(), successorTable[0].getIpAddress(), false));
			thread.start();
//			AppConfig.chordState.myFiles.remove(file);
		}
		return true;
	}

	public Boolean containsNode(ServentInfo serventInfo){
		for(ServentInfo node:allNodeInfo){
			if(node.equals(serventInfo)){
				return true;
			}
		}
		return false;
	}

	public void addBackup(ServentInfo serventInfo, NetworkFile networkFile, byte[] bytes){
//		if(backup.containsKey(serventInfo)){
//			return;
//		}
//		backup.put(serventInfo, new ArrayList<>());
//		for(var fileBackup: files){
//			if(backup.get(serventInfo).contains(fileBackup.networkFile)){
//				continue;
//			}
//			backup.get(serventInfo).add(fileBackup.networkFile);
//			fileUsage.putIfAbsent(fileBackup.networkFile, 0);
//			fileUsage.put(fileBackup.networkFile, fileUsage.get(fileBackup.networkFile) + 1);
//			String filePath = AppConfig.rootDirectory+ "/" + fileBackup.networkFile.getName();
//			AppConfig.timestampedStandardPrint("Adding file " + fileBackup.networkFile.getName() + " to backup (receiving end)");
//			File file = new File(filePath);
//			try{
//				OutputStream outputStream = new FileOutputStream(file);
//				outputStream.write(fileBackup.bytes);
//				outputStream.close();
//			}
//			catch (Exception e){
//				AppConfig.timestampedErrorPrint(e.getMessage());
//				AppConfig.timestampedErrorPrint(filePath);
//			}
//		}
		backup.putIfAbsent(serventInfo, new ArrayList<>());
		if(backup.get(serventInfo).contains(networkFile)){
			return;
		}
		fileUsage.putIfAbsent(networkFile.getHashedName(), 0);
		fileUsage.put(networkFile.getHashedName(), fileUsage.get(networkFile.getHashedName()) + 1);
		backup.get(serventInfo).add(networkFile);
		String filePath = AppConfig.rootDirectory + "/" + networkFile.getName();
		File file = new File(filePath);
//		if(file.exists()){
//			return;
//		}
		try{
			OutputStream outputStream = new FileOutputStream(file);
			outputStream.write(bytes);
			outputStream.close();
		}
		catch (Exception e){
			AppConfig.timestampedErrorPrint(e.getMessage());
			AppConfig.timestampedErrorPrint(filePath);
		}
	}

	public void removeFromBackup(ServentInfo serventInfo, List<NetworkFile> files){
		for(var file: files){
			backup.get(serventInfo).remove(file);
			fileUsage.put(file.getHashedName(), fileUsage.get(file.getHashedName()) - 1);
			if(fileUsage.get(file.getHashedName()) == 0) {
				fileUsage.remove(file.getHashedName());
				String filePath = AppConfig.rootDirectory + "/" + file.getName();
				File fileToRemove = new File(filePath);
				if (fileToRemove.delete()) {
					AppConfig.timestampedStandardPrint("File " + file.getName() + " deleted");
				} else {
					AppConfig.timestampedErrorPrint("File " + file.getName() + " could not be deleted");
				}
			}
		}
	}

	public List<ServentInfo> getAllNodeInfo() {
		return allNodeInfo;
	}
}
