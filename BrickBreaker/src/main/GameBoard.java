import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Objects;
import java.util.Scanner;

public class GameBoard extends JPanel {

    private Timer timer;
    private String message = "Game Over!";
    private Ball ball;
    public Racket racket;
    public Brick[] bricks;
    public int racketType = 0;
    private boolean inGame = true;
    //levels work
    public static final int EASY = 5;
    public static final int MEDIUM = 10;
    public static final int HARD = 15;
    //private BrickBreaker mainApp;
    private int ballSpeed = MEDIUM; // Set default to MEDIUM
    int score = 0;
    double speed = 1;
    String speedLevel = "x1";
    JButton pauseButton = new JButton("Pause");
    JButton resumeButton = new JButton("Resume");
    JButton restartButton = new JButton("Restart");
    JButton returnButton = new JButton("Return");
    private BrickBreaker mainApp; // Reference to the main application

    public GameBoard(BrickBreaker mainApp) throws IOException {
        this.mainApp = mainApp;
        initBoard();
    }

    boolean restartClicked = false;
    int keySelect = 0;

    public int livesLeft;

    public GameBoard() throws IOException {
        initBoard();
    }

    private void initBoard() throws IOException {

        PauseHandler settingHandler = new PauseHandler();
        ResumeHandler resumeHandler = new ResumeHandler();
        RestartHandler restartHandler = new RestartHandler();
        ReturnHandler returnHandler = new ReturnHandler(); // Create a handler for the Return button
        setFocusable(true);
        setPreferredSize(new Dimension(Configurations.WIDTH, Configurations.HEIGHT));

        // Read from BackGroundColor.txt to get background color
        FileReader fr = new FileReader("BackGroundColor.txt");
        BufferedReader br = new BufferedReader(fr);
        String color = br.readLine();

        // Read Color object String and convert to Color object
        final Scanner scan = new Scanner(color);
        scan.useDelimiter("(r|\\,g|\\,b)=|\\]").next(); // Use proper delimiter and ignore first part (which is the class name)
        final int r, g, b;
        // Verify RGB Values
        System.out.println(r = scan.nextInt());
        System.out.println(g = scan.nextInt());
        System.out.println(b = scan.nextInt());

        Color c = new Color(r, g, b);
        setBackground(c);

        System.out.println(color);
        fr.close();
        br.close();

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridLayout(0, 4)); // Adjust the grid layout to accommodate four buttons
        buttonPane.setPreferredSize(new Dimension(330, 30)); // Adjust size if necessary
        JPanel blank = new JPanel();
        blank.setVisible(false);
        buttonPane.add(pauseButton);
        buttonPane.add(restartButton);
        buttonPane.add(returnButton); // Add the Return button

        add(buttonPane);

        pauseButton.addActionListener(settingHandler);
        resumeButton.addActionListener(resumeHandler);
        restartButton.addActionListener(restartHandler);
        returnButton.addActionListener(returnHandler); // Add action listener for the Return button
        pauseButton.setFocusable(false);
        restartButton.setFocusable(false);
        resumeButton.setFocusable(false);
        returnButton.setFocusable(false); // Set the Return button as not focusable

        addKeyListener(new TAdapter());
        setFocusable(true);
        setPreferredSize(new Dimension(Configurations.WIDTH, Configurations.HEIGHT));
        gameInit();
    }

    // Method to set the ball speed based on the selected difficulty
    public void setBallSpeed(int ballSpeed) {      // speed of levell
        this.ballSpeed = ballSpeed;
        switch (ballSpeed) {
            case EASY:
                speed = 1;
                speedLevel = "x1";
                break;
            case MEDIUM:
                speed = 1.5;
                speedLevel = "x1.5";
                break;
            case HARD:
                speed = 2;
                speedLevel = "x2";
                break;
        }
    }

    private void gameInit() throws IOException {
        bricks = new Brick[Configurations.N_OF_BRICKS];

        ball = new Ball();
        racket = new Racket(racketType);

        int k = 0;

        livesLeft = 3;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                bricks[k] = new Brick(j * 40 + 30, i * 10 + 50);
                k++;
            }
        }

        timer = new Timer(Configurations.PERIOD, new GameCycle());
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        var g2d = (Graphics2D) g;
        Image im = new ImageIcon(GameBoard.class.getResource("/images/lava.gif")).getImage();
        g.drawImage(im, 0, 365, null);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        if (inGame) {
            try {
                drawObjects(g2d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                gameFinished(g2d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawObjects(Graphics2D g2d) throws IOException {
        var font = new Font("Verdana", Font.BOLD, 15);
        FontMetrics fontMetrics = this.getFontMetrics(font);

        g2d.setColor(Color.black);
        g2d.setFont(font);
        // Draw current score at bottom of panel
        g2d.drawString("Score: " + score, 120, 390);
        g2d.drawString("Lives: " + livesLeft, 230, 390);

        if (restartClicked) {
            racketType = 0;
            speed = 1;
            speedLevel = "x1";
            restartClicked = false;
            racket = new Racket(racketType);
        }

        g2d.drawString("Speed: " + speedLevel, 10, 390);
        g2d.drawImage(ball.getImage(), (int) ball.getX(), (int) ball.getY(),
                ball.getImageWidth(), ball.getImageHeight(), this);
        g2d.drawImage(racket.getImage(), (int) racket.getX(), (int) racket.getY(),
                racket.getImageWidth(), racket.getImageHeight(), this);
        for (int i = 0; i < Configurations.N_OF_BRICKS; i++) {
            if (!bricks[i].isDestroyed()) {
                g2d.drawImage(bricks[i].getImage(), (int) bricks[i].getX(),
                        (int) bricks[i].getY(), bricks[i].getImageWidth(),
                        bricks[i].getImageHeight(), this);
            }
        }
    }

    private void gameFinished(Graphics2D g2d) throws IOException {
        var font = new Font("Verdana", Font.BOLD, 18);
        FontMetrics fontMetrics = this.getFontMetrics(font);

        g2d.setColor(Color.BLACK);
        g2d.setFont(font);
        g2d.drawString(message,
                (Configurations.WIDTH - fontMetrics.stringWidth(message)) / 2,
                Configurations.WIDTH / 2);

        FileWriter out = new FileWriter("ScoreList.txt", true);
        BufferedWriter bw = new BufferedWriter(out);

        if (score != 0) {
            bw.write(Integer.toString(score));
            bw.newLine();
        }

        bw.close();
        out.close();
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
            racket.keyReleased(e, keySelect);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            racket.keyPressed(e, keySelect);
        }
    }

    private class GameCycle implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                doGameCycle();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void doGameCycle() throws IOException {
        ball.move();
        racket.move();
        checkCollision();
        repaint();
    }

    private void stopGame() throws IOException {
        livesLeft--;
        racketType = 0;
        if (livesLeft == 0) {
            inGame = false;
            timer.stop();
        }

        ball = new Ball();
        racket = new Racket(racketType);

        timer.stop();
        timer = new Timer(Configurations.PERIOD, new GameCycle());
        timer.start();
    }

    // pause game once click on pause button
    private class PauseHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            pauseGame();
        }
    }

    // method to pause game
    private void pauseGame() {
        Container parent = pauseButton.getParent();
        parent.add(resumeButton, 0, 0);
        parent.remove(pauseButton);
        parent.revalidate();
        parent.repaint();
        timer.stop();
    }

    private class ResumeHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            resumeGame();
        }
    }

    private void resumeGame() {
        Container parent = resumeButton.getParent();
        parent.add(pauseButton, 0, 0);
        parent.remove(resumeButton);
        parent.revalidate();
        parent.repaint();
        timer.stop();
        timer = new Timer(Configurations.PERIOD, new GameCycle());
        timer.start();
    }

    private class RestartHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                speed = 1;
                speedLevel = "x1";
                restartClicked = true;
                inGame = true;
                timer.stop();
                gameInit();
            } catch (IOException er) {
                er.printStackTrace();
            }
        }
    }

    private class ReturnHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // Assuming you have a method in your main frame to switch to the MenuScreen
            Container parent = GameBoard.this.getParent();
            CardLayout cardLayout = (CardLayout) parent.getLayout();
            cardLayout.show(parent, "MenuScreen");
        }
    }

    public void checkCollision() throws IOException {
        if (ball.getRect().getMaxY() > Configurations.BOTTOM_EDGE) {
            stopGame();
        }

        // game over when the ball hit the top edge
        if (ball.getRect().getMaxY() < Configurations.TOP_EDGE) {
            stopGame();
        }

        // Speeds up the ball every time
        // 5 bricks are destroyed until the 15th destroyed brick
        // if (score >= 5 && score < 10) {
        //     speed = ballSpeed * 1.2 / 10; // Modify speed based on selected difficulty
        //     speedLevel = "x1.2";
        // } else if (score >= 10 && score < 15) {
        //     speed = ballSpeed * 1.5 / 10; // Modify speed based on selected difficulty
        //     speedLevel = "x1.5";
        // } else if (score >= 15) {
        //     speed = ballSpeed * 2 / 10; // Modify speed based on selected difficulty
        //     speedLevel = "x2";
        // }

        for (int i = 0, j = 0; i < Configurations.N_OF_BRICKS; i++) {
            if (bricks[i].isDestroyed()) {
                j++;
            }
            // added score keeper
            score = j;
            if (j == Configurations.N_OF_BRICKS) {
                message = "Victory";
                stopGame();
            }
        }

        if ((ball.getRect()).intersects(racket.getRect())) {
            int paddleLPos = (int) racket.getRect().getMinX();
            int ballLPos = (int) ball.getRect().getMinX();

            int first = paddleLPos + 8;
            int second = paddleLPos + 16;
            int third = paddleLPos + 24;
            int fourth = paddleLPos + 32;

            if (ballLPos < first) {
                ball.setXDir(-speed);
                ball.setYDir(-speed);
            }

            if (ballLPos >= first && ballLPos < second) {
                ball.setXDir(-speed);
                ball.setYDir(-speed * ball.getYDir());
            }

            if (ballLPos >= second && ballLPos < third) {
                ball.setXDir(0);
                ball.setYDir(-speed);
            }

            if (ballLPos >= third && ballLPos < fourth) {
                ball.setXDir(speed);
                ball.setYDir(-speed * ball.getYDir());
            }

            if (ballLPos > fourth) {
                ball.setXDir(speed);
                ball.setYDir(-speed);
            }
        }
        for (int i = 0; i < Configurations.N_OF_BRICKS; i++) {
            if ((ball.getRect()).intersects(bricks[i].getRect())) {
                int ballLeft = (int) ball.getRect().getMinX();
                int ballHeight = (int) ball.getRect().getHeight();
                int ballWidth = (int) ball.getRect().getWidth();
                int ballTop = (int) ball.getRect().getMinY();

                var pointRight = new Point(ballLeft + ballWidth + 1, ballTop);
                var pointLeft = new Point(ballLeft - 1, ballTop);
                var pointTop = new Point(ballLeft, ballTop - 1);
                var pointBottom = new Point(ballLeft, ballTop + ballHeight + 1);

                if (!bricks[i].isDestroyed()) {
                    if (bricks[i].getRect().contains(pointRight)) {
                        ball.setXDir(-speed);
                    } else if (bricks[i].getRect().contains(pointLeft)) {
                        ball.setXDir(speed);
                    }

                    if (bricks[i].getRect().contains(pointTop)) {
                        ball.setYDir(speed);
                    } else if (bricks[i].getRect().contains(pointBottom)) {
                        ball.setYDir(-speed);
                    }
                    bricks[i].doDamage();
                }
            }
        }
    }
}
