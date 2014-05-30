import java.security.acl.LastOwnerException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
	
	
	public boolean checkRoyalOccurrences()
	{
		
		String stripped = content.replaceAll("(,|.|;|:)", "");
		String [] contentArray = stripped.split(" ");
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
		if(royalPassedTen && (( (double)totalRoyalCounter / (double)contentArray.length ) > 0.05))
			return true;
		return false;
	}
	
	public boolean nameRankLocation()
	{
		/*
		 * 
		 * TODO: NEED TO COMPLETE 
		 * 
		 * */
		
		return false;
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
