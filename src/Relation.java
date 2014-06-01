
public class Relation {

	Page x;
	Page y;


public Relation(Page x, Page y) {
	this.x = x;
	this.y = y;
}



@Override
public boolean equals(Object o) {
	if(o.getClass() == this.getClass())
	{
		if( (this.x.equals(((Relation)o).x) && this.y.equals(((Relation)o).y) ) ||
			(this.y.equals(((Relation)o).x) && this.x.equals(((Relation)o).y) )) //relation(x,y) = relation(y,x) 
		{
			return true;
		}
		else
			return false;
	}
	return super.equals(o);
}



@Override
public int hashCode() 
{
	int val = this.x.URL.compareTo(this.y.URL);
	if(val > 0)
	{
		return (this.x.URL + this.y.URL).hashCode();
	}
	else
		return (this.y.URL + this.x.URL).hashCode();
}




}
