package es.unizar.tmdad.lab2;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import dataBase.opsDatabase;
import es.unizar.tmdad.lab2.domain.TweetBDRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Application implements CommandLineRunner {
	


	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	private TweetBDRepository tweetRepository;
	
	@Override
	public void run(String... strings) throws Exception {

		// Tabla de usuarios
		jdbcTemplate.execute("CREATE TABLE Configuracion("
				+ "id SERIAL, query VARCHAR(100), juego VARCHAR(100), restriccion VARCHAR(100))");

		jdbcTemplate
				.execute("CREATE TABLE Tweets(id SERIAL, idConfiguracion long, tweet VARCHAR(300))");


	}

}