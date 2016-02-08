import client.clientInterface;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StartClient {

	public static void main(String[] args) {
		
            try {
                // TODO Auto-generated method stub
                //Create match service
                //byte[] ipAddr = new byte[]{(byte) 127, (byte) 168, 100, 1};                
                InetAddress ipDestination;                
                //ipDestination = InetAddress.getByAddress(ipAddr);
                ipDestination = InetAddress.getLocalHost();
                int port = 6780;
                
                Thread interfaceClient = new Thread( new  clientInterface( ipDestination,port ));
                interfaceClient.start();	
            } catch (UnknownHostException ex) {
                Logger.getLogger(StartClient.class.getName()).log(Level.SEVERE, null, ex);
            }

	}

}