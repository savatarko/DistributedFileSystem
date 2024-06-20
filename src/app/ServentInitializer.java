package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import servent.message.BasicMessage;
import servent.message.NewNodeMessage;
import servent.message.PingMessage;
import servent.message.util.MessageUtil;
import servent.model.Token;

public class ServentInitializer implements Runnable {

	private void notifyBootstrapOfFailure(ServentInfo serventInfo){
		int bsPort = AppConfig.BOOTSTRAP_PORT;

		try {
			Socket bsSocket = new Socket("localhost", bsPort);

			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("Dead\n" + serventInfo.getListenerPort() + "\n" + serventInfo.getIpAddress() + "\n");
			bsWriter.flush();

			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ServentInfo getSomeServent() {
		int bsPort = AppConfig.BOOTSTRAP_PORT;
		
		try {
			Socket bsSocket = new Socket("localhost", bsPort);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("Hail\n" + AppConfig.myServentInfo.getListenerPort() + "\n" + AppConfig.myServentInfo.getIpAddress() + "\n");
			bsWriter.flush();
			
			Scanner bsScanner = new Scanner(bsSocket.getInputStream());
			int port = bsScanner.nextInt();
			String ip;
			if(port != -1){
				ip= bsScanner.nextLine();
			}
			else ip = "";
			
			bsSocket.close();
			return new ServentInfo(ip, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public ServentInfo initialize() {
		ServentInfo someServent = getSomeServent();

		if (someServent == null) {
			AppConfig.timestampedErrorPrint("Error in contacting bootstrap. Exiting...");
			System.exit(0);
		}
		if (someServent.getListenerPort() == -1) { //bootstrap gave us -1 -> we are first
			AppConfig.timestampedStandardPrint("First node in Chord system.");
			AppConfig.chordState.token = new Token(AppConfig.myServentInfo);

		} else { //bootstrap gave us something else - let that node tell our successor that we are here
			//TODO: kako da dobijem ip adresu ovde?
//			NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo.getListenerPort(),
//					AppConfig.myServentInfo.getIpAddress(),
//					someServentPort,
//					"localhost");
//			MessageUtil.sendMessage(nnm);
			PingMessage pingMessage = new PingMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					someServent.getListenerPort(),
					someServent.getIpAddress());
			MessageUtil.sendMessage(pingMessage);
		}
		return someServent;
	}
	
	@Override
	public void run() {
		ServentInfo someServent = initialize();

		if(someServent == null ||someServent.getListenerPort() == -1){
			return;
		}

			try{
				Thread.sleep(AppConfig.strongLimit + AppConfig.weakLimit * 3/2);
			} catch (InterruptedException e) {
				AppConfig.timestampedErrorPrint(e.getMessage());
				return;
			}
			if(!AppConfig.connection){
				notifyBootstrapOfFailure(someServent);
				AppConfig.timestampedErrorPrint("Servent " + someServent + " failed to respond in time, trying to contact bootstrap again");
				run();
		}
	}

}
