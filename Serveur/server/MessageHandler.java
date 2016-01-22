package server;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.log4j.Logger;

import dataManagement.ListeDesMatchs;
import dataObject.Bet;
import dataObject.BetRespond;
import dataObject.ListMatchName;
import dataObject.Match;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import protocole.Message;
import protocole.MessageError;
import protocole.Reply;
import protocole.Request;
import utils.Marshallizer;

//Extact data from datagram
//send respons
public class MessageHandler implements Runnable {

	private DatagramSocket serverSocket;
	private ListeDesMatchs data = null;
	private static final Logger logger = Logger.getLogger(MessageHandler.class);
	private DatagramPacket packetReceive;

	public MessageHandler(DatagramPacket packetReceive, DatagramSocket serverSocket) {
		super();
		this.packetReceive = packetReceive;
		this.serverSocket = serverSocket;
		logger.info("new runnable");
	}
	
	@Override
	public void run() {
		//extract data from packet
		data = ListeDesMatchs.getInstance();
		Message message = (Message) Marshallizer.unmarshall(packetReceive);
		logger.info("message receive " + String.valueOf(message.getType()));	
		if (message.isRequest()) {
			//build the response of the request
			logger.info("build answer");
			Reply reply = buildResponse((Request)message);
			respond(reply);
		}
		logger.info("reply done");
	}
	
		//Answer to the request
		private Reply buildResponse(Request request) {
			Reply reply = new Reply(packetReceive.getAddress(),packetReceive.getPort(),request.getNumero());
			switch(request.getMethode()) {
			case list :
				//old version with every detail
				//Match[] listMatch = data.getAllMatch();
				//reply.setValue(listMatch);	
				
				//new version
				ListMatchName listMatch = data.getAllMatchName();
				reply.setValue(listMatch);	
				
				break;
				
			case detail:
				Object[] arguments = request.getArgument();
				int matchID = (int) arguments[0];
				logger.info("detail received with param : "+ String.valueOf(matchID));
				Match matchDetail = data.getMatch(matchID);
				reply.setValue(matchDetail);			
				break;
                        case betInfo:
                            
                                //We fetch the arguments
                                Object[] args = request.getArgument();                
                                int betMatchID = (int) args [0]; // The matchID of the request                            
                                String betID = (String) args[1]; // The betID of the request
                                
                                logger.info("detail received with params : "+ String.valueOf(betMatchID + " and the betID: " + betID.toString() ));  
                                
                                //Respond object
                                BetRespond betRespond = null;
                                
                                try {
                                    //We get the total betting amount for the current match
                                    float matchTotalBettingAmount = BetHandler.getTotalBettingAmount(betMatchID);
                                    
                                    logger.info("Total betting amount for the match #"+ String.valueOf(betMatchID + " is : " + String.valueOf(matchTotalBettingAmount) + "$" ));
                                    
                                    //We retreive the data of the Match
                                    Match betMatchDetail = data.getMatch(betMatchID);
                                    
                                    if(betMatchDetail.getWinner() != null){

                                        //We get the winnerTable that contains all the Bet object that won for that match.
                                        Hashtable<String, Bet> winnerTable = BetHandler.getWinnerTable(betMatchDetail);

                                        //If the current betID (from the request) is inside the winnerTable we proceed
                                        if(winnerTable.containsKey(betID)){

                                            //We set currentBet to the resquested bet with the betID 
                                            Bet currentBet = winnerTable.get(betID);

                                            logger.info("The current bet amount for the match #"+ String.valueOf(betMatchID + " is : " + String.valueOf(currentBet) + "$" ));

                                            float currentBetAmount = currentBet.getBetAmount(); //contains the resquested bet dollar amount 

                                            float totalWinningBetAmount = 0; //contains the sum of all the bet

                                            //This iterator will go through the winnerTable 
                                            Iterator<Map.Entry<String, Bet>> it = winnerTable.entrySet().iterator();

                                            while (it.hasNext()) {
                                              //We get the current entry of the winnerTable  
                                              Map.Entry<String, Bet> entry = it.next();

                                              //We get the value of that entry
                                              Bet b = entry.getValue();

                                              //We sum the Bet value to the existing totalWinningBetAmount
                                              totalWinningBetAmount += b.getBetAmount();
                                            }

                                            logger.info("The total of the winning bet made for the match #"+ String.valueOf(betMatchID + " is : " + String.valueOf(totalWinningBetAmount) + "$" ));

                                            float wonAmount = (float)( (currentBetAmount / totalWinningBetAmount) * (matchTotalBettingAmount * 0.75) );

                                            betRespond = new BetRespond(betID, betMatchID, 1, currentBetAmount, wonAmount ); // won the bet
                                            reply.setValue(betRespond);
                                            
                                            logger.info("The bet was a winning bet, and we reply.setValue contains a betRespond Object" );

                                        }
                                        else{
                                            betRespond = new BetRespond(betID, betMatchID, 0 ); // Did not win the bet
                                            reply.setValue(betRespond);
                                            
                                            logger.info("The bet was not a winning bet, and we reply.setValue contains a betRespond Object" );
                                        }
                                    }
                                    else{
                                        betRespond = new BetRespond(betID, betMatchID, 2 ); // Match isn't finish yet
                                        reply.setValue(betRespond);
                                        
                                        logger.info("The match isn't finish yet for that match" );
                                    }
                                } catch (IOException ex) {
                                        java.util.logging.Logger.getLogger(MessageHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                                
				
			default:
				reply.setValue(new MessageError(MessageError.METHODEERROR)); 
				break;
			}								
			logger.info("Message reply crafted");		
			return reply;
		}

	protected void respond(Reply message) {
		try {
			//Message.getData()
			logger.debug(message.toString());
			byte[] reply = Marshallizer.marshallize(message);
			DatagramPacket datagram = new DatagramPacket(reply,
					reply.length, 
					message.getDestination(),
					message.getDestinationPort());
			serverSocket.send(datagram); // émission non-bloquante
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}
	}


}
