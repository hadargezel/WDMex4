
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class PageEvaluator {
	
	String content;
	
	private static final String[] ROYAL_RANKS = new String[] 
			{ "prince", "princess", "king", "queen", "duchess", "earl", "duke", "member of the royal family"};
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
		/*This function check if there are at least 10 occurrences of a specific royal rank and if at least 2.5% of the words
		 * are royal ranks.
		 * For short wiki pages (700 words) since the royal static analysis doesn't match, we check that the
		 * first sentence contains a "undoubted" phrase - "member of the british royal family"
		 *  */
		Document doc = Jsoup.parse(content);
		
		String pageTextOnly = doc.text();
		String stripped = pageTextOnly.replaceAll("[^a-z]", " ");
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
		if(royalPassedTen && (( (double)totalRoyalCounter / (double)contentArray.length ) > 0.025))
			return true;
		
		
		if ( (contentArray.length <= 700) && (pageTextOnly.substring(0, pageTextOnly.indexOf(".")).contains("member of the british royal family")) )
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
		else if (nameRankLocationHeading.getValue().isEmpty()) // heading is for example "queen", "prince of wales" etc. (there isn't any word that can be candidate as "name" of person)
			return false;
		
		String name = nameRankLocationHeading.getValue();
		Document doc = Jsoup.parse(content);
		String textOnly = doc.text();
		for(String sentence: textOnly.split("[.]"))
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
		
		wordsCloneB.removeAll(wordsCloneA); //removing royal ranks from heading
		wordsCloneB.removeAll(words); //removing locations
		wordsCloneB.remove(RomanDigits);
		// removing moderating words:
		wordsCloneB.remove("of");
		wordsCloneB.remove("the");
		wordsCloneB.remove("");
		
		if (wordsCloneB.size() == 0)
			return new AbstractMap.SimpleEntry<Boolean, String>(false,"");
		if((words.size() > 0) && (!RomanDigits.isEmpty() || wordsCloneA.size() > 0)) //Location + (Rank or Roman Digit)
		{
			return new AbstractMap.SimpleEntry<Boolean, String>(true,"");
		}
		else
		{
			String value = "";
			if(isHeading) //extracting "Name" from heading 
				value = wordsCloneB.iterator().next();
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
			if(m.group(1).contains("firstheading"))//check if first heading
			{
				s = m.group(2);
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
