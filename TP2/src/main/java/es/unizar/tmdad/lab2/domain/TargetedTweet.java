package es.unizar.tmdad.lab2.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class TargetedTweet implements Serializable {
	
	private MyTweet tweet;
	
	private List<String> targets;

	public TargetedTweet(MyTweet tweet, List<String> targets) {
		this.tweet = tweet;
		this.targets = targets;
	}
	
	public TargetedTweet(MyTweet tweet, String target) {
		this.tweet = tweet;
		this.targets = Collections.singletonList(target);
	}

	public MyTweet getTweet() {
		return tweet;
	}
	
	public List<String> getTargets() {
		return targets;
	}

	public String getFirstTarget() {
		return targets.get(0);
	}

}
