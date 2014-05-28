import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Word
{
	String word;
	TreeMap<String, Double> pageUrlToScore;
	
	public Word(String word) 
	{
		this.word = word;
		this.pageUrlToScore = null;
	}
	
	public void initPageUrlToScore()
	{
		this.pageUrlToScore = new TreeMap<String,Double>();
	}
	
	public LinkedHashMap<String, Double> getPageToScoreMapOrderbyScore()
	{		
		HashMap<Double, Integer> currInstanceOfRank = new HashMap<Double, Integer>();
		TreeMap<String, String> ScoreToPageOrderbyScore = new TreeMap<String, String>(new LetMeHaveDuplicateDoubleKeysComparator());
		
		LinkedHashMap<String, Double> pageToScoreMapOrderbyScore = new LinkedHashMap<String, Double>();

		for (Entry<String, Double> pageWithScore : this.pageUrlToScore.entrySet())
		{
			Double rank = pageWithScore.getValue();
			if (!currInstanceOfRank.containsKey(rank))
				currInstanceOfRank.put(rank, 1);
			else
				currInstanceOfRank.put(rank, currInstanceOfRank.get(rank) + 1);
			
			String keyWithInstanceCounting = String.valueOf(rank) + '#' + String.valueOf(currInstanceOfRank.get(rank));
			ScoreToPageOrderbyScore.put(keyWithInstanceCounting, pageWithScore.getKey());
		}
		// create hash map from ScoreToPage that already ordered by Score	
		for (Entry<String, String> ScoreToPage : ScoreToPageOrderbyScore.descendingMap().entrySet())
			pageToScoreMapOrderbyScore.put(ScoreToPage.getValue(), Double.valueOf(ScoreToPage.getKey().split("#")[0]));
		
		return pageToScoreMapOrderbyScore;
	}
	
	
}
