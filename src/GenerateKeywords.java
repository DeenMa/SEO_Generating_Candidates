import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
/*
 * Algorithm:
 * 1. get data from source
 * 2. generate data: 
 *    HashMap<String, List<Combo> candidates>: string is the base Chinese word, 
 *    candidate is a class of Combo which represent the information of a candidate
 *    English representation of the Chinese word. 
 *    Combo include the String of the English representation, and a double for the
 *    possibility of this representation.
 * 3. Get the target Chinese combinations. And generate all possible 
 *    English representations based on it, with an overall possibility.
 * 4. Sort the possibility.
 * */
public class GenerateKeywords {
	/*
	 * Parameters define here ... 
	 * */
	private static String pathEntities = ".\\data\\keywords\\entities.txt";
	private static String pathIntents = ".\\data\\keywords\\intents.txt";
	private static String pathWords = ".\\data\\keywords\\candidateWords.txt";
	private static String pathPossibilities = ".\\data\\keywords\\candidatePossibilities.txt";
	
	/*
	 * Code starts here ...
	 * */
	private static class StringBinder {
		public String source;
		public String target;
	}
	
	private static class Combo {
		public String englishRepresentation;
		public double possibility;
		
		public Combo() {
			englishRepresentation = "";
			possibility = 1.0;
		}
		
		public Combo(String e, double p) {
			englishRepresentation = e;
			possibility = p;
		}
	}
	
	private static List<String> getStringsFromFile(String path) {
		List<String> lines = new ArrayList<>();
		BufferedReader br = null;
		FileReader fr = null;
		try {
			fr = new FileReader(path);
			br = new BufferedReader(fr);
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				lines.add(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return lines;
	}
	
	private static HashMap<String, List<Combo>> parseLineSource(List<String> lines) {
		checkValidity(lines);
		HashMap<String, List<Combo>> data = new HashMap<>();
		for (int curLineIndex = 0; curLineIndex < lines.size(); curLineIndex++) {
			String curLine = lines.get(curLineIndex);
			if (curLine == null || curLine.length() == 0) {
				throw new RuntimeException("parseLineSource: line " + curLineIndex + " is empty!");
			}
			int len = curLine.length();
			int index = 0;
			StringBuilder chineseWordBuilder = new StringBuilder();
			while (curLine.charAt(index) != ':') {
				chineseWordBuilder.append(curLine.charAt(index));
				index++;
			}
			String chineseWord = chineseWordBuilder.toString();
			if (data.containsKey(chineseWord)) {
				throw new RuntimeException("parseLine: we already have Chinese word "
						+ chineseWord);
			}
			index++;
			List<Combo> candidates = new ArrayList<>();
			while (index < len) {
				Combo combo = new Combo();
				index = getEntry(curLine, index, combo);
				candidates.add(combo);
				index++;
			}
			data.put(chineseWord, candidates);
		}
		return data;
	}
		
	private static int getEntry(String line, int index, Combo combo) {
		StringBuilder englishWord = new StringBuilder();
		StringBuilder possibilityString = new StringBuilder();
		boolean isNumber = false;
		while (index < line.length() && line.charAt(index) != ',') {
			if (line.charAt(index) == '/') { // divider
				isNumber = true;
			} else if (isNumber == false) {
				englishWord.append(line.charAt(index));
			} else {
				possibilityString.append(line.charAt(index));
			}
			index++;
		}
		combo.englishRepresentation = englishWord.toString();
		combo.possibility = convertToDouble(possibilityString.toString());
		return index;
	}
	
	private static double convertToDouble(String str) {
		if ("1".equals(str)) {
			return 1.0;
		}
		return Double.valueOf(str);
	}
	
	private static List<Combo> analyzeTarget(HashMap<String, List<Combo>> data, 
			List<String> lines) {
		List<Combo> result = new ArrayList<>();
		checkValidity(lines);
		for (int curLineIndex = 0; curLineIndex < lines.size(); curLineIndex++) {
			String curLine = lines.get(curLineIndex);
			List<String> chineseWords = getWords(curLine);
			List<Combo> currentEnglishCandidates = new ArrayList<>();
			collectWords(data, result, currentEnglishCandidates, chineseWords, 0);
		}
		return result;
	}
	
	private static void checkValidity(List<String> lines) {
		if (lines == null || lines.size() == 0) {
			throw new RuntimeException("parseLineSource: the content is empty!");
		}
	}
	
	private static void collectWords(HashMap<String, List<Combo>> data, 
			List<Combo> result, List<Combo> currentEnglishCandidates, 
			List<String> chineseWords, int index) {
		if (index == chineseWords.size()) {
			double comboOverall = 1.0;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < currentEnglishCandidates.size(); i++) {
				Combo current = currentEnglishCandidates.get(i);
				sb.append(current.englishRepresentation);
				if (i < currentEnglishCandidates.size() - 1) {
					sb.append(' ');
				}
				comboOverall *= current.possibility;				
			}
			String completeEnglishSentence = sb.toString();
			result.add(new Combo(completeEnglishSentence, comboOverall));
			return;
		}
		String curChineseWord = chineseWords.get(index);
		if (!data.containsKey(curChineseWord)) {
			throw new RuntimeException("collectWords: cannot find Chinese word " + 
				curChineseWord + " inside dataset");
		}
		List<Combo> availableCandidates = data.get(curChineseWord);
		for (int i = 0; i < availableCandidates.size(); i++) {
			Combo curCandidate = availableCandidates.get(i);
			currentEnglishCandidates.add(curCandidate);
			collectWords(data, result, currentEnglishCandidates, chineseWords, index + 1);
			currentEnglishCandidates.remove(currentEnglishCandidates.size() - 1);
		}
	}
	
	private static List<String> getWords(String line) {
		List<String> result = new ArrayList<>();
		if (line == null || line.length() == 0) {
			throw new RuntimeException("getWords: empty line");
		}
		int len = line.length();
		int index = 0;
		StringBuilder sb = new StringBuilder();
		while (index < len) {
			if (line.charAt(index) == ' ') {
				String str = sb.toString();
				if (!(" ".equals(str))) {
					result.add(str);
				}
				sb = new StringBuilder();
			} else {
				sb.append(line.charAt(index));
			}
			index++;
		}
		result.add(sb.toString());
		return result;
	}

	private static void sort(List<Combo> set) {
		Collections.sort(set, new Comparator<Combo>() {
			@Override
			public int compare(Combo arg0, Combo arg1) {
				if (arg0.possibility > arg1.possibility) {
					return -1;
				}
				if (arg0.possibility < arg1.possibility) {
					return 1;
				}
				return 0;
			}
		});
	}
	
	private static void writeSetToFile(List<Combo> set) {
		try {
		    PrintWriter writer = new PrintWriter(pathWords, "UTF-8");
		    for (int i = 0; i < set.size(); i++) {
		    	writer.println(set.get(i).englishRepresentation);
		    }
		    writer.close();
		} catch (IOException e) {
		   e.printStackTrace();
		}
		
		try {
		    PrintWriter writer = new PrintWriter(pathPossibilities, "UTF-8");
		    for (int i = 0; i < set.size(); i++) {
		    	writer.println(set.get(i).possibility);
		    }
		    writer.close();
		} catch (IOException e) {
		   e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		List<String> linesSource = getStringsFromFile(pathEntities);
		HashMap<String, List<Combo>> data = parseLineSource(linesSource);
		List<String> linesTarget = getStringsFromFile(pathIntents);
		List<Combo> set = analyzeTarget(data, linesTarget);
		sort(set);
		writeSetToFile(set);
	}
}
