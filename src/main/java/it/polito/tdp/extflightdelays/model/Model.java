package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {

	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer, Airport> idMap; // integre perchè è dato dall'id degli aereoporti
	Map<Airport, Airport> visita; 
	
	public Model() {
		dao = new ExtFlightDelaysDAO();
		idMap = new HashMap<Integer,Airport>();
		dao.loadAllAirports(idMap); // passo la mappa vuota e il metodo del DAO la riempie
	}
	
	public void creaGrafo(int x) {
		// il grafo lo creo qui perchè ogni volta che
		// l'utente preme il bottone, il grafo viene creato da 0,
		// così non interferisce con quello che era stato precedentemente creato
		
		grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		// non possiamo inserire nel grafo tutti gli aeroporti, perchè a noi servono gli aeroporti che rispettano il vincolo
		// aggiungo quindi i vertici filtrati
		Graphs.addAllVertices(grafo, dao.getVertici(x, idMap));
		
		// aggiungo gli archi
		for(Rotta r: dao.getRotte(idMap)) {
			// devo fare un controllo aggiuntivo perchè le rotte includono anche aeroporti 
			// non effettivamente contenuti nel grafo
			
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				DefaultWeightedEdge e = this.grafo.getEdge(r.getA1(), r.getA2());
				if(e == null) {
					// non c'è ancora un arco tra quei due nodi
					Graphs.addEdgeWithVertices(grafo, r.getA1(), r.getA2(), r.getN());
				} else { // l'arco non è null, ce n'è già uno, incremento il peso
					double pesoVecchio = this.grafo.getEdgeWeight(e);
					double pesoNuovo = pesoVecchio + r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
				}
			}
			
		}
		System.out.println("Grafo creato");
		
		System.out.println("# Vertici: "+grafo.vertexSet().size());
		System.out.println("# Archi: "+grafo.edgeSet().size());
	}

	public Set<Airport> getVertici() {
		return this.grafo.vertexSet();
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2) {
		List<Airport> percorso = new LinkedList<>();
		// Visita in ampiezza
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it = new BreadthFirstIterator<>(grafo, a1);
		
		visita = new HashMap<>();
		// con questa mappa posso inserire due aeroporti e mi dice che l'aeroporto 1 viene scoperto dall'aeroporto2
		visita.put(a1, null);
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				Airport airport1 = grafo.getEdgeSource(e.getEdge());
				Airport airport2 = grafo.getEdgeTarget(e.getEdge());
				
				if(visita.containsKey(airport1) && !visita.containsKey(airport2)) {
					// ciò vuol dicìre cha a2 viene scoperto da a1
					visita.put(airport2, airport1);
				} else if(visita.containsKey(airport2) && !visita.containsKey(airport1)){
					// a2 è il padre di a1
					visita.put(airport2, airport1);
				}
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
				
			}
		});
		
		
		while(it.hasNext()) {
			it.next(); // questo metodo vista, ma noi vogliamo proprio trovare un percorso
			
		}
		
		// da visita a ritroso posso andare da a2 ad a1 e vedere se c'è un percorso
		percorso.add(a2);
		Airport step = a2;
		while(visita.get(step)!=null) {
			step = visita.get(step); // al primo ste, chi era il padre del mio nodo destinazione? e lo aggiungo al percorso
			percorso.add(step);
		}
		return percorso;
	}
}
