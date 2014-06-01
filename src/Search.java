import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;



public class Search
{
	private String startingURL;
	private WebGraph linksGraph;
	private InvertedIndex words;
	private Queue<String> crawlingQueue;//why double init?! = new LinkedList<String>();
	private Set<String> alreadyInQueue;//why double init?! = new TreeSet<String>();
	private HashSet<Relation> royalRelationCandidates;
	private HashSet<Page> royalPages;


	public Search(String startingURL)
	{
		this.startingURL = startingURL;
		//this.linksGraph = new WebGraph(new Page(this.startingURL));
		//this.words = new InvertedIndex();
		this.crawlingQueue = new LinkedList<String>();
		this.alreadyInQueue = new TreeSet<String>();
		this.royalRelationCandidates = new HashSet<Relation>();
		this.royalPages = new HashSet<Page>();
	}


	public LinkedHashMap<String, Double> TA(ArrayList<LinkedHashMap<String, Double>> rankMaps, int k)
	{
		ArrayList<Iterator<Entry<String, Double>>> rankMapIteratorArr = new ArrayList<Iterator<Entry<String, Double>>>(rankMaps.size());
		for (LinkedHashMap<String, Double> rankMap : rankMaps)
			rankMapIteratorArr.add(rankMap.entrySet().iterator());

		HashMap<Integer, Entry<String, Double>> minPerListInSortedAccess = new HashMap<Integer, Entry<String, Double>>(rankMaps.size());

		double[] maxRankPerList = new double[rankMaps.size()];

		// init threshold
		double threshold = 0.0;
		for (int i = 0; i < rankMaps.size(); i++)
		{
			LinkedHashMap<String, Double> rankMap = rankMaps.get(i);

			double maxRankInList = 0.0;
			if (rankMap.size() > 0)
			{
				maxRankInList = rankMap.get(rankMap.keySet().iterator().next());
				maxRankPerList[i] = maxRankInList;
				threshold = updateThreshold(threshold, rankMaps.size(), i, 0, maxRankInList);
			}
		}
		TreeMap<String, String> topK = new TreeMap<String, String>(new LetMeHaveDuplicateDoubleKeysComparator());
		HashMap<Double, Integer> currInstanceOfTotalRank = new HashMap<Double, Integer>();

		HashMap<String, Boolean> seenIdsDecisions = new HashMap<String, Boolean>();

		boolean[] isRankListEnded = new boolean[rankMaps.size()];

		while ((topK.size() < k) || (Double.valueOf(topK.firstKey().split("#")[0]) < threshold))
		{
			// round of "parallel" sorted access to all rank lists
			for (int i = 0; i < rankMaps.size(); i++)
			{
				Entry<String, Double> currMinItem = minPerListInSortedAccess.get(i);
				Entry<String, Double> nextItemWithLowerScore = null;

				Iterator<Entry<String, Double>> rankListIterator = rankMapIteratorArr.get(i);
				if (rankListIterator.hasNext())
				{
					// walk down on the i-th ranks list
					nextItemWithLowerScore = rankListIterator.next();
					// update the lists minimum items hash table
					minPerListInSortedAccess.put(i, nextItemWithLowerScore);

					// if item not already seen
					if (!seenIdsDecisions.containsKey(nextItemWithLowerScore.getKey()))
					{
						// now its seen
						seenIdsDecisions.put(nextItemWithLowerScore.getKey(), false);

						// calculate total rank
						double total = calculateTotalRank(rankMaps, nextItemWithLowerScore.getKey());

						// add to topK if there aren't K items yet OR if its score is better than the lowest score
						boolean lessthanK = (topK.size() < k);
						if (lessthanK || (Double.valueOf(topK.firstKey().split("#")[0]) < total) )
						{
							// remove lowest item (we're replacing it with the better-total item)
							if (!lessthanK)
								topK.pollFirstEntry();

							if (!currInstanceOfTotalRank.containsKey(total))
								currInstanceOfTotalRank.put(total, 1);
							else
								currInstanceOfTotalRank.put(total, currInstanceOfTotalRank.get(total + 1));

							String keyWithInstanceCounting = String.valueOf(total) + '#' + String.valueOf(currInstanceOfTotalRank.get(total));
							topK.put(keyWithInstanceCounting, nextItemWithLowerScore.getKey());

							// mark that it's taken
							seenIdsDecisions.put(nextItemWithLowerScore.getKey(), true);
						}
					}

					/* after handling the next element in the i-th list (sorted access), the max score it can contribute is the score of that next element
					proof: let's assume there's upper score of some element and the element not taken  - we continued, which means that the threshold was bigger than
					the smallest total score, but the element wasn't taken which means it was smaller than the lowest total score, so it wouldn't be taken in the future 
					update the threshold if we're not at the first element of the list */
					if (currMinItem != null)
						threshold = updateThreshold(threshold, rankMaps.size(), i, currMinItem.getValue(), nextItemWithLowerScore.getValue());

				}
				/* we've reached the end of the rank list, so this list can contribute to the total rank only 0 from now on */
				else
				{
					// first time iterator is empty. update potential threshold - this rank list can contribute only 0 to total
					if (!isRankListEnded[i])
					{
						double oldScoreContributedToThreshold = 0.0;
						if (currMinItem != null)
							oldScoreContributedToThreshold = currMinItem.getValue();

						threshold = updateThreshold(threshold, rankMaps.size(), i, oldScoreContributedToThreshold, 0.0);
						isRankListEnded[i] = true;
					}
				}
			}
		}
		LinkedHashMap<String, Double> idWithScoreOrderbyScore = new LinkedHashMap<String, Double>();

		for (Entry<String, String> scoreWithId : topK.descendingMap().entrySet())
			idWithScoreOrderbyScore.put(scoreWithId.getValue(), Double.valueOf(scoreWithId.getKey().split("#")[0]));

		return idWithScoreOrderbyScore;
	}

	/* specific to the aggregation func
	aggregation func: 5%-pagerank + 95%-avg(each word rank) 
	The following weights have been chosen in order to maintain balance of the following considerations:
		1. page with many instances of some word from the query, but really low Page Rank.
		2. page having few instances of the word, but has very high page rank.
	after testing with several examples (such as 50-50) we have concluded that the weights below are the most suitable.
	convention: first rank list is of the pagerank
	 */
	private double calculateTotalRank(ArrayList<LinkedHashMap<String, Double>> rankMaps, String id) 
	{
		double totalWordsRank = 0.0;
		double pageRank = 0.0;
		double numOfRanks = rankMaps.size();

		for (int i = 0; i < rankMaps.size(); i++)
		{
			Double rankOfId = rankMaps.get(i).get(id);

			if (i == 0)
				pageRank = rankOfId;
			else if (rankOfId != null)
				totalWordsRank += rankOfId;
		}

		return ( 0.95*(totalWordsRank / (numOfRanks-1.0)) + 0.05*pageRank );
	}



	// Specific to the aggregation func
	// aggregation func: 5%-pagerank + 95%-avg(each word rank) 
	// 	convention: first rank list is of the pagerank
	public double updateThreshold(double threshold, double numOfRankMaps, int rankListIndexThatChanged, double oldVal, double newVal)
	{
		if (rankListIndexThatChanged == 0) //pagerank!
			return (threshold - oldVal*0.05 + newVal*0.05);
		else
			return (threshold - 0.95*oldVal/(numOfRankMaps-1.0) + 0.95*newVal/(numOfRankMaps-1.0));
	}

	public void crawl(int crawlPagesLimit)
	{
		int counter = 0;
		String nextCrawlURL = this.startingURL;
		this.crawlingQueue.add(nextCrawlURL);

		while ((nextCrawlURL = crawlingQueue.poll()) != null && counter <= crawlPagesLimit )
		{
			if (!alreadyInQueue.contains(nextCrawlURL))
			{
				String content = fetchPage(nextCrawlURL);
				System.out.println("crawling: "+nextCrawlURL);
				if (!content.isEmpty())
				{
					Page p = new Page(nextCrawlURL);
					PageEvaluator PE = new PageEvaluator(content);
					if(PE.pageEvaluation())
					{
						this.royalPages.add(p);
						System.out.println("added to royal! : "+p.URL);
						addLinksToQueueAndRoyalRelationCandidates(content, p);
					}
					//old pos. this.linksGraph.addNewPage(p);


					// evaluate II - the "entity" (page) itself - based its content (evaluate I - the decision to crawl on the link to this page)

					//this.linksGraph.addNewPage(p);
					/*
					PageWordsParser parser = new PageWordsParser(nextCrawlURL, content);
					parser.extractWords();
					parser.calculateScores();
					parser.addWordsAndScoresToInvertedIndex(this.words);
					 */
				}
				this.alreadyInQueue.add(nextCrawlURL);
				counter++;
			}
		}
	}

	//This function extract the links from each sentence and omits the empty links - links that lead to empty pages in wikipedia
	// also, omits cite notes
	public List<String> extractLinksFromSentence(String sentence)
	{
		/*

		String linkElementPattern = "(?i)<a([^>]+)>(.+?)</a>";
		Pattern patt = Pattern.compile(linkElementPattern);
		 */
		List <String> links = new LinkedList<String>();
		/*
		Matcher m = patt.matcher(sentence);

		while (m.find())
			if(!m.group(1).toLowerCase().contains("not yet started"))//check if not empty link
				links.add(m.group(1));
		 */
		Document doc = Jsoup.parse(sentence);
		Elements linkElements = doc.select("a");
		for(Element link : linkElements)
		{
			String linkStr = link.toString();
			if (!linkStr.contains("cite_note") && !linkStr.contains("not yet started"))
				links.add(linkStr);
		}

		return links;
	}

	public String extractUrlFromLinkElement(String link)
	{
		Document doc = Jsoup.parse(link);
		Element linkElement = doc.select("a").first();
		return linkElement.attr("href");
		/*
		String urlInHrefRegex = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
		Pattern urlPattern = Pattern.compile(urlInHrefRegex);
		Matcher m = urlPattern.matcher(link);

		if (m.find())
			return m.group(2);

		return "";
		 */
	}
	public void addLinksToQueueAndRoyalRelationCandidates(String content, Page p)
	{
		String firstP = extractFirstParagraph(content);
		List <String>  sentences = generateSentencesFromParagraph(firstP);

		for(String sentence : sentences)
		{
			List<String> links = extractLinksFromSentence(sentence);
			LinksEvaluator eval = new LinksEvaluator(sentence);
			for(String link : links)
			{
				if (LinksEvaluator.isWorthyLinkBased(link) || eval.getSentenceEvalResult())
				{
					// get the url from the whole link element
					String url = this.extractUrlFromLinkElement(link);

					if (url != null)
					{
						url = "http://simple.wikipedia.org" + url;
						// add to queue (if not already in it)
						if (!alreadyInQueue.contains(url))
						{
							this.crawlingQueue.add(url);
						}

						// (Page, Page(url)) is candidate for being royal relation
						Relation candidate = new Relation(p, new Page(url));
						// add it to the candidates set TODO: remember that currently it's possible for a candidate royal1, royal2 but we will not crawl on royal2 at the end
						this.royalRelationCandidates.add(candidate);
					}
				}
			}
		}
	}

	private List <String> generateSentencesFromParagraph(String firstP) {
		LinkedList<String> result = new LinkedList<String>();
		for(String s : firstP.split("[.]"))
		{
			if(!s.isEmpty())
				result.add(s);
		}
		return result;
	}


	private String extractFirstParagraph(String content) {
		String toc = "<div id=\"toc\" class=\"toc\">";
		String headline = "<span class=\"mw-headline\""; //alternative ending for the first paragraph.
		String initialChunk;
		String res="";
		if(content.contains(toc))
		{
			initialChunk = content.split(toc)[0];
		}
		else
		{
			initialChunk = content.split(headline)[0];
		}

		Document doc = Jsoup.parse(initialChunk);
		Elements paragraphs = doc.select("p");
		for(Element p : paragraphs)
		{
			res += p;
			//System.out.println(p.text());
		}
		/*
		if(!initialChunk.isEmpty())
		{
			int firstIndexOfPtag = initialChunk.indexOf("<p>");
			if(firstIndexOfPtag == -1)
				return "";//according to guidance links must be found in first paragraph.   
			int lastIndexOfPtag = initialChunk.lastIndexOf("</p>");//, firstIndexOfPtag);//under the assumption that the html file is properly written and there must be a <p> opening tag.
			String paragraphs = initialChunk.substring(firstIndexOfPtag,lastIndexOfPtag + "</p>".length());
			for(String s : paragraphs.split("<p>"))
			{
				if(!s.isEmpty())
				{
					res+= s.substring(0, s.indexOf("</p>")) +"."; // the dot was added in order to make sure that each paragraph ends with a dot.
				}
			}
		}
		 */
		if(res == "") System.out.println("Empty split according to 2 cretiria");
		return res;
	}


	public String fetchPage(String urlAddress)
	{
		URL url;
		InputStream inputStream = null;
		BufferedReader bufferedReader;
		String content="";
		String line;
		try {
			url = new URL(urlAddress);
			inputStream = url.openStream();  // throws an IOException
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			while ((line = bufferedReader.readLine()) != null) {
				content+=line;
			}
		}
		catch (MalformedURLException mue) 
		{
			mue.printStackTrace();
		}
		catch (IOException ioe) 
		{
			ioe.printStackTrace();
		} 
		finally 
		{
			try 
			{
				if (inputStream != null) inputStream.close();
			}
			catch (IOException ioe) 
			{
				ioe.printStackTrace();
			}

		}
		return content;
	}

	public static void main(String[] args)
	{
		Search s = new Search("http://simple.wikipedia.org/wiki/Prince_William,_Duke_of_Cambridge");
		s.crawl(50); // crawl, stop crawling after 200 diff. urls (in addition to the start page)
		s.filterCandidatesAndPrintResultToFile("royal.txt");
		System.out.println("done");
		//s.printPagesToFile("urls.txt");
		//PageRank pr = new PageRank(s.linksGraph, 0.00005); // init PageRankAlgorithm obj. with the stopping cond. - epsilon = 0.00005
		//pr.findPR(); // calculate Page Ranks
		//pr.printTopKPageRanks("rank.txt", 5);

		//String query = "";
		//Scanner scanner = new Scanner(System.in);
		//System.out.println("Enter keywords: ");
		//query = scanner.nextLine();

		//while(!query.equals("exit"))
		//{
		//String[] queryWords = query.toLowerCase().split(" ");

		//ArrayList<LinkedHashMap<String, Double>> rankMaps = new ArrayList<LinkedHashMap<String, Double>>(queryWords.length);
		//rankMaps.add(pr.getUrlsWithRanksOrderbyRank()); //add PageRank, convention with the aggregation func: pageRank always the first list

		// for each word, add its rank list
		//for (String word : queryWords)
		//{
		//Word wordInDB = s.words.words.get(word);
		//LinkedHashMap<String, Double> rankListOfWord = (wordInDB != null) ? wordInDB.getPageToScoreMapOrderbyScore() : InvertedIndex.getEmptyPageToScoreMap();
		//rankMaps.add(rankListOfWord); 
		//}

		//LinkedHashMap<String, Double> topUrlsWithScores = s.TA(rankMaps, 5); // compute top 5 results using Threshold Algorithm
		//printTAtopKresults(topUrlsWithScores, 5);

		//System.out.println("Enter keywords: ");
		//query = scanner.nextLine();
		//}

		//scanner.close();
	}


	private void filterCandidatesAndPrintResultToFile()
	{
		filterCandidatesAndPrintResultToFile("");
	}
	
	
	private void filterCandidatesAndPrintResultToFile(String fileName) 
	{
		Iterator<Relation> candidatesIter = royalRelationCandidates.iterator();
		PrintWriter writer = null;
		if(!fileName.isEmpty())
		try 
		{
			writer = new PrintWriter(fileName, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		while(candidatesIter.hasNext())
		{
			Relation candidateRelation = candidatesIter.next();
			if(!royalPages.contains(candidateRelation.x) || !royalPages.contains(candidateRelation.y))
			{
				candidatesIter.remove();
			}
			else
			{
				if(writer != null)
				{
					writer.write(candidateRelation.x.URL + " related_to " + candidateRelation.y.URL + System.getProperty("line.separator"));
				}
			}

		}
		writer.close();
	}

	/*
	private static void printTAtopKresults(LinkedHashMap<String, Double> topUrlsWithScores, int k)
	{
		int count = 0;

		for (String url : topUrlsWithScores.keySet())
		{
			System.out.println(url);
			count++;
			if(count == k)
				break;
		}
	}

	private void printPagesToFile(String filename) 
	{
		PrintWriter writer;
		try
		{
			writer = new PrintWriter(filename, "UTF-8");
			for (Page p : this.linksGraph.allPages)
				writer.write(p.URL + System.getProperty("line.separator"));
			writer.close();
		}
		catch (FileNotFoundException | UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}


	}
	 */
}
