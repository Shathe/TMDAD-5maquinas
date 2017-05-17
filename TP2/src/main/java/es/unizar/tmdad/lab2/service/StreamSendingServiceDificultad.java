package es.unizar.tmdad.lab2.service;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.client.RestTemplate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import es.unizar.tmdad.lab2.domain.TargetedTweet;
import es.unizar.tmdad.lab2.service.MyExecutor;

@Service
public class StreamSendingServiceDificultad {
	private final static String PROCESSOR_NAME = "TweetProcessors";
	private final static String CLIENT_NAME = "TweetHint";
	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
	ConnectionFactory factory = null;
	String amqpURL = null;

	Connection connection = null;
	// Con un solo canal
	Channel channel = null;
	Channel channel2 = null;

	
	

	@PostConstruct
	public void initialize() {
		if(channel==null){
			//inicializar el cana
			factory = new ConnectionFactory();
			amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME)
					: "amqp://kmzrhisv:vWB9WsYyVR-IkEHhEpCN9XCuAp-NI3_3@penguin.rmq.cloudamqp.com/kmzrhisv";
			
			try {
				factory.setUri(amqpURL);

				System.out.println("TweetProcesor2 [*] AQMP broker found in " + amqpURL);
				connection = factory.newConnection();
				// Con un solo canal
				channel = connection.createChannel();
				channel2 = connection.createChannel();
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				channel.exchangeDeclare(PROCESSOR_NAME, "fanout");
				channel2.exchangeDeclare(CLIENT_NAME, "fanout");
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
				channel.queueBind(queueName, PROCESSOR_NAME, "");
				channel2.queueBind(queueName2, CLIENT_NAME, "");
				
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

									try{
									System.out.println("esperando a recibir TP2 ");

									QueueingConsumer.Delivery delivery = consumer.nextDelivery();
									TargetedTweet tweetT = (TargetedTweet) SerializationUtils.deserialize(delivery.getBody());
									System.out.println("Recibido TP1-> TP2 ");
									sendTweet(tweetT);
									}catch(Exception a){
										System.out.println("EEROR TP1-> TP2 ");

						        	}
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
		String[] palabras = tweet.getTweet().getText().split(" ");
		ArrayList<String> palabrasValidas = new ArrayList<String>();
		for (int i = 0; i < palabras.length; i++) {
			if (!palabras[i].contains("@") && !palabras[i].contains("#") && !palabras[i].contains("http"))
				palabrasValidas.add(palabras[i]);
		}
		boolean sinonimo = false;
		RestTemplate restTemplate = new RestTemplate();
		String palabraCambiada ="";
		
		switch (dificultad) {
		case "facil":
			// cambiar palabra al azar por otra al azar
			String[] palabrasArecomendar = { "a", "no", "ah", "ehmmmm"};
			int aletarorioDiccionario = (int) (Math.random() * palabrasArecomendar.length);
			int aletaroriopalabrasValidas = (int) (Math.random() * palabrasValidas.size());
			tweet.getTweet().setUnmodifiedText(tweet.getTweet().getUnmodifiedText().replaceFirst(
					 " " + palabrasValidas.get(aletaroriopalabrasValidas) + " ", " "+ palabrasArecomendar[aletarorioDiccionario] +  " "));
			palabraCambiada = palabrasArecomendar[aletarorioDiccionario];

			break;
		case "medio":
			// usar una api de sinonimos y cambiar una palabra al azar
			// desd ela primera palabra a la ultima palabra intentar obtener un
			// sinonimo de las palabras importantes

		
			for (int i = 0; i < palabrasValidas.size() && !sinonimo; i++) {
				int j = (int) (Math.random() * palabrasValidas.size());
				try {
					ResponseEntity<String> response = restTemplate
							.getForEntity("http://words.bighugelabs.com/api/2/c302f07e3593264f58a7366800330462/"
									+ palabrasValidas.get(j) + "/json", String.class);
					String body = response.getBody();
					// a partir de la posicion 5 estan los resultados
					// se anaden dos sugerencias si la api ha devuelto
					// resultados
					// son en posiciones impares (en las pares hay comas)
					String sugerencias[] = body.split("\"");
					if (sugerencias.length > (5)) {
						String sinonimo_recomendado = sugerencias[5];
						sinonimo = true;
						tweet.getTweet().setUnmodifiedText(tweet.getTweet().getUnmodifiedText()
								.replaceFirst(" " + palabrasValidas.get(j) + " ",  " " + sinonimo_recomendado + " "));
						palabraCambiada = sinonimo_recomendado;


					}

				} catch (Exception e) {
				}
			}
			break;
		case "dificil":
			// desde la palabra mas larga hasta la mas pequena, de las palabras
			// importantes

			palabrasValidas.sort(new Comparator<String>() {
				@Override
				public int compare(String a, String b) {

					return b.length() - a.length();
				}
			});
			System.out.println(palabrasValidas.get(0));
			System.out.println(palabrasValidas.get(4));
			for (int i = 0; i < palabrasValidas.size() && !sinonimo; i++) {
				try {
					ResponseEntity<String> response = restTemplate
							.getForEntity("http://words.bighugelabs.com/api/2/c302f07e3593264f58a7366800330462/"
									+ palabrasValidas.get(i) + "/json", String.class);
					String body = response.getBody();
					// a partir de la posicion 5 estan los resultados
					// se anaden dos sugerencias si la api ha devuelto
					// resultados
					// son en posiciones impares (en las pares hay comas)
					String sugerencias[] = body.split("\"");
					if (sugerencias.length > (5)) {
						String sinonimo_recomendado = sugerencias[5];
						sinonimo = true;
						tweet.getTweet().setUnmodifiedText(tweet.getTweet().getUnmodifiedText()
								.replaceFirst( " " + palabrasValidas.get(i) + " ", " " + sinonimo_recomendado +  " "));
						palabraCambiada = sinonimo_recomendado;


					}

				} catch (Exception e) {
				}
			}

			break;

		default:
			;
			break;
		}

		try{
				byte[] data_palabra = SerializationUtils.serialize(palabraCambiada);
				channel2.basicPublish(CLIENT_NAME, "", null, data_palabra);

				byte[] data = SerializationUtils.serialize(tweet);
				channel2.basicPublish(CLIENT_NAME, "", null, data);
				System.out.println("SEND: TP2 -> END ");

			}
			catch(Exception a){}

	}

}
