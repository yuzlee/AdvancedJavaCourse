import java.io.*;
import java.nio.file.Paths;
import java.util.*;


/**
 * Created by Rolrence on 2017/5/24.
 *
 * Chinese segmentation based on Hidden Markov Model
 */
public class HMMSegmentation {
    private Matrix<Character, Integer> count = new Matrix<>();
    private HashMap<Character, Integer> cnt = new HashMap<>();
    private ArrayList<Character> words = new ArrayList<>();

    private String strs;

    HashMap<Character, Double> pi = new HashMap<>();
    Matrix<Character, Double> matrixA = new Matrix<>();
    Matrix<Character, Double> matrixB = new Matrix<>();

    private char[] state = { 'B', 'M', 'E', 'S' };

    public HMMSegmentation() {
        this.pi.put('B', 0.5);
        this.pi.put('M', 0.0);
        this.pi.put('E', 0.0);
        this.pi.put('S', 0.5);
    }

    private char findIndex(int i, int lens) {
        if (lens == 1) return 'S';
        if (i == 0) return  'B';
        if (i == lens - 1) return 'E';
        return 'M';
    }

    private void parseData(String[] train_set) {
        StringBuilder sb = new StringBuilder();

        try {
            for (String path: train_set) {
                Scanner data = new Scanner(Paths.get(path));
                while (data.hasNextLine()) {
                    String line = data.nextLine();
                    line = line.trim();
                    String[] grap = line.split("  "); // two spaces
                    for (String scen: grap) {
                        scen = scen.trim();
                        int lens = scen.length();
                        for (int i = 0; i < lens; i++) {
                            char wd = scen.charAt(i);
                            if (!this.words.contains(wd)) {
                                this.words.add(wd);
                            }
                            char st = findIndex(i, lens);
                            this.cnt.putIfAbsent(st, 0);
                            // this.cnt[st] += 1
                            this.cnt.put(st, this.cnt.get(st) + 1);
                            sb.append(st);

                            this.count.set(st, wd, 0);
                            // this.count[st][wd] += 1
                            this.count.opt(st, wd, v -> v + 1);
                        }
                    }
                    sb.append(',');
                }
                data.close();
            }
            this.strs = sb.toString();
        } catch (IOException e) {
            System.out.println("Catch IOException while open file");
        }
    }

    public void train(String[] train_set) {
        System.out.println("Training...");

        this.parseData(train_set);

        // var ast = new HashMap<char, int>()
        Matrix<Character, Integer> ast = new Matrix<>();
        for (int i = 0; i < this.strs.length() - 2; i++) {
            char st1 = this.strs.charAt(i);
            char st2 = this.strs.charAt(i + 1);
            if (st1 == ',' || st2 == ',') continue;
            ast.set(st1, st2, 0);
            ast.opt(st1, st2, v -> v + 1);
        }

        // init
        for (char st1: state) {
            for (char st2 : state) {
                this.matrixA.set(st1, st2, 0.0);
            }
        }
        ast.mapTo(this.matrixA, (keys, v) -> {
            return v / this.cnt.get(keys.key1).doubleValue();
        });

        for (char st: state) {
            for (char wd: this.words) {
                this.matrixB.set(st, wd, 1.0 / this.cnt.get(st).doubleValue());
            }
        }
        this.count.mapTo(this.matrixB, (keys, v) -> {
            return v / this.cnt.get(keys.key1).doubleValue();
        });

        System.out.println("Training completed");
    }

    public void save(String path) throws IOException {
        String pathA = path + "A.matrix";
        String pathB = path + "B.matrix";
        String pathCnt = path + "Cnt.matrix";

        DataOutputStream out = new DataOutputStream(new FileOutputStream(pathA));
        for (Matrix<Character, Double>.DataObject o: this.matrixA.asList()) {
            out.writeChar(o.k1);
            out.writeChar(o.k2);
            out.writeDouble(o.v);
        }
        out.close();

        out = new DataOutputStream(new FileOutputStream(pathB));
        for (Matrix<Character, Double>.DataObject o: this.matrixB.asList()) {
            out.writeChar(o.k1);
            out.writeChar(o.k2);
            out.writeDouble(o.v);
        }
        out.close();

        out = new DataOutputStream(new FileOutputStream(pathCnt));
        for (Character k: this.cnt.keySet()) {
            out.writeChar(k);
            out.writeInt(this.cnt.get(k));
        }
        out.close();

        System.out.println("Saved to" + path);
    }

    public void loadModel(String path) throws IOException {
        String pathA = path + "A.matrix";
        String pathB = path + "B.matrix";
        String pathCnt = path + "Cnt.matrix";

        int data_object_length = 12;

        RandomAccessFile in = new RandomAccessFile(pathA,"r");
        long length = in.length() / data_object_length;
        for (long i = 0; i < length; i++) {
            in.seek(i * data_object_length);
            this.matrixA.reset(in.readChar(), in.readChar(),in.readDouble());
        }
        in.close();
        in = new RandomAccessFile(pathB,"r");
        length = in.length() / data_object_length;
        for (long i = 0; i < length; i++) {
            in.seek(i * data_object_length);
            this.matrixB.reset(in.readChar(), in.readChar(),in.readDouble());
        }
        in.close();

        in = new RandomAccessFile(pathCnt,"r");
        data_object_length = 6;  // 6 = char(1) + int(1)
        length = in.length() / data_object_length;
        for (long i = 0; i < length; i++) {
            in.seek(i * data_object_length);
            this.cnt.put(in.readChar(), in.readInt());
        }
        in.close();

        System.out.println("Loaded from" + path);
    }

    // Viterbi Algorithm, https://en.wikipedia.org/wiki/Viterbi_algorithm
    public String test_sentence(String line) {
        int lens = line.length();
        if (lens < 1) { return ""; }

        GenericMatrix<Integer, Character, Double> fi = new GenericMatrix<>();
        // step 1
        char wd = line.charAt(0);
        for (char st: state) {
            // if null
            this.matrixB.set(st, wd, 1.0 / this.cnt.get(st).doubleValue());

            fi.set(1, st, this.pi.get(st) * this.matrixB.get(st, wd));
        }

        // step 2
        for (int i = 1; i < lens; i++) {
            wd = line.charAt(i);
            for (char st1: state) {
                fi.set(i + 1, st1, 0.0);
                double max_num = 0;
                for (char st2: state) {
                    max_num = Math.max(max_num, fi.get(i, st2) * this.matrixA.get(st2, st1));
                }
                // if null
                this.matrixB.set(st1, wd, 1.0 / this.cnt.get(st1).doubleValue());

                fi.reset(i + 1, st1, max_num * this.matrixB.get(st1, wd));
            }
        }

        // step 3
        ArrayList<Character> links = new ArrayList<>();
        // find max
        char st1 = fi.max(lens, (v, max_val) -> v > max_val);
        links.add(st1);
        for (int i = lens; i > 1; i--) {
            Double max_val = null;
            for (char st: state) {
                double val = fi.get(i - 1, st) * this.matrixA.get(st, st1);
                if (max_val == null) {
                    max_val = val;
                    st1 = st;
                    continue;
                }
                if (val > max_val) {
                    max_val = val;
                    st1 = st;
                }
            }
            links.add(st1);
        }

        Collections.reverse(links);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < links.size(); i++) {
            char st = links.get(i);
            if (st == 'S') {
                sb.append(line.charAt(i) + "  ");
            } else if (st == 'B' || st == 'M') {
                sb.append(line.charAt(i));
            } else if (st == 'E') {
                sb.append(line.charAt(i) + "  ");
            }
        }
        return sb.toString();
    }

    public void test(String test_set) {
        System.out.println("Testing...");

        String filename = "result.utf8";
        StringBuilder sb = new StringBuilder();

        try {
            Scanner test_data = new Scanner(Paths.get(test_set));

            while (test_data.hasNextLine()) {
                String line = test_data.nextLine().trim();
                sb.append(this.test_sentence(line) + '\n');
            }
            test_data.close();

            FileWriter out_data = new FileWriter(filename);
            out_data.write(sb.toString());
            out_data.close();

            System.out.println("Test completed");
        } catch (IOException e) {
            System.out.println("Catch IOException while open " + test_set);
        }
    }
}
