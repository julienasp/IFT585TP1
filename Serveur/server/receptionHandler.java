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
    private int seq = 0;
    private int ack = 0;
    private int fin = 0;
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

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public int getFin() {
        return fin;
    }

    public void setFin(int fin) {
        this.fin = fin;
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
            //Gestion de la connexion
            connectionSocket = new DatagramSocket(); // port convenu avec les clients
                
            logger.info("receptionHandler: server start on port " + String.valueOf(connectionSocket.getPort()));
            boolean connectionNotEstablished = true; 
            byte[] buffer = new byte[1500];
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            Timer handShakeTimer = new Timer(); //Timer pour les timeouts            

            //Création du paquet pour la confirmation de connexion
            this.setSeq(1);            
            UDPPacket confirmConnectionPacket = buildPacket(seq,ack,fin,new byte[1024]);

            //Envoi d'un paquet avec un seq 1. 
            handShakeTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                  sendPacket(confirmConnectionPacket);
                }
              }, 0, 1000);


            //Tant que la connexion n'est pas établi le timer ci-dessus va envoyé notre paquet de confirmation.
            while(connectionNotEstablished){

                //En attente du paquet de notre client avec seq1, ack1. Pour terminé le handshake.
                connectionSocket.receive(datagram); // reception bloquante

                //extract data from packet		
                UDPPacket handShakePacket = (UDPPacket) Marshallizer.unmarshall(datagram);
                if(handShakePacket.getSeq() >= 1 && handShakePacket.getAck() >= 1){ // Si on recoit un paquet de data ou bien la confirmation, alors on arret le timer
                    connectionNotEstablished = false;
                    logger.info("Connection accepted by the client");

                    //Arret du timer
                    handShakeTimer.cancel();
                }
            }
            
            //Fin de la gestion de la connexion
            
            //Nous sommes maintenant pret à recevoir des données            
            
            //PREMIERE RECEPTION DE DATA
            int seqAttendu = 1;
            int ackRetour=1;

            FileOutputStream fileOut = new FileOutputStream("clientToServerUpload.jpg");
            do
            {
                    connectionSocket.receive(datagram);


                    //CREATION PAQUET A RECEVOIR ET ACK A RENVOYER
                    UDPPacket UDPReceive = (UDPPacket) Marshallizer.unmarshall(datagram);
                    UDPPacket receveACK = buildPacket(seqAttendu, ackRetour,0,new byte[1024] );

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
    }
    @Override
	public void run() {
		start();	
	}
}
