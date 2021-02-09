package org.colomoto.biolqm.metadata.constants;

import org.colomoto.biolqm.metadata.annotations.URI;
import org.colomoto.biolqm.metadata.constants.Reference;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;

/**
 * One instance per model opened to keep a trace of the external metadata involved
 * When a URI concerning a reference is added, the bibtex informations concerning this reference are found on the internet and stored here
 *
 * @author Martin Boutroux
 */
public class ExternalMetadata {
	
	// variables
	public Map<URI, Reference> externalMetadata;
	
	// constructors
	public ExternalMetadata() {
		this.externalMetadata = new HashMap<URI, Reference>();
	}

	// functions
	public boolean isSetExternalMetadata(URI uri) {
		if (this.externalMetadata.containsKey(uri)) {
			return true;
		}
		return false;
	}
	
	public void updateExternalMetadata(URI uri, String title, String year) {
		if (!this.externalMetadata.containsKey(uri)) {
			this.externalMetadata.put(uri, new Reference(title, year));
		}
	}
	
	public String getDescription() {
		String help = "";
		
		for (Entry<URI, Reference> entry : this.externalMetadata.entrySet()) {
			help += "\t" + entry.getKey().getIdentifier() + ": " + entry.getValue().getTitle() + ", " + entry.getValue().getYear() + "\n"; 
		}
		
		return help;
	}
	
	public ArrayList<String> getReferencesWithYear(String year) {
		
		ArrayList<String> refs = new ArrayList<String>();
		
		for (Entry<URI, Reference> entry : this.externalMetadata.entrySet()) {
			Reference ref = entry.getValue();
			
			if (ref.getYear().equals(year)) {
				URI uri = entry.getKey();
				String doi = uri.getCollection()+":"+uri.getIdentifier();
				
				refs.add(doi);
			}
		}
		return refs;
	}
	
	public ArrayList<String> getReferencesWithKeyword(String word) {
		
		ArrayList<String> refs = new ArrayList<String>();
		
		for (Entry<URI, Reference> entry : this.externalMetadata.entrySet()) {
			Reference ref = entry.getValue();
			
			if (ref.getTitle().contains(word)) {
				URI uri = entry.getKey();
				String doi = uri.getCollection()+":"+uri.getIdentifier();
				
				refs.add(doi);
			}
		}
		return refs;
	}
}