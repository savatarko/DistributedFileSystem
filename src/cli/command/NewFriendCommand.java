package cli.command;

import app.AppConfig;
import servent.message.NewFriendMessage;
import servent.message.util.MessageUtil;

public class NewFriendCommand implements CLICommand{
    @Override
    public String commandName() {
        return "add_friend";
    }

    @Override
    public void execute(String args) {
        String[] split = args.split(" ");
        if(split.length!=1){
            AppConfig.timestampedErrorPrint("Add friend command requires 1 argument.");
            return;
        }
        String[] split1 = split[0].split(":");
        if(split1.length!=2){
            AppConfig.timestampedErrorPrint("Add friend command requires 1 argument in format IP:port.");
            return;
        }
        String ip = split1[0];
        int port = Integer.parseInt(split1[1]);
        if(ip.equals(AppConfig.myServentInfo.getIpAddress()) && port == AppConfig.myServentInfo.getListenerPort()){
            AppConfig.timestampedErrorPrint("You can't add yourself as a friend.");
            return;
        }
        NewFriendMessage newFriendMessage = new NewFriendMessage(AppConfig.myServentInfo.getListenerPort(),
                AppConfig.myServentInfo.getIpAddress(),
                port,
                ip);
        MessageUtil.sendMessage(newFriendMessage);
    }
}
