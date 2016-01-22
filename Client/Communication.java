package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import affichage.Menu;
import dataObject.ListMatchName;
import dataObject.Match;
import protocole.Message;
import protocole.Reply;
import protocole.Request;
import utils.Marshallizer;

public class Communication {
	private static Communication INSTANCE = new Communication();
	public static Communication getInstance(){return INSTANCE;}
	
	private int TIMEOUT = 5000;
	private int MAX_TENTATIVE = 5;
	
	private DatagramSocket aSocket = null;
	private InetAddress adress;
	private int serveurPort;
	private int clientPort;
	private Thread WaitingMessage = null;	
	private Reply reponse = null;
	
	private Object MutexLock = new Object();
	
	private boolean error = false;
	private int tentative = 0;

	private Communication(){
	}

	public void setServeur(InetAddress adress, int serveurPort, int clientPort){
		this.adress = adress;
		this.serveurPort = serveurPort;
		this.clientPort = clientPort;
	}
	
	
	/**
	 * Demande au serveur les d�tails d'un match avec son ID
	 * @param idMatch Id du match � r�cup�rer
	 * @author CharlyBong
	 * @return Array de Matchs
	 * @deprecated Old version -> use getListMatchName()
	 */
	public Match[] getListMatch(){
		tentative = 0;
		do{
			error = false;
			try {
				aSocket = new DatagramSocket(this.clientPort);
			
				Message ask = Request.craftGetMatchList(this.adress,this.serveurPort);		
				Protocole.send(ask,aSocket);
				
				WaitingMessage = new Thread(new Menu.WaitMessage(1000));
				WaitingMessage.start();
				
				synchronized (MutexLock) {
					new Thread(new WaitReponse()).start();
					MutexLock.wait();
				}			
			}
			catch (SocketException e){System.out.println("Socket: " + e.getMessage());} 
			catch (InterruptedException e) {e.printStackTrace();} 
			
			finally {aSocket.close(); aSocket = null; WaitingMessage.interrupt();tentative++;}			
		}while((error)&&(tentative < MAX_TENTATIVE));
				
		if (error){
			System.out.println("-- Erreur Serveur TimeOut --");
			return null;
		}
		return (Match[]) this.reponse.getValue();
	}
	
	/**
	 * Demande au serveur la liste des matchs
	 * @author Uldax
	 * @return Objet ListMatchName contenant la liste des matchs
	 */
	public ListMatchName getListMatchName(){
		tentative = 0;
		do{
			error = false;
			try {
				aSocket = new DatagramSocket(this.clientPort);
			
				Message ask = Request.craftGetMatchList(this.adress,this.serveurPort);		
				Protocole.send(ask,aSocket);
				
				System.out.println("message send");
				
				WaitingMessage = new Thread(new Menu.WaitMessage(1000));
				WaitingMessage.start();
				
				synchronized (MutexLock) {
					new Thread(new WaitReponse()).start();
					MutexLock.wait();
				}			
			}
			catch (SocketException e){System.out.println("Socket: " + e.getMessage());} 
			catch (InterruptedException e) {e.printStackTrace();} 
			finally {aSocket.close(); aSocket = null; WaitingMessage.interrupt();tentative++;}			
		}while((error)&&(tentative < MAX_TENTATIVE));
				
		if (error){
			System.out.println("-- Erreur Serveur TimeOut --");
			return null;
		}
			
		return (ListMatchName) this.reponse.getValue();
	}

	/**
	 * Demande au serveur les d�tails d'un match avec son ID
	 * @param idMatch Id du match � r�cup�rer
	 * @author CharlyBong
	 * @return Objet Match contenant les d�tails du match
	 */
	public Match getMatchDetail(int idMatch){
		tentative = 0;
		do{
			error = false;
			try {
				aSocket = new DatagramSocket(this.clientPort);
			
				Message ask = Request.craftGetMatchDetail(this.adress,this.serveurPort, idMatch);		
				Protocole.send(ask,aSocket);
				
				WaitingMessage = new Thread(new Menu.WaitMessage(1000));
				WaitingMessage.start();
				
				synchronized (MutexLock) {
					new Thread(new WaitReponse()).start();
					MutexLock.wait();
				}			
			}
			catch (SocketException e){System.out.println("Socket: " + e.getMessage());} 
			catch (InterruptedException e) {e.printStackTrace();} 
			
			finally {aSocket.close(); aSocket = null; WaitingMessage.interrupt();tentative++;}			
		}while((error)&&(tentative < MAX_TENTATIVE));
				
		if (error){
			System.out.println("-- Erreur Serveur TimeOut --");
			return null;
		}
		return (Match) this.reponse.getValue();
	}
	
	
	
	/**
	 * Thread pour l'affichage de point d'indiquand � l'utilisateur d'attendre
	 * @author CharlyBong
	 */
	private class WaitReponse implements Runnable {
		
		public WaitReponse() {}
		
		@Override
		public void run() {
			synchronized (MutexLock) {
				try { 		                        
					byte[] buffer = new byte[10000];
					DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
					aSocket.setSoTimeout(TIMEOUT);
					aSocket.receive(reply);
					reponse = (Reply) Marshallizer.unmarshall(reply);			
				}
				catch (SocketTimeoutException  e) { error = true; }
				catch (IOException e) { e.printStackTrace(); error = true;}
				
				finally { MutexLock.notify(); }
			}
				         
		}

	}
	
	
}
