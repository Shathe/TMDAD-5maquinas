package es.unizar.tmdad.lab2.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.social.twitter.api.FilterStreamParameters;
import org.springframework.social.twitter.api.Stream;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import org.springframework.social.twitter.api.StreamDeleteEvent;
import org.springframework.social.twitter.api.StreamListener;
import org.springframework.social.twitter.api.StreamWarningEvent;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import es.unizar.tmdad.lab2.service.MyExecutor;
import es.unizar.tmdad.lab2.domain.TargetedTweet;

@Service
public class StreamSendingService {

	@Autowired
	private TwitterTemplate twitterTemplate;

	
	private Stream stream;

	@Autowired
	private StreamListener integrationStreamListener;
	
	@PostConstruct
	public void initialize() {
		stream = twitterTemplate.streamingOperations().sample(Collections.singletonList(integrationStreamListener));
	}

	public Stream getStream() {
		return stream;
	}

}
