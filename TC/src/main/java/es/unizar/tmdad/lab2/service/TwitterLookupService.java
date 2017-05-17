package es.unizar.tmdad.lab2.service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import es.unizar.tmdad.lab2.configuration.Configuracion;

@Service
public class TwitterLookupService {

	private final ConcurrentMap<String, Configuracion> connections = new ConcurrentLinkedHashMap.Builder<String, Configuracion>()
			.maximumWeightedCapacity(10).build();

	public void search(Configuracion nueva) {
		connections.putIfAbsent(nueva.toString(), nueva);
	}
	
	public Set<String> getClaveSubscripciones() {
		return connections.keySet();
	}
	
	public Collection<Configuracion> getConfiguraciones() {
		return connections.values();
	}
	
}
