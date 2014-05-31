import java.security.acl.LastOwnerException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageEvaluator {
	
	String content;
	
	private static final String[] FAMILY_TREE_RELATIONS = new String[] 
			{ "son", "sons", "grandson", "grandsons", "married", "husband", "wife",  "daughter", "daughters", "granddaughter", "granddaughters", "grandfather", "grandmother"};
	private static final Set<String> FAMILY_TREE_RELATIONS_SET = new HashSet<String>(Arrays.asList(FAMILY_TREE_RELATIONS));
	
	private static final String[] ROYAL_RANKS = new String[] 
			{ "prince", "princess", "king", "queen", "dutchess", "earl", "duke", "member of the royal family"};//of the united kingdom"
	private static final Set<String> ROYAL_RANKS_SET = new HashSet<String>(Arrays.asList(ROYAL_RANKS));
	
	private static final String [] COUNTIES_AND_CITIES_UK = new String[] {"edinburgh","cornwall","cambridge","wales","york","wessex","gloucester","kent"};
	private static final Set<String> COUNTIES_AND_CITIES_UK_SET = new HashSet<String>(Arrays.asList(COUNTIES_AND_CITIES_UK));
	
	private static final HashMap<String, Integer> ROYAL_RANKS_MAP = buildHashMapFromArray(ROYAL_RANKS);
	
	public PageEvaluator(String content) 
	{
		this.content = content.toLowerCase();
	}
	
	
	public boolean checkRoyalOccurrencesAndPercentage()
	{
		/*This function check if there are at least 10 occurrences of a specific royal rank and if at least 5% of the words
		 * are royal ranks
		 *  */
		String stripped = content.replaceAll("<.*?>", " "); 
		stripped = stripped.replaceAll("[(),.;:]", " ");
		String [] contentArray = stripped.split(" +",0);
		int [] countRoyal = new int[ROYAL_RANKS.length];
		boolean royalPassedTen = false;
		int totalRoyalCounter = 0;
		for(String word : contentArray)
		{
			Integer index = ROYAL_RANKS_MAP.get(word);
			if(index != null)
			{
				countRoyal[index]++;
				totalRoyalCounter++;
				if(countRoyal[index] >= 10)
					royalPassedTen = true;
			}
		}
		/*TODO: continue debug from here*/
		if(royalPassedTen && (( (double)totalRoyalCounter / (double)contentArray.length ) > 0.05))
			return true;
		return false;
	}
	
	public boolean pageEvaluation()
	{
		if(!checkRoyalOccurrencesAndPercentage()) //see function documentation
			return false;
		String heading = retrieveHeading();
		Map.Entry<Boolean, String> nameRankLocationHeading = checkTextForNameRankLocation(heading,true); 
		if(nameRankLocationHeading.getKey())
			return true;
		String name = nameRankLocationHeading.getValue();
		for(String sentence: content.split("[.]"))
			if(!sentence.isEmpty() && sentence.contains(name) && checkTextForNameRankLocation(sentence).getKey())
				return true;
		return false;
	}


	private Map.Entry<Boolean, String> checkTextForNameRankLocation(String text){
		return checkTextForNameRankLocation(text, false);
	}
	
	private Map.Entry<Boolean, String> checkTextForNameRankLocation(String text, boolean isHeading) {
		String strippedText = text.replaceAll("[(),.;:]", "");
		Set<String> words = new LinkedHashSet<String> (Arrays.asList(strippedText.split(" ")));
		Set<String> wordsCloneA = new LinkedHashSet<String>(words);
		Set<String> wordsCloneB = new LinkedHashSet<String>(words);
		wordsCloneA.retainAll(ROYAL_RANKS_SET);
		words.retainAll(COUNTIES_AND_CITIES_UK_SET);
		/*
		 * words = matches locations
		 * wordsCloneA = matches Royal Rank
		 * wordsCloneB = original words (used to match Roman digits and later to extract "Name")
		 * */
		String RomanDigits = "";
		for(String word : wordsCloneB)
		{
			if(word.matches("(i|v|x)+"))
			{
				RomanDigits = word;
				break;
			} 
		}
		if((words.size() > 0) && (!RomanDigits.isEmpty() || wordsCloneA.size() > 0)) //Location + (Rank or Roman Digit)
		{
			return new AbstractMap.SimpleEntry<Boolean, String>(true,"");
		}
		else
		{
			String value = "";
			if(isHeading) //extracting "Name" from heading 
			{
				wordsCloneB.removeAll(wordsCloneA); //removing royal ranks from heading
				wordsCloneB.removeAll(words); //removing locations
				wordsCloneB.remove("of");
				wordsCloneB.remove(RomanDigits);
				wordsCloneB.remove("");
				value = wordsCloneB.iterator().next();
			}
			return new	AbstractMap.SimpleEntry<Boolean, String>(false,value);
		}

	}
	
	
	public String retrieveHeading() //under the assumption that each wiki has a heading
	{
		String HeadingPattern = "(?i)<h1([^>]+)>(.+?)</h1>";
		Pattern patt = Pattern.compile(HeadingPattern);
		Matcher m = patt.matcher(this.content);
		String s = "";
		while (m.find())
		{
			s = m.group(1);
			if(s.contains("firstheading"))//check if first heading
			{
				s = s.substring(0,s.lastIndexOf("</span>"));
				s = s.substring(s.lastIndexOf(">")+1);
			}
		}
		return s;
	}
	
	private static HashMap<String, Integer> buildHashMapFromArray(String [] array)
	{
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		for (int i = 0; i < array.length; i++) 
		{
			result.put(array[i], i);
		}
		return result;
		
	}
	
	
}
