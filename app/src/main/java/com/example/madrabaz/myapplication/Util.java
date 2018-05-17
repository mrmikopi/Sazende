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
                    // TODO Clearlar burada yapilabilir
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
	public Map<Float, String> getMakams(String uri1){
		
		Uri uri = Uri.parse(uri1);
		// TODO Calisacak mi?
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
		// TODO Calisacak mi
		mmr.setDataSource(context, uri);
		String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		int millSecond = Integer.parseInt(durationStr);
		return ((double)millSecond * 1000);
	}
	
    private List<List<SpectralPeakProcessor.SpectralPeak>> getPeaks(String path1, final double songDuration){

        final List<List<SpectralPeakProcessor.SpectralPeak>> peaks = new LinkedList<>();
        final String path = path1;
        System.out.println("path1: " + path1 + ", path: " + path);
        // TODO Duzgun bi path check yap, null check filan
        assert path != null;
		/* ********** THREAD STARTS ********** */
        Thread thisOne = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: Dosya wav ise pipe atma
                // TODO Dosya boyutu cok buyukse alma

                //1432 (512) 739 (1024)
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path,5500,256,128);
				// TODO dispatcher.skip(songDuration/2);
                final SpectralPeakProcessor spectralPeakFollower = new SpectralPeakProcessor(256, 128, 5500);
                dispatcher.addAudioProcessor(spectralPeakFollower);
                dispatcher.addAudioProcessor(new AudioProcessor() {
                    /* ********** TEKRAR EDEN KISIM ********** */
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        // TODO: Değerleri değiştir, tekrar dene.
                        //System.out.println("workworkworkworkworkwork");
                        float[] magnitudes = spectralPeakFollower.getMagnitudes();
                        float[] freqs = spectralPeakFollower.getFrequencyEstimates();
                        float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(magnitudes, 3, 1f);
                        List<Integer> localMaximaIndexes = SpectralPeakProcessor.findLocalMaxima(magnitudes, noiseFloor);
                        // TODO: Number of Peaks ve Distance in Cents degisecek.
                        // Cent=20>40
                        List<SpectralPeakProcessor.SpectralPeak> peak =
                                SpectralPeakProcessor.findPeaks(magnitudes, freqs, localMaximaIndexes, 5, 80);
                        peaks.add(peak);
                        return true;
                    }
                    @Override
                    public void processingFinished() {
                        // Loading bar? tarsosun sitesinde vardi sanirim
                    }
                });

                dispatcher.run();

            }
        });
        thisOne.start();
        // TODO Threadi kapatacaksin veya diger activity e gecince threadin durumunu dusuneceksin.
        try {
			// Load bari buraya da ekleyebilirsin
            thisOne.join();
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        return peaks;
    }


    private Map<Float, Weight> toTreeMap(List<List<SpectralPeakProcessor.SpectralPeak>> peaks){
        // TODO: Bu (sout veya iterate eden) forlar da uzun surebilir, yeni bi thread e tasiyabilirsin.
        // TODO: Ici bos spectralPeakler var mi yok mu, onlari es gectim
        // TODO: Virgulden sonra tek bi hane birakmayi dene
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		DecimalFormat formatter = (DecimalFormat)nf;
		formatter.applyPattern("#.#");
		float something;
        String fString;
        // Burasi TreeMap, cunku frekans siralamasi yapip gondermeli
        TreeMap<Float, Weight> myMap = new TreeMap<>();
        for(List<SpectralPeakProcessor.SpectralPeak> a : peaks){
            // TODO a'dakilerin en yuksek magnitudelarini al
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
//        try{
//            printMap(myMap);
//        } catch (IOException e){
//            e.printStackTrace();
//        }
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
        // TODO ARRAY OUT OF BOUNDS YEDIM, BELKI TRY CATCH
        // TODO: degerleri degistir ve tekrar dene. Muhtemelen farkli olmayacaktir.
//        for(int i = 0; i < holds[1].length ; i++){
//            System.out.println(holds[1][i]);
//        }
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
//        try{
//            FileWriter fw = new FileWriter(new File("/storage/emulated/0/Download/freqs.txt"), true);
//            BufferedWriter bw = new BufferedWriter(fw);
//
//            bw.write("PRINTING SELECTED PEAK NOTES: \nNot sure if they are in order or not.\n");
//
//            for(Map.Entry<Float, Float> a : lastOne.entrySet() ){
//                bw.write(Float.toString(a.getKey()) + "\t" + Float.toString(a.getValue()) + "\n");
//            }
//            bw.write("\n\n");
//            bw.close();
//            fw.close();
//        }   catch (IOException e){
//            e.printStackTrace();
//        }
        return lastOne;
    }
    TreeMap<Float,Float> peaksToLastMapValueOrder(List<SpectralPeakProcessor.SpectralPeak> peaks1, ValueComparator vc) {
        TreeMap<Float, Float> lastOne = new TreeMap<>(vc);
        for(SpectralPeakProcessor.SpectralPeak a : peaks1){
            lastOne.put(a.getFrequencyInHertz(), a.getMagnitude());
        }
        return lastOne;
    }


    // TODO********* MAIN METHOD MAIN METHOD MAIN METHOD *********
    // TODO********* MAIN METHOD MAIN METHOD MAIN METHOD *********
    // TODO********* MAIN METHOD MAIN METHOD MAIN METHOD *********
    private List<List<Float>> getSongNotes(Map<Float, Float> lastOne){
        float[] notes = getMaxNotes(lastOne.entrySet());
        List<List<Float>> songNotes = getNotes(lastOne, notes);
        // TODO: Burayi songNotes = getCommas(songNotes); yap, print testler bitince
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
//        List<Float> octaveFreqs;
        List<Integer> tempIndices;
        List<Float> freqs = new ArrayList<>(lastOne.keySet());
        //System.out.println(freqs.toString());
        List<Float> values = new ArrayList<>(lastOne.values());
        //System.out.println(values.toString());

		// TODO 
		// TODO
		// TODO a yi notalari yazdir, tonicleri, ayni olabilir!!!!!!!
		// TODO
		// TODO
        for(float a : notes){
            octaveIndices = new ArrayList<>();

            //System.out.println("FIRST OCTAVE BEFORE AZALTMA");
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
                // TODO Buraya default 0 0 0 0 0 0 0 makamini koyabilirsin
            } else {
                // TODO Gives index=-1 error! Nasil oluyorsa...
                for (int b : tempIndices) {
                        toAdd.add(freqs.get(b));
                }
            }

            // ****** IKINCI OKTAV ******* //

            // Octave.get(7) hata verirse octave.get(octave.size()-1) yapabilirsin, son elemana ulastirir.
            // Octave daha az elemanli olursa dedigim lazim olabilir.
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

            //  TODO if(octave.size() >= 6) vardi burada, bunun else ini 0 0 0 0 0default makami icin kullanabilirsin
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

            // Should never come here
            if(tempIndices.isEmpty()){
                System.out.println("THIS IS EMPTY");
                // TODO Buraya default 0 0 0 0 0 0 0 makamini koyabilirsin
            } else {
                // Used to give index = -1 error, should be solved by the b!=0 check
                for (int b : tempIndices) {
                    // There was a check for, if not -1, this. Else, put 0f for hertz.(It crashes)
                        toAdd.add(freqs.get(b));
                }
            }
            // Sorting is necessary
            Collections.sort(toAdd);
            allNotes.add(toAdd);
        }

        return allNotes;
    }

    public List<Float> fillEmptyIndices(List<Float> temp, float currentTonic) {

//        // Set first note if it is 0
//        if(temp.get(0) == 0f){
//            temp.set(0, currentTonic);
//        }
        // Set last note if it is 0
//        if(temp.get(temp.size() - 1) == 0f){
//            double tonic = PitchConverter.hertzToAbsoluteCent((double)currentTonic);
//            double last = PitchConverter.absoluteCentToHertz(tonic + 1200);
//            temp.set(temp.size() - 1, (float)last);
//        }
        //System.out.println("Changed First and Last indices, list is now " + temp.toString());

        // TODO Sifirli elementleri aralari en acik olan degerlerin arasina yerlestir.
        int countOfZeros = 0;
        for(Float f : temp){
            if(f == 0f){
                countOfZeros++;
            }
        }

        // Quit method if possible
        if(countOfZeros == 0)
            return temp;

        //Replace non zero elments with their absoulteCents
        for(Float f : temp){
            if(f != 0f){
                f = (float)PitchConverter.hertzToAbsoluteCent(f);
            }
        }

        // Find largest Gaps at number of zeros
        List<Double> gaps = new LinkedList<>();

        for(int c = 0; c < countOfZeros; c++){
            for(int d = 0; d < temp.size(); d++){

            }
        }





        // 0 li elementleri degistir
        for(int i = 1; i < temp.size(); i++){
            // Found element with 0
            if(temp.get(i) == 0f){
                // Cevresine bak, Onceki dolu mu, Sonraki dolu mu.
                // Onceki bos olamaz muhtemelen, cunku bu adimdan gecip dolmustur.
                // Aslinda olabilir de bilmiyorum
                int notesTilNextNote = 1;
                int tempI = i+1;
                // Has spaces bet
                while(temp.get(tempI) == 0f && tempI < temp.size()-1){
                    /*if(tempI < temp.size()-1){
                        break;
                    }*/
                    notesTilNextNote++;
                    tempI++;
                    //System.out.println("ENTERED INCREMENT, notesTil is now " + notesTilNextNote);
                }
                //System.out.println("Next 0 is " + notesTilNextNote + " notes away");
                // TODO if(son notaya kadar sifir cikmis ise){...}
                double previous = PitchConverter.hertzToAbsoluteCent(temp.get(i-1));
                // TODO temp.get kismi error veriyor, sanirim bi ustteki todoyla alakali
                double next = PitchConverter.hertzToAbsoluteCent(temp.get(i + notesTilNextNote));

                //if(next == 0.0)
                    //System.out.println("NEXT IS STILL 0!!!");
                double distance = (next - previous) / (notesTilNextNote + 1);
                double newNote = PitchConverter.absoluteCentToHertz(previous + distance);
                temp.set(i, (float)newNote);


            }
        }
        return temp;
    }

    private List<List<Float>> getCommas(List<List<Float>> songNotes) {

		List<List<Float>> allCommas = new LinkedList<>();
        for(List<Float> a : songNotes){
            List<Float> compared = new LinkedList<>();
            double base = PitchConverter.hertzToAbsoluteCent(a.get(0));
			// Skips the first element, since it is the tonic note
            for(int i = 1; i < a.size(); i++){
                double delta = PitchConverter.hertzToAbsoluteCent(a.get(i)) - base;
                // Cents to coma, possible: 19,9989 - 22 - 22.65
				delta = delta / (1200f/53f);
                compared.add((float)delta);
            }
			allCommas.add(compared);
        }
        System.out.println("Comma versions are:\n " + allCommas.toString());
        //System.out.println("SONG NOTES ARE (Comma): \n" + allCommas.toString() + "\n");
        return allCommas;
    }

    // TODO Incele neden Float String de String Float degil?
    private Map<Float, String> getResults(List<List<Float>> songNotes, Map<String, List<List<Float>>> makams) {
        HashMap<Float, String> result = new HashMap<>();

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
                    // TODO: Buna girmedigi oluyor, cozum bul.
                    if(a.size() >= c.size()) {
                        for (int i = 0; i < c.size(); i++) {
                            // TODO IndexOutOfBounds yedik
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
        System.out.println("PRINTING RESULTS");
        for(Map.Entry<Float, String> a : result.entrySet()){
            System.out.println("Makam: " + a.getValue() + ", distance: " + a.getKey());
        }
        return result;
    }
}