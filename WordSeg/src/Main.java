import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String[] train_set = {
                "data/train/as_training.utf8",
                "data/train/cityu_training.utf8",
                "data/train/msr_training.utf8",
                "data/train/pku_training.utf8"
        };
        HMMSegmentation hmm = new HMMSegmentation();

        // hmm.train(train_set);

        try {
            // hmm.save("./model/");
            hmm.loadModel("./model/");
        } catch (IOException e) {
            System.out.println("IOException");
        }

        hmm.test("data/test/pku_test.utf8");
//        Scanner sc = new Scanner(System.in);
//        while (true) {
//            System.out.println("Input next line: ");
//            String sentence = sc.nextLine().trim();
//            String result = hmm.test_sentence(sentence);
//            System.out.println(result);
//        }

    }
}
