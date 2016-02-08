package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import org.apache.log4j.Logger;

import protocole.UDPPacket;
import utils.Marshallizer;

public class clientInterface implements Runnable{
	
	private InetAddress ipDestination;
	private int portDestination;
	private DatagramSocket userRequest =null;
	private static final Logger logger = Logger.getLogger(clientInterface.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/   
    public clientInterface(InetAddress ipDestination, int portDestination) {
        this.ipDestination = ipDestination;
        this.portDestination = portDestination;
    }

    /*************************************************************/
    /*****************   GETTER AND SETTER   *********************/
    /*************************************************************/
    public InetAddress getIpDestination() {
        return ipDestination;
    }

    public void setIpDestination(InetAddress ipDestination) {
        this.ipDestination = ipDestination;
    }

    public int getPortDestination() {
        return portDestination;
    }

    public void setPortDestination(int portDestination) {
        this.portDestination = portDestination;
    }

    public DatagramSocket getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(DatagramSocket userRequest) {
        this.userRequest = userRequest;
    }
    
    /*************************************************************/
    /*****************      FUNCTIONS        *********************/
    /*************************************************************/
   
    private void sendPacket(UDPPacket udpPacket) {
		try {
			byte[] packetData = Marshallizer.marshallize(udpPacket);
			DatagramPacket datagram = new DatagramPacket(packetData,
					packetData.length, 
					udpPacket.getDestination(),
					udpPacket.getDestinationPort());
			userRequest.send(datagram); // Ã©mission non-bloquante
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}
    }
    
    /*************************************************************/
    /*****************      RUNNABLE         *********************/
    /*************************************************************/

	public void start() throws SocketException, UnknownHostException {
		// TODO Auto-generated method stub
		
		logger.info(ipDestination.toString());

		Scanner sc = new Scanner(System.in);
                System.out.println("Quelle est l'action choisie ? ");
                System.out.println("1 --- Upload --- ");
                System.out.println("2 --- Download --- ");
                System.out.println("0 --- Exit --- ");
                int choixUser = sc.nextInt();

			
                //Ouverture Socket pour envoie packet (upl ou downl)
                userRequest = new DatagramSocket();                
                logger.info("clientInterface: sourceAdr: " + userRequest.getInetAddress());
                logger.info("clientInterface: sourcePort: " + userRequest.getPort());
                switch (choixUser) 
                {
                case 0:

                        sc.close();
                        break;

                case 1:
                        UDPPacket uplPacket = new UDPPacket(UDPPacket.FILEUPLOAD,InetAddress.getLocalHost(),userRequest.getPort(),ipDestination,portDestination);

                        Thread uplThread = new Thread(new transmissionHandler(userRequest));
                        uplThread.start();
                        logger.info("clientInterface:transmissionHandler started");
                        sendPacket(uplPacket);
                        logger.info("clientInterface: envoit du paquet pour signaler l'intention d'UPLOAD");
                        logger.info("clientInterface: infos du paquet:" + uplPacket.toString());
                        sc.close();
                        break;

                case 2: 
                        UDPPacket downPacket = new UDPPacket(UDPPacket.FILEDOWNLOAD,InetAddress.getLocalHost(),userRequest.getPort(),ipDestination,portDestination);

                        Thread downThread = new Thread(new receptionHandler(userRequest));
                        downThread.start();
                        logger.info("clientInterface:receptionHandler started");
                        sendPacket(downPacket);
                        logger.info("clientInterface: envoi du paquet pour signaler l'intention de DOWNLOAD");
                        logger.info("clientInterface: infos du paquet:" + downPacket.toString());                        
                        sc.close();
                        break;
                }
			//sc.close();
		}
		
        @Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			start();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



}