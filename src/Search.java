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
	private Queue<String> crawlingQueue;
	private Set<String> alreadyInQueue;
	private HashSet<Relation> royalRelationCandidates;
	private HashSet<Page> royalPages;


	public Search(String startingURL)
	{
		this.startingURL = startingURL;
		this.crawlingQueue = new LinkedList<String>();
		this.alreadyInQueue = new TreeSet<String>();
		this.royalRelationCandidates = new HashSet<Relation>();
		this.royalPages = new HashSet<Page>();
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
				}
				this.alreadyInQueue.add(nextCrawlURL);
				counter++;
			}
		}
	}

	
	public List<String> extractLinksFromSentence(String sentence)
	{
		/*This function extract the links from each sentence and omits the empty 
		 * links - links that lead to empty pages in wikipedia also, omits cite notes
		 */
		List <String> links = new LinkedList<String>();
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
						// add it to the candidates set
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
			res += p;
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
		s.crawl(50); // crawl, stop crawling after 50 diff. urls (in addition to the start page)
		s.filterCandidatesAndPrintResultToFile("royal.txt");//print results to file
		System.out.println("done");
	}


	private void filterCandidatesAndPrintResultToFile()//overloading if wishes not to write to file.
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

}
