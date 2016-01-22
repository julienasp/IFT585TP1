package affichage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import dataObject.ListMatchName;
import dataObject.Match;
import dataObject.Team;

public class Menu {

	/**
	 * Affichage du tableau des match
	 * @param ListMatch Liste des matchs  afficher
	 * @author CharlyBong
	 * @deprecated use affListMatchName(ListMatchName ListMatch)
	 */
	public static void affListMatch(Object[] ListMatch){
		System.out.println(" -- ");
		for(int i=0; i<ListMatch.length;i++){
			int a = i + 1;
			if(ListMatch[i] != null) System.out.println(" "+a+" - "+ListMatch[i].toString());
		}
		System.out.println(" 0 - exit");
		System.out.println(" -- ");
	}

	/**
	 * Affichage de la liste des matchs (Domicile vs Exterieur - timer)
	 * @param ListMatch Liste des matchs  afficher
	 * @author Uldax
	 */
	@SuppressWarnings("rawtypes")
	public static void affListMatchName(ListMatchName ListMatch){
		System.out.println(" -- ");		
		HashMap<Integer, String> map = ListMatch.getMatchName();
		Iterator<Entry<Integer, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
	        System.out.println("(id match :" +pair.getKey()+ " Press "+ ((int)pair.getKey()+1) + " for detail) = " + pair.getValue());
	        //it.remove(); // avoids a ConcurrentModificationException
	    }	
		System.out.println(" 0 - exit");
		System.out.println(" -- ");
	}
	
	/**
	 * Affichage des details d'un match
	 * @param Match objet  afficher
	 * @author CharlyBong
	 */
	public static void affDetailsMatch(Object Match){
		System.out.println(" -- ");
		System.out.println(Match.toString());
		System.out.println(" 0 - back");
                System.out.println(" 1 - faire un pari sur ce match");
		System.out.println(" -- ");
	}
        
        /**
	 * Affichage des details d'un match
	 * @param Match objet  afficher
	 * @author CharlyBong
	 */
	public static void affDetailsMatchPourPari(Match oMatch){
                Team domicile = oMatch.getDomicile();
                Team visiteur = oMatch.getExterieur();
		System.out.println(" -- ");
		System.out.println(oMatch.toString());
		System.out.println(" 0 - back");
                System.out.println(" 1 - Parier sur l'équipe domicile - " + domicile.getName());
                System.out.println(" 2 - Parier sur l'équipe visiteur - " + visiteur.getName());
		System.out.println(" -- ");
	}
	
	/**
	 * Thread pour l'affichage de point d'indiquand  l'utilisateur d'attendre
	 * @author CharlyBong
	 */
	public static class WaitMessage implements Runnable {
		private int timer = 0;

		public WaitMessage(int time) {
			this.timer = time;
		}
		
		@Override
		public void run() {
			while(true) {
				System.out.print(".");	        	
	            try {
	                Thread.sleep(this.timer);
	            } catch (InterruptedException e) {
	            	System.out.println("");
	            	Thread.currentThread().interrupt();
	            	break;
	            }
	        }
		}

	}
	
}
 
