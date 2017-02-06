package de.ipbhalle.phenomenal.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import de.ipbhalle.phenomenal.datatypes.Spectrum;
import de.ipbhalle.phenomenal.mzml.ImportMzML;

/**
 * Reads a mzML file and exports detected MS/MS spectra as command line parameters to an output folder to be ready to processed by MetFrag. 
 * 
 * @author cruttkie
 *
 */
public class MetFragParameterExporter {

	private static final String FILESEPARATOR = System.getProperty("file.separator");
	private static final String TMPFOLDER = System.getProperty("java.io.tmpdir");
	
	//parameter names used as command line arguments
	public static Vector<String> parameterNames = new Vector<String>(); 
		
	static {
		parameterNames.add("DatabaseSearchRelativeMassDeviation");
		parameterNames.add("FragmentPeakMatchAbsoluteMassDeviation");
		parameterNames.add("FragmentPeakMatchRelativeMassDeviation");
		parameterNames.add("MaximumTreeDepth");
		parameterNames.add("MetFragDatabaseType");
		parameterNames.add("MetFragScoreTypes");
		parameterNames.add("MetFragPreProcessingCandidateFilter");
		parameterNames.add("MetFragPostProcessingCandidateFilter");
		parameterNames.add("ChemSpiderToken");
		parameterNames.add("InputFile");								//spectral data file (mzML)
		parameterNames.add("OutputFolder");		
		parameterNames.add("OutputFile");		
		parameterNames.add("MaximumSpectrumLimit");		
	};
	
	//parameters used for MetFrag 
	public static Hashtable<String, String> parameters = new Hashtable<String, String>();
	
	static {
		parameters.put("DatabaseSearchRelativeMassDeviation", "10");
		parameters.put("FragmentPeakMatchAbsoluteMassDeviation", "0.01");
		parameters.put("FragmentPeakMatchRelativeMassDeviation", "10");
		parameters.put("MaximumTreeDepth", "2");
		parameters.put("MetFragDatabaseType", "KEGG");
		parameters.put("MetFragScoreWeights", "1.0");
		parameters.put("MetFragScoreTypes", "FragmenterScore");
		parameters.put("MetFragPreProcessingCandidateFilter", "IsotopeFilter,UnconnectedCompoundFilter");
		parameters.put("MetFragPostProcessingCandidateFilter", "InChIKeyFilter");
		parameters.put("MetFragPeakListReader", "de.ipbhalle.metfraglib.peaklistreader.FilteredStringTandemMassPeakListReader");
	}

	public static String InputFile;
	public static String OutputFolder;
	public static String OutputFile;
	public static int maximumSpectrumLimit = -1;
	
	private static Logger jlog =  Logger.getLogger(MetFragParameterExporter.class.getName());
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args == null || args.length == 0) {
			help();
			return;
		}
		// first read command line parameters
		if(!readParameters(args)) {
			return;
		}
		//check input mzML file
		if(InputFile == null) {
			jlog.warning("No input file given. Execution halted.");
			return;
		}
		//check output folder
		if(OutputFolder == null && OutputFile == null) {
			jlog.warning("No output folder/file given. Define at least OutputFolder or OutputFile. Execution halted.");
			return;
		}
		
		ImportMzML MzML_importer = new ImportMzML(InputFile);
		Vector<Spectrum> spectraMSMS = null;
		try {
			spectraMSMS = MzML_importer.getSpectraMSMS(maximumSpectrumLimit);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		if(spectraMSMS != null) {
			//get general parameter string
			String generalParameterString = "";
			Enumeration<String> keys = parameters.keys();
			while(keys.hasMoreElements()) {
				String key = keys.nextElement();
				generalParameterString += " " + key + "=" + parameters.get(key);
			}
			// write commands to output folder
			// for each spectrum a command is written in a single file, like command_1.txt 
			// each file contains the arguments passed to MetFrag CL
			Vector<String> commands = new Vector<String>();
			for(int i = 0; i < spectraMSMS.size(); i++) {
				String specificParameterString = generalParameterString.replaceAll("^\\s+", "");
				specificParameterString += " SampleName=" + spectraMSMS.get(i).getName();
				specificParameterString += " IonizedPrecursorMass=" + spectraMSMS.get(i).getPrecursorIonMass();
				specificParameterString += " PeakListString=" + spectraMSMS.get(i).getPeakString();
				specificParameterString += " PrecursorIonType=" + spectraMSMS.get(i).getPrecursorAdduct();
				specificParameterString += " MetFragCandidateWriter=CSV";
				specificParameterString += " ResultsPath=" + TMPFOLDER;
				commands.add(specificParameterString);
			}
			
			if(OutputFolder != null) {
				int numberFilePositions = (int)(1 + Math.floor(Math.log10(spectraMSMS.size())));
				for(int i = 0; i < commands.size(); i++) {
					try {
						BufferedWriter bwriter = new BufferedWriter(new FileWriter(new File(OutputFolder + FILESEPARATOR + "command_" + String.format("%0" + numberFilePositions + "d", (i + 1)) + ".txt")));
						bwriter.write(commands.get(i));
						bwriter.close();
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				}
			} 
			else if(OutputFile != null) {
				try {
					BufferedWriter bwriter = new BufferedWriter(new FileWriter(new File(OutputFile)));
					for(String command : commands) {
						bwriter.write(command);
						bwriter.newLine();
					}
					bwriter.close();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
			
		}
		
	}

	public static boolean readParameters(String[] args) {
		String argumentString = args[0];
        for(int i = 1; i < args.length; i++)
        	argumentString += " " + args[i];
        argumentString = argumentString.replaceAll("\\\\=", "|").replaceAll("\\s+=", "=").replaceAll("=\\s+", "=").replaceAll("\\s+", " ").replaceAll("\\t", " ");
        String[] arguments_splitted = argumentString.split("\\s+");
        for(int i = 0; i < arguments_splitted.length; i++) {
			String[] tmp = arguments_splitted[i].split("=");
			if(tmp.length != 2) {
				if(tmp[0].equals("-help") || tmp[0].equals("--help") || tmp[0].equals("help")) {
					help();
					return false;
				}
				jlog.warning("Error at " + arguments_splitted[i]);
                return false;
			}
			if(parameterNames.contains(tmp[0])) {
				if(tmp[0].equals("InputFile")) InputFile = tmp[1];
				else if(tmp[0].equals("OutputFolder")) OutputFolder = tmp[1];
				else if(tmp[0].equals("OutputFile")) OutputFile = tmp[1];
				else if(tmp[0].equals("MaximumSpectrumLimit")) maximumSpectrumLimit = Integer.parseInt(tmp[1]);
				else parameters.put(tmp[0], tmp[1]);
				//adapt MetFragScoreWeights if MetFragScoreTypes are set
				//each score adds a weight
				if(tmp[0].equals("MetFragScoreTypes")) {
					for(int k = 0; k < tmp[1].length(); k++)
						if(tmp[1].charAt(i) == ',') parameters.put("MetFragScoreWeights", parameters.get("MetFragScoreWeights") + ",1.0");
				}
			} 
			else {
				jlog.warning("Error: Unknown parameter " + tmp[0]);
                return false;
			}
		}
        return true;
	}
	
	public static void help() {
		System.out.println("Usage: java -jar MetFragParameterExporter.jar InputFile=... OutputFolder=... [OutputFile=...] [options]");
		System.out.println();
		System.out.println("Options:");
		for(String parameterName : parameterNames) {
			System.out.println();
			System.out.println("\t" + parameterName + "=...");
		}
		System.out.println();
		System.out.println("\t--help, -help, help");
	}
}
