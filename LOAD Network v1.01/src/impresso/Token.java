package impresso;

import java.util.Comparator;

import org.json.JSONObject;

public class Token implements Comparable<Token>{

	private String lemma;
	private String pos;
	private String surface;
	private String language;
	private int offset;
	
	public Token() {
		
	}
	public Token(JSONObject token, String tokLanguage, int contentItemOffset) {
		this.pos = token.getString("p");
		this.lemma = token.getString("l");
		this.offset = token.getInt("o") + contentItemOffset;
		this.surface = token.getString("t");
		this.language = tokLanguage;
	}
	
	public void setLemma(String lemma) {
		this.lemma = lemma;
		return;
	}
	
	public void setPOS(String pos) {
		this.pos = pos;
		return;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
		return;
	}
	
	public void setSurface(String surface) {
		this.surface = surface;
		return;
	}
	
	public void setLanguage(String lang) {
		this.language = lang;
		return;
	}
	
	
	public String getLemma() {
		return lemma;
	}
	
	public String getPOS() {
		return pos;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public String getSurface() {
		return surface;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public int compareTo(Token compareToken) {

		int compareQuantity = ((Token) compareToken).getOffset();

		//ascending order
		return this.offset - compareQuantity;

	}
}
