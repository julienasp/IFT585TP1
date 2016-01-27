/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.File;
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
    private File theFile; // Static, nous allons toujours utlisé le même fichier pour la transmission
    private static final Logger logger = Logger.getLogger(transmissionHandler.class);

    public transmissionHandler(UDPPacket connectionPacket) {
        this.connectionPacket = connectionPacket;
    }

  
            
    protected void sendPacket(UDPPacket udpPacket) {
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
    
     protected UDPPacket buildPacket(int seq, int ack, byte[] data) {
        
                UDPPacket packet = new UDPPacket(connectionPacket.getType(),connectionPacket.getDestination(),connectionPacket.getDestinationPort());
                packet.setData(data);
                packet.setSeq(seq);
                packet.setSeq(ack);
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
                UDPPacket confirmConnectionPacket = buildPacket(1,0,new byte[1024]);
                
                //Envoi d'un paquet avec un seq 1. 
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                      sendPacket(confirmConnectionPacket);
                    }
                  }, 0, 10000);
                //En attente du paquet de notre client avec seq1, ack1. Pour terminé le handshake.
                do  {
                    
                    connectionSocket.receive(datagram); // reception bloquante

                    logger.info("datagramreceive");
                    
                    					
                }while (run);
                
                
                do  {                   
                    connectionSocket.receive(datagram); // reception bloquante
                    logger.info("ack received");
                    
                    gestionFenetre(datagram);					
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
