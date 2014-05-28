import java.util.TreeSet;


public class WebGraph 
{
	Page root;
	TreeSet<Page> allPages;
	
	public WebGraph(Page root) 
	{
		this.root = root;
		this.allPages = new TreeSet<Page>(new PageComparator());
	}

	// 18.04 aner
	public void addNewPage(Page p)
	{
		p.initLinks();
		this.allPages.add(p);
	}
}
