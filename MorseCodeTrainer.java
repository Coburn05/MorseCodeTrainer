import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class MorseCodeTrainer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MorseAudioPlayer audioPlayer = new MorseAudioPlayer();
            MorseTrainer trainer = new MorseTrainer();
            new MorseUI(trainer, audioPlayer);
        });
    }
}

class MorseConstants {
    // Timing constants (ms)
    public static final int DIT_DURATION = 100;
    public static final int DAH_DURATION = 300;
    public static final int INTRA_CHAR_PAUSE = 100;
    public static final int CHARACTER_TIMEOUT = 1000;

    // Audio constants
    public static final int SAMPLE_RATE = 44100;
    public static final int FREQUENCY = 600;
    public static final double VOLUME = 0.5;

    // Training constants
    public static final String[] TARGET_WORDS = {
            "SOS", "CODE", "JAVA", "TEST", "LEARN", "MORSE", "TRAINER", "PROGRAM", "CAT", "HAT", "THE", "QUICK",
            "BROWN", "FOX", "JUMPED", "OVER", "THE", "LAZY", "DOG", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "0", "× (error)", "+ (wait)"
    };

    // Morse mappings
    public static final Map<Character, String> CHAR_TO_MORSE = new HashMap<>();
    static {
        CHAR_TO_MORSE.put('A', ".-");
        CHAR_TO_MORSE.put('B', "-...");
        CHAR_TO_MORSE.put('C', "-.-.");
        CHAR_TO_MORSE.put('D', "-..");
        CHAR_TO_MORSE.put('E', ".");
        CHAR_TO_MORSE.put('F', "..-.");
        CHAR_TO_MORSE.put('G', "--.");
        CHAR_TO_MORSE.put('H', "....");
        CHAR_TO_MORSE.put('I', "..");
        CHAR_TO_MORSE.put('J', ".---");
        CHAR_TO_MORSE.put('K', "-.-");
        CHAR_TO_MORSE.put('L', ".-..");
        CHAR_TO_MORSE.put('M', "--");
        CHAR_TO_MORSE.put('N', "-.");
        CHAR_TO_MORSE.put('O', "---");
        CHAR_TO_MORSE.put('P', ".--.");
        CHAR_TO_MORSE.put('Q', "--.-");
        CHAR_TO_MORSE.put('R', ".-.");
        CHAR_TO_MORSE.put('S', "...");
        CHAR_TO_MORSE.put('T', "-");
        CHAR_TO_MORSE.put('U', "..-");
        CHAR_TO_MORSE.put('V', "...-");
        CHAR_TO_MORSE.put('W', ".--");
        CHAR_TO_MORSE.put('X', "-..-");
        CHAR_TO_MORSE.put('Y', "-.--");
        CHAR_TO_MORSE.put('Z', "--..");

        // Numbers 0-9
        CHAR_TO_MORSE.put('0', "-----");
        CHAR_TO_MORSE.put('1', ".----");
        CHAR_TO_MORSE.put('2', "..---");
        CHAR_TO_MORSE.put('3', "...--");
        CHAR_TO_MORSE.put('4', "....-");
        CHAR_TO_MORSE.put('5', ".....");
        CHAR_TO_MORSE.put('6', "-....");
        CHAR_TO_MORSE.put('7', "--...");
        CHAR_TO_MORSE.put('8', "---..");
        CHAR_TO_MORSE.put('9', "----.");

        // Special Characters (ITU Standard)
        CHAR_TO_MORSE.put('.', ".-.-.-");   // Period
        CHAR_TO_MORSE.put(',', "--..--");   // Comma
        CHAR_TO_MORSE.put('?', "..--..");   // Question mark
        CHAR_TO_MORSE.put('\'', ".----.");  // Apostrophe
        CHAR_TO_MORSE.put('!', "-.-.--");   // Exclamation
        CHAR_TO_MORSE.put('/', "-..-.");    // Slash
        CHAR_TO_MORSE.put('(', "-.--.");    // Left parenthesis
        CHAR_TO_MORSE.put(')', "-.--.-");   // Right parenthesis
        CHAR_TO_MORSE.put('&', ".-...");    // Ampersand
        CHAR_TO_MORSE.put(':', "---...");   // Colon
        CHAR_TO_MORSE.put(';', "-.-.-.");   // Semicolon
        CHAR_TO_MORSE.put('=', "-...-");    // Equals
        CHAR_TO_MORSE.put('-', "-....-");   // Hyphen
        CHAR_TO_MORSE.put('_', "..--.-");   // Underscore
        CHAR_TO_MORSE.put('$', "...-..-"); // Dollar sign
        CHAR_TO_MORSE.put('@', ".--.-.");   // At symbol

        // Special Prosigns
        CHAR_TO_MORSE.put('§', "...---..."); // SOS prosign
        CHAR_TO_MORSE.put('+', ".-.-.");     // Wait symbol
        CHAR_TO_MORSE.put('×', "-..-");      // Error symbol
    }
}

class MorseAudioPlayer {
    private final AudioFormat format;
    private final ExecutorService executor;
    private final Map<Character, byte[]> soundCache;

    public MorseAudioPlayer() {
        format = new AudioFormat(MorseConstants.SAMPLE_RATE, 16, 1, true, false);
        executor = Executors.newSingleThreadExecutor();
        soundCache = new HashMap<>();
        cacheSounds();
    }

    private void cacheSounds() {
        soundCache.put('.', generateSound(MorseConstants.DIT_DURATION));
        soundCache.put('-', generateSound(MorseConstants.DAH_DURATION));
    }

    private byte[] generateSound(int duration) {
        int samples = duration * MorseConstants.SAMPLE_RATE / 1000;
        byte[] buffer = new byte[samples * 2];

        for(int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i * MorseConstants.FREQUENCY / MorseConstants.SAMPLE_RATE;
            short value = (short) (Short.MAX_VALUE * MorseConstants.VOLUME * Math.sin(angle));
            buffer[i*2] = (byte) (value & 0xff);
            buffer[i*2+1] = (byte) ((value >> 8) & 0xff);
        }
        return buffer;
    }

    public void playSymbol(char symbol) {
        executor.execute(() -> playSound(soundCache.get(symbol)));
    }

    public void playSequence(String sequence) {
        executor.execute(() -> {
            for(char c : sequence.toCharArray()) {
                playSound(soundCache.get(c));
                try { Thread.sleep(MorseConstants.INTRA_CHAR_PAUSE); }
                catch (InterruptedException e) { break; }
            }
        });
    }

    private void playSound(byte[] sound) {
        try(SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(sound, 0, sound.length);
            line.drain();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}

class MorseTrainer {
    private final Random random = new Random();
    private String targetWord;
    private final StringBuilder currentInput = new StringBuilder();
    private final StringBuilder translated = new StringBuilder();
    private Timer inputTimer;

    public MorseTrainer() {
        setRandomTarget();
        setupTimer();
    }

    private void setupTimer() {
        inputTimer = new Timer(MorseConstants.CHARACTER_TIMEOUT, e -> finalizeCharacter());
        inputTimer.setRepeats(false);
    }

    public void setRandomTarget() {
        targetWord = MorseConstants.TARGET_WORDS[
                random.nextInt(MorseConstants.TARGET_WORDS.length)
                ];
    }

    public void addSymbol(char symbol) {
        currentInput.append(symbol);
        resetTimer();
    }

    public void finalizeCharacter() {
        inputTimer.stop();
        String code = currentInput.toString();
        currentInput.setLength(0);
        translated.append(lookupCode(code));
    }

    public void reset() {
        inputTimer.stop();
        currentInput.setLength(0);
        translated.setLength(0);
        setRandomTarget();
    }

    private void resetTimer() {
        // Fix 6: Use Swing Timer methods
        if (inputTimer.isRunning()) {
            inputTimer.restart();
        } else {
            inputTimer.start();
        }
    }

    private char lookupCode(String code) {
        return MorseConstants.CHAR_TO_MORSE.entrySet().stream()
                .filter(e -> e.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse('?');
    }

    public String getCurrentCode() { return currentInput.toString(); }
    public String getTranslated() { return translated.toString(); }
    public String getTargetWord() { return targetWord; }
}

class MorseUI extends JFrame {
    private final MorseTrainer trainer;
    private final MorseAudioPlayer audioPlayer;
    private final JPanel mainPanel;

    public MorseUI(MorseTrainer trainer, MorseAudioPlayer audioPlayer) {
        this.trainer = trainer;
        this.audioPlayer = audioPlayer;
        mainPanel = createMainPanel();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Morse Code Trainer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);

        setupKeyHandling();
        setSize(600, 400);
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.add(createLabel("Current: ", Font.BOLD, 24));
        panel.add(createLabel("Translated: ", Font.BOLD, 24));
        panel.add(createLabel("Target: ", Font.BOLD, 24));
        return panel;
    }

    private JLabel createLabel(String text, int style, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", style, size));
        return label;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.add(createButton("Show Reference", e -> showReferenceSheet()));
        panel.add(createButton("New Target", e -> resetTraining()));
        return panel;
    }

    private JButton createButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        button.addActionListener(e -> mainPanel.requestFocusInWindow());
        return button;
    }

    private void setupKeyHandling() {
        mainPanel.addKeyListener(new KeyHandler());
        mainPanel.setFocusable(true);
        mainPanel.requestFocusInWindow();
    }

    private void showReferenceSheet() {
        JFrame refFrame = new JFrame("Morse Reference");
        JPanel panel = new JPanel(new GridLayout(0, 3, 5, 5));

        // Create sorted entries
        List<Map.Entry<Character, String>> entries = new ArrayList<>(
                MorseConstants.CHAR_TO_MORSE.entrySet()
        );
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        // Populate panel
        for (Map.Entry<Character, String> entry : entries) {
            JLabel charLabel = new JLabel(String.valueOf(entry.getKey()));
            JLabel codeLabel = new JLabel(entry.getValue());

            JButton playButton = new JButton("Play");
            playButton.addActionListener(e ->
                    audioPlayer.playSequence(entry.getValue())
            );

            panel.add(charLabel);
            panel.add(codeLabel);
            panel.add(playButton);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(400, 600));

        refFrame.add(scrollPane);
        refFrame.pack();
        refFrame.setLocationRelativeTo(this);
        refFrame.setVisible(true);
    }

    private void resetTraining() {
        trainer.reset();
        updateDisplay();
        mainPanel.requestFocusInWindow();
    }

    private void updateDisplay() {
        ((JLabel)mainPanel.getComponent(0)).setText("Current: " + trainer.getCurrentCode());
        ((JLabel)mainPanel.getComponent(1)).setText("Translated: " + trainer.getTranslated());
        ((JLabel)mainPanel.getComponent(2)).setText("Target: " + trainer.getTargetWord());
    }

    private class KeyHandler extends KeyAdapter {
        private boolean spacePressed;
        private boolean shiftPressed;

        @Override
        public void keyPressed(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> handleSpace();
                case KeyEvent.VK_SHIFT -> handleShift(e);
                case KeyEvent.VK_ENTER -> finalizeCharacter();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> spacePressed = false;
                case KeyEvent.VK_SHIFT -> shiftPressed = false;
            }
        }

        private void handleSpace() {
            if (!spacePressed) {
                spacePressed = true;
                audioPlayer.playSymbol('.');
                trainer.addSymbol('.');
                updateDisplay();
            }
        }

        private void handleShift(KeyEvent e) {
            if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT && !shiftPressed) {
                shiftPressed = true;
                audioPlayer.playSymbol('-');
                trainer.addSymbol('-');
                updateDisplay();
            }
        }

        private void finalizeCharacter() {
            trainer.finalizeCharacter();
            updateDisplay();
        }
    }
}