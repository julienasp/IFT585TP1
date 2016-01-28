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
    private UDPPacket connectionPacket;
    private DatagramSocket connectionSocket = null;
    private DatagramPacket packetReceive;
    private int seq = 0;
    private int ack = 0;
    private int fin = 0;
    private Hashtable<Integer, UDPPacket> fenetre = new Hashtable<Integer,UDPPacket>();
    private File theFile = new File("hd.jpg"); // Static, nous allons toujours utlisé le même fichier pour la transmission
    
    private static final Logger logger = Logger.getLogger(transmissionHandler.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/
    
    public transmissionHandler(UDPPacket connectionPacket) {
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
    //Prépare les objets pour l'envoi
    private void prepWindow(){       
        try {            
            BufferedInputStream bis; 
            bis = new BufferedInputStream(new FileInputStream(theFile));
            byte[] buffer = new byte[1024];
            bis.skip(seq-1);
            while(bis.read(buffer) != -1 || fenetre.size() < 5){
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
    private void sendWindow(){
        for (UDPPacket value : fenetre.values()) {
            sendPacket(value);
        }        
    }
    
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
                connectionSocket = new DatagramSocket(); // port convenu avec les clients
                
                logger.info("server start on port " + String.valueOf(connectionSocket.getPort()));
                
                boolean run = true; 
                byte[] buffer = new byte[1500];
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                Timer timer = new Timer(); //Timer pour les timeouts

                //Création du paquet pour la confirmation de connexion
                this.setSeq(1);
                UDPPacket confirmConnectionPacket = buildPacket(seq,ack,fin,new byte[1024]);
                
                //Envoi d'un paquet avec un seq 1. 
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                      sendPacket(confirmConnectionPacket);
                    }
                  }, 0, 10000);
                
                //En attente du paquet de notre client avec seq1, ack1. Pour terminé le handshake.
                connectionSocket.receive(datagram); // reception bloquante
                
                logger.info("Connection accepted by the client");
                
                //Arret du timer
                timer.cancel();
               
                //We can start to send data to the client
                do  {                   
                    
                    prepWindow();
                    sendWindow();
                    connectionSocket.receive(datagram); // reception bloquante
                    
                    logger.info("an ack was received");
                    
                    					
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
        Thread.currentThread().interrupt();
        return;
    }
    @Override
	public void run() {
		start();	
	}
}
