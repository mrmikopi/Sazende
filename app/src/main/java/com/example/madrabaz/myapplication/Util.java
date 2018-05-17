package com.example.madrabaz.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.PitchConverter;

import static android.content.ContentValues.TAG;

/**
 * Created by Madrabaz on 10.4.2018.
 */

class Util {

    private Map<String, List<List<Float>>> makams = new LinkedHashMap<>();
    private List<Map<String, List<Float>>> makamsSecond = new LinkedList<>();
	private Context context;
	
    Util(Context context){
		this.context = context;
	}

	void loadMakams2() throws IOException{
        final Resources resources = context.getResources();
        int[] raws = {R.raw.aeuntervals2, R.raw.arelezgiuzdilek, R.raw.yarman1, R.raw.yarman2,
                R.raw.yarman3, R.raw.karadeniz, R.raw.yavuzoglu};
        String currentLine;
        List<Map<String, List<Float>>> makamList = new LinkedList<>();
        String[] words;
        List<Float> insideList;
        Map<String, List<Float>> insideMap;
        for(int c : raws){
            InputStream inStream = resources.openRawResource(c);
            BufferedReader bis = new BufferedReader(new InputStreamReader(inStream));
            insideMap = new HashMap<>();
            try{
                while ((currentLine = bis.readLine()) != null) {
                    insideList = new ArrayList<>();
                    words = currentLine.split("\\t");
                    for (int i = 1; i < words.length; i++) {
                        insideList.add(Float.parseFloat(words[i]));
                    }
                    // Fill the list to 13 elements
                    int length = insideList.size();

                    System.out.println("");
                    for (int i = 0; i < 13 - length; i++) {
                        insideList.add(insideList.get(insideList.size() - 7) + 53);
                    }

                    insideMap.put(words[0], insideList);
                }
                makamList.add(insideMap);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        makamsSecond = makamList;
    }

    void loadMakams() throws IOException {

        final Resources resources = context.getResources();
        int[] raws = {R.raw.aeuntervals2, R.raw.arelezgiuzdilek, R.raw.yarman1, R.raw.yarman2,
                R.raw.yarman3, R.raw.karadeniz, R.raw.yavuzoglu};
        String currentLine;
        HashMap<String, List<List<Float>>> makamMap = new HashMap<>();
        String[] words;
        List<Float> insideList;
        List<List<Float>> outerList;
        for(int c : raws) {
            InputStream inStream = resources.openRawResource(c);
            BufferedReader bis = new BufferedReader(new InputStreamReader(inStream));

            try {
                while ((currentLine = bis.readLine()) != null) {
                    insideList = new ArrayList<>();
                    words = currentLine.split("\\t");
                    for (int i = 1; i < words.length; i++) {
                        insideList.add(Float.parseFloat(words[i]));
                    }
                    // Fill the list to 13 elements
                    int length = insideList.size();

                    System.out.println("");
                    for (int i = 0; i < 13 - length; i++) {
                        insideList.add(insideList.get(insideList.size() - 7) + 53);
                    }
                    if (!makamMap.containsKey(words[0])) {
                        outerList = new ArrayList<>();
                        outerList.add(insideList);
                        makamMap.put(words[0], outerList);
                    } else {
                        outerList = makamMap.get(words[0]);
                        outerList.add(insideList);
                        makamMap.put(words[0], outerList);
                    }
                }

            } catch (IOException e) {
                // if any I/O error occurs
                e.printStackTrace();
            }

            finally {
                // releases any system resources associated with the stream
                //if (inStream != null)
                    inStream.close();
                //if (bis != null)
                    bis.close();
            }
        }
        makams = makamMap;
    }

	// Holds the Algorithm
	public String getMakams(String uri1){
		
		Uri uri = Uri.parse(uri1);
        String path = PathFinder.getPath(context, uri);
		double songDuration = getSongDuration(uri);
		
		List<List<SpectralPeakProcessor.SpectralPeak>> peaks = getPeaks(path, songDuration);
		Map<Float, Weight> allFrequencies = toTreeMap(peaks);
		float[][] freqMagArrays = mapToArrays(allFrequencies);
		List<SpectralPeakProcessor.SpectralPeak> cleanPeaks = peaksOfPeaks(freqMagArrays);
		Map<Float,Float> cleanMap = peaksToLastMapFrequencyOrder(cleanPeaks);
		List<List<Float>> songNotes = getSongNotes(cleanMap);

        return getResults(songNotes, makams);
	}

	// Returns length of the song as Seconds, double type.
	private double getSongDuration(Uri uri){
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(context, uri);
		String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		int millSecond = Integer.parseInt(durationStr);
		return ((double)millSecond * 1000);
	}
	
    private List<List<SpectralPeakProcessor.SpectralPeak>> getPeaks(String path1, final double songDuration){

        final List<List<SpectralPeakProcessor.SpectralPeak>> peaks = new LinkedList<>();
        final String path = path1;
        System.out.println("path1: " + path1 + ", path: " + path);
        assert path != null;
		/* ********** THREAD STARTS ********** */
        Thread thisOne = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path,5500,256,128);
				// TODO dispatcher.skip(songDuration/2);
                final SpectralPeakProcessor spectralPeakFollower = new SpectralPeakProcessor(256, 128, 5500);
                dispatcher.addAudioProcessor(spectralPeakFollower);
                dispatcher.addAudioProcessor(new AudioProcessor() {
                    /* ********** TEKRAR EDEN KISIM ********** */
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        float[] magnitudes = spectralPeakFollower.getMagnitudes();
                        float[] freqs = spectralPeakFollower.getFrequencyEstimates();
                        float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(magnitudes, 6, 1f);
                        List<Integer> localMaximaIndexes = SpectralPeakProcessor.findLocalMaxima(magnitudes, noiseFloor);
                        List<SpectralPeakProcessor.SpectralPeak> peak =
                                SpectralPeakProcessor.findPeaks(magnitudes, freqs, localMaximaIndexes, 8, 80);
                        peaks.add(peak);
                        return true;
                    }
                    @Override
                    public void processingFinished() {
                    }
                });

                dispatcher.run();

            }
        });
        thisOne.start();
        try {
            thisOne.join();
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        return peaks;
    }


    private Map<Float, Weight> toTreeMap(List<List<SpectralPeakProcessor.SpectralPeak>> peaks){
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		DecimalFormat formatter = (DecimalFormat)nf;
		formatter.applyPattern("#.#");
		float something;
        String fString;
        // Using Treemap, since the order of keys are important
        TreeMap<Float, Weight> myMap = new TreeMap<>();
        for(List<SpectralPeakProcessor.SpectralPeak> a : peaks){
            for(SpectralPeakProcessor.SpectralPeak b : a){
				fString = formatter.format(b.getFrequencyInHertz());
                something = Float.parseFloat(fString);
                if(!myMap.containsKey(something)){
                    myMap.put(something, new Weight(b.getMagnitude()));
                } else {
                    myMap.put(something, myMap.get(something).addMagnitude(b.getMagnitude()));
                }
            }
        }
        return myMap;
    }

    private void printMap(Map<Float, Weight> map) throws IOException{
        System.out.println("WRITING FREQS on directory: " + context.getFilesDir());

        FileWriter fw = new FileWriter(new File("/storage/emulated/0/Download/freqs.txt"), true);
        BufferedWriter bw = new BufferedWriter(fw);

        Iterator<Map.Entry<Float, Weight>> it = map.entrySet().iterator();
        int count = 0;
        bw.write("PRINTING EVERY NOTE: \n");
        while (it.hasNext() && count < map.size()) {

            // the key/value pair is stored here in pairs
            Map.Entry<Float, Weight> pairs = it.next();

            // since you only want the value, we only care about pairs.getValue(), which is written to out
            bw.write(Float.toString(pairs.getKey()) + "\t" + Float.toString(pairs.getValue().getAverage()) + "\n");

            // increment the record count once we have printed to the file
            count++;
        }
        bw.write("\n\n");
        bw.close();
        fw.close();
    }



    private float[][] mapToArrays(Map<Float, Weight> myMap){
        float[][] toReturn = new float[2][myMap.size()];
        int i = 0;
        for(Map.Entry<Float, Weight> a: myMap.entrySet()){
            toReturn[0][i] = a.getValue().getAverage();
            toReturn[1][i] = a.getKey();
            i++;
        }
        return toReturn;
    }

    private List<SpectralPeakProcessor.SpectralPeak> peaksOfPeaks(float[][] holds){
        System.out.println("DONE PRINTING FREQUENCES");
        float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(holds[0], 5, 1f);
        List<Integer> localMaximas =SpectralPeakProcessor.findLocalMaxima(holds[0], noiseFloor);
        return SpectralPeakProcessor.findPeaks(holds[0], holds[1], localMaximas, 185, 80);
    }


    private Map<Float,Float> peaksToLastMapFrequencyOrder(List<SpectralPeakProcessor.SpectralPeak> peaks1) {
        LinkedHashMap<Float, Float> lastOne = new LinkedHashMap<>();
        for(SpectralPeakProcessor.SpectralPeak a : peaks1){
            lastOne.put(a.getFrequencyInHertz(), a.getMagnitude());
        }
        return lastOne;
    }
    TreeMap<Float,Float> peaksToLastMapValueOrder(List<SpectralPeakProcessor.SpectralPeak> peaks1, ValueComparator vc) {
        TreeMap<Float, Float> lastOne = new TreeMap<>(vc);
        for(SpectralPeakProcessor.SpectralPeak a : peaks1){
            lastOne.put(a.getFrequencyInHertz(), a.getMagnitude());
        }
        return lastOne;
    }

    private List<List<Float>> getSongNotes(Map<Float, Float> lastOne){
        float[] notes = getMaxNotes(lastOne.entrySet());
        List<List<Float>> songNotes = getNotes(lastOne, notes);
        songNotes = getCommas(songNotes);

        return songNotes;
    }

    // This method may return 0s for max-valued notes.
    private float[] getMaxNotes(Set<Map.Entry<Float, Float>> lastOne){
        float maxValue = 0;
        float[] notes = new float[3];
        for(Map.Entry<Float, Float> a : lastOne){
            if (maxValue < a.getValue()){
                notes[0] = a.getKey();
                maxValue = a.getValue();
            }
        }
        maxValue = 0;
        for(Map.Entry<Float, Float> a : lastOne){
            if (maxValue < a.getValue()){
                if(a.getKey()!= notes[0]) {
                    notes[1] = a.getKey();
                    maxValue = a.getValue();
                }
            }
        }
        maxValue = 0;
        for(Map.Entry<Float, Float> a : lastOne){
            if (maxValue < a.getValue()){
                if(a.getKey() != notes[0] && a.getKey() != notes[1]) {
                    notes[2] = a.getKey();
                    maxValue = a.getValue();
                }
            }
        }
        return notes;
    }

    private List<List<Float>> getNotes(Map<Float, Float> lastOne, float[] notes) {

        List<List<Float>> allNotes = new LinkedList<>();
        List<Float> toAdd;
        List<Integer> octaveIndices;
        List<Integer> tempIndices;
        List<Float> freqs = new ArrayList<>(lastOne.keySet());
        List<Float> values = new ArrayList<>(lastOne.values());
        
        for(float a : notes){
            octaveIndices = new ArrayList<>();

            for (int i = freqs.indexOf(a) + 1; i < freqs.size(); i++){

                if(freqs.get(i) < a*2.015f){
                    octaveIndices.add(i);
                }
            }

            tempIndices = new LinkedList<>();

            tempIndices.add(freqs.indexOf(a));
            for (int i = 1; i < 8; i++) {
                float max = 0f;

                for (int b = 0; b < octaveIndices.size(); b++)
                    if (values.get(octaveIndices.get(b)) > max && !tempIndices.contains(octaveIndices.get(b))) {
                        max = values.get(octaveIndices.get(b));
                    }
                if (max == 0f) {
                    tempIndices.add(tempIndices.get(tempIndices.size() - 1));
                    System.out.println("Added Duplicate!");
                } else {
                    tempIndices.add(values.indexOf(max));
                }
            }

            Collections.sort(tempIndices);
            // Add First Octave to the return list.
            toAdd = new LinkedList<>();
            if(tempIndices.isEmpty()){
                System.out.println("THIS IS EMPTY");
				// Default makam can be put here.
			} else {
                // Used to give index = -1 error, solved by replacing previous note if no other max is found.
                for (int b : tempIndices) {
                        toAdd.add(freqs.get(b));
                }
            }

            // ****** IKINCI OKTAV ******* //
			
            // Get last element's index + 1 as new starting index
            int startingIndex = octaveIndices.get(octaveIndices.size() - 1) + 1;
            octaveIndices.clear();

            // Get all the notes for second octave
            for (int i = startingIndex; i < freqs.size(); i++){
                if(freqs.get(i) < a*3.97){
                    octaveIndices.add(i);
                }
            }

            tempIndices.clear();

            //  if(octave.size() >= 6) was used to be here,
			// Can be put 0*0*0*0*0 default makam for its else block.
            for (int i = 0; i < 6; i++) {

                float max = 0f;

                for (int b = 0; b < octaveIndices.size(); b++) {
                    if (values.get(octaveIndices.get(b)) > max && !tempIndices.contains(octaveIndices.get(b))) {
                        max = values.get(octaveIndices.get(b));
                    }
                }if (max == 0f) {
                    tempIndices.add(tempIndices.get(tempIndices.size() - 1));
                    System.out.println("Added Duplicate!");
                } else {
                    tempIndices.add(values.indexOf(max));
                }
            } // End of second octave

            if(tempIndices.isEmpty()){
                System.out.println("THIS IS EMPTY");
                // Default makam can be put here.
            } else {
                // Used to give index = -1 error, should be solved by the b!=0 check
                for (int b : tempIndices) {
                    // There was a check for -1 index. No longer since replaced notes for maxes
                        toAdd.add(freqs.get(b));
                }
            }
            // Sorting is necessary
            Collections.sort(toAdd);
            allNotes.add(toAdd);
        }

        return allNotes;
    }

    private List<List<Float>> getCommas(List<List<Float>> songNotes) {

		List<List<Float>> allCommas = new LinkedList<>();
        for(List<Float> a : songNotes){
            List<Float> compared = new LinkedList<>();
            double base = PitchConverter.hertzToAbsoluteCent(a.get(0));
			// Skips the first element, since it is the tonic note
            for(int i = 1; i < a.size(); i++){
                double delta = PitchConverter.hertzToAbsoluteCent(a.get(i)) - base;
                // Cents to coma, possible: 19,9989 - 22 - 22.65. 1200/53 for AREL rules.
				delta = delta / (1200f/53f);
                compared.add((float)delta);
            }
			allCommas.add(compared);
        }
        System.out.println("Comma versions are:\n " + allCommas.toString());
        return allCommas;
    }

    // Keys are floats, so it can be ordered if wanted.
    private String getResults(List<List<Float>> songNotes, Map<String, List<List<Float>>> makams) {
        TreeMap<Float, String> result = new TreeMap<>();

        // for three probabilities
        for(List<Float> a : songNotes){
            double temp = 0;
            float distance = Float.MAX_VALUE;
            String makam = "";
            // for each makam
            for(Map.Entry<String, List<List<Float>>> b : makams.entrySet()){
                // for each implementation of that makam
                for(List<Float> c : b.getValue()){
                    // Calculate distance between our song and point.
                    if(a.size() >= c.size()) {
                        for (int i = 0; i < c.size(); i++) {
                            temp += Math.pow((double) (c.get(i) - a.get(i)), 2);
                        }
                    }
                    temp = Math.sqrt(temp);
                    if(temp < distance){
                        distance = (float)temp;
                        makam = b.getKey();
                    }
                    temp= 0;
                }
            }
            result.put(distance, makam);
        }
        return result.get(result.firstKey());
    }
}