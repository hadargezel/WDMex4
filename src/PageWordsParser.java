import java.util.ArrayList;
import java.util.TreeSet;


public class PageWordsParser
{
	private String URL;
	private String content;
	private ArrayList<String> uniqueWords;
	private String[] allWords;
	private double[] scores;

	public PageWordsParser(String URL, String content)
	{
		this.URL = URL;
		this.content = content.toLowerCase();
		this.uniqueWords = null;
		this.allWords = null;
		this.scores = null;
	}

	public void extractWords()
	{
		TreeSet<String> uniqueWordsSet = new TreeSet<String>();
		this.allWords = this.content.split(" ");

		for (String word : this.allWords)
		{
			if (!word.isEmpty())
			{
				uniqueWordsSet.add(word.toLowerCase());
			}
		}

		this.uniqueWords = new ArrayList<String>(uniqueWordsSet);
	}

	public void calculateScores()
	{
		this.scores = new double[this.uniqueWords.size()];

		for (String word : this.allWords)
		{
			if (!word.isEmpty())
			{
				int wordIndex = this.uniqueWords.indexOf(word);
				this.scores[wordIndex]++;
			}
		}

		for (int i = 0; i < this.scores.length; i++)
		{
			this.scores[i] /= this.allWords.length;
		}
	}

	public void addWordsAndScoresToInvertedIndex(InvertedIndex wordsDS)
	{
		for (String word : this.uniqueWords)
		{
			Word w = new Word(word);
			if (!wordsDS.words.containsKey(word))
			{
				wordsDS.addWord(w);
			}
			else
			{
				w = wordsDS.words.get(word);
			}
			int wordIndex = this.uniqueWords.indexOf(word);
			w.pageUrlToScore.put(this.URL, this.scores[wordIndex]); 
		}

	}
}



