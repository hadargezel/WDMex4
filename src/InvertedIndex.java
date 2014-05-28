import java.util.LinkedHashMap;
import java.util.TreeMap;


public class InvertedIndex 
{
	TreeMap<String, Word> words;
	
	public InvertedIndex()
	{
		this.words = new TreeMap<String, Word>();
	}
	
	public void addWord(Word word)
	{
		word.initPageUrlToScore();
		this.words.put(word.word, word);
	}
	
	public static LinkedHashMap<String, Double> getEmptyPageToScoreMap()
	{
		return new LinkedHashMap<String, Double>();
	}
}
