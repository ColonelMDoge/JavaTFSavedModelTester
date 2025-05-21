import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class JavaTeachableMachine {
    private final HashMap<Integer, String> labels = new HashMap<>();
    private static final int TM_IMG_SIZE = 224;
    private String INPUT_OPERATION = "";
    private String OUTPUT_OPERATION = "";

    public void setInputOperation(String input) {
        INPUT_OPERATION = input;
    }
    public void setOutputOperation(String output) {
        OUTPUT_OPERATION = output;
    }
    public void setLabels(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;
            while((line = br.readLine()) != null) {
                labels.put(Integer.valueOf(line.split(" ")[0]), line.split(" ")[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static float[][][][] prepareDataFromImage(BufferedImage inputImage) {
        BufferedImage resizedImage = new BufferedImage(TM_IMG_SIZE, TM_IMG_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(inputImage, 0,0, TM_IMG_SIZE, TM_IMG_SIZE, null);
        graphics2D.dispose();

        float[][][][] inputData = new float[1][TM_IMG_SIZE][TM_IMG_SIZE][3];
        for (int i = 0; i < TM_IMG_SIZE; i++) {
            for (int j = 0; j < TM_IMG_SIZE; j++) {
                int rgb = resizedImage.getRGB(j, i);
                inputData[0][i][j][0] =  ((rgb >> 16) & 0xFF) / 255.0f;
                inputData[0][i][j][1] =  ((rgb >> 8) & 0xFF) / 255.0f;
                inputData[0][i][j][2] =  (rgb & 0xFF) / 255.0f;
            }
        }
        return inputData;
    }
    public String getOutput(SavedModelBundle savedModelBundle, float[][][][] inputData) {
        try (Tensor<Float> input = Tensor.create(inputData, Float.class)) {
            Tensor<Float> output = savedModelBundle.session()
                    .runner()
                    .feed(INPUT_OPERATION, input)
                    .fetch(OUTPUT_OPERATION)
                    .run()
                    .getFirst().expect(Float.class);
            float[][] outputData = new float[1][5];
            output.copyTo(outputData);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < outputData[0].length; i++) {
                builder.append(String.format("Class - %s: %.4f, ", labels.get(i), outputData[0][i]));
            }
            return builder.toString();
        }
    }
    public String getClassPrediction(SavedModelBundle savedModelBundle, float[][][][] inputData) {
        try (Tensor<Float> input = Tensor.create(inputData, Float.class)) {
            Tensor<Float> output = savedModelBundle.session()
                    .runner()
                    .feed(INPUT_OPERATION, input)
                    .fetch(OUTPUT_OPERATION)
                    .run()
                    .getFirst().expect(Float.class);
            float[][] outputData = new float[1][5];
            output.copyTo(outputData);
            TreeMap<Float, String> sorted = new TreeMap<>();
            for (int i = 0; i < outputData[0].length; i++) {
                sorted.put(outputData[0][i], labels.get(i));
            }
            return sorted.get(sorted.lastKey());
        }
    }
    public int getImageSize() {
        return TM_IMG_SIZE;
    }
}
