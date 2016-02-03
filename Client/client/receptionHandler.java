/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.apache.log4j.Logger;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
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
    private Hashtable<Integer, UDPPacket> fenetre = new Hashtable<Integer,UDPPacket>();
    private File theFile = new File("hd.jpg"); // Static, nous allons toujours utlisé le même fichier pour la transmission
    
    private static final Logger logger = Logger.getLogger(transmissionHandler.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/
    
    public receptionHandler(UDPPacket connectionPacket) {
        this.connectionPacket = connectionPacket;
    }

    /*************************************************************/
    /*****************   GETTER AND SETTER   *********************/
    /*************************************************************/
    
    public DatagramSocket getConnectionSocket() {
        return connectionSocket;
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
                connectionSocket.send(datagram); // émission non-bloquante
        } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
        }
    }
    
     private UDPPacket buildPacket(int seq, int ack, int fin, byte[] data) {
        
                UDPPacket packet = new UDPPacket(connectionPacket.getType(),connectionPacket.getDestination(),connectionPacket.getDestinationPort());
                packet.setData(data);
                packet.setSeq(seq);
                packet.setSeq(ack);
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
                connectionSocket = new DatagramSocket();

		//on set le pckt a recevoir
		byte[] buffer = new byte[1500];
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

		//reception bloquante du paquet seq=1
		connectionSocket.receive(datagram);
		connectionPacket = (UDPPacket)Marshallizer.unmarshall(datagram);

		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU SEQ=1 ACK=1
		UDPPacket confirmConnectionPacket = buildPacket(1,1,0,new byte[1024]);
		timer.scheduleAtFixedRate(new TimerTask() 
		{
			public void run() 
			{
				sendPacket(confirmConnectionPacket);
			}
		}, 0, 1000);

		//PREMIERE RECEPTION DE DATA
		int seqAttendu = 1;
		int ackRetour=1;

		FileOutputStream fileOut = new FileOutputStream("hd.jpg");
		do
		{
			connectionSocket.receive(datagram);
			

			//CREATION PAQUET A RECEVOIR ET ACK A RENVOYER
			UDPPacket UDPReceive = (UDPPacket) Marshallizer.unmarshall(datagram);
			UDPPacket receveACK = buildPacket(seqAttendu, ackRetour,0,new byte[1024] );
			
			if(UDPReceive.getSeq() ==1)timer.cancel();
			//ON AJOUTE LE PAQUET RECU AU H_TABLE
			if(fenetre.containsKey(UDPReceive.getSeq())==false)fenetre.put(UDPReceive.getSeq(), UDPReceive);

			//SI SEQ RECUE =SEQ ATTENDUE
			if (UDPReceive.getSeq()==seqAttendu)
			{

				//ON ECRIT LES DONNES RECUES DANS LE FICHIER
				fileOut.write(UDPReceive.getData(),UDPReceive.getSeq() -1,UDPReceive.getData().length);
				//ACK CONFIRME RECEPTION DU PAQUET ATTENDU
				ackRetour = seqAttendu;
				if(UDPReceive.getFin() == 0) seqAttendu +=UDPReceive.getData().length;
			}
			sendPacket(receveACK);
			if(UDPReceive.getFin() == 1 && UDPReceive.getSeq() == seqAttendu )
			{
				closeConnection(UDPReceive.getSeq(),UDPReceive.getAck()) ;
				fileOut.close();
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
		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU ACK DE FIN
		UDPPacket endPqt = buildPacket(seqNum, ackNum, 1, new byte[1024]);
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
