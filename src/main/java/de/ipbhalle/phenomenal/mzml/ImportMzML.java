package de.ipbhalle.phenomenal.mzml;

import io.github.msdk.datamodel.rawdata.IsolationInfo;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.io.mzml.MzMLFileImportMethod;

import java.util.logging.Logger;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;
import java.util.List;

import de.ipbhalle.phenomenal.datatypes.Spectrum;

public class ImportMzML {

	private final String PRECURSOR_ADDUCT_TYPE_NAME = "Precursor_Ion";
	private final String DEFAULT_PRECURSOR_ADDUCT_TYPE = "[M+H]+";
	
	private static Logger jlog =  Logger.getLogger(ImportMzML.class.getName());
	
	private String fileName;
	
	public ImportMzML(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Read MSMS spectra entries from the mzML file. The mzML should contain user parameter annotations 
	 * containing adduct type information. If no adduct type information is provided [M+H]+ is assumed
	 * and a warning will be thrown.
	 * 
	 * 
	 * @return
	 * @throws Exception
	 */
	public Vector<Spectrum> getSpectraMSMS(int limit) throws Exception {
		Vector<Spectrum> spectraMSMS = new Vector<Spectrum>();
		File file = new File(this.fileName);
		if(!file.exists()) throw new Exception(file.getAbsolutePath() + " not found!");
		if(!file.canRead()) throw new Exception("No read permission for " + file.getAbsolutePath() + "!");
		
		MzMLFileImportMethod mzml = new MzMLFileImportMethod(file);
		RawDataFile result = mzml.execute();
		
		List<MsScan> scans = result.getScans();
		
		System.out.println("read " + scans.size() + " spectra");
		for(int i = 0; i < scans.size(); i++) {
			if(limit != -1 && spectraMSMS.size() == limit) break; 
			MsScan scan = scans.get(i);
            List<IsolationInfo> iso = scan.getIsolations();
            for(int k = 0; k < iso.size(); k++) {
            	IsolationInfo isoInfo = iso.get(k);
            	
            	String adductType = DEFAULT_PRECURSOR_ADDUCT_TYPE;
            	//get user parameter for adduct type information
            	Hashtable<String, String> userParams = scan.getUserParams();
            	if(userParams == null) 
            		jlog.warning("No adduct information provided for MS/MS spectrum: ID " + scan.getScanNumber() + ", Mass: " + isoInfo.getPrecursorMz() + ". Assuming " + DEFAULT_PRECURSOR_ADDUCT_TYPE);
            	else {
            		if(userParams.containsKey(PRECURSOR_ADDUCT_TYPE_NAME)) {
            			adductType = userParams.get(PRECURSOR_ADDUCT_TYPE_NAME);
            		} else {
                		jlog.warning("No adduct information provided for MS/MS spectrum: ID " + scan.getScanNumber() + ", Mass: " + isoInfo.getPrecursorMz() + ". Assuming " + DEFAULT_PRECURSOR_ADDUCT_TYPE);
            		}
            	}
            	
            	double[] mzValues = scan.getMzValues(); 
        		float[] intensities = scan.getIntensityValues();
        		
        		//check peak data and skip scan if inconsistencies are detected
        		if(mzValues == null || intensities == null || mzValues.length == 0 || intensities.length == 0) {
        			jlog.warning("No peak information provided for MS/MS spectrum: ID " + scan.getScanNumber() + ", Mass: " + isoInfo.getPrecursorMz() + ".");
        			break;
        		}
        		if(mzValues.length != intensities.length) {
        			jlog.warning("Peak information provided for MS/MS spectrum inconsistent: ID " + scan.getScanNumber() + ", Mass: " + isoInfo.getPrecursorMz() + ". Found " + mzValues.length + " masses and " + intensities.length + " intensities.");
        			break;
        		}
        		
        		//write peak string
        		String peakString = mzValues[0] + "_" + intensities[0];
        		for(int kk = 1; kk < mzValues.length; kk++) {
        			peakString += ";" + mzValues[kk] + "_" + intensities[kk];
        		}
        		
        		spectraMSMS.add(new Spectrum(peakString, adductType, isoInfo.getPrecursorMz(), "Spectrum_" + scan.getScanNumber() + "_" + (k + 1)));
            }
		}
		return spectraMSMS;
	}
	
	public Vector<Spectrum> getSpectraMSMS() throws Exception {
		return this.getSpectraMSMS(-1);
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
