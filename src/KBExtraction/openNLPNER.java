package KBExtraction;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class openNLPNER {

	public FeatureFactory featureFactory;

	public openNLPNER() {
		featureFactory = new FeatureFactory();
	}

	public ArticleSlots mainMethod(String theText, Map<String, String> map) {
		String mainDir = "C:/Users/Niloufar/Desktop/Projectdata/";
		// String txtFilepath = mainDir+"testFile.txt";
		String modelsDir = mainDir + "OpenNLP_Models/";
		String sent_model_Path = mainDir + "OpenNLP_Models/en-sent.bin";
		String token_model_Path = mainDir + "OpenNLP_Models/en-token.bin";

		ArticleSlots aSlots = new ArticleSlots();

		try {
			InputStream sent_modelIn = new FileInputStream(sent_model_Path);
			InputStream token_modelIn = new FileInputStream(token_model_Path);

			SentenceModel sent_model = new SentenceModel(sent_modelIn);
			SentenceDetectorME sentenceDetector = new SentenceDetectorME(
					sent_model);

			// Obtain reference to a tokenizer to split sentence into individual
			// words and symbols.
			TokenizerModel token_model = new TokenizerModel(token_modelIn);
			TokenizerME tokenizer = new TokenizerME(token_model);

			String sentences[] = sentenceDetector.sentDetect(theText);

			// initialize an array to hold the name of models
			NameFinderME[] Finders = new NameFinderME[3];
			String[] modelNames = { "person", "location", "date" };
			/**
			 * Initialize new model for identifying people, locations, and dates
			 * based on the binary compressed model in the files
			 * en-nerperson.bin, en-nerlocation.bin, en-ner-date.bin.
			 */
			for (int mi = 0; mi < modelNames.length; mi++) {
				// read the model from the directory it was stored
				InputStream modelIn = new FileInputStream(modelsDir + "en-ner-"
						+ modelNames[mi] + ".bin");

				TokenNameFinderModel tknameFinder = new TokenNameFinderModel(
						modelIn);
				Finders[mi] = new NameFinderME(tknameFinder);
			}
			System.out.println("============OPEN NLP===================");

			aSlots.nameArray = new ArrayList<String>();
			aSlots.nameArrayNegative = new ArrayList<String>();
			aSlots.placeOfBirthArray = new ArrayList<String>();
			aSlots.placeOfBirthNegative = new ArrayList<String>();
			aSlots.dateOfBirthArray = new ArrayList<String>();
			aSlots.dateOfBirthNegative = new ArrayList<String>();

			aSlots.DateOfBithFeatures = new ArrayList<ExtractedFeature>();
			aSlots.LocationFeaures = new ArrayList<ExtractedFeature>();
			// Iterate over each sentence.
			for (String sentence : sentences) {
				// print out the sentences
				System.out.println(sentence);
				// List<Annotation> allAnnotations = new
				// ArrayList<Annotation>();
				// Split sentence into array of tokens.
				String[] tokens = tokenizer.tokenize(sentence);
				// Iterate over each name finder (person,location, date).
				for (int fi = 0; fi < Finders.length; fi++) {
					// Identify names in sentence and return token based
					// offsets.
					Span[] spans = Finders[fi].find(tokens);
					// Get probabilities with associated matches.
					double[] prob = Finders[fi].probs(spans);
					// invoke the method that will display the entities
					// recognised.
					displayNameEntity(spans, tokens, aSlots, sentence, map);
					
				}
				CheckForEntityBetween(aSlots, sentence);
				System.out.println("-----------------------------------");
			}

		} catch (Exception e) {
			System.out.println("exception caught");
			e.printStackTrace();
		}
		return aSlots;
	}

	private void CheckForEntityBetween(ArticleSlots aSlots, String sentence) {
		int sizeDate = aSlots.DateOfBithFeatures.size();
		int sizeLocation = aSlots.LocationFeaures.size();
		
		if(sizeDate == 0 || sizeLocation == 0){
			return;
		}
		ExtractedFeature dateFeature = aSlots.DateOfBithFeatures.get(sizeDate - 1);


		ExtractedFeature LocationFeature = aSlots.LocationFeaures.get(sizeLocation - 1);

		if (dateFeature.Sentence.equals(LocationFeature.Sentence)) {
			int dateIndex = sentence.indexOf(dateFeature.Slot);
			int locationIndex = sentence.indexOf(LocationFeature.Slot);
			int entityIndex = sentence.indexOf(LocationFeature.Entity);

			if (entityIndex < dateIndex) {
				if (entityIndex < locationIndex) {
					if (locationIndex < dateIndex) {
						dateFeature.HasLocationBetween = true;
					} else {
						LocationFeature.HasDateBetween = true;
					}
				}
			} else {
				if (entityIndex > locationIndex) {
					if (locationIndex > dateIndex) {
						dateFeature.HasLocationBetween = true;
					} else {
						LocationFeature.HasDateBetween = true;
					}
				}
			}
		}
	}

	/**
	 * method to display the entities detected in a sentence
	 * 
	 * @param names
	 * @param tokens
	 */
	private void displayNameEntity(Span[] names, String[] tokens,
			ArticleSlots aSlots, String sentence, Map<String, String> map) {
		for (int si = 0; si < names.length; si++) {
			StringBuilder cb = new StringBuilder();
			for (int ti = names[si].getStart(); ti < names[si].getEnd(); ti++) {
				cb.append(tokens[ti]).append(" ");
			}
			String entityName = cb.substring(0, cb.length() - 1);

			String entityType = names[si].getType();
			String entity = map.get("name");
			sentence = sentence.replaceAll("[^a-zA-Z0-9]", " ");
			if (entityType.equals("location")) {

				getLocation(aSlots, sentence, map, entityName, entity);

			} else if (entityType.equals("date")) {

				getDateOfBirth(aSlots, sentence, map, entityName, entity);

			}
			System.out.println("<Start:" + entityType + "/>" + entityName
					+ " </end> ");
		}
	}

	private void getDateOfBirth(ArticleSlots aSlots, String sentence,
			Map<String, String> map, String slotValue, String entity) {
		String mapValue = "";
		if (map.containsKey("birthdate")) {
			mapValue = map.get("birthdate");
		} else if (map.containsKey("dateofbirth")) {
			mapValue = map.get("dateofbirth");
		} else if (map.containsKey("Birthdate")) {
			mapValue = map.get("Birthdate");
		} else if (map.containsKey("birth_date")) {
			mapValue = map.get("birth_date");
		}else if(map.containsKey("born")) {
			   mapValue = map.get("born");
		}else if(map.containsKey("date of birth")) {
			   mapValue = map.get("date of birth");
		}else if(map.containsKey("Born")) {
			   mapValue = map.get("Born");
		}else if(map.containsKey("yearofbirth")) {
			   mapValue = map.get("yearofbirth");
		}
		if ((sentenceContainsEntiryPart(slotValue, mapValue) || sentenceContainsEntiryPart(
				mapValue, slotValue))
				&& !mapValue.equals("")
				&& sentenceContainsEntiryPart(sentence, entity)) {
			if (!aSlots.dateOfBirthArray.contains(sentence)) {
				if (aSlots.dateOfBirthNegative.contains(sentence)) {
					aSlots.dateOfBirthNegative.remove(sentence);

				}

				aSlots.dateOfBirthArray.add(sentence);
				aSlots.DateOfBithFeatures.add(featureFactory.CreateFeature(
						sentence, entity, slotValue, true, SlotEnumType.Dob));

			}
		} else if (sentenceContainsEntiryPart(sentence, entity)) {
			if (!aSlots.dateOfBirthArray.contains(sentence)
					&& !aSlots.dateOfBirthNegative.contains(sentence)) {
				aSlots.dateOfBirthNegative.add(sentence);
				aSlots.DateOfBithFeatures.add(featureFactory.CreateFeature(
						sentence, entity, slotValue, false, SlotEnumType.Dob));
			}

		}
	}

	private boolean sentenceContainsEntiryPart(String sentence, String entity) {
		String[] entityPart = entity.split(" ");
		for (String part : entityPart) {
			if (sentence.contains(part))
				return true;

		}
		return false;
	}

	private void getLocation(ArticleSlots aSlots, String sentence,
			Map<String, String> map, String slotValue, String entity) {
		String mapValue = "";
		if (map.containsKey("birthplace")) {
			mapValue = map.get("birthplace");
		} else if (map.containsKey("countryofbirth")) {
			mapValue = map.get("countryofbirth");
		} else if (map.containsKey("placeofbirth")) {
			mapValue = map.get("placeofbirth");
		} else if (map.containsKey("Birthplace")) {
			mapValue = map.get("Birthplace");
		} else if (map.containsKey("birth_place")) {
			mapValue = map.get("birth_place");
		}else if(map.containsKey("cityofbirth")) {
			   mapValue = map.get("cityofbirth");
		   }else if(map.containsKey("location")) {
			   mapValue = map.get("location");
		   }else if(map.containsKey("place of birth")) {
			   mapValue = map.get("place of birth");
		   }else if(map.containsKey("Origin")) {
			   mapValue = map.get("Origin");
		   }else if(map.containsKey("location")) {
			   mapValue = map.get("location");
		   }
		if ((sentenceContainsEntiryPart(slotValue, mapValue) || sentenceContainsEntiryPart(
				mapValue, slotValue))
				&& !mapValue.equals("")
				&& sentenceContainsEntiryPart(sentence, entity)) {
			if (!aSlots.placeOfBirthArray.contains(sentence)) {
				if (aSlots.placeOfBirthNegative.contains(sentence)) {
					aSlots.placeOfBirthNegative.remove(sentence);
				}
				aSlots.placeOfBirthArray.add(sentence);
				aSlots.LocationFeaures.add(featureFactory.CreateFeature(
						sentence, entity, slotValue, true,
						SlotEnumType.Location));
			}
		} else if (sentenceContainsEntiryPart(sentence, entity)) {
			if (!aSlots.placeOfBirthArray.contains(sentence)
					&& !aSlots.placeOfBirthNegative.contains(sentence)
					&& sentenceContainsEntiryPart(sentence, entity)) {
				aSlots.placeOfBirthNegative.add(sentence);
				aSlots.LocationFeaures.add(featureFactory.CreateFeature(
						sentence, entity, slotValue, false,
						SlotEnumType.Location));
			}
		}
	}

}
