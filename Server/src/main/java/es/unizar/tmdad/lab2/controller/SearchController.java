package es.unizar.tmdad.lab2.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dataBase.opsDatabase;
import es.unizar.tmdad.lab2.configuration.Configuracion;
import es.unizar.tmdad.lab2.domain.TweetBD;
import es.unizar.tmdad.lab2.domain.TweetBDRepository;

@Controller
public class SearchController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	private final static String CONFIG_NAME = "TweetChoooserConfig";
	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
	ConnectionFactory factory = null;
	String amqpURL = null;

	Connection connection = null;
	// Con un solo canal
	Channel channel = null;
	@Autowired
	private TweetBDRepository tweetRepository;

	@RequestMapping("/")
	public String greeting() {
		return "index";
	}

	/*
	 * Devuelve historico de una query
	 */
	@RequestMapping("/search")
	public ResponseEntity search(@RequestParam("q") String query, @RequestParam("restriccion") String restriccion,
			@RequestParam("dificultad") String dificultad, Model m) {
		System.out.println("Tweets en BD: " + tweetRepository.count());
		List<TweetBD> listaTweets = tweetRepository.findByQueryOrderByIdDesc(query);
		System.out.println("lista: " + listaTweets.size());
		ArrayList<Tweet> tweets = new ArrayList<Tweet>(listaTweets.size());

		return new ResponseEntity<>(listaTweets, HttpStatus.OK);

	}

	/*
	 * Realiza un cambio en la configuracion
	 */
	@PostMapping("/config")
	public ResponseEntity set_config(@RequestParam("query") String q, @RequestParam("restriccion") String restriccion,
			@RequestParam("dificultad") String dificultad, @RequestParam("hint") String hint, Model m) {
		Configuracion config = new Configuracion(q, dificultad, restriccion, hint);
		System.out.println("Query: " + q + ", dificultad: " + dificultad + ", restriccion: " + restriccion);
		opsDatabase ops = new opsDatabase(jdbcTemplate);
		// Si no esta la configuracion en la Bd, agregarla
		if (ops.getNumberConfigurations(q, dificultad, restriccion) < 1) {
			ops.addConfiguracion(q, dificultad, restriccion);
		}

		return new ResponseEntity<>(config, HttpStatus.OK);

	}

	/*
	 * Agrega una nueva cola de stream de query
	 */
	@MessageMapping("/search")
	public void search(String claveSubscripcion) {
		String[] claves = claveSubscripcion.split("-");
		String q = claves[0];
		String dificultad = claves[1];
		String restriccion = claves[2];
		String hint = claves[3];
		System.out.println("Query: " + q + ", dificultad: " + dificultad + ", restriccion: " + restriccion);
		opsDatabase ops = new opsDatabase(jdbcTemplate);
		// Si no esta la configuracion en la Bd, agregarla
		if (ops.getNumberConfigurations(q, dificultad, restriccion) < 1) {
			ops.addConfiguracion(q, dificultad, restriccion);
		}
		// twitter.search(q, dificultad, restriccion, hint);
		Configuracion conf = new Configuracion(q, dificultad, restriccion, hint);
		// enviar por fanaot
		if (channel == null) {
			// inicializar el cana
			factory = new ConnectionFactory();
			amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME)
					: "amqp://localhost";
			try {
				factory.setUri(amqpURL);
				System.out.println("TweeetChooser [*] AQMP broker found in " + amqpURL);
				connection = factory.newConnection();
				// Con un solo canal
				channel = connection.createChannel();

				// Declaramos una centralita de tipo fanout llamada
				// EXCHANGE_NAME
				channel.exchangeDeclare(CONFIG_NAME, "fanout");
			} catch (Exception e) {
				System.out.println("Chooser [*] AQMP broker not found in " + amqpURL);
				System.exit(-1);
			}
		}
		try{
			byte[] data = SerializationUtils.serialize(conf);
			channel.basicPublish(CONFIG_NAME, "", null, data);		
			System.out.println(	"ENVIADA config-> chooser: "+ conf.getQuery() );

		}catch(Exception a){
			System.out.println(	"error al enviar: " + a.toString() );
		}
		
		

	}
}