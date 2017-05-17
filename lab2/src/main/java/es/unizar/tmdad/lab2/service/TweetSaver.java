package es.unizar.tmdad.lab2.service;

import org.springframework.beans.factory.annotation.Autowired;
import es.unizar.tmdad.lab2.domain.TweetBD;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import es.unizar.tmdad.lab2.service.MyExecutor;
import es.unizar.tmdad.lab2.domain.TweetBDRepository;

@Service
public class TweetSaver {
	private final static String EXCHANGE_NAME = "TweetSaver";
	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
	ConnectionFactory factory = null;
	String amqpURL = null;

	Connection connection = null;
	// Con un solo canal
	Channel channel = null;

	@Autowired
	private TweetBDRepository tweetRepository;
	

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	
	@PostConstruct
	public void initialize() {
		if(channel==null){
			//inicializar el cana
			factory = new ConnectionFactory();
			amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME)
					: "amqp://localhost";
			
			try {
				factory.setUri(amqpURL);

				System.out.println("TweetSaver [*] AQMP broker found in " + amqpURL);
				connection = factory.newConnection();
				// Con un solo canal
				channel = connection.createChannel();
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
				channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
				// Creamos una nueva cola temporal (no durable, exclusiva y
				// que se borrará automáticamente cuando nos desconectemos
				// del servidor de RabbitMQ). El servidor le dará un
				// nombre aleatorio que guardaremos en queueName
				String queueName = channel.queueDeclare().getQueue();
				// E indicamos que queremos que la centralita EXCHANGE_NAME
				// envíe los mensajes a la cola recién creada. Para ello creamos
				// una unión (binding) entre ellas (la clave de enrutado
				// la ponemos vacía, porque se va a ignorar)	
				channel.queueBind(queueName, EXCHANGE_NAME, "");
				
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
									TweetBD tweetT = (TweetBD) SerializationUtils.deserialize(delivery.getBody());
									tweetRepository.save(tweetT);
									System.out.println("query saved:" + tweetT.getQuery());
									System.out.println("Tweets en BD: " + tweetRepository.count());
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


}
