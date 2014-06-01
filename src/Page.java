import java.util.TreeSet;


public class Page{
	String URL;
	
	
	public Page(String URL)
	{
		this.URL = URL;
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

	@Override
	public int hashCode() {
		return URL.hashCode();
	}
	
	
}

