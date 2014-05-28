import java.util.Comparator;


public class LetMeHaveDuplicateDoubleKeysComparator implements Comparator<String>
{

	public int compare(String key1, String key2) {
		String[] splittedKey1 = key1.split("#");
		String[] splittedKey2 = key2.split("#");
		
		int mstRes = Double.valueOf(splittedKey1[0]).compareTo(Double.valueOf(splittedKey2[0]));
		
		if (mstRes == 0)
			return Integer.valueOf(splittedKey1[1]).compareTo(Integer.valueOf(splittedKey2[1]));
		else
			return mstRes;
	}
	
}
