import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * QuizApplet.java
 * =============================================================================
 * CSE4019 — Advanced Java Programming | Unit 5
 *
 * Self-contained multiplayer quiz Applet.
 * No server needed — players sit at the same machine and pass turns.
 *
 * Syllabus coverage (Unit 5):
 *   Applet lifecycle   — init(), start(), stop(), destroy()
 *   Applet HTML tag    — see quiz.html  (<applet code="QuizApplet.class">)
 *   Animation          — TimerBar inner class (Thread + repaint loop)
 *   JAR files          — jar cvf QuizApplet.jar *.class
 *   Multimedia / AWT   — Graphics, Color, Font, custom painting
 *   Swing GUI          — JPanel, JButton, JLabel, CardLayout, GridBagLayout
 *
 * Run with AppletViewer (JDK 8):
 *   javac QuizApplet.java
 *   appletviewer quiz.html
 *
 * OR as standalone application:
 *   java QuizApplet
 * =============================================================================
 */
@SuppressWarnings("deprecation")
public class QuizApplet extends Applet implements Runnable {

    // =========================================================================
    // COLOURS
    // =========================================================================
    private static final Color C_BG     = new Color(10,  14,  26);
    private static final Color C_CARD   = new Color(20,  26,  46);
    private static final Color C_CARD2  = new Color(28,  36,  60);
    private static final Color C_BLUE   = new Color(70, 120, 255);
    private static final Color C_CYAN   = new Color(0,  200, 210);
    private static final Color C_GREEN  = new Color(46, 204, 140);
    private static final Color C_RED    = new Color(240,  70,  70);
    private static final Color C_GOLD   = new Color(250, 185,  30);
    private static final Color C_ORANGE = new Color(250, 130,  40);
    private static final Color C_PURPLE = new Color(170,  80, 255);
    private static final Color C_TEXT   = new Color(225, 232, 255);
    private static final Color C_DIM    = new Color(110, 128, 165);
    private static final Color C_BORDER = new Color(40,  52,  88);

    private static final Color[] OPT_COLORS = { C_BLUE, C_GREEN, C_ORANGE, C_PURPLE };

    // =========================================================================
    // FONTS
    // =========================================================================
    private static final Font F_HUGE  = new Font("SansSerif", Font.BOLD,  28);
    private static final Font F_TITLE = new Font("SansSerif", Font.BOLD,  20);
    private static final Font F_BIG   = new Font("SansSerif", Font.BOLD,  17);
    private static final Font F_BODY  = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font F_BOLD  = new Font("SansSerif", Font.BOLD,  14);
    private static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_MONO  = new Font("Monospaced",Font.BOLD,  13);
    private static final Font F_OPT   = new Font("SansSerif", Font.BOLD,  13);

    // =========================================================================
    // QUESTIONS  (10 questions — Java + general knowledge)
    // =========================================================================
    private static final String[][] QUESTIONS = {
            {"What does JVM stand for?",
                    "Java Variable Method", "Java Virtual Machine",
                    "Just Variable Memory", "Java Verified Module", "B"},
            {"Which keyword is used to create an object in Java?",
                    "create", "build", "new", "make", "C"},
            {"What is the size of an int in Java?",
                    "2 bytes", "4 bytes", "8 bytes", "Depends on OS", "B"},
            {"Which of these is NOT a Java primitive type?",
                    "int", "char", "String", "boolean", "C"},
            {"What does 'static' mean in Java?",
                    "Cannot be changed", "Belongs to class, not instance",
                    "Always public", "Runs at startup", "B"},
            {"Which collection allows duplicate elements?",
                    "Set", "Map", "List", "HashSet", "C"},
            {"What is the output of: System.out.println(10 / 3)?",
                    "3.33", "3", "4", "Error", "B"},
            {"Which access modifier is most restrictive?",
                    "public", "protected", "default", "private", "D"},
            {"What is polymorphism?",
                    "One class, many objects", "One name, many forms",
                    "Multiple inheritance", "None of the above", "B"},
            {"Which exception is thrown on null pointer access?",
                    "NullPointerException", "IndexOutOfBoundsException",
                    "IllegalArgumentException", "ClassCastException", "A"}
    };

    private static final int TOTAL_Q    = QUESTIONS.length;
    private static final int TIMER_SECS = 15;

    // =========================================================================
    // SCREEN IDs
    // =========================================================================
    private static final String S_NAMES    = "NAMES";
    private static final String S_READY    = "READY";
    private static final String S_QUESTION = "QUESTION";
    private static final String S_RESULT   = "RESULT";
    private static final String S_FINAL    = "FINAL";

    // =========================================================================
    // GAME STATE
    // =========================================================================
    private java.util.List<String> players  = new ArrayList<String>();
    private int[]   scores;
    private boolean[] answered;
    private char[]  chosenOpt;
    private int     currentQ   = 0;
    private int     turnIndex  = 0;
    private boolean timerExpired = false;

    // =========================================================================
    // UI
    // =========================================================================
    private JPanel     root;
    private CardLayout cardLayout;

    // Name screen
    private JPanel     playerListPanel;
    private JTextField nameInput;
    private JLabel     nameStatus;
    private JButton    btnAddPlayer;
    private JButton    btnGoQuiz;

    // Ready screen
    private JLabel readyLabel;

    // Question screen
    private JLabel    qHeading;
    private JLabel    qNumLabel;
    private JLabel    qTextLabel;
    private JButton[] optBtns = new JButton[4];
    private TimerBar  timerBar;
    private JLabel    waitLabel;

    // Result & final panels (rebuilt dynamically)
    private JPanel resultPanel;
    private JPanel finalPanel;

    // =========================================================================
    // APPLET LIFECYCLE
    // =========================================================================

    @Override
    public void init() {
        setBackground(C_BG);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        root = makePanel(cardLayout);

        root.add(buildNamesScreen(),    S_NAMES);
        root.add(buildReadyScreen(),    S_READY);
        root.add(buildQuestionScreen(), S_QUESTION);
        root.add(buildResultScreen(),   S_RESULT);
        root.add(buildFinalScreen(),    S_FINAL);

        add(root, BorderLayout.CENTER);
    }

    @Override
    public void start() {
        cardLayout.show(root, S_NAMES);
    }

    @Override
    public void stop() {
        if (timerBar != null) timerBar.stopAnim();
    }

    @Override
    public void destroy() {
        if (timerBar != null) timerBar.stopAnim();
    }

    // =========================================================================
    // SCREEN: NAME ENTRY
    // =========================================================================
    private JPanel buildNamesScreen() {
        JPanel outer = makePanel(new BorderLayout(0, 0));
        outer.setBorder(BorderFactory.createEmptyBorder(28, 48, 28, 48));

        // Title
        JLabel title = makeLabel("QUIZ COMPETITION", F_HUGE, C_CYAN);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = makeLabel("Add participants, then click Go to Quiz", F_SMALL, C_DIM);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel top = makePanel(new GridLayout(2, 1, 0, 4));
        top.add(title); top.add(sub);
        outer.add(top, BorderLayout.NORTH);

        // Center
        JPanel center = makePanel(new BorderLayout(0, 12));
        center.setBorder(BorderFactory.createEmptyBorder(18, 0, 8, 0));

        // Input row
        JPanel inputRow = makePanel(new BorderLayout(10, 0));
        nameInput = new JTextField();
        styleField(nameInput);
        btnAddPlayer = makeBtn("+ Add Player", C_BLUE);
        inputRow.add(nameInput,    BorderLayout.CENTER);
        inputRow.add(btnAddPlayer, BorderLayout.EAST);
        center.add(inputRow, BorderLayout.NORTH);

        // Player list
        playerListPanel = makePanel(new GridLayout(0, 1, 0, 5));
        JScrollPane scroll = new JScrollPane(playerListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        center.add(scroll, BorderLayout.CENTER);

        nameStatus = makeLabel("Add at least 1 player to start.", F_SMALL, C_DIM);
        nameStatus.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(nameStatus, BorderLayout.SOUTH);

        outer.add(center, BorderLayout.CENTER);

        // Go button
        btnGoQuiz = makeBtn("  GO TO QUIZ  \u2192", C_GREEN);
        btnGoQuiz.setFont(F_BIG);
        btnGoQuiz.setEnabled(false);
        JPanel bottom = makePanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        bottom.add(btnGoQuiz);
        outer.add(bottom, BorderLayout.SOUTH);

        // Listeners
        btnAddPlayer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doAddPlayer(); }
        });
        nameInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doAddPlayer(); }
        });
        btnGoQuiz.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doStartGame(); }
        });

        return outer;
    }

    // =========================================================================
    // SCREEN: READY SPLASH
    // =========================================================================
    private JPanel buildReadyScreen() {
        JPanel p = makePanel(new GridBagLayout());
        readyLabel = makeLabel("Get Ready!", F_HUGE, C_GOLD);
        readyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(readyLabel);
        return p;
    }

    // =========================================================================
    // SCREEN: QUESTION
    // =========================================================================
    private JPanel buildQuestionScreen() {
        JPanel outer = makePanel(new BorderLayout(0, 10));
        outer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Top bar
        JPanel topBar = makePanel(new BorderLayout());
        qHeading  = makeLabel("Chance of Player", F_TITLE, C_GOLD);
        qNumLabel = makeLabel("Question 1 of 10", F_SMALL, C_DIM);
        qNumLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topBar.add(qHeading,  BorderLayout.WEST);
        topBar.add(qNumLabel, BorderLayout.EAST);
        outer.add(topBar, BorderLayout.NORTH);

        // Center: question card + options
        JPanel center = makePanel(new BorderLayout(0, 12));

        JPanel qCard = new JPanel(new BorderLayout());
        qCard.setBackground(C_CARD);
        qCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        qTextLabel = makeLabel("", F_BIG, C_TEXT);
        qCard.add(qTextLabel, BorderLayout.CENTER);
        center.add(qCard, BorderLayout.NORTH);

        JPanel optsGrid = makePanel(new GridLayout(2, 2, 10, 10));
        String[] keys = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            final String key = keys[i];
            optBtns[i] = makeOptBtn(key, OPT_COLORS[i]);
            optBtns[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { onOptionChosen(key); }
            });
            optsGrid.add(optBtns[i]);
        }
        center.add(optsGrid, BorderLayout.CENTER);
        outer.add(center, BorderLayout.CENTER);

        // Bottom: timer + wait label
        JPanel bottom = makePanel(new BorderLayout(0, 4));
        timerBar  = new TimerBar();
        timerBar.setPreferredSize(new Dimension(0, 28));
        waitLabel = makeLabel("", F_SMALL, C_DIM);
        waitLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(timerBar,  BorderLayout.CENTER);
        bottom.add(waitLabel, BorderLayout.SOUTH);
        outer.add(bottom, BorderLayout.SOUTH);

        return outer;
    }

    // =========================================================================
    // SCREEN: ROUND RESULT  (empty shell — filled dynamically)
    // =========================================================================
    private JPanel buildResultScreen() {
        resultPanel = makePanel(new BorderLayout());
        return resultPanel;
    }

    // =========================================================================
    // SCREEN: FINAL  (empty shell — filled dynamically)
    // =========================================================================
    private JPanel buildFinalScreen() {
        finalPanel = makePanel(new BorderLayout());
        return finalPanel;
    }

    // =========================================================================
    // LOGIC: ADD PLAYER
    // =========================================================================
    private void doAddPlayer() {
        String name = nameInput.getText().trim();
        if (name.isEmpty()) {
            nameStatus.setText("Please type a name first!");
            nameStatus.setForeground(C_RED);
            return;
        }
        if (players.contains(name)) {
            nameStatus.setText("'" + name + "' is already in the list!");
            nameStatus.setForeground(C_RED);
            return;
        }
        players.add(name);
        nameInput.setText("");
        nameInput.requestFocus();
        refreshPlayerList();
        nameStatus.setText(players.size() + " player(s) added. Add more or click Go to Quiz.");
        nameStatus.setForeground(C_GREEN);
        btnGoQuiz.setEnabled(true);
    }

    private void refreshPlayerList() {
        playerListPanel.removeAll();
        for (int i = 0; i < players.size(); i++) {
            final int idx = i;
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(C_CARD2);
            row.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 8));

            JLabel nameLbl = makeLabel((i + 1) + ".   " + players.get(i), F_BOLD, C_TEXT);

            JButton rm = new JButton("x");
            rm.setFont(F_SMALL);
            rm.setForeground(C_DIM);
            rm.setBackground(C_CARD2);
            rm.setBorderPainted(false);
            rm.setFocusPainted(false);
            rm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            rm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    players.remove(idx);
                    refreshPlayerList();
                    if (players.isEmpty()) {
                        btnGoQuiz.setEnabled(false);
                        nameStatus.setText("Add at least 1 player to start.");
                        nameStatus.setForeground(C_DIM);
                    } else {
                        nameStatus.setText(players.size() + " player(s) added.");
                        nameStatus.setForeground(C_GREEN);
                    }
                }
            });
            row.add(nameLbl, BorderLayout.CENTER);
            row.add(rm,      BorderLayout.EAST);
            playerListPanel.add(row);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    // =========================================================================
    // LOGIC: START GAME
    // =========================================================================
    private void doStartGame() {
        int n     = players.size();
        scores    = new int[n];
        answered  = new boolean[n];
        chosenOpt = new char[n];
        currentQ  = 0;
        turnIndex = 0;
        Arrays.fill(answered,  false);
        Arrays.fill(chosenOpt, '-');

        readyLabel.setText("Get Ready!   " + n + " player(s)");
        cardLayout.show(root, S_READY);

        // Implements Runnable — run() waits then shows first question
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    /**
     * run() — Runnable implementation (Unit 1: Multithreading).
     * Sleeps briefly on the ready screen then transitions to first question.
     */
    @Override
    public void run() {
        try { Thread.sleep(1500); } catch (InterruptedException e) { return; }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { showCurrentTurn(); }
        });
    }

    // =========================================================================
    // LOGIC: SHOW TURN
    // =========================================================================
    private void showCurrentTurn() {
        String[] q          = QUESTIONS[currentQ];
        String   playerName = players.get(turnIndex);

        qHeading .setText("   Chance of  " + playerName);
        qNumLabel.setText("Question " + (currentQ + 1) + " of " + TOTAL_Q);
        qTextLabel.setText(
                "<html><body style='width:360px; padding:2px'>" + q[0] + "</body></html>");

        String[] keys = {"A","B","C","D"};
        for (int i = 0; i < 4; i++) {
            optBtns[i].setText("<html><b>[" + keys[i] + "]</b>  " + q[i + 1] + "</html>");
            optBtns[i].setEnabled(true);
            optBtns[i].setBackground(C_CARD2);
        }

        waitLabel.setText("");
        waitLabel.setForeground(C_DIM);
        timerExpired = false;
        cardLayout.show(root, S_QUESTION);

        // Start animated timer bar
        timerBar.start(TIMER_SECS, new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { onTimerExpired(); }
                });
            }
        });
    }

    // =========================================================================
    // LOGIC: OPTION CHOSEN
    // =========================================================================
    private void onOptionChosen(String key) {
        timerBar.stopAnim();
        disableOptions();

        char chosen  = key.charAt(0);
        char correct = QUESTIONS[currentQ][5].charAt(0);

        answered [turnIndex] = true;
        chosenOpt[turnIndex] = chosen;

        if (chosen == correct) {
            scores[turnIndex] += 1000;
            waitLabel.setText(players.get(turnIndex) + "  answered!");
            waitLabel.setForeground(C_GREEN);
        } else {
            waitLabel.setText(players.get(turnIndex) + "  answered!");
            waitLabel.setForeground(C_DIM);
        }

        pauseThen(700, new Runnable() {
            public void run() { advanceTurn(); }
        });
    }

    private void onTimerExpired() {
        if (timerExpired) return;
        timerExpired = true;
        disableOptions();
        answered [turnIndex] = false;
        chosenOpt[turnIndex] = '-';
        waitLabel.setText("Time's up for " + players.get(turnIndex) + "!");
        waitLabel.setForeground(C_RED);
        pauseThen(700, new Runnable() {
            public void run() { advanceTurn(); }
        });
    }

    // =========================================================================
    // LOGIC: ADVANCE TURN / ROUND
    // =========================================================================
    private void advanceTurn() {
        turnIndex++;
        if (turnIndex < players.size()) {
            showCurrentTurn();          // next player's turn
        } else {
            showRoundResult();          // everyone answered
        }
    }

    // =========================================================================
    // LOGIC: ROUND RESULT
    // =========================================================================
    private void showRoundResult() {
        String[] q       = QUESTIONS[currentQ];
        char     correct = q[5].charAt(0);
        int      corrIdx = correct - 'A';        // 0-based index into options

        resultPanel.removeAll();
        resultPanel.setBorder(BorderFactory.createEmptyBorder(16, 26, 16, 26));

        JPanel inner = makePanel(new BorderLayout(0, 12));

        // Header
        JLabel rTitle = makeLabel("Round " + (currentQ + 1) + " — Results", F_TITLE, C_CYAN);
        rTitle.setHorizontalAlignment(SwingConstants.CENTER);

        String correctText = q[corrIdx + 1];
        JLabel corrLbl = makeLabel(
                "Correct Answer:  [" + correct + "]  " + correctText, F_BOLD, C_GREEN);
        corrLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel hdr = makePanel(new GridLayout(2, 1, 0, 6));
        hdr.add(rTitle); hdr.add(corrLbl);
        inner.add(hdr, BorderLayout.NORTH);

        // Per-player rows
        JPanel rows = makePanel(new GridLayout(0, 1, 0, 6));
        rows.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        for (int i = 0; i < players.size(); i++) {
            boolean ans   = answered[i];
            char    chose = chosenOpt[i];
            boolean right = ans && chose == correct;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(right ? new Color(18, 55, 38) : new Color(55, 18, 18));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(right ? C_GREEN : C_RED, 1),
                    BorderFactory.createEmptyBorder(10, 16, 10, 16)));

            String icon = right ? "Correct" : (ans ? "Wrong" : "No Answer");
            String desc;
            if (right)      desc = "[" + chose + "]  +1000 pts";
            else if (ans)   desc = "[" + chose + "]  +0 pts";
            else            desc = "Timed out  +0 pts";

            Color iconColor = right ? C_GREEN : C_RED;

            JLabel iconLbl  = makeLabel(icon, F_BOLD, iconColor);
            iconLbl.setPreferredSize(new Dimension(80, 0));
            JLabel nameLbl  = makeLabel(players.get(i), F_BIG, C_TEXT);
            JLabel descLbl  = makeLabel(desc, F_SMALL, right ? C_GREEN : C_DIM);
            JLabel scoreLbl = makeLabel("Total: " + scores[i] + " pts", F_BOLD, C_GOLD);

            JPanel leftSide = makePanel(new BorderLayout(10, 0));
            leftSide.setOpaque(false);
            leftSide.add(iconLbl, BorderLayout.WEST);
            JPanel mid = makePanel(new GridLayout(2, 1, 0, 2));
            mid.setOpaque(false);
            mid.add(nameLbl); mid.add(descLbl);
            leftSide.add(mid, BorderLayout.CENTER);

            row.add(leftSide, BorderLayout.CENTER);
            row.add(scoreLbl, BorderLayout.EAST);
            rows.add(row);
        }

        JScrollPane sp = new JScrollPane(rows);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        inner.add(sp, BorderLayout.CENTER);

        // Next / Final button
        String btnText = (currentQ < TOTAL_Q - 1)
                ? "Next Question  \u2192"
                : "See Final Results  \u2192";
        Color btnColor = (currentQ < TOTAL_Q - 1) ? C_BLUE : C_GOLD;
        JButton nextBtn = makeBtn(btnText, btnColor);
        nextBtn.setFont(F_BIG);
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doNextQuestion(); }
        });
        JPanel btnRow = makePanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        btnRow.add(nextBtn);
        inner.add(btnRow, BorderLayout.SOUTH);

        resultPanel.add(inner, BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
        cardLayout.show(root, S_RESULT);
    }

    private void doNextQuestion() {
        currentQ++;
        if (currentQ >= TOTAL_Q) {
            showFinalResults();
            return;
        }
        turnIndex = 0;
        Arrays.fill(answered,  false);
        Arrays.fill(chosenOpt, '-');
        showCurrentTurn();
    }

    // =========================================================================
    // LOGIC: FINAL RESULTS
    // =========================================================================
    private void showFinalResults() {
        // Sort indices by score descending
        Integer[] order = new Integer[players.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) { return scores[b] - scores[a]; }
        });

        finalPanel.removeAll();
        finalPanel.setBorder(BorderFactory.createEmptyBorder(22, 32, 22, 32));

        JPanel inner = makePanel(new BorderLayout(0, 14));

        JLabel title     = makeLabel("FINAL RESULTS", F_HUGE, C_GOLD);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel winnerLbl = makeLabel(
                "Winner:  " + players.get(order[0]) + "  !", F_TITLE, C_GREEN);
        winnerLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel topArea = makePanel(new GridLayout(2, 1, 0, 6));
        topArea.add(title); topArea.add(winnerLbl);
        inner.add(topArea, BorderLayout.NORTH);

        // Leaderboard
        JPanel rows  = makePanel(new GridLayout(0, 1, 0, 8));
        rows.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        String[] medals  = {"WINNER", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th"};
        Color[]  mColors = {
                C_GOLD, new Color(192,192,192), new Color(205,127,50),
                C_DIM, C_DIM, C_DIM, C_DIM, C_DIM
        };

        for (int rank = 0; rank < order.length; rank++) {
            int pi = order[rank];

            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setBackground(rank == 0 ? new Color(38, 34, 8) : C_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(rank == 0 ? C_GOLD : C_BORDER,
                            rank == 0 ? 2 : 1),
                    BorderFactory.createEmptyBorder(12, 18, 12, 18)));

            String badge  = rank < medals.length  ? medals [rank]  : (rank + 1) + "th";
            Color  bColor = rank < mColors.length ? mColors[rank]  : C_DIM;

            JLabel rankLbl  = makeLabel(badge, F_BOLD, bColor);
            rankLbl.setPreferredSize(new Dimension(72, 0));
            JLabel nameLbl  = makeLabel(players.get(pi), F_BIG,
                    rank == 0 ? C_GOLD : C_TEXT);
            JLabel scoreLbl = makeLabel(scores[pi] + " pts", F_BIG,
                    rank == 0 ? C_GOLD : C_DIM);

            row.add(rankLbl,  BorderLayout.WEST);
            row.add(nameLbl,  BorderLayout.CENTER);
            row.add(scoreLbl, BorderLayout.EAST);
            rows.add(row);
        }

        JScrollPane sp = new JScrollPane(rows);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        inner.add(sp, BorderLayout.CENTER);

        // Play again
        JButton again = makeBtn("  PLAY AGAIN  ", C_BLUE);
        again.setFont(F_BIG);
        again.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doPlayAgain(); }
        });
        JPanel btnRow = makePanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        btnRow.add(again);
        inner.add(btnRow, BorderLayout.SOUTH);

        finalPanel.add(inner, BorderLayout.CENTER);
        finalPanel.revalidate();
        finalPanel.repaint();
        cardLayout.show(root, S_FINAL);
    }

    private void doPlayAgain() {
        players.clear();
        refreshPlayerList();
        nameStatus.setText("Add at least 1 player to start.");
        nameStatus.setForeground(C_DIM);
        btnGoQuiz.setEnabled(false);
        cardLayout.show(root, S_NAMES);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private void disableOptions() {
        for (JButton b : optBtns) b.setEnabled(false);
    }

    private void pauseThen(final long ms, final Runnable task) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(task);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // WIDGET FACTORIES
    // =========================================================================
    private JPanel makePanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(C_BG);
        p.setOpaque(true);
        return p;
    }

    private JLabel makeLabel(String text, Font font, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        l.setOpaque(false);
        return l;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(F_BOLD);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        return b;
    }

    private JButton makeOptBtn(String key, Color accent) {
        JButton b = new JButton();
        b.setFont(F_OPT);
        b.setBackground(C_CARD2);
        b.setForeground(C_TEXT);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 2),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled())
                    b.setBackground(new Color(
                            accent.getRed()   / 5,
                            accent.getGreen() / 5,
                            accent.getBlue()  / 5));
            }
            public void mouseExited(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(C_CARD2);
            }
        });
        return b;
    }

    private void styleField(JTextField tf) {
        tf.setFont(F_BODY);
        tf.setBackground(C_CARD);
        tf.setForeground(C_TEXT);
        tf.setCaretColor(C_CYAN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BLUE, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    // =========================================================================
    // INNER CLASS: TimerBar  (Unit 5 — Animation)
    // =========================================================================
    /**
     * Animated countdown bar.
     * Uses a background Thread + Graphics2D painting — covers both
     * Applet animation (Unit 5) and multithreading (Unit 1).
     */
    class TimerBar extends JComponent {
        private volatile float   progress  = 1.0f;
        private volatile int     remaining = 0;
        private volatile boolean running   = false;
        private Thread           thread;
        private Runnable         onExpire;

        void start(final int seconds, final Runnable callback) {
            stopAnim();
            remaining = seconds;
            progress  = 1.0f;
            running   = true;
            onExpire  = callback;

            final long t0 = System.currentTimeMillis();

            thread = new Thread(new Runnable() {
                public void run() {
                    while (running) {
                        long elapsed = System.currentTimeMillis() - t0;
                        progress  = Math.max(0f, 1f - (float) elapsed / (seconds * 1000f));
                        remaining = Math.max(0, seconds - (int)(elapsed / 1000));

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { repaint(); }
                        });

                        if (progress <= 0f) {
                            running = false;
                            if (onExpire != null) onExpire.run();
                            break;
                        }
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        void stopAnim() {
            running = false;
            if (thread != null) thread.interrupt();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Track
            g.setColor(C_CARD2);
            g.fillRoundRect(0, 3, w, h - 6, 10, 10);

            // Fill — colour shifts green → amber → red
            Color bar;
            if      (progress > 0.60f) bar = C_GREEN;
            else if (progress > 0.30f) bar = new Color(240, 170, 30);
            else                       bar = C_RED;

            int filled = (int)(w * progress);
            if (filled > 0) {
                g.setColor(bar);
                g.fillRoundRect(0, 3, filled, h - 6, 10, 10);
            }

            // Text
            g.setFont(F_MONO);
            g.setColor(C_TEXT);
            String txt = remaining + "s";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt, (w - fm.stringWidth(txt)) / 2, (h + fm.getAscent()) / 2 - 2);
        }
    }

    // =========================================================================
    // STANDALONE MAIN  (run: java QuizApplet)
    // =========================================================================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Quiz Competition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 580);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(C_BG);

        QuizApplet applet = new QuizApplet();
        applet.init();
        applet.start();

        frame.add(applet);
        frame.setVisible(true);
    }
}
