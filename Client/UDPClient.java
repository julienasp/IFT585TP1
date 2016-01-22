package test;

import java.net.*;
import org.apache.log4j.Logger;
import affichage.Menu;
import dataObject.Bet;
import dataObject.ListMatchName;
import dataObject.Match;
import dataObject.Team;
import java.io.*; 
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;


//UDP client sends a message to the server and gets a reply

public class UDPClient{
    
	private static final Logger logger = Logger.getLogger(UDPClient.class);
        private static List<Bet> betHistory = new ArrayList<Bet>();
        private static final int betServerPort = 1248;
        private static AtomicLong betCounter = new AtomicLong();
	
	/**
	 * Menu principal
	 * @param args
	 * @author CharlyBong & Uldax
	 */
    public static void main(String args[]){
    	int choix = -1;
    	ListMatchName matchList = null;
    	InetAddress aHost = null;
    	int serveurPort = 6780;
    	int clientPort = 6779;
    	Communication commObject = Communication.getInstance();
    	
 		try {
 			aHost = InetAddress.getByName("localhost");
 		} catch (UnknownHostException e) {
 			e.printStackTrace();
 		}
 		
 		//Set server port and host
    	commObject.setServeur(aHost, serveurPort, clientPort);
    	do{
    		
			System.out.println("Recuperation de la liste des matchs, veuillez patienter");
			//ListMatch = commObject.getListMatch(); 
			// Affichage de la liste des matchs
    		//Menu.affListMatch(ListMatch);
			
			//Send datagrame in new thread and wait for answer
			matchList = commObject.getListMatchName();   
    		if(matchList == null) return;
    		//Display the available match
    		Menu.affListMatchName(matchList);
       	 		
        	// Choix dans le menu
    		System.out.println("Faites votre choix ?");
    		do{
    			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	    	try{
    	            choix = Integer.parseInt(br.readLine());
    	        }catch(NumberFormatException nfe){
    	            System.err.println("Invalid Format!");
    	            choix = -1;
    	        } catch (IOException e) {
    				e.printStackTrace();
    				choix = -1;
    			}        
    	    	
    		}while(choix == -1);
    		//if((choix > 0)&&(choix <= ListMatch.length)) {
    		logger.info("Choix = " + choix + " and contain = " + matchList.getMatchName().containsKey( (choix-1 ) ));
    		
    		if ((choix > 0)&& (matchList.getMatchName().containsKey(choix-1))){
    	   		logger.info("call to detailMatch");
    			detailMatch(choix-1);
    		}
    		//  recuperation des details du match et affichage
    		
    	}while(choix != 0);
    	// choix == 0 -> exit
    	// other choix -> refresh list
    	
    	System.out.println("Bye :3");
    }
    
    /**
     * Sous menu pour le detail d'un match
     * @param idMatch ID du match choisit
     * @author CharlyBong
     */
    public static void detailMatch(int idMatch){
    	Object Match = null;
    	int choix = 0;
    	do{
    		Match = Communication.getInstance().getMatchDetail(idMatch);
    		if(Match == null) return;
        	// Affichage des infomations
    		Menu.affDetailsMatch(Match);
    		
    		// R�ponse utilisateur
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    	try{
	            choix = Integer.parseInt(br.readLine());
                    if (choix == 1){
                       makeABet(idMatch);
                    }
	        }
	    	catch(NumberFormatException nfe){} 
	    	catch (IOException e) {}        
    		
    	}while(choix != 0);
    	// choix = 0 -> back
    	// else refresh
    }
    
       /**
     * Sous menu pour le detail d'un match
     * @param idMatch ID du match choisit
     * @author CharlyBong
     */
    public static void makeABet(int idMatch){
    	Match oMatch = null;
    	int choix = 0;
    	do{
    		//Récupère l'objet match
                oMatch = Communication.getInstance().getMatchDetail(idMatch);
                
                //Récupération des informations relatives aux équipes
                Team domicile = oMatch.getDomicile();
                Team visiteur = oMatch.getExterieur();                
                
        	// Affichage des infomations
    		Menu.affDetailsMatchPourPari(oMatch);
    		
    		// Réponse utilisateur
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    	try{
	            //On détermine le choix d'équipe pour le pari
                    choix = Integer.parseInt(br.readLine());
                    
                    String teamName = null;
                    
                    //On assosie le nom de l'équipe selon le choix fait
                    if (choix == 1){ teamName = domicile.getName();}
                    if (choix == 2){ teamName = visiteur.getName();}
                    
                    if(teamName != null){
                        
                        System.out.println(" -- ");
		        System.out.println("Veuillez inscrire le montant à parier (Format attendu: 100.50 )");
		        System.out.println(" -- ");
                        
                        //On récupère le montant
                        float montant = Float.parseFloat(br.readLine());
                       
                        //Construction de l'objet de bet
                        DateFormat dateFormat = new SimpleDateFormat("yyy-MM-dd-HH-mm-ss");
                        Date date = new Date();                        
                        
                        Bet b = new Bet(dateFormat.format(date) + "-" + createID(),idMatch, teamName, montant);
                        
                        try {
                            
                            int result = Protocole.sendTCP(b, betServerPort);                   

                            if(result == 1)
                            {
                             //Succès alors on montre un message de succès
                             System.out.println(" -- L'objet Bet suivant -- ");
                             System.out.println(b.toString() + " au montant de: " + String.valueOf(b.getBetAmount()) + "$ en faveur de l'équipe:" + teamName );
                             System.out.println(" -- à été enregistré avec succès! --");
                             
                             //On ajoute le bet courant à notre liste de Bet
                             betHistory.add(b);
                             
                            }
                            else if(result == 0)
                            {
                             System.out.println("l'ajout à echoué, car la période est plus grande que 2");
                            }
                            else
                            {
                                System.out.println("l'ajout à echoué, error de stream"); 
                            }
                
                        } catch (Exception e) {
                            java.util.logging.Logger.getLogger(TCPClient.class.getName()).log(Level.SEVERE, null, e);
                        }
                        
                    }
	        }catch (Exception ex) { 
                    java.util.logging.Logger.getLogger(TCPClient.class.getName()).log(Level.SEVERE, null, ex);
                }        
    		
    	}while(choix != 0);
    	// choix = 0 -> back
    	// else refresh
    }
    
 
    //Generate an unique ID. thanks to atomiclong class
    public static String createID()
    {
        return String.valueOf(betCounter.getAndIncrement());
    }
   
}
