/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import org.apache.log4j.Logger;


import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import protocole.UDPPacket;
import utils.Marshallizer;
/**
 *
 * @author JUASP-G73-Android
 */
public class receptionHandler implements Runnable{
     private UDPPacket connectionPacket;
    private DatagramSocket connectionSocket = null;
    private DatagramPacket packetReceive;
    private InetAddress destination; 		// destinataire du message
    private int destinationPort;		// port du destinataire
    private Hashtable<Integer, UDPPacket> fenetre = new Hashtable<Integer,UDPPacket>();
    private File theFile = new File("hd.jpg"); // Static, nous allons toujours utlisÃ© le mÃªme fichier pour la transmission
    
    private static final Logger logger = Logger.getLogger(receptionHandler.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/
    
    public receptionHandler(DatagramSocket connectionSocket) {
    	logger.info("new runnable receptionHandler (client)");
        this.connectionSocket = connectionSocket;
    }

    /*************************************************************/
    /*****************   GETTER AND SETTER   *********************/
    /*************************************************************/
    
    public DatagramSocket getConnectionSocket() {
        return connectionSocket;
    }

    public InetAddress getDestination() {
		return destination;
	}

	public void setDestination(InetAddress destination) {
		this.destination = destination;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	public void setConnectionSocket(DatagramSocket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    public DatagramPacket getPacketReceive() {
        return packetReceive;
    }

    public void setPacketReceive(DatagramPacket packetReceive) {
        this.packetReceive = packetReceive;
    }

  

    public Hashtable<Integer, UDPPacket> getFenetre() {
        return fenetre;
    }

    public void setFenetre(Hashtable<Integer, UDPPacket> fenetre) {
        this.fenetre = fenetre;
    }

    public File getTheFile() {
        return theFile;
    }

    public void setTheFile(File theFile) {
        this.theFile = theFile;
    }

    
    /*************************************************************/
    /*****************        METHODS        *********************/
    /*************************************************************/
          
    private void sendPacket(UDPPacket udpPacket) {
        try {
               
                logger.debug(udpPacket.toString());
                byte[] packetData = Marshallizer.marshallize(udpPacket);
                DatagramPacket datagram = new DatagramPacket(packetData,
                                packetData.length, 
                                udpPacket.getDestination(),
                                udpPacket.getDestinationPort());
                connectionSocket.send(datagram); // Ã©mission non-bloquante
        } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
        }
    }
    
     private UDPPacket buildPacket(int seq, int ack, int fin, byte[] data) {
    	 		logger.info("receptionHandler: (client) buildPacket executed");
                UDPPacket packet = new UDPPacket(connectionPacket.getType(),getDestination(),getDestinationPort());
                packet.setData(data);
                packet.setSeq(seq);
                packet.setAck(ack);
                packet.setFin(fin);
                logger.debug(packet.toString());
                return packet;
     }
    
     public UDPPacket getConnectionPacket() {
        return connectionPacket;
    }

    public void setConnectionPacket(UDPPacket udpPacket) {
        this.connectionPacket = udpPacket;
    }
    
    public void start() {		
        try {
               // connectionSocket = new DatagramSocket();
        logger.info("receptionHandler: client start on port " + String.valueOf(connectionSocket.getPort()));
		//on set le pckt a recevoir
		byte[] buffer = new byte[1500];
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

		//reception bloquante du paquet seq=1
		connectionSocket.receive(datagram);
		this.setDestinationPort(datagram.getPort());
		this.setDestination(datagram.getAddress());
		connectionPacket = (UDPPacket)Marshallizer.unmarshall(datagram);

		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU SEQ=1 ACK=1
		final UDPPacket confirmConnectionPacket = buildPacket(1,1,0,new byte[1024]);
		timer.scheduleAtFixedRate(new TimerTask() 
		{
			public void run() 
			{
				logger.info("receptionHandler: (client) handShakeTimer, envoit d'un paquet pour demander la connexion.");
				sendPacket(confirmConnectionPacket);
			}
		}, 0, 1000);

		//PREMIERE RECEPTION DE DATA
		int seqAttendu = 1;
		int ackRetour=1;

		
		BufferedOutputStream bos; 
        bos = new BufferedOutputStream(new FileOutputStream("hd.jpg"));
         
     
		do
		{
			connectionSocket.receive(datagram);
			logger.info("receptionHandler: (client) reception d'un paquet du serveur");
			

			//CREATION PAQUET A RECEVOIR ET ACK A RENVOYER
			UDPPacket UDPReceive = (UDPPacket) Marshallizer.unmarshall(datagram);
			UDPPacket receveACK = buildPacket(seqAttendu, ackRetour,0,new byte[1024] );
			logger.info("receptionHandler: (client) voici le paquet recu:"+ UDPReceive.toString());
			logger.info("receptionHandler: (client) voici le paquet envoyé:"+ receveACK.toString());
			
			if(UDPReceive.getSeq() ==1)timer.cancel();
			//ON AJOUTE LE PAQUET RECU AU H_TABLE
			if(fenetre.containsKey(UDPReceive.getSeq())==false)fenetre.put(UDPReceive.getSeq(), UDPReceive);

			//SI SEQ RECUE =SEQ ATTENDUE
			if (UDPReceive.getSeq()==seqAttendu)
			{
				logger.info("receptionHandler: (client) on recupere le fichier");
				//ON ECRIT LES DONNES RECUES DANS LE FICHIER
				bos.write(UDPReceive.getData(),UDPReceive.getSeq() -1,UDPReceive.getData().length);
				bos.flush();
				logger.info("receptionHandler: (client) nous avons écrit:" + UDPReceive.getSeq());
				//ACK CONFIRME RECEPTION DU PAQUET ATTENDU
				ackRetour = seqAttendu;
				if(UDPReceive.getFin() == 0) seqAttendu +=UDPReceive.getData().length;
			}
			sendPacket(receveACK);
			if(UDPReceive.getFin() == 1 && UDPReceive.getSeq() == seqAttendu )
			{
				closeConnection(UDPReceive.getSeq(),UDPReceive.getAck()) ;
				bos.close();
			}

		}while(true);
                
        } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
        }
        finally {
                logger.info("end of transmission");
                stop();
        }
	}
    public void closeConnection(int seqNum, int ackNum) throws IOException
	{
    	logger.info("receptionHandler: (client-closeConnection)");
		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU ACK DE FIN
		final UDPPacket endPqt = buildPacket(seqNum, ackNum, 1, new byte[1024]);
		timer.scheduleAtFixedRate(new TimerTask() 
		{
			public void run() 
			{
				sendPacket(endPqt);
			}
		}, 0, 1000);
		timer.cancel();

		connectionSocket.close();
	}
    public void stop(){
        Thread.currentThread().interrupt();
        return;
    }
    @Override
	public void run() {
		start();	
	}
}
