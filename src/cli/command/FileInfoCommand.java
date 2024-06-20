package cli.command;

import app.AppConfig;
import servent.message.InfoMessage;
import servent.message.util.MessageUtil;
import servent.model.NetworkFile;

public class FileInfoCommand implements CLICommand{
    @Override
    public String commandName() {
        return "view_files";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if(splitArgs.length != 1){
            AppConfig.timestampedErrorPrint("View files command requires 1 argument.");
            return;
        }
        String[] split1 = splitArgs[0].split(":");
        if(split1.length != 2){
            AppConfig.timestampedErrorPrint("View files command requires 1 argument in format IP:port.");
            return;
        }
        String ip = split1[0];
        int port = Integer.parseInt(split1[1]);
        if(ip.equals(AppConfig.myServentInfo.getIpAddress()) && port == AppConfig.myServentInfo.getListenerPort()){
            String output = "Files on this node: ";
            for(NetworkFile networkFile : AppConfig.chordState.getMyFiles()){
                output += networkFile.getName() + ", ";
            }
            output = output.substring(0, output.length() - 2);
            AppConfig.timestampedStandardPrint(output);
        }
        else{
            InfoMessage infoMessage = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.myServentInfo.getIpAddress(),
                    port,
                    ip,
                    AppConfig.chordState.getFriends());
            MessageUtil.sendMessage(infoMessage);
        }
    }
}
