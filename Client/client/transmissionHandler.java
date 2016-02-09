/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
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
public class transmissionHandler implements Runnable{
    private static final int DATA_SIZE = 1024;
    private UDPPacket connectionPacket;
    private DatagramSocket connectionSocket = null;
    private DatagramPacket packetReceive;
    private int base = 0; //Premier paquet de la fenêtre
    private int seq = 0;
    private int ack = 0;
    private int fin = 0;
    private Hashtable<Integer, UDPPacket> fenetre = new Hashtable<Integer,UDPPacket>();
    //private File theFile = new File("md.jpg"); // Static, nous allons toujours utlisé le même fichier pour la transmission
    private File theFile = new File("textfile.txt"); // Static, nous allons toujours utlisé le même fichier pour la transmission
    private Timer windowTimer = null;
    
    private static final Logger logger = Logger.getLogger(transmissionHandler.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/
    
    public transmissionHandler(DatagramSocket connectionSocket) {
        logger.info("transmissionHandler: (client) new runnable");
        this.connectionSocket = connectionSocket;
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

    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }
    
    public UDPPacket getConnectionPacket() {
        return connectionPacket;
    }

    public void setConnectionPacket(UDPPacket udpPacket) {
        this.connectionPacket = udpPacket;
    }

    
    /*************************************************************/
    /*****************        METHODS        *********************/
    /*************************************************************/
    //Prépare les objets pour l'envoi
    private void prepWindow(){       
        try {            
            BufferedInputStream bis; 
            bis = new BufferedInputStream(new FileInputStream(theFile));
            byte[] buffer = new byte[1024];
            bis.skip(seq-1);            
            while(bis.read(buffer) != -1 && fenetre.size() < 5){
                
                //Lorsqu'il ne reste plus aucun byte à lire par la suite, on signal la fin de la transmission                
                if( bis.available() <= 0 ){
                    this.setFin(1);
                    Timer finTimer = new Timer(); //Timer pour les timeouts
                    finTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                      stop();
                    }
                  }, 35000); // 35 secondes avant la fermeture du thread de connection.
                }
                UDPPacket packetTemp = buildPacket(seq, ack,fin, buffer);
                fenetre.put(seq, packetTemp);//On ajoute à la liste
                this.setSeq(this.getSeq() + buffer.length);      
            }             

        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(transmissionHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(transmissionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    
    //Envoi de la fenetre
    private synchronized void sendWindow(){
        for (UDPPacket value : fenetre.values()) {
            sendPacket(value);
        }        
    }
    
    private void sendPacket(UDPPacket udpPacket) {
        try {
                logger.info("transmissionHandler: (client) sendPacket " + udpPacket.toString());                
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
                logger.info("transmissionHandler: (client) buildPacket "); 
                UDPPacket packet = new UDPPacket(connectionPacket.getType(),connectionSocket.getInetAddress(),connectionSocket.getPort(),connectionPacket.getSourceAdr(),connectionPacket.getSourcePort());
                packet.setData(data);
                packet.setSeq(seq);
                packet.setAck(ack);
                packet.setFin(fin);
                logger.debug(packet.toString());
                return packet;
     }
     
       private synchronized void gestionAck(UDPPacket udpPacket) {
        logger.info("gestionAck: (client) Vérification du ack reçu");
        logger.info("gestionAck: (client) ack reçu:" + udpPacket.getAck());
        //Si le ack reçu correspond au premier paquet de la fenetre courante, alors on retire ce dernier de la table
        if(udpPacket.getAck() == this.getBase()){
            this.fenetre.remove(udpPacket.getAck());
            logger.info("gestionAck: (client) le paquet correspondant au ack reçu à été retirer de la fenêtre");
            this.setBase(this.getBase() + DATA_SIZE); 
            logger.info("gestionAck: (client) base est incrémenté à:" + this.getBase());
        }
        else{
            logger.info("gestionAck: (client) le ack recu ne correspond pas à notre premier paquet, alors la fenêtre reste inchangé");
        }
                
        
    }
    
 
    
    public void start() {		
        try {
                
                
                logger.info("Client.transmissionHandler: (client) started on port " + String.valueOf(connectionSocket.getPort()));                
                boolean run = true; 
                byte[] buffer = new byte[1500];
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                Timer windowTimer = new Timer(); //Timer pour les timeouts

                logger.info("transmissionHandler: (client) en attente de la confirmation du serveur "); 
                //Reception bloquante du paquet seq=1
		connectionSocket.receive(datagram);
		connectionPacket = (UDPPacket)Marshallizer.unmarshall(datagram); //On enregistre les informations du server ici
                
                //insersion des informations sources du datagram
                connectionPacket.setSourceAdr(datagram.getAddress());
                connectionPacket.setSourcePort(datagram.getPort());
                
                //Création du paquet pour la confirmation de connexion
                this.setSeq(1);
                this.setAck(1);
                this.setBase(1); //premier element de la fenêtre
                UDPPacket confirmConnectionPacket = buildPacket(seq,ack,fin,new byte[1024]);
                 
                //Envoi d'un paquet avec un seq 1 et ack 1 qui confirme la connexion. 
                sendPacket(confirmConnectionPacket);               
                logger.info("transmissionHandler: (client) en attente de la confirmation envoi de la confirmation au serveur ");          
                                                   
               //Envoi de la fenetre. 
                windowTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        prepWindow();
                        sendWindow();
                    }
                  }, 0, 500);
                
                //We can start to send data to the client
                do  { 
                     logger.info("transmissionHandler: (client) en attente d'un ack ");  
                    connectionSocket.receive(datagram); // reception bloquante
                    logger.info("transmissionHandler: (client) ack reçu ");
                    UDPPacket ackPacket = (UDPPacket) Marshallizer.unmarshall(datagram);
                    gestionAck(ackPacket);  
                      
                    //La packet recu signal la fin de la transmission.
                    if(ackPacket.getFin() == 1){
                        run = false;
                        windowTimer.cancel();
                    }
                    
                   
                    
                    					
                }while (run);
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
    public void stop(){
        connectionSocket.close();
        Thread.currentThread().interrupt();
        windowTimer.cancel();
    }
    @Override
	public void run() {
		start();	
	}
}
