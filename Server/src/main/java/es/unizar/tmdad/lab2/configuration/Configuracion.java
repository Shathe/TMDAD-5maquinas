package es.unizar.tmdad.lab2.configuration;

import java.io.Serializable;

public class Configuracion implements Serializable {
	private String query, dificultad, restriccion;
	private String hint;

	public Configuracion(String query, String dificultad, String restriccion, String hint) {
		super();
		this.query = query;
		this.dificultad = dificultad;
		this.restriccion = restriccion;
		this.hint = hint;
	}

	public String isHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getDificultad() {
		return dificultad;
	}

	public void setDificultad(String dificultad) {
		this.dificultad = dificultad;
	}

	public String getRestriccion() {
		return restriccion;
	}

	public void setRestriccion(String restriccion) {
		this.restriccion = restriccion;
	}

	@Override
	public String toString() {
		return query + "-" + dificultad + "-" + restriccion  + "-" + hint;
	}
}
