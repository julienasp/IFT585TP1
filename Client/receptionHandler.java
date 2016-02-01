package client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;


//UDP client sends a message to the server and gets a reply

public class UDPClient{

	private DatagramSocket connectionSocket = null;
	private UDPPacket connectionPacket;
	private int serverPort = 0;
	private Hashtable<Integer, UDPPacket> fenetre = new Hashtable<Integer,UDPPacket>();

	public UDPClient(int port){ 
		serverPort = port;
	}

	protected UDPPacket buildPacket(int seq, int ack, byte[] data) {

		UDPPacket packet = new UDPPacket(connectionPacket.getType(),connectionPacket.getDestination(),connectionPacket.getDestinationPort());
		packet.setData(data);
		packet.setSeq(seq);
		packet.setSeq(ack);
		//logger.debug(packet.toString());
		return packet;
	}

	protected void sendPacket(UDPPacket udpPacket) {
		try {

			//logger.debug(udpPacket.toString());
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

	public void reconstruirePaquet()
	{

	}

	public void start() throws IOException
	{
		connectionSocket = new DatagramSocket();

		//on set le pckt a recevoir
		byte[] buffer = new byte[1500];
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

		//reception bloquante du paquet seq=1
		connectionSocket.receive(datagram);
		connectionPacket = (UDPPacket)Marshallizer.unmarshall(datagram);

		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU SEQ=1 ACK=1
		UDPPacket confirmConnectionPacket = buildPacket(1,1,new byte[1024]);
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
			UDPPacket receveACK = buildPacket(seqAttendu, ackRetour,new byte[1024] );
			
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

	}

	public void closeConnection(int seqNum, int ackNum) throws IOException
	{
		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU ACK DE FIN
		UDPPacket endPqt = buildPacket(seqNum, ackNum, new byte[1024]);
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

}