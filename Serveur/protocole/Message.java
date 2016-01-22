package protocole;

import java.io.Serializable;
import java.net.InetAddress;

public class Message implements Serializable {

	/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		
		
		
		final static public int REQUEST = 0;
		final static public int REPLY = 1;
		
		protected int type;						// type de message request or reply
		protected int messageID; 					// numéro de la requéte originelle  

		protected InetAddress destination; 		// destinataire du message
		protected int destinationPort;			// port du destinataire

		public boolean isRequest(){
			return (type == REQUEST);
		}
		
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public int getNumero() {
			return messageID;
		}
		public void setNumero(int numero) {
			this.messageID = numero;
		}

		public InetAddress getDestination() {
			return destination;
		}
		public int getDestinationPort() {
			return destinationPort;
		}
		public void setDestinationPort(int destinationPort) {
			this.destinationPort = destinationPort;
		}
		public void setDestination(InetAddress destination) {
			this.destination = destination;
		}
		@Override
		public String toString() {
			String output = "Message [type=" + type + ", numero=" + messageID 
					+ ", destination=" + destination + ", destinationPort=" + destinationPort; 
			return output;	
		}
}
