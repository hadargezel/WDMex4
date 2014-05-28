import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class PageRank
{
	double[][] A;
	double[] v;
	WebGraph linksGraph;
	double epsilon;
	HashMap<String, Integer> urlToMatrixIndex;
	HashMap<Integer, String> matrixIndexToUrl;
	
	LinkedHashMap<String, Double> urlsWithRankOrderbyRank;
	
	public PageRank(WebGraph linksGraph, double epsilon) 
	{
		this.urlsWithRankOrderbyRank = null;
		this.linksGraph = linksGraph;
		this.epsilon = epsilon;
		this.initMappings();
		this.initA();
		
		this.v = new double[linksGraph.allPages.size()];
		Arrays.fill(this.v, ((double)1)/((double)linksGraph.allPages.size()) );
	}
	
	private void initMappings()
	{
		this.urlToMatrixIndex = new HashMap<String, Integer>();
		this.matrixIndexToUrl = new HashMap<Integer, String>();

		int i = 0;
		
		for (Page p : this.linksGraph.allPages)
		{
			this.urlToMatrixIndex.put(p.URL, i);
			this.matrixIndexToUrl.put(i, p.URL);
			i++;
		}

	}
	private void initA()
	{		
		this.A = new double[this.linksGraph.allPages.size()][this.linksGraph.allPages.size()];
		
		//walk on webgraph, fill A
		for (Page p : this.linksGraph.allPages)
		{
			
			// fill column of p as a boolean + count number of crawled links from total links
			int countCrawledLinks = 0;
			for (String link : p.links)
			{
				Page temp = new Page(link);
				if (this.linksGraph.allPages.contains(temp))
				{
					countCrawledLinks++;
					this.A[this.urlToMatrixIndex.get(link)][this.urlToMatrixIndex.get(p.URL)] = 1;
				}
			}
			
			double eachLinkProb = (countCrawledLinks != 0) ? (double) 1 / (double)countCrawledLinks : 0;
			for (int i = 0; i < this.linksGraph.allPages.size(); i++)
				if (this.A[i][this.urlToMatrixIndex.get(p.URL)] == 1)
					this.A[i][this.urlToMatrixIndex.get(p.URL)] = eachLinkProb;
		}
	}
	
	private double calculateRoundAv()
	{
		double[] oldV = this.v.clone();
		
		for (int i = 0; i < this.v.length; i++)
		{
			double sum = 0;
			
			for (int j = 0; j < this.v.length; j++)
				sum += this.A[i][j]*oldV[j];
			this.v[i] = sum;
		}
		
		return calculateDistance(oldV, this.v);
	}
	
	// distance between two vectors
	private double calculateDistance(double[] v1, double[] v2)
	{
		if (v1.length != v2.length)
			return -1;
		
		double sumOfPows = 0;
		
		for (int i = 0; i < v1.length; i++)
			sumOfPows += (v1[i]-v2[i])*(v1[i]-v2[i]);
		
		return Math.sqrt(sumOfPows);
	}
	
	// continue multiplying until the delta ("distance") between prob. vector before multiplication and afterwards is smaller than epsilon - which actually means that
	// if we continue the multiplication process until the result will converge into single vector, the differences between each vector before multiplication and the 
	// result are negligible (and a waste of computable time).
	public void findPR()
	{
		double distance = Double.POSITIVE_INFINITY;
		
		while (distance >= this.epsilon)
		{
			distance = calculateRoundAv();
		}
	}
	
	public LinkedHashMap<String, Double> calculateUrlsWithRanksOrderbyRank()
	{
		TreeMap<String, String> rankToUrlMapping = new TreeMap<String, String>(new LetMeHaveDuplicateDoubleKeysComparator());
		LinkedHashMap<String, Double> urlToRankMappingOrderbyRank = new LinkedHashMap<String, Double>();
		HashMap<Double, Integer> currInstanceOfRank = new HashMap<Double, Integer>();
		
		for (int i = 0; i < this.v.length; i++)
		{
			if (!currInstanceOfRank.containsKey(this.v[i]))
				currInstanceOfRank.put(this.v[i], 1);
			else
				currInstanceOfRank.put(this.v[i], currInstanceOfRank.get(this.v[i]) + 1);
			
			String keyWithInstanceCounting = String.valueOf(this.v[i]) + '#' + String.valueOf(currInstanceOfRank.get(this.v[i]));
			rankToUrlMapping.put(keyWithInstanceCounting, this.matrixIndexToUrl.get(i));
		}
		
		// reverse the key-value while keeping order (DESC sorted by the original key == rank)
		for (Entry<String, String> rankToUrl : rankToUrlMapping.descendingMap().entrySet())
			urlToRankMappingOrderbyRank.put(rankToUrl.getValue(), Double.valueOf(rankToUrl.getKey().split("#")[0]));
			
		return urlToRankMappingOrderbyRank;
	}

	public void printTopKPageRanks(String filename, int k)
	{
			PrintWriter writer;
			try
			{
				writer = new PrintWriter(filename, "UTF-8");
				int count = 0;
				
				for (String url : this.getUrlsWithRanksOrderbyRank().keySet())
				{
					writer.write(url + System.getProperty("line.separator"));
					count++;
					if(count == k)
						break;
				}
				writer.close();
			}
			catch (FileNotFoundException | UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
	}

	public LinkedHashMap<String, Double> getUrlsWithRanksOrderbyRank()
	{
		if (this.urlsWithRankOrderbyRank == null)
			this.urlsWithRankOrderbyRank = this.calculateUrlsWithRanksOrderbyRank();
		
		return this.urlsWithRankOrderbyRank;
	}
}

