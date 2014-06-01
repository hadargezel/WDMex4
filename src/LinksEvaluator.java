import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


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
			{ "son", "sons", "grandson", "grandsons", "married", "husband", "wife",  "daughter", "daughters", "granddaughter", "granddaughters", "grandfather", "grandmother"};
	private static final Set<String> FAMILY_TREE_RELATIONS_SET = new HashSet<String>(Arrays.asList(FAMILY_TREE_RELATIONS));
	
	private static final String[] ROYAL_RANKS = new String[] 
			{ "prince", "princess", "king", "queen", "duchess", "earl", "duke", "member of the royal family"};
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
	
	public boolean getSentenceEvalResult()
	{
		if(!isWorthyBySentenceEvaluated)
		{
			String cleanStentence = Jsoup.parse(this.sentence.toLowerCase()).text().replaceAll("[^a-z]", " ");
			Set<String> wordsInSentence = new HashSet<String> (Arrays.asList(cleanStentence.split(" ")));
			Set<String> wordsInSentenceClone = new HashSet<String>(wordsInSentence);
			wordsInSentence.retainAll(ROYAL_RANKS_SET);
			wordsInSentenceClone.retainAll(FAMILY_TREE_RELATIONS_SET);
			
			if ( (wordsInSentence.size() > 0) && (wordsInSentenceClone.size() > 0) )
				this.isWorthyBySenteceEvaluationResult = true;
			
			this.isWorthyBySentenceEvaluated = true;
		}
		
		return isWorthyBySenteceEvaluationResult;
	}
}
