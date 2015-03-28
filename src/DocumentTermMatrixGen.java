
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.math3.linear.*;

public class DocumentTermMatrixGen {

    public static Boolean useSVD = false;

    public static int k = 5;
    public static final int NUMBER_OF_RESULTS = 10; // must be less than the total number of documents
    static ArrayList<String> terms;
    static double[][] documentTermMatrix;

    static String[] documents;

    static RealMatrix A, U, S, V, Sk;

    public static Boolean initialized = false;

    public static void initialize(String file) throws Exception {
        if (!initialized) {
            initialized = true;
            System.out.println("Reading input from file: " + file.toString());
            System.out.print("Lines counted: ");

            int numDocs = countLines(file);
            documents = new String[numDocs + 1];

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int d_it = 0;
            while ((line = br.readLine()) != null) {
                //System.out.print((d_it+1)+", ");
                documents[d_it++] = line;
            }
            br.close();

            k = numDocs / 2; //HARD-CODED K!!
            terms = new ArrayList<String>();

            //construct database
            indexWords();
            A = constructDocumentTermMatrix();
            SingularValueDecomposition SVD = new SingularValueDecomposition(A);
            U = (RealMatrix) SVD.getU();
            S = (RealMatrix) SVD.getS();
            V = (RealMatrix) SVD.getV();
            V = V.transpose();

            Sk = firstKterms(S, k);

            //K IS SUBJECT TO CHANGE BASED ON THE LEVEL OF PRECISION WE WANT IN SEARCHES
        } else {
            return;
        }
    }

    /*
     public static void main(String args[]) throws Exception{
     initialize();
     String query = "";
     //answer queries! hooray!
     Scanner scanner = new Scanner(System.in);
     while(!query.toLowerCase().equals("q")){
     System.out.println("Enter a query to search for, or \"q\" to quit.");
     query = scanner.nextLine();
     searchQuery(query);
     }
     }
     */
    static ArrayList<String> searchQuery(String query) {
        ArrayList<String> results = new ArrayList<String>();
        System.out.println("Searching for: " + query);
        ArrayRealVector q = constructQueryVector(query);
        RealMatrix Uk = U.multiply(Sk);
        Uk = (RealMatrix) Uk.getSubMatrix(0, Uk.getRowDimension() - 1, 0, k - 1);
        //dot product query vector with each document vector in Uk
        ArrayRealVector dotProductVector = new ArrayRealVector(Uk.getRowDimension());
        for (int i = 0; i < dotProductVector.getDimension(); i++) {
            double dot = q.dotProduct(Uk.getRowVector(i));
            dotProductVector.setEntry(i, dot);
        }
        //index of X highest values in dotProductVector = index of most Xth most relevant tweet
        for (int i = 0; i < NUMBER_OF_RESULTS; i++) {
            int resultIndex = dotProductVector.getMaxIndex();
            System.out.println("The " + i + "th best result is: tweet #" + resultIndex + ": " + documents[resultIndex]);
            results.add(documents[resultIndex]);
            dotProductVector.setEntry(resultIndex, -1);
        }
        return results;
    }

    static ArrayRealVector constructQueryVector(String query) {
        //find index of query words in word list
        String[] queryWords = query.split(" ");
        System.out.print("Dimensions of Sk:" + Sk.getRowDimension() + "," + Sk.getColumnDimension() + "\n");
        System.out.print("Dimensions of V:" + V.getRowDimension() + "," + V.getColumnDimension() + "\n");
        RealMatrix Vk = Sk.multiply(V);
        Vk = (RealMatrix) Vk.getSubMatrix(0, k - 1, 0, Vk.getColumnDimension() - 1);
        ArrayRealVector q = new ArrayRealVector(k, 0);
        for (int i = 0; i < queryWords.length; i++) {
            if (!terms.contains(queryWords[i])) {
                System.out.println(queryWords[i] + " not found in any tweets, continuing...");
                continue;
            }
            int indexOfTerm = terms.indexOf(queryWords[i]);
            q = q.add(Vk.getColumnVector(indexOfTerm));
        }
        q.mapDivide(queryWords.length);
        return q;
    }

    static void indexWords() {
        for (int i = 0; i < documents.length; i++) {
            documents[i] = documents[i].replace(",", "");
            documents[i] = documents[i].replace("/", " ");
            documents[i] = documents[i].replace(".", "");
            documents[i] = documents[i].replace("\'", "");
            documents[i] = documents[i].replace("\"", "");
            documents[i] = documents[i].replace("\\", " ");
            documents[i] = documents[i].replace("-", " ");
            documents[i] = documents[i].replace("(s)", "");
            documents[i] = documents[i].replace("$", "");
            documents[i] = documents[i].replace("(", "");
            documents[i] = documents[i].replace(")", "");
            documents[i] = documents[i].replace("[", "");
            documents[i] = documents[i].replace("]", "");
        }

        for (String document : documents) {
            String[] words = document.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (!terms.contains(words[i])) {
                    terms.add(words[i]);
                }
            }
        }
    }

    static RealMatrix constructDocumentTermMatrix() {
        int frequency;
        documentTermMatrix = new double[terms.size()][documents.length];
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            for (int j = 0; j < documents.length; j++) {
                frequency = 0;
                String[] document = documents[j].split("\\s+");
                for (int k = 0; k < document.length; k++) {
                    if (document[k].equals(term)) {
                        frequency++;
                    }
                }
                documentTermMatrix[i][j] = frequency;
            }
        }
        //TODO: tranpose may not be necessary
        return new Array2DRowRealMatrix(documentTermMatrix).transpose();
    }

    //used to zero out K+1 terms from S in the SVD decomposition
    public static RealMatrix firstKterms(RealMatrix A, int k) {
        System.out.println(A + "Foo");
        RealMatrix S = (RealMatrix) A.copy();
        int max = Math.min(A.getRowDimension(), A.getColumnDimension());

        //iterate through both rows/columns (on a diagonal matrix) zeroing out everything after the kth entry,[k-1][k-1]
        for (int i = k; i < max; i++) {
            S.setEntry(i, i, 0);
        }
        return S;
    }

    //taken from http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
    public static int countLines(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }
}
