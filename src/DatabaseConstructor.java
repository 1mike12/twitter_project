import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class DatabaseConstructor {

    public static HashMap<Integer, String> OriginalTweetIndex;
    public static HashMap<String, HashMap<Integer, Integer>> WordTweetDatabase;

    public static int TWEET_COUNT;

    public static Boolean initialized = false;
    public static void initialize(String file){
        if(!initialized) {
            initialized = true;
            //matching individual tweets to an index integer
            OriginalTweetIndex = new HashMap<Integer, String>();
            //maps each unique word that appear s in the entire set of tweets to a table of tweet#(int)/word frequencies(int)
            WordTweetDatabase = new HashMap<String, HashMap<Integer, Integer>>();
            TWEET_COUNT = 0;

//        String file = "SampleInputForProject1.txt";
            Read_Tweets(file);
        }
        else{
            return;
        }
    }

    public static void Read_Tweets(String file) {
        //read tweets into hash map <tween_index : int, tweet_content : string>
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                OriginalTweetIndex.put(TWEET_COUNT, line);
                for(String word : line.split(" ")){
                    word = word.replace(",", "");
                    word = word.replace("/", " ");
                    word = word.replace(".", "");
                    word = word.replace("\'", "");
                    word = word.replace("\"", "");
                    word = word.replace("\\", " ");
                    word = word.replace("(s)", "");
                    word = word.replace("?", "");
                    word = word.replace("$", "");
                    word = word.replace("(", "");
                    word = word.replace(")", "");
                    word = word.replace("[", "");
                    word = word.replace("]", "");
                    word = word.replace("flipit", "");
                    insertWordIntoDictionary(TWEET_COUNT, word);
                }
                TWEET_COUNT++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertWordIntoDictionary(int tweetNumber, String word){
        //find the word in the dictionary; dictionary length = # of unique words in set of tweets
        if(WordTweetDatabase.containsKey(word)){
            //find if this tweet already counted this word once
            if(WordTweetDatabase.get(word).containsKey(tweetNumber)){
                //increment the count for this word in this tweet
                int wordCount = WordTweetDatabase.get(word).get(tweetNumber)+1;
                WordTweetDatabase.get(word).put(tweetNumber, wordCount);
            }
            else{
                //otherwise, put '1' in for the frequency of this word, add the tweet # to the table
                WordTweetDatabase.get(word).put(tweetNumber, 1);
            }
        }
        //if the dictionary does not yet contain the word, we add it in for the first tie
        else{
            WordTweetDatabase.put(word, new HashMap<Integer, Integer>());
            WordTweetDatabase.get(word).put(tweetNumber, 1);
        }
    }

    public static HashMap<Integer, Integer> matchQuery(String query){
        //construct a new hashmap;
        //key = tweet #
        //value = sum of frequencies of query words
        HashMap<Integer, Integer> results = new HashMap<Integer, Integer>();
        for(String word : query.split(" ")){
            if(!WordTweetDatabase.containsKey(word)) continue;
            for(int tweetNumber : WordTweetDatabase.get(word).keySet()){
                //if our results already contain the tweet, increment its weight (total frequency of occurrences)
                if(results.containsKey(tweetNumber)){
                    int tweetWeight = results.get(tweetNumber)+1;
                    results.put(tweetNumber, tweetWeight);
                    System.out.print(word+" "+tweetNumber+" "+tweetWeight);
                }
                //otherwise, add it to the table
                else {
                    results.put(tweetNumber, WordTweetDatabase.get(word).get(tweetNumber));
                    System.out.println("Tweet added to results: " + tweetNumber + ": " + OriginalTweetIndex.get(tweetNumber));
                    System.out.print(word+" "+tweetNumber+" ");
                }
            }
        }
        return results;
    }

    //code modified from http://stackoverflow.com/questions/5176771/sort-hashtable-by-values
    public static ArrayList<String> readResults(HashMap<Integer, Integer> results){
        //create an array out of the results table
        ArrayList<String> printout = new ArrayList<String>();

        ArrayList<Map.Entry<Integer, Integer>> resultList = new ArrayList<Map.Entry<Integer, Integer>>(results.entrySet());
        //sort the array in order of keys (total match frequency for result)
        //collections.sort uses a modified mergesort http://docs.oracle.com/javase/6/docs/api/java/util/Collections.html#sort(java.util.List)
        Collections.sort(resultList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        for(int i = 0; i<10; i++){
            if(!(i<resultList.size())){
                System.out.println("Less than 10 matches to query.");
                break;
            }
            //System.out.println("The "+i+"th best result is tweet number: "+resultList.get(i).getKey()+" with total weight "+resultList.get(i).getValue());
            printout.add(OriginalTweetIndex.get(resultList.get(i).getKey()));
        }
        return printout;
    }

    /*
    public static void main(String [] args){
        initialize();
        System.out.println("Initializing tweet database... \n\treading from file...");

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a query: ");
        String query = scanner.nextLine();

        System.out.println(readResults(matchQuery(query)).toString());
    }
    */

}
