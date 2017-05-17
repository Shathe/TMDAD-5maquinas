/**
 * Autor: Inigo Alonso Ruiz Quality supervised by: F.J. Lopez Pellicer
 */

package dataBase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Component;

/**
 * Users database operations savetweet, getLatest50tweets, paginados,
 * addconfiguracion, get idconfiguracion,
 * 
 * @author shathe
 *
 */
@Component
public class opsDatabase {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	public opsDatabase(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public ArrayList<String> getLastestTweets(Long idConfiguracion) {
		String queryString = "SELECT tweet FROM Tweets  where idConfiguracion = " + String.valueOf(idConfiguracion);
		List<String> data = jdbcTemplate.query(queryString, new RowMapper<String>() {
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(1);
			}
		});
		return (ArrayList<String>) data;
	}

	public int addTweet(String content, Long idConfiguracion) {
		return this.jdbcTemplate.update("insert into Tweets (idConfiguracion, tweet) values (?,?)", idConfiguracion,
				content);

	}

	public int addConfiguracion(String query, String juego, String restriccion) {
		return this.jdbcTemplate.update("insert into Configuracion (query, juego, restriccion) values (?,?,?)", query, juego,
				restriccion);

	}
	
	

	public Long getIdConfiguracion(String query, String juego, String restriccion) {
		String querySQL = "select id from Configuracion where query ='" + query + "' and juego = '" + juego
				+ "' and restriccion ='" + restriccion + "'";
		return jdbcTemplate.queryForObject(querySQL, Long.class);
	}

	public int getNumberConfigurations(String query, String juego, String restriccion) {
		String querySQL = "select count(*) from Configuracion where query ='" + query + "' and juego = '" + juego
				+ "' and restriccion ='" + restriccion + "'";
		return jdbcTemplate.queryForObject(querySQL, Integer.class);

	}

}
