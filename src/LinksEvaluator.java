import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class LinksEvaluator
{
	String sentence;
	boolean isWorthyBySentenceEvaluated;
	boolean isWorthyBySenteceEvaluationResult; 
	
	public LinksEvaluator(String sentence) 
	{
		this.sentence = sentence.toLowerCase();
		this.isWorthyBySentenceEvaluated = false;
		this.isWorthyBySenteceEvaluationResult = false;
	}


	private static final String[] FAMILY_TREE_RELATIONS = new String[] 
			{ "son", "grandson", "married", "husband", "wife",  "daughter", "granddaughter", "grandfather"};
	private static final Set<String> FAMILY_TREE_RELATIONS_SET = new HashSet<String>(Arrays.asList(FAMILY_TREE_RELATIONS));
	
	private static final String[] ROYAL_RANKS = new String[] 
			{ "prince", "princess", "king", "queen", "dutchess", "earl", "duke", "member of the royal family"};//of the united kingdom"
	private static final Set<String> ROYAL_RANKS_SET = new HashSet<String>(Arrays.asList(ROYAL_RANKS));
	
	 
	public static boolean isWorthyLinkBased(String link)
	{
		link = link.toLowerCase();
		link = link.replace("</a>", "");
		link = link.substring(link.lastIndexOf(">")+1);
		if(link.contains(","))
		{
			Set<String> linkTitleWords = new HashSet<String> (Arrays.asList(link.split(",")[1].split(" ")));
			linkTitleWords.retainAll(ROYAL_RANKS_SET);
			if(linkTitleWords.size() > 0)
				return true;	
		}
		Set<String> wordsInLinkSet = new HashSet<String> (Arrays.asList(link.split(" ")));
		Set<String> wordsInLinkSetClone = new HashSet<String>(wordsInLinkSet);
		wordsInLinkSetClone.retainAll(ROYAL_RANKS_SET);
		if(wordsInLinkSetClone.size() > 0)
			return true;
		for(String word : wordsInLinkSet)
		{
			if(word.matches("(i|v|x)+")) //Checking regex for Roman digits
				return true;
		}
		return false;
	}
	
	public boolean getSentenceEvalResult() {
		if(!isWorthyBySentenceEvaluated)
		{
			//TODO:compute and change this flag to true and set result accordingly;
		}
		return isWorthyBySenteceEvaluationResult;
	}
}
