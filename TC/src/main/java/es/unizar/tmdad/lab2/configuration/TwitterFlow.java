package es.unizar.tmdad.lab2.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.social.twitter.api.StreamListener;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.util.SerializationUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import es.unizar.tmdad.lab2.domain.MyTweet;
import es.unizar.tmdad.lab2.domain.TargetedTweet;
import es.unizar.tmdad.lab2.service.MyExecutor;

import es.unizar.tmdad.lab2.domain.TweetBD;
import es.unizar.tmdad.lab2.domain.TweetBDRepository;
import es.unizar.tmdad.lab2.service.TwitterLookupService;

@Configuration
@EnableIntegration
@IntegrationComponentScan
@ComponentScan
public class TwitterFlow {
	
	private Tweet example = null;

	private final static String EXCHANGE_NAME = "TweetChooser";
	private final static String SAVER_NAME = "TweetSaver";
		private final static String CONFIG_NAME = "TweetChoooserConfig";

	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
	ConnectionFactory factory = null;
	String amqpURL = null;

	Connection connection = null;
	// Con un solo canal
	Channel channel = null;
	Channel channel2 = null;
	
	@Autowired
	private TwitterLookupService twitterlookupService;
	
	
	@Bean
	public DirectChannel requestChannel() {
		return new DirectChannel();
	}


	// Tercer paso
	// Los mensajes se leen de "requestChannel" y se envian al método "sendTweet" del
	// componente "streamSendingService"
	@Bean
	public IntegrationFlow sendTweet() {
		if(channel==null){
			//inicializar el cana
			factory = new ConnectionFactory();
			amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME)
					: "amqp://kmzrhisv:vWB9WsYyVR-IkEHhEpCN9XCuAp-NI3_3@penguin.rmq.cloudamqp.com/kmzrhisv";
			try {
				factory.setUri(amqpURL);
				System.out.println("TweeetChooser [*] AQMP broker found in " + amqpURL);
				connection = factory.newConnection();
				// Con un solo canal
				channel = connection.createChannel();

				
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
				channel.exchangeDeclare(SAVER_NAME, "fanout");


				channel2 = connection.createChannel();
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				channel2.exchangeDeclare(CONFIG_NAME, "fanout");
				// Creamos una nueva cola temporal (no durable, exclusiva y
				// que se borrarÃ¡ automÃ¡ticamente cuando nos desconectemos
				// del servidor de RabbitMQ). El servidor le darÃ¡ un
				// nombre aleatorio que guardaremos en queueName
				String queueName = channel2.queueDeclare().getQueue();
				// E indicamos que queremos que la centralita EXCHANGE_NAME
				// envÃ­e los mensajes a la cola reciÃ©n creada. Para ello creamos
				// una uniÃ³n (binding) entre ellas (la clave de enrutado
				// la ponemos vacÃ­a, porque se va a ignorar)	
				channel2.queueBind(queueName, CONFIG_NAME, "");
				
				MyExecutor myExec = new MyExecutor();
				  myExec.execute(new Runnable() {

				        @Override
				        public void run() {
				        	try{
				        		QueueingConsumer consumer = new QueueingConsumer(channel2);
								// autoAck a true
								channel2.basicConsume(queueName, true, consumer);

								while (true) {
									// bloquea hasta que llege un mensaje 
									QueueingConsumer.Delivery delivery = consumer.nextDelivery();
									Configuracion config = (Configuracion) SerializationUtils.deserialize(delivery.getBody());
									System.out.println("config recv:");
									twitterlookupService.search(config);
								}
				        		
				        	}catch(Exception a){
				        											System.out.println("error al receibir" + a.toString());

				        	}
				        	
				        }
				    });

			} catch (Exception e) {
				System.out.println("Chooser [*] AQMP broker not found in " + amqpURL);
				System.exit(-1);
			}

			
		}
		
		return IntegrationFlows.from(requestChannel()).filter(tuit -> tuit instanceof Tweet).// Filter --> asegurarnos que el mensaje es un Tweet
				<Tweet,TargetedTweet>transform(tuit -> 
				{	MyTweet tweet = new MyTweet(tuit); 
					example = tuit;
					List<String> topics = twitterlookupService.getClaveSubscripciones().stream().filter(clave -> tweet.getText().contains((clave.split("-"))[0])).collect(Collectors.toList());
					//topics que coinciden con la query. ahora guardar el tweet 
					ArrayList<String> queries =  new ArrayList<String>();
					topics.forEach(topic -> {
						String nuevaQuery = topic.split("-")[0];
						if(!queries.contains(nuevaQuery))queries.add(nuevaQuery);
					});
					queries.forEach(query -> {
						try{
							byte[] data = SerializationUtils.serialize(new TweetBD(query, tuit.getFromUser(), tuit.getIdStr(), tuit.getText()));
							channel.basicPublish(SAVER_NAME, "", null, data);		
						}catch(Exception a){
							System.out.println(	"error al enviar chooser-> tweetSaver: " + a.toString() );
						}
						
					});
					return new TargetedTweet(tweet,topics); // Transform --> convertir un Tweet en un TargetedTweet con tantos tópicos como coincida
				}).split(TargetedTweet.class, tuit -> 
					{		List<TargetedTweet> tweets = new ArrayList<TargetedTweet>(tuit.getTargets().size());
							tuit.getTargets().forEach(q -> 
								{	tweets.add(new TargetedTweet(tuit.getTweet(),q));								
								
							
							});
							tweets.forEach(publicacion -> {
								try{
									byte[] data = SerializationUtils.serialize(publicacion);
									channel.basicPublish(EXCHANGE_NAME, "", null, data);		
									System.out.println(	"ENVIADO chooser-> tweetprocesor1: "+ publicacion.getTweet().getUnmodifiedText() );

								}catch(Exception a){
									System.out.println(	"error al enviar: " + a.toString() );
								}
								
								});
							return tweets;// Split --> dividir un TargetedTweet con muchos tópicos en tantos TargetedTweet como tópicos haya
					}).handle("TwitterFlow", "publishTweet").get();// Transform --> señalar el contenido de un TargetedTweet
					//		

		
	}
	
	public void publishTweet(TargetedTweet tweet){
		System.out.println("LLEGA");
	}
}



// Segundo paso
// Los mensajes recibidos por este @MessagingGateway se dejan en el canal	"requestChannel"
@MessagingGateway(name = "integrationStreamListener", defaultRequestChannel = "requestChannel")
interface MyStreamListener extends StreamListener {

}
