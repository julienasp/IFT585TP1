package protocole;

import java.io.Serializable;
import java.net.InetAddress;

public class UDPPacket implements Serializable {

    private static final long serialVersionUID = 2622663736791338175L;
		
    final static public int FILEUPLOAD = 0;
    final static public int FILEDOWNLOAD = 1;
		
    private int type;				// type de connexion upload ou download
    private int seq = 0; 			// numÃ©ro de sÃ©quence 
    private int ack = 0;                        // numÃ©ro d'acknowledgement
    private int fin = 0;                        // attribut pour savoir si le paquet udp Ã©tait le dernier
    private byte[] data = new byte[1024];       //Array avec les data de notre objets
    private InetAddress destination; 		// destinataire du message
    private int destinationPort;		// port du destinataire

    /*************************************************************/
    /***BELOW WE CAN FIND ALL CONSTRUCTORS FOR AN UDPPACKET*******/
    /*************************************************************/
    
    public UDPPacket(int type, InetAddress destination, int destinationPort) {
        this.type = type;
        this.destination = destination;
        this.destinationPort = destinationPort;
    }

    
    /*************************************************************/
    /**BELOW WE CAN FIND ALL THE USEFUL METHODS FOR AN UDPPACKET**/
    /*************************************************************/
    
    public boolean isForDownload(){
        return (type == FILEDOWNLOAD);
    }
    
    public boolean isForUpload(){
        return (type == FILEUPLOAD);
    }
    
    public boolean isTheLastPacket(){
        return (fin == 1);
    }

 
    /**************************************************/
    /**BELOW YOU WILL FIND EVERYONE SETTER AND GETTER**/
    /**************************************************/

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
            this.type = type;
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
            String output = "UDPPacket [type=" + type + ", seq=" + this.getSeq() + ", ack=" + this.getAck() + ", fin=" + this.getFin()
                            + ", destination=" + destination + ", destinationPort=" + destinationPort; 
            return output;	
    }
}
