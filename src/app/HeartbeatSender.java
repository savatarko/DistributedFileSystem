package app;

import servent.message.HeartbeatMessage;
import servent.message.util.MessageUtil;

import java.util.List;

import static app.ChordState.working;

public class HeartbeatSender implements Runnable{

    @Override
    public void run() {
        while(working){
            //nzm jel mi ovo treba, al za svaki slucaj da se ne menja niz dok se iterira
            synchronized (AppConfig.heartbeatManager.heartbeatLock) {
            List<ServentInfo> nodes = AppConfig.heartbeatManager.getNodes();
                for (ServentInfo serventInfo : nodes) {
                    HeartbeatMessage heartbeatMessage = new HeartbeatMessage(AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.myServentInfo.getIpAddress(),
                            serventInfo.getListenerPort(),
                            serventInfo.getIpAddress());
                    MessageUtil.sendMessage(heartbeatMessage);
                }
            }
            try{
                Thread.sleep(AppConfig.weakLimit/2);
            } catch (InterruptedException e){
                AppConfig.timestampedErrorPrint(e.getMessage());
            }
        }
    }
}
