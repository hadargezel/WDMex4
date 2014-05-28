import java.util.Comparator;


public class PageComparator implements Comparator<Page>
{
	public int compare(Page o1, Page o2) 
	{
		return o1.URL.compareTo(o2.URL);
	}
}
