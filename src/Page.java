import java.util.TreeSet;


public class Page{
	String URL;
	TreeSet<String> links;
	
	
	public Page(String URL)
	{
		this.URL = URL;
		this.links = null;
	}
	
	public void initLinks()
	{
		this.links = new TreeSet<String>();
	}

	@Override
	public boolean equals(Object arg0) {
		if(arg0.getClass() == this.getClass())
		{
			return this.URL.equals(((Page)arg0).URL);
		}
		else
		{
			return super.equals(arg0);
		}
	}
	
	
}

