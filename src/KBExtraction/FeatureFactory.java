package KBExtraction;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;


public class FeatureFactory {
	
	TokenizerME tokenizer;
	public static Set<String> set = new TreeSet<>();
	public static List<List<String>> listlist = new LinkedList<>();
	
	
	public FeatureFactory(){
		try{
			String mainDir = "C:/Users/Niloufar/Desktop/Projectdata/";

			String token_model_Path = mainDir+"OpenNLP_Models/en-token.bin";
			
			InputStream token_modelIn = new FileInputStream(token_model_Path);
			
			TokenizerModel token_model = new TokenizerModel(token_modelIn);
			tokenizer = new TokenizerME(token_model);
			
	
		}catch(Exception e){
			e.printStackTrace();
		}
				
	}
	
	public ExtractedFeature CreateFeature(String sentence,String entity,String slot,boolean isPositive, SlotEnumType type){
		
			ExtractedFeature feature = new ExtractedFeature();
			
			String[] tokens = tokenizer.tokenize(sentence);
			String[] filteredToken = removeUnwantedElement(tokens);
			
			String[] entityParts = entity.split(" ");
			String[] slotParts = slot.split(" ");
			
			
			
			int entityFirstLocation = java.util.Arrays.asList(filteredToken).indexOf(entityParts[0]);
			
			int entityLastLocation = -1;
			
			if(entityParts.length > 1){
				entityLastLocation = java.util.Arrays.asList(filteredToken).indexOf(entityParts[entityParts.length -1]);
			}
			
			
			int slotFirstLocation = java.util.Arrays.asList(filteredToken).indexOf(slotParts[0]);
			
			int slotLastLocation = -1;
			if(slotParts.length > 1){
				slotLastLocation = java.util.Arrays.asList(filteredToken).indexOf(slotParts[slotParts.length-1]);
			}
			
			if(entityFirstLocation < slotFirstLocation){
				feature.IsEntityMentionedFirst = true;
			}
			feature.Entity = entity;
			feature.Slot = slot;
			feature.IsPositive = isPositive;
			feature.Type = type;
			feature.Sentence = sentence;
			
			
			checkAndPopulateBeforeArray(feature, filteredToken, entityFirstLocation,
					slotFirstLocation);
			
			checkAndPopulateAfter(feature, filteredToken, slotFirstLocation,
					slotLastLocation);
			
			
			checkAndPopulaterBetween(feature, filteredToken, entityFirstLocation,
					entityLastLocation, slotFirstLocation, slotLastLocation);
			
			return feature;

	}

	private String[] removeUnwantedElement(String[] tokens) {
		ArrayList<String> result = new ArrayList<String>();

	    for(String item : tokens)
	        if( item.length() > 1)
	            result.add(item);

	    return result.toArray(new String[result.size()]);
	}

	private void checkAndPopulaterBetween(ExtractedFeature feature,
			String[] tokens, int entityFirstLocation, int entityLastLocation,
			int slotFirstLocation, int slotLastLocation) {
		if(feature.IsEntityMentionedFirst){
			populateBetween(feature, tokens, entityFirstLocation,entityLastLocation, slotFirstLocation);
			
		}else{
			populateBetween(feature, tokens, slotFirstLocation,slotLastLocation, entityFirstLocation);
			
		}
	}

	private void populateBetween(ExtractedFeature feature, String[] tokens,int entityFirstLocation, int entityLastLocation,int slotfirstLocation) {
		try{

			int start = entityFirstLocation;
			if(entityLastLocation > 0){
				start = entityLastLocation;
			}
			String[] test = new String[slotfirstLocation-start-1];
			System.arraycopy(tokens, start +1, test, 0, slotfirstLocation -start -1 );	
			feature.WordsBetween = new ArrayList<String>(Arrays.asList(test));
			putListintoSet(feature.WordsBetween);
			listlist.add(feature.WordsBetween);
		
			
		}catch(Exception e){
			e.getMessage();
		}
	}

	private void checkAndPopulateAfter(ExtractedFeature feature,
			String[] tokens, int slotFirstLocation, int slotLastLocation) {
		if(feature.IsEntityMentionedFirst){
			
			int lastLocation = slotFirstLocation;
			if(slotLastLocation > 0){
				lastLocation = slotLastLocation;
			}
			populateAfterArray(feature, tokens, lastLocation);
		}else{
			populateBeforeArray(feature, tokens, slotFirstLocation);
		}
	}

	private void populateAfterArray(ExtractedFeature feature, String[] tokens,int lastLocation) {
		
		ArrayList<String> after = new ArrayList<String>();
		if(lastLocation < tokens.length -1){
			after.add(tokens[lastLocation + 1]);
		}
		
		if(lastLocation < tokens.length -2){
			after.add(tokens[lastLocation + 2]);

		}
		//feature.WordsAfter = after;
	}

	private void checkAndPopulateBeforeArray(ExtractedFeature feature,
			String[] tokens, int entityFirstLocation, int slotFirstLocation) {
		if(feature.IsEntityMentionedFirst){
			populateBeforeArray(feature, tokens, entityFirstLocation);
		}else{
			populateBeforeArray(feature, tokens, slotFirstLocation);
		}
	}

	private void populateBeforeArray(ExtractedFeature feature, String[] tokens,int firstLocation) {
		ArrayList<String> before = new ArrayList<String>();
		if(firstLocation > 0){
			before.add(tokens[firstLocation -1]);
		}
		
		if(firstLocation > 1){
			before.add(tokens[firstLocation -2]);

		}
		feature.WordsBefore = before;
	}
	public  Set<String> putListintoSet(ArrayList<String> list) {
		for(String item:list){
			if(!((item.matches("[A-Z].*")))){
				set.add(item);
			}
			
			//(".*[0-9]+.*")
		}
		return set;
		
	}
	public  static List<String> checkListAndSet(Set<String> set,List<String> list){
		LinkedHashMap<String,Integer> map = new LinkedHashMap<>();
		ArrayList<String> finalList = new ArrayList<>();
		int i = 0 ;
		for(String item:set) {
			map.put(item, i);
			i++;
		}
		for(Entry<String, Integer> item:map.entrySet()){
			String key = item.getKey();
			Integer position = item.getValue();
			
			 if(list.contains(key)){
				finalList.add(position, "1");
			}
			else{
				finalList.add(position, "0");
			}
		}
		return finalList;
		
	}
	
	
	

}
