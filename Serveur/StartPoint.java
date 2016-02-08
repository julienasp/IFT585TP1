import server.UDPServer;


public class StartPoint {	
	
	public static void main(String[] args) {	
		
		//Create match service
		int port = 55557;
		int threadPoolSize = 4;	
		Thread udpServer = new Thread( new  UDPServer(port,threadPoolSize));
		udpServer.start();	
		
	}

}
