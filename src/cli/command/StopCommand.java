package cli.command;

import app.AppConfig;
import app.ServentInfo;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.StoppedMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;
import servent.model.Token;

public class StopCommand implements CLICommand {

	private CLIParser parser;
	private SimpleServentListener listener;

	public StopCommand(CLIParser parser, SimpleServentListener listener) {
		this.parser = parser;
		this.listener = listener;
	}

	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		synchronized (AppConfig.tokenLock) {
			if (AppConfig.chordState.token != null) {
				AppConfig.chordState.token.inUse = true;
			} else {
				Token.requestToken();
				while (AppConfig.chordState.token == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			StoppedMessage stoppedMessage = new StoppedMessage(AppConfig.myServentInfo.getListenerPort(),
					AppConfig.myServentInfo.getIpAddress(),
					AppConfig.chordState.getNextNodePort(),
					AppConfig.chordState.getNextNodeIp());
			MessageUtil.sendMessage(stoppedMessage);
		}

	}
}
