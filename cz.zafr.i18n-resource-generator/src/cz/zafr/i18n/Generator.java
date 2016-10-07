package cz.zafr.i18n;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Generator {

	public static void main(String... args) {
		if (args.length == 1) {
			try {
				generate(args[0]);
			} catch (EncryptedDocumentException | InvalidFormatException
					| IOException e) {
				System.err.println("Unable to open resource file: " + e.getMessage());
			}
		} else {
			printUsage();
		}
	}

	private static void generate(String path) throws EncryptedDocumentException, InvalidFormatException, IOException {
		Workbook wb = WorkbookFactory.create(new File(path));
		Map<String, List<String>> dictionary = new LinkedHashMap<String, List<String>>();
		String[] languages = new String[0];
		boolean isHeader = true;

		for (Sheet sheet : wb ) {
	        for (Row row : sheet) {
	        	int colIndex = 0;
	            for (Cell cell : row) {
	            	if (isHeader) {
	            		String languageCode = cell.getStringCellValue().trim();
	            		if (languageCode.isEmpty()) {
	            			break;
	            		}
	            		dictionary.put(languageCode, new ArrayList<String>());
	            	} else if (colIndex < languages.length) {
	    	        	List<String> phrases = dictionary.get(languages[colIndex++]);
	            		phrases.add(cell.getStringCellValue().trim());
	            	} else {
	            		break;
	            	}
	            }
	            
	            if (isHeader) {
	            	isHeader = false;
	            	languages = dictionary.keySet().toArray(new String[dictionary.size()]);
	            }
	        }
	        
	        break; //process just the first sheet and leave the loop
	    }
		
		System.out.print(generateC(dictionary));
	}

	private static String generateC(Map<String, List<String>> dictionary) {
		StringBuilder builder = new StringBuilder();
		StringBuilder summaryBuilder = new StringBuilder("\nconst char** RESOURCES[] = {\n");
		int langIndex = 0;
		builder.append("//GENERATED - DO NOT EDIT!\n");
		builder.append("//i18n resource file\n");

		for (Map.Entry<String, List<String>> entry : dictionary.entrySet()) {
			String lang = entry.getKey();
			int index = 0;
			StringBuilder arrayBuilder = new StringBuilder(String.format("\n\nstatic const char* %s[] PROGMEM = {\n", lang));
			
			builder.append("\n//Language: ");
			builder.append(lang);
			builder.append("\n");
			
			for (String phrase : entry.getValue()) {
				builder.append(String.format("\nstatic const char %s%d[] PROGMEM = \"%s\";", lang, index, phrase));
				if (index > 0) {
					arrayBuilder.append(',');
					if (index % 8 == 0) {
						arrayBuilder.append('\n');
					} else {
						arrayBuilder.append(' ');
					}
				}
				if (index % 8 == 0) {
					arrayBuilder.append("  ");
				}
				arrayBuilder.append(String.format("%s%d", lang, index++));
			}
			
			arrayBuilder.append("\n};\n");
			builder.append(arrayBuilder);
			
			if (langIndex > 0) {
				summaryBuilder.append(',');
				if (langIndex % 8 == 0) {
					summaryBuilder.append('\n');
				} else {
					summaryBuilder.append(' ');
				}
			}
			if (index % 8 == 0) {
				summaryBuilder.append("  ");
			}
			langIndex++;

			summaryBuilder.append(lang);
		}

		summaryBuilder.append("\n};\n");
		builder.append(summaryBuilder);
		
		return builder.toString();
	}

	private static void printUsage() {
		System.out.println("Usage: java -jar i18n-resource-generator.jar <path to resources>");
	}

}
