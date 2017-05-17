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

import org.springframework.social.twitter.api.StreamDeleteEvent;
import org.springframework.social.twitter.api.StreamWarningEvent;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;

import dataBase.opsDatabase;

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
	private final static String EXCHANGE_NAME = "TweetChooser";
	private final static String PROCESSOR_NAME = "TweetProcessors";
	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
	ConnectionFactory factory = null;
	String amqpURL = null;

	Connection connection = null;
	// Con un solo canal
	Channel channel = null;
	Channel channel2 = null;


	
	private Stream stream;



	@Autowired
	JdbcTemplate jdbcTemplate;
	
	
	@PostConstruct
	public void initialize() {
		FilterStreamParameters fsp = new FilterStreamParameters();
		if(channel==null){
			//inicializar el cana
			factory = new ConnectionFactory();
			amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME)
					: "amqp://kmzrhisv:vWB9WsYyVR-IkEHhEpCN9XCuAp-NI3_3@penguin.rmq.cloudamqp.com/kmzrhisv";
			
			try {
				factory.setUri(amqpURL);

				System.out.println("TweetProcesor1 [*] AQMP broker found in " + amqpURL);
				connection = factory.newConnection();
				// Con un solo canal
				channel = connection.createChannel();
				channel2 = connection.createChannel();
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
				channel2.exchangeDeclare(PROCESSOR_NAME, "fanout");
				// Creamos una nueva cola temporal (no durable, exclusiva y
				// que se borrará automáticamente cuando nos desconectemos
				// del servidor de RabbitMQ). El servidor le dará un
				// nombre aleatorio que guardaremos en queueName
				String queueName = channel.queueDeclare().getQueue();
				String queueName2 = channel2.queueDeclare().getQueue();
				// E indicamos que queremos que la centralita EXCHANGE_NAME
				// envíe los mensajes a la cola recién creada. Para ello creamos
				// una unión (binding) entre ellas (la clave de enrutado
				// la ponemos vacía, porque se va a ignorar)	
				channel.queueBind(queueName, EXCHANGE_NAME, "");
				channel2.queueBind(queueName2, PROCESSOR_NAME, "");
				
				MyExecutor myExec = new MyExecutor();
				  myExec.execute(new Runnable() {

				        @Override
				        public void run() {
				        	try{
				        		QueueingConsumer consumer = new QueueingConsumer(channel);
								// autoAck a true
								channel.basicConsume(queueName, true, consumer);

								while (true) {
									// bloquea hasta que llege un mensaje 
									QueueingConsumer.Delivery delivery = consumer.nextDelivery();
									TargetedTweet tweetT = (TargetedTweet) SerializationUtils.deserialize(delivery.getBody());
									System.out.println(" [x] Recibido TC -TP1 ");
									sendTweet(tweetT);
								}
				        		
				        	}catch(Exception a){
				        		
				        	}
				        	
				        }
				    });
				
/*
				// El objeto consumer guardará los mensajes que lleguen
				// a la cola queueName hasta que los usemos
				*/
			
			} catch (Exception e) {
				System.out.println(" [*] AQMP broker not found in " + amqpURL);
				System.exit(-1);
			}
			
		}
		
	}

	

	public void sendTweet(TargetedTweet tweet) {
		// Crea un mensaje que envie un tweet a un único tópico destinatario
		
		Map<String, Object> mapa = new HashMap<>();
		mapa.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
		String[] claves = tweet.getFirstTarget().split("-");
		String query = claves[0];
		String dificultad = claves[1];
		String restriccion = claves[2];
		boolean aceptado = false;
		String [] palabras = tweet.getTweet().getText().split(" ");
		ArrayList <String> palabrasValidas = new ArrayList <String>();
		for (int i = 0; i<palabras.length ; i++){
			if(!palabras[i].contains("@") && !palabras[i].contains("#") && !palabras[i].contains("http"))palabrasValidas.add(palabras[i]);
		}
		switch (restriccion) {
        case "poco restrictivo":
        	// mas de 25 letras dentro de ellas, vale
        	if(tweet.getTweet().getText().length() > 20)aceptado = true;
                 break;
        case "normal": 
        	//Si tiene mas de 3 palabras no hashtags ni menciones ni links y no links y mas de 25 letras dentro de ellas, vale
        	if(tweet.getTweet().getText().length() > 25 && palabrasValidas.size() > 5)aceptado = true;

                 break;
        case "muy restrictivo":  
        	//Si tiene mas de 8 palabras no hashtags  ni menciones ni links y no links y mas de 80 letras dentro de ellas, vale
        	if(tweet.getTweet().getText().length() > 80 && palabrasValidas.size() > 11)aceptado = true;

                 break;
      
        default: ;
                 break;
    }
		
		if(aceptado){
			//StreamSendingServiceDificultad controller = (StreamSendingServiceDificultad) context.getBean("employeeController");
			//dificultadService.sendTweet(tweet);
			try{
				byte[] data = SerializationUtils.serialize(tweet);
				channel2.basicPublish(PROCESSOR_NAME, "", null, data);
				System.out.println("SEND: TP1 -> TP2 ");

			}
			catch(Exception a){}
			/*
			ops.convertAndSend("/queue/search/" + tweet.getFirstTarget(),
					tweet.getTweet(),mapa);
			System.out.println("sendTweet TargetedTweet: " + tweet.getTweet().getText());
			opsDatabase opsDB = new opsDatabase(jdbcTemplate);
			Long idConf = opsDB.getIdConfiguracion((tweet.getFirstTarget()).split("-")[0], 
					(tweet.getFirstTarget().split("-"))[1], (tweet.getFirstTarget().split("-"))[2]);
			opsDB.addTweet(tweet.getTweet().getText(), idConf);
			System.out.println("insertado idconfig: " + idConf);
			*/
		}


	}


	public Stream getStream() {
		return stream;
	}
	

	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	}
	

}
