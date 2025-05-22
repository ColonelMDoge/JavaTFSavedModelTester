import com.github.sarxos.webcam.*;
import org.tensorflow.SavedModelBundle;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;

public class OutputGUI extends JFrame implements WebcamMotionListener {
    private static final JavaTeachableMachine jtm = new JavaTeachableMachine();
    private static WebcamMotionDetector detector;
    private static SavedModelBundle savedModelBundle;
    Queue<String> textAreaConsole = new LinkedList<>();
    Webcam webcam;
    WebcamPanel webcamPanel;
    TextAreaOutputStream textAreaOutputStream;
    File modelFolder;
    JButton initializeModel, loadModelDirectory, netronSite;
    JLabel directory, titleLabel, information1, information2;
    JPanel inputPanel, webcamPanelControl;
    JTextField modelInputField, modelOutputField;
    JTextArea output;
    JScrollPane scrollPane;
    Robot robot;

    public OutputGUI() {
        setTitle("Java Tensorflow SavedModel Tester");
        setSize(1000, 600);
        setIconImage(new ImageIcon("src/main/resources/TensorflowIcon.png").getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        titleLabel = new JLabel("Java Tensorflow SavedModel Tester", SwingConstants.LEFT);
        titleLabel.setBorder(new EmptyBorder(5,10,0,10));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.setPreferredSize(new Dimension(300, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loadModelDirectory = new JButton("Set Model Path");
        loadModelDirectory.setPreferredSize(new Dimension(275,25));
        loadModelDirectory.setFocusable(false);
        loadModelDirectory.setBackground(Color.LIGHT_GRAY);

        directory = new JLabel("No directory selected...");
        directory.setPreferredSize(new Dimension(275,25));

        netronSite = new JButton("Click Here To Determine Signatures");
        netronSite.setPreferredSize(new Dimension(275,25));
        netronSite.setFocusable(false);
        netronSite.setBackground(Color.LIGHT_GRAY);

        information1 = new JLabel("Type in input signature of model:");
        modelInputField = new JTextField();
        modelInputField.setPreferredSize(new Dimension(275,25));
        information2 = new JLabel("Type in output signature of model:");
        modelOutputField = new JTextField();
        modelOutputField.setPreferredSize(new Dimension(275, 25));

        initializeModel = new JButton("Initialize Model!");
        initializeModel.setBackground(Color.LIGHT_GRAY);
        initializeModel.setFocusable(false);

        webcam = Webcam.getDefault();

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcamPanelControl = new JPanel();
        webcamPanelControl.setPreferredSize(new Dimension(280,200));
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setPreferredSize(new Dimension(280,200));
        webcamPanel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 3), BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Webcam")));
        webcamPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(true);
        webcamPanelControl.add(webcamPanel);

        inputPanel.add(loadModelDirectory);
        inputPanel.add(directory);
        inputPanel.add(new JPanel() {{setPreferredSize(new Dimension(280, 15));}});
        inputPanel.add(netronSite);
        inputPanel.add(information1);
        inputPanel.add(modelInputField);
        inputPanel.add(information2);
        inputPanel.add(modelOutputField);
        inputPanel.add(new JPanel() {{setPreferredSize(new Dimension(280, 15));}});
        inputPanel.add(initializeModel);
        inputPanel.add(new JPanel() {{setPreferredSize(new Dimension(280, 35));}});
        inputPanel.add(webcamPanelControl);

        output = new JTextArea();
        output.setBackground(Color.LIGHT_GRAY);
        output.setEditable(false);
        output.setFont(new Font("Consolas", Font.BOLD, 15));
        output.setFocusable(false);
        scrollPane = new JScrollPane(output);

        textAreaOutputStream = new TextAreaOutputStream(output);
        System.setOut(new PrintStream(textAreaOutputStream));
        System.setErr(new PrintStream(textAreaOutputStream));

        netronSite.addActionListener(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI("https://netron.app"));
                } catch (IOException | URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        loadModelDirectory.addActionListener(e -> {
            modelFolder = setFolder();
            if (modelFolder == null) return;
            directory.setText(modelFolder.getAbsolutePath());
        });

        initializeModel.addActionListener(e -> {
            output.setText("");
            textAreaConsole.clear();
            if (modelFolder == null) {
                JOptionPane.showMessageDialog(null, "Please set the model directory!");
                return;
            } else if (webcam == null) {
                JOptionPane.showMessageDialog(null, "Webcam is not detected!");
                return;
            } else if (modelInputField.getText().equalsIgnoreCase("")) {
                JOptionPane.showMessageDialog(null, "Please set the input signature!");
                return;
            } else if (modelOutputField.getText().equalsIgnoreCase("")) {
                JOptionPane.showMessageDialog(null, "Please set the output signature!");
                return;
            }
            jtm.setInputOperation(modelInputField.getText());
            jtm.setOutputOperation(modelOutputField.getText());
            jtm.setLabels(modelFolder.getPath().substring(0,modelFolder.getPath().lastIndexOf('\\')) + "\\labels.txt");
            savedModelBundle = SavedModelBundle.load(modelFolder.getPath(), "serve");
            detector = new WebcamMotionDetector(webcam);
            detector.addMotionListener(this);
            detector.setInterval(250);
            detector.start();
        });
        add(inputPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
    }
    @Override
    public void motionDetected(WebcamMotionEvent webcamMotionEvent) {
        BufferedImage img = new BufferedImage(jtm.getImageSize(),jtm.getImageSize(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(webcamMotionEvent.getCurrentImage(), jtm.getImageSize(),0,-jtm.getImageSize(),jtm.getImageSize(), null);
        g2d.dispose();
        float[][][][] inputData = JavaTeachableMachine.prepareDataFromImage(img);
        textAreaConsole.add(jtm.getOutput(savedModelBundle, inputData) + "Model assumes Class: " + jtm.getClassPrediction(savedModelBundle, inputData) + "\n");
        output.setText("");
        for (String text : textAreaConsole) output.append(text);
        if (textAreaConsole.size() > 50) textAreaConsole.poll();
        //performRobotAction(jtm.getClassPrediction(savedModelBundle, inputData));
    }

    public static File setFolder() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException ex) {
            throw new RuntimeException(ex);
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int choice = fileChooser.showOpenDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    // Use this method if you want to set key-binds to your classes!
    public void performRobotAction(String className) {
        switch(className) {
            case "Up" -> {
                robot.keyPress(KeyEvent.VK_UP);
                robot.keyRelease(KeyEvent.VK_UP);
            }
            case "Down" -> {
                robot.keyPress(KeyEvent.VK_DOWN);
                robot.keyRelease(KeyEvent.VK_DOWN);
            }
            case "Left" -> {
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_LEFT);
            }
            case "Right" -> {
                robot.keyPress(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_RIGHT);
            }
        }
    }
}
