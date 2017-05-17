package es.unizar.tmdad.lab2.domain;


import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.springframework.social.twitter.api.Tweet;

@Entity
public class TweetBD implements Serializable{

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    private String query;
    private String fromUser;
    private String idStr;
    private String unmodifiedText;
    //private TargetedTweet targetedTweet;

    protected TweetBD() {}

    public TweetBD(String query, String fromUser, String tweetId, String text) {
        this.query = query;
        this.fromUser = fromUser;
        this.unmodifiedText = text;
        this.idStr = tweetId;
    }

    
    

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getFromUser() {
		return fromUser;
	}

	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}

	public String getIdStr() {
		return idStr;
	}

	public void setIdStr(String idStr) {
		this.idStr = idStr;
	}

	public String getUnmodifiedText() {
		return unmodifiedText;
	}

	public void setUnmodifiedText(String unmodifiedText) {
		this.unmodifiedText = unmodifiedText;
	}

	@Override
    public String toString() {
        return String.format(
                "TweetDB[ query='%s', tweet='%s']",
                query, unmodifiedText);
    }

}