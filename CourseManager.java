import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/* =========================================================
 *  COURSE — the model (unchanged from the working version)
 * ========================================================= */
class Course {
    private final String code;
    private final String title;
    private final int unit;

    public Course(String code, String title, int unit) {
        if (unit <= 0)
            throw new IllegalArgumentException("Unit must be a positive whole number.");
        this.code = code;
        this.title = title;
        this.unit = unit;
    }

    public String getCode()  { return code; }
    public String getTitle() { return title; }
    public int    getUnit()  { return unit; }

    public String toFileLine() { return code + "|" + title + "|" + unit; }

    public static Course fromFileLine(String line) {
        String[] parts = line.trim().split("\\|");
        if (parts.length != 3) throw new IllegalArgumentException("Malformed record: " + line);
        return new Course(parts[0].trim(), parts[1].trim(), Integer.parseInt(parts[2].trim()));
    }
}

/* =========================================================
 *  PALETTE — 60 / 30 / 10 strictly enforced
 * ========================================================= */
final class Palette {
    // 60% warm porcelain
    static final Color PORCELAIN      = new Color(0xF5, 0xF4, 0xF0);
    // 30% charcoal
    static final Color CHARCOAL       = new Color(0x25, 0x25, 0x25);
    static final Color CHARCOAL_HOVER = new Color(0x33, 0x33, 0x33);
    // 10% muted champagne
    static final Color CHAMPAGNE      = new Color(0xB4, 0x9A, 0x72);
    static final Color CHAMPAGNE_SOFT = new Color(0xF0, 0xE9, 0xDC);
    // support
    static final Color WHITE          = Color.WHITE;
    static final Color MUTED          = new Color(0x85, 0x85, 0x85);
    static final Color BORDER         = new Color(0xE7, 0xE5, 0xE0);
    static final Color SUCCESS        = new Color(0x6B, 0x92, 0x78);
    static final Color ERROR          = new Color(0xC4, 0x6B, 0x64);
    static final Color SIDEBAR_TEXT   = new Color(0xDD, 0xDD, 0xDD);
    static final Color SIDEBAR_MUTED  = new Color(0x8A, 0x8A, 0x8A);
}

/* =========================================================
 *  TYPOGRAPHY — native-first font family
 * ========================================================= */
final class Typography {
    private static final String FAMILY = pick();

    private static String pick() {
        Set<String> available = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] prefs;
        if (os.contains("mac"))      prefs = new String[]{"SF Pro Text", "Helvetica Neue", "SansSerif"};
        else if (os.contains("win")) prefs = new String[]{"Segoe UI", "Tahoma", "SansSerif"};
        else                         prefs = new String[]{"Inter", "Cantarell", "SansSerif"};
        for (String f : prefs) if (available.contains(f)) return f;
        return "SansSerif";
    }

    static String family()  { return FAMILY; }
    static Font display()   { return new Font(FAMILY, Font.BOLD,  32); }
    static Font heading()   { return new Font(FAMILY, Font.BOLD,  20); }
    static Font section()   { return new Font(FAMILY, Font.BOLD,  14); }
    static Font body()      { return new Font(FAMILY, Font.PLAIN, 14); }
    static Font small()     { return new Font(FAMILY, Font.PLAIN, 12); }
    static Font label()     { return new Font(FAMILY, Font.BOLD,  10); }
    static Font number()    { return new Font(FAMILY, Font.BOLD,  40); }
}

/* =========================================================
 *  PILL BUTTON — rounded, animated hover
 * ========================================================= */
class PillButton extends JButton {
    enum Style { PRIMARY, SECONDARY, DANGER }

    private final Style style;
    private float hover = 0f;
    private Timer hoverTimer;

    PillButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(new Font(Typography.family(), Font.BOLD, 13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 20, 10, 20));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animateTo(1f); }
            @Override public void mouseExited(MouseEvent e)  { animateTo(0f); }
        });
    }

    private void animateTo(float target) {
        if (hoverTimer != null) hoverTimer.stop();
        hoverTimer = new Timer(16, e -> {
            hover += (target - hover) * 0.35f;
            if (Math.abs(target - hover) < 0.01f) { hover = target; hoverTimer.stop(); }
            repaint();
        });
        hoverTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        Color bg;
        Color fg;
        switch (style) {
            case PRIMARY:
                bg = blend(Palette.CHAMPAGNE, Palette.CHARCOAL, hover * 0.15f);
                fg = Palette.WHITE;
                break;
            case DANGER:
                bg = blend(Palette.ERROR, Palette.ERROR.darker(), hover * 0.25f);
                fg = Palette.WHITE;
                break;
            default:
                bg = blend(Palette.WHITE, Palette.PORCELAIN, hover);
                fg = Palette.CHARCOAL;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, h, h);
        if (style == Style.SECONDARY) {
            g2.setColor(Palette.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
        }
        g2.dispose();

        setForeground(fg);
        super.paintComponent(g);
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            (int)(a.getRed()   * (1 - t) + b.getRed()   * t),
            (int)(a.getGreen() * (1 - t) + b.getGreen() * t),
            (int)(a.getBlue()  * (1 - t) + b.getBlue()  * t));
    }
}

/* =========================================================
 *  STYLED INPUT — rounded field with champagne focus ring
 * ========================================================= */
class StyledInput extends JPanel {
    private final JTextField field;

    StyledInput(int cols) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(9, 14, 9, 14));
        field = new JTextField(cols);
        field.setOpaque(false);
        field.setBorder(null);
        field.setFont(Typography.body());
        field.setForeground(Palette.CHARCOAL);
        field.setCaretColor(Palette.CHAMPAGNE);
        add(field, BorderLayout.CENTER);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)  { repaint(); }
        });
    }

    JTextField field()      { return field; }
    String text()           { return field.getText(); }
    void   setText(String s) { field.setText(s); }
    void   clear()          { field.setText(""); }
    void   focus()          { field.requestFocusInWindow(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Palette.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.setColor(field.hasFocus() ? Palette.CHAMPAGNE : Palette.BORDER);
        g2.setStroke(new BasicStroke(field.hasFocus() ? 1.5f : 1f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}

/* =========================================================
 *  NAV BUTTON — sidebar item with animated hover + active bar
 * ========================================================= */
class NavButton extends JButton {
    private final String label;
    private boolean active = false;
    private float hover = 0f;
    private Timer hoverTimer;

    NavButton(String label) {
        this.label = label;
        setPreferredSize(new Dimension(200, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { animate(1f); }
            @Override public void mouseExited(MouseEvent e)  { animate(0f); }
        });
    }

    void setActive(boolean a) { active = a; repaint(); }

    private void animate(float t) {
        if (hoverTimer != null) hoverTimer.stop();
        hoverTimer = new Timer(16, e -> {
            hover += (t - hover) * 0.35f;
            if (Math.abs(t - hover) < 0.01f) { hover = t; hoverTimer.stop(); }
            repaint();
        });
        hoverTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        if (hover > 0.01f) {
            g2.setColor(new Color(255, 255, 255, (int)(hover * 16)));
            g2.fillRoundRect(0, 2, w, h - 4, 10, 10);
        }
        if (active) {
            g2.setColor(Palette.CHAMPAGNE);
            g2.fillRoundRect(0, 10, 3, h - 20, 3, 3);
        }

        g2.setColor(active ? Color.WHITE : Palette.SIDEBAR_TEXT);
        g2.setFont(new Font(Typography.family(), active ? Font.BOLD : Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        int y = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, 22, y);

        g2.dispose();
    }
}

/* =========================================================
 *  TOAST — sliding notification on the glass pane
 * ========================================================= */
class ToastManager extends JComponent {
    private String msg;
    private boolean success;
    private float alpha = 0f;
    private int   yOffset = 20;

    ToastManager() {
        setOpaque(false);
        setVisible(false);
    }

    @Override
    public boolean contains(int x, int y) { return false; }  // pass clicks through

    void show(String message, boolean success) {
        this.msg = message;
        this.success = success;
        this.alpha = 0f;
        this.yOffset = 20;
        setVisible(true);

        Timer in = new Timer(16, null);
        in.addActionListener(e -> {
            alpha = Math.min(1f, alpha + 0.10f);
            yOffset = Math.max(0, yOffset - 3);
            repaint();
            if (alpha >= 1f) {
                in.stop();
                Timer hold = new Timer(2000, ev -> {
                    Timer out = new Timer(16, null);
                    out.addActionListener(e2 -> {
                        alpha -= 0.08f;
                        yOffset += 2;
                        if (alpha <= 0) { out.stop(); setVisible(false); }
                        repaint();
                    });
                    out.start();
                });
                hold.setRepeats(false);
                hold.start();
            }
        });
        in.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (msg == null || alpha <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));

        Font f = Typography.body();
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(msg);
        int padX = 22, padY = 13;
        int dot = 8, gap = 10;
        int w = padX * 2 + dot + gap + textW;
        int h = fm.getHeight() + padY * 2;
        int x = (getWidth() - w) / 2;
        int y = getHeight() - h - 32 + yOffset;

        // subtle shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(x, y + 3, w, h, h, h);
        // body
        g2.setColor(Palette.CHARCOAL);
        g2.fillRoundRect(x, y, w, h, h, h);
        // dot
        g2.setColor(success ? Palette.SUCCESS : Palette.ERROR);
        g2.fillOval(x + padX, y + h / 2 - dot / 2, dot, dot);
        // text
        g2.setColor(Palette.WHITE);
        g2.drawString(msg, x + padX + dot + gap,
                y + (h + fm.getAscent() - fm.getDescent()) / 2);

        g2.dispose();
    }
}

/* =========================================================
 *  FADE CARDS — content-area fade-in on navigation
 * ========================================================= */
class FadeCards extends JPanel {
    private float overlay = 0f;

    FadeCards(CardLayout layout) {
        super(layout);
        setBackground(Palette.PORCELAIN);
    }

    void flashFade() {
        overlay = 1f;
        repaint();
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            overlay -= 0.08f;
            if (overlay <= 0) { overlay = 0; ((Timer) e.getSource()).stop(); }
            repaint();
        });
        t.start();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (overlay > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(overlay));
            g2.setColor(Palette.PORCELAIN);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}

/* =========================================================
 *  CARD PANEL — rounded white card with hairline border
 * ========================================================= */
class CardPanel extends JPanel {
    CardPanel(LayoutManager lm) {
        super(lm);
        setOpaque(false);
        setBorder(new EmptyBorder(22, 24, 22, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setColor(new Color(0, 0, 0, 6));
        g2.fillRoundRect(0, 1, w, h - 2, 16, 16);
        g2.setColor(Palette.WHITE);
        g2.fillRoundRect(0, 0, w, h - 1, 16, 16);
        g2.setColor(Palette.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}

/* =========================================================
 *  COURSE MANAGER — the app
 * ========================================================= */
public class CourseManager extends JFrame {

    private static final String FILE_NAME = "courses.txt";

    // ---------- model ----------
    private final List<Course> courses = new ArrayList<>();

    // ---------- navigation ----------
    private final String[] VIEW_KEYS = {"overview", "courses", "add", "search", "summary", "files"};
    private final String[] VIEW_NAV  = {"Overview", "My Courses", "Add Course", "Search", "Summary", "Files"};
    private final List<NavButton> navButtons = new ArrayList<>();
    private String currentView = "overview";

    // ---------- content ----------
    private CardLayout cardLayout;
    private FadeCards cards;
    private ToastManager toast;

    // refreshable containers
    private JPanel overviewBody;
    private JPanel coursesBody;
    private JPanel summaryBody;
    private JPanel searchResultArea;

    // add-view fields
    private StyledInput addCode, addTitle, addUnit;
    private JLabel addError;

    // search-view field
    private StyledInput searchCode;
    private JLabel searchError;

    // files-view status
    private JLabel filesStatus;

    // =========================================================
    public CourseManager() {
        super("Student Course Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);

        // toast via glass pane
        toast = new ToastManager();
        setGlassPane(toast);

        // assemble
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContentArea(), BorderLayout.CENTER);

        navigateTo("overview");
    }

    // ---------- sidebar ----------
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Palette.CHARCOAL);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        side.setPreferredSize(new Dimension(240, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(36, 20, 24, 20));

        JLabel eyebrow = new JLabel("COS201");
        eyebrow.setFont(Typography.label());
        eyebrow.setForeground(Palette.CHAMPAGNE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        eyebrow.setBorder(new EmptyBorder(0, 22, 6, 0));

        JLabel word1 = new JLabel("Course");
        word1.setFont(new Font(Typography.family(), Font.BOLD, 22));
        word1.setForeground(Color.WHITE);
        word1.setAlignmentX(Component.LEFT_ALIGNMENT);
        word1.setBorder(new EmptyBorder(0, 22, 0, 0));

        JLabel word2 = new JLabel("Manager");
        word2.setFont(new Font(Typography.family(), Font.BOLD, 22));
        word2.setForeground(Color.WHITE);
        word2.setAlignmentX(Component.LEFT_ALIGNMENT);
        word2.setBorder(new EmptyBorder(0, 22, 0, 0));

        JPanel divider = new JPanel();
        divider.setBackground(Palette.CHARCOAL_HOVER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setBorder(new EmptyBorder(22, 22, 22, 22));

        side.add(eyebrow);
        side.add(word1);
        side.add(word2);
        side.add(divider);

        for (int i = 0; i < VIEW_NAV.length; i++) {
            final String key = VIEW_KEYS[i];
            NavButton nb = new NavButton(VIEW_NAV[i]);
            nb.addActionListener(e -> navigateTo(key));
            navButtons.add(nb);
            side.add(nb);
            side.add(Box.createVerticalStrut(4));
        }

        side.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("v1.0 · Mid-Semester");
        footer.setFont(Typography.label());
        footer.setForeground(Palette.SIDEBAR_MUTED);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setBorder(new EmptyBorder(0, 22, 0, 0));
        side.add(footer);

        return side;
    }

    // ---------- content area ----------
    private JPanel buildContentArea() {
        cardLayout = new CardLayout();
        cards = new FadeCards(cardLayout);
        cards.add(buildOverviewView(), "overview");
        cards.add(buildCoursesView(),  "courses");
        cards.add(buildAddView(),      "add");
        cards.add(buildSearchView(),   "search");
        cards.add(buildSummaryView(),  "summary");
        cards.add(buildFilesView(),    "files");
        return cards;
    }

    // ---------- helpers ----------
    private JPanel page(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 28));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(36, 48, 36, 48));

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);

        JLabel h = new JLabel(title);
        h.setFont(Typography.display());
        h.setForeground(Palette.CHARCOAL);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(Typography.body());
        s.setForeground(Palette.MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        head.add(h);
        head.add(Box.createVerticalStrut(6));
        head.add(s);
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    private JPanel statCard(String label, String value) {
        CardPanel c = new CardPanel(new BorderLayout(0, 6));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(Typography.label());
        l.setForeground(Palette.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(Typography.number());
        v.setForeground(Palette.CHARCOAL);
        c.add(l, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        return c;
    }

    private JPanel labelled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(Typography.label());
        l.setForeground(Palette.MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCourseRow(Course c) {
        JPanel row = new JPanel(new BorderLayout(16, 0)) {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) {
                    g2.setColor(Palette.PORCELAIN);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel code = new JLabel(c.getCode());
        code.setFont(new Font(Typography.family(), Font.BOLD, 14));
        code.setForeground(Palette.CHARCOAL);
        code.setPreferredSize(new Dimension(88, 20));

        JLabel title = new JLabel(c.getTitle());
        title.setFont(Typography.body());
        title.setForeground(Palette.MUTED);

        JLabel unit = new JLabel(c.getUnit() + " units");
        unit.setFont(new Font(Typography.family(), Font.BOLD, 12));
        unit.setForeground(Palette.CHAMPAGNE);
        unit.setOpaque(true);
        unit.setBackground(Palette.CHAMPAGNE_SOFT);
        unit.setBorder(new EmptyBorder(4, 10, 4, 10));

        JPanel mid = new JPanel(new BorderLayout(16, 0));
        mid.setOpaque(false);
        mid.add(code, BorderLayout.WEST);
        mid.add(title, BorderLayout.CENTER);

        row.add(mid, BorderLayout.CENTER);
        row.add(unit, BorderLayout.EAST);
        return row;
    }

    private JPanel emptyState(String heading, String body, String ctaText, Runnable cta) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(50, 0, 50, 0));

        JLabel h = new JLabel(heading);
        h.setFont(Typography.heading());
        h.setForeground(Palette.CHARCOAL);
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel b = new JLabel(body);
        b.setFont(Typography.body());
        b.setForeground(Palette.MUTED);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(h);
        p.add(Box.createVerticalStrut(8));
        p.add(b);
        if (ctaText != null) {
            p.add(Box.createVerticalStrut(22));
            PillButton btn = new PillButton(ctaText, PillButton.Style.PRIMARY);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> cta.run());
            p.add(btn);
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    // =========================================================
    //  VIEWS
    // =========================================================
    private JPanel buildOverviewView() {
        JPanel p = page("Overview", "Welcome back. Here's a snapshot of your semester.");
        overviewBody = new JPanel();
        overviewBody.setOpaque(false);
        overviewBody.setLayout(new BoxLayout(overviewBody, BoxLayout.Y_AXIS));
        p.add(overviewBody, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCoursesView() {
        JPanel p = page("My Courses", "Every course you've registered this semester.");
        coursesBody = new JPanel(new BorderLayout());
        coursesBody.setOpaque(false);
        p.add(coursesBody, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAddView() {
        JPanel p = page("Add Course", "Register a new course for this semester.");

        addCode  = new StyledInput(20);
        addTitle = new StyledInput(20);
        addUnit  = new StyledInput(8);
        addError = new JLabel(" ");
        addError.setFont(Typography.small());
        addError.setForeground(Palette.ERROR);
        addError.setVisible(false);

        CardPanel card = new CardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 14, 0);

        gbc.gridx = 0; gbc.gridy = 0; card.add(labelled("Course Code", addCode), gbc);
        gbc.gridy = 1;                card.add(labelled("Course Title", addTitle), gbc);
        gbc.gridy = 2;                card.add(labelled("Credit Units", addUnit), gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        PillButton addBtn = new PillButton("+  Add Course", PillButton.Style.PRIMARY);
        addBtn.addActionListener(e -> doAdd());
        btnRow.add(addBtn);
        btnRow.add(addError);

        gbc.gridy = 3; gbc.insets = new Insets(6, 0, 0, 0);
        card.add(btnRow, gbc);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 0, 340)); // keep card from stretching
        wrap.add(card, BorderLayout.NORTH);

        p.add(wrap, BorderLayout.CENTER);

        addCode.field().addActionListener(e -> doAdd());
        addTitle.field().addActionListener(e -> doAdd());
        addUnit.field().addActionListener(e -> doAdd());
        return p;
    }

    private JPanel buildSearchView() {
        JPanel p = page("Search", "Find a course by its code.");

        CardPanel searchBar = new CardPanel(new BorderLayout(10, 0));
        searchCode = new StyledInput(20);
        PillButton btn = new PillButton("Search", PillButton.Style.PRIMARY);
        btn.addActionListener(e -> doSearch());
        searchBar.add(searchCode, BorderLayout.CENTER);
        searchBar.add(btn, BorderLayout.EAST);

        searchError = new JLabel(" ");
        searchError.setFont(Typography.small());
        searchError.setForeground(Palette.MUTED);
        searchError.setBorder(new EmptyBorder(10, 4, 0, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(searchBar, BorderLayout.NORTH);
        top.add(searchError, BorderLayout.SOUTH);

        searchResultArea = new JPanel(new BorderLayout());
        searchResultArea.setOpaque(false);
        searchResultArea.setBorder(new EmptyBorder(24, 0, 0, 0));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 0, 340));
        wrap.add(top, BorderLayout.NORTH);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(wrap, BorderLayout.NORTH);
        south.add(searchResultArea, BorderLayout.CENTER);

        p.add(south, BorderLayout.CENTER);

        searchCode.field().addActionListener(e -> doSearch());
        return p;
    }

    private JPanel buildSummaryView() {
        JPanel p = page("Academic Summary", "A clean statement of your registered workload.");
        summaryBody = new JPanel();
        summaryBody.setOpaque(false);
        summaryBody.setLayout(new BoxLayout(summaryBody, BoxLayout.Y_AXIS));
        p.add(summaryBody, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFilesView() {
        JPanel p = page("File Management", "Persist your course list to disk, or restore it.");

        CardPanel card = new CardPanel(new BorderLayout(0, 18));

        JLabel info = new JLabel("Records are stored in \"" + FILE_NAME + "\" alongside the program.");
        info.setFont(Typography.small());
        info.setForeground(Palette.MUTED);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        PillButton save = new PillButton("Save to File",   PillButton.Style.PRIMARY);
        PillButton load = new PillButton("Load from File", PillButton.Style.SECONDARY);
        save.addActionListener(e -> doSave());
        load.addActionListener(e -> doLoad());
        btnRow.add(save);
        btnRow.add(load);

        filesStatus = new JLabel(" ");
        filesStatus.setFont(Typography.small());
        filesStatus.setForeground(Palette.MUTED);
        filesStatus.setBorder(new EmptyBorder(6, 4, 0, 0));

        card.add(info, BorderLayout.NORTH);
        card.add(btnRow, BorderLayout.CENTER);
        card.add(filesStatus, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 0, 400));
        wrap.add(card, BorderLayout.NORTH);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    //  REFRESH
    // =========================================================
    private void rebuildOverview() {
        overviewBody.removeAll();

        // stats row
        JPanel stats = new JPanel(new GridLayout(1, 2, 20, 0));
        stats.setOpaque(false);
        stats.add(statCard("Total Courses", String.valueOf(courses.size())));
        stats.add(statCard("Total Units",   String.valueOf(totalUnits())));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        overviewBody.add(stats);
        overviewBody.add(Box.createVerticalStrut(28));

        // quick action row
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quick.setOpaque(false);
        PillButton addBtn = new PillButton("+  Add Course", PillButton.Style.SECONDARY);
        addBtn.addActionListener(e -> navigateTo("add"));
        quick.add(addBtn);
        quick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        overviewBody.add(quick);
        overviewBody.add(Box.createVerticalStrut(28));

        // recent section
        JLabel recent = new JLabel("Recent Courses");
        recent.setFont(Typography.section());
        recent.setForeground(Palette.CHARCOAL);
        recent.setAlignmentX(Component.LEFT_ALIGNMENT);
        overviewBody.add(recent);
        overviewBody.add(Box.createVerticalStrut(12));

        CardPanel list = new CardPanel(new BoxLayout(this, BoxLayout.Y_AXIS));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (courses.isEmpty()) {
            list.add(emptyState("No courses yet",
                    "Add your first course to get started.",
                    "+  Add a Course",
                    () -> navigateTo("add")));
        } else {
            int start = Math.max(0, courses.size() - 3);
            for (int i = start; i < courses.size(); i++) {
                JPanel row = buildCourseRow(courses.get(i));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(row);
                if (i < courses.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(Palette.BORDER);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    list.add(sep);
                }
            }
        }
        list.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        overviewBody.add(list);
        overviewBody.add(Box.createVerticalGlue());

        overviewBody.revalidate();
        overviewBody.repaint();
    }

    private void rebuildCourses() {
        coursesBody.removeAll();

        if (courses.isEmpty()) {
            coursesBody.add(emptyState(
                    "No courses yet",
                    "Your course list is empty. Add your first course to get started.",
                    "+  Add Course",
                    () -> navigateTo("add")), BorderLayout.CENTER);
        } else {
            CardPanel list = new CardPanel(new BorderLayout());
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            for (int i = 0; i < courses.size(); i++) {
                JPanel row = buildCourseRow(courses.get(i));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(row);
                if (i < courses.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(Palette.BORDER);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    list.add(sep);
                }
            }
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(list, BorderLayout.NORTH);
            coursesBody.add(wrap, BorderLayout.CENTER);
        }
        coursesBody.revalidate();
        coursesBody.repaint();
    }

    private void rebuildSummary() {
        summaryBody.removeAll();

        if (courses.isEmpty()) {
            summaryBody.add(emptyState(
                    "Nothing to summarise yet",
                    "Add courses to see your academic summary.",
                    "+  Add Course",
                    () -> navigateTo("add")));
        } else {
            JPanel stats = new JPanel(new GridLayout(1, 2, 20, 0));
            stats.setOpaque(false);
            stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            stats.add(statCard("Total Courses", String.valueOf(courses.size())));
            stats.add(statCard("Total Units",   String.valueOf(totalUnits())));
            stats.setAlignmentX(Component.LEFT_ALIGNMENT);
            summaryBody.add(stats);
            summaryBody.add(Box.createVerticalStrut(24));

            double avg = (double) totalUnits() / courses.size();
            String msg = String.format(
                "You are registered for <b>%d</b> course%s totalling <b>%d</b> credit unit%s this semester (average %.1f units per course).",
                courses.size(), courses.size() == 1 ? "" : "s",
                totalUnits(),   totalUnits()   == 1 ? "" : "s",
                avg);

            JLabel body = new JLabel("<html><body style='width:600px;font-family:" +
                    Typography.family() + ";color:#252525;font-size:14px'>" + msg + "</body></html>");
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            summaryBody.add(body);
            summaryBody.add(Box.createVerticalGlue());
        }
        summaryBody.revalidate();
        summaryBody.repaint();
    }

    private void refreshAll() {
        rebuildOverview();
        rebuildCourses();
        rebuildSummary();
    }

    // =========================================================
    //  NAVIGATION
    // =========================================================
    private void navigateTo(String key) {
        if (key.equals(currentView) && "overview".equals(key) == false) {
            // allow re-click on same page silently
        }
        currentView = key;
        cardLayout.show(cards, key);
        cards.flashFade();

        for (int i = 0; i < VIEW_KEYS.length; i++) {
            navButtons.get(i).setActive(VIEW_KEYS[i].equals(key));
        }

        if (key.equals("overview")) rebuildOverview();
        if (key.equals("courses"))  rebuildCourses();
        if (key.equals("summary"))  rebuildSummary();

        if (key.equals("add"))    SwingUtilities.invokeLater(addCode::focus);
        if (key.equals("search")) SwingUtilities.invokeLater(searchCode::focus);
    }

    // =========================================================
    //  RECURSION (genuine, not simulated)
    // =========================================================
    private Course searchByCode(String code, int index) {
        if (index >= courses.size()) return null;                     // base case
        if (courses.get(index).getCode().equalsIgnoreCase(code))
            return courses.get(index);
        return searchByCode(code, index + 1);                          // recurse
    }

    private int totalUnits(int index) {
        if (index >= courses.size()) return 0;                        // base case
        return courses.get(index).getUnit() + totalUnits(index + 1);  // recurse
    }

    private int totalUnits() { return totalUnits(0); }

    // =========================================================
    //  STRING CLEANING
    // =========================================================
    private static String cleanCode(String s) { return s.trim().toUpperCase(); }

    private static String cleanTitle(String s) {
        String[] words = s.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    // =========================================================
    //  EVENT HANDLERS
    // =========================================================
    private void doAdd() {
        addError.setVisible(false);
        try {
            String code  = cleanCode(addCode.text());
            String title = cleanTitle(addTitle.text());
            if (code.isEmpty())  throw new IllegalArgumentException("Course code cannot be empty.");
            if (title.isEmpty()) throw new IllegalArgumentException("Course title cannot be empty.");
            int unit = Integer.parseInt(addUnit.text().trim());

            Course c = new Course(code, title, unit);
            courses.add(c);
            refreshAll();

            addCode.clear();
            addTitle.clear();
            addUnit.clear();
            addCode.focus();
            toast.show("Added " + c.getCode() + " — " + c.getTitle() + " (" + c.getUnit() + " units)", true);
        } catch (NumberFormatException ex) {
            showAddError("Units must be a whole number.");
        } catch (IllegalArgumentException ex) {
            showAddError(ex.getMessage());
        }
    }

    private void showAddError(String msg) {
        addError.setText(msg);
        addError.setVisible(true);
        toast.show(msg, false);
    }

    private void doSearch() {
        String input = searchCode.text().trim();
        if (input.isEmpty()) {
            searchResultArea.removeAll();
            searchResultArea.revalidate();
            searchResultArea.repaint();
            searchError.setText("Enter a course code above.");
            return;
        }
        searchError.setText(" ");
        searchResultArea.removeAll();

        Course found = searchByCode(input, 0);
        if (found == null) {
            searchResultArea.add(emptyState(
                    "No match",
                    "No course found with code \"" + input + "\".",
                    null, null), BorderLayout.CENTER);
        } else {
            CardPanel card = new CardPanel(new BorderLayout(16, 0));
            JLabel code = new JLabel(found.getCode());
            code.setFont(new Font(Typography.family(), Font.BOLD, 20));
            code.setForeground(Palette.CHARCOAL);
            code.setPreferredSize(new Dimension(120, 30));

            JLabel title = new JLabel(found.getTitle());
            title.setFont(Typography.body());
            title.setForeground(Palette.MUTED);

            JLabel unit = new JLabel(found.getUnit() + " units");
            unit.setFont(new Font(Typography.family(), Font.BOLD, 12));
            unit.setForeground(Palette.CHAMPAGNE);
            unit.setOpaque(true);
            unit.setBackground(Palette.CHAMPAGNE_SOFT);
            unit.setBorder(new EmptyBorder(6, 12, 6, 12));

            card.add(code, BorderLayout.WEST);
            card.add(title, BorderLayout.CENTER);
            card.add(unit, BorderLayout.EAST);

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(card, BorderLayout.NORTH);
            searchResultArea.add(wrap, BorderLayout.CENTER);
            toast.show("Found " + found.getCode(), true);
        }
        searchResultArea.revalidate();
        searchResultArea.repaint();
    }

    private void doSave() {
        try {
            try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
                for (Course c : courses) out.println(c.toFileLine());
            }
            filesStatus.setForeground(Palette.SUCCESS);
            filesStatus.setText("Saved " + courses.size() + " course(s) to " + FILE_NAME + ".");
            toast.show("Saved to " + FILE_NAME, true);
        } catch (IOException ex) {
            filesStatus.setForeground(Palette.ERROR);
            filesStatus.setText("Save failed: " + ex.getMessage());
            toast.show("Save failed", false);
        }
    }

    private void doLoad() {
        File f = new File(FILE_NAME);
        if (!f.exists()) {
            filesStatus.setForeground(Palette.ERROR);
            filesStatus.setText("No saved file found: " + FILE_NAME + ".");
            toast.show("No saved file", false);
            return;
        }
        courses.clear();
        int loaded = 0;
        try (Scanner in = new Scanner(f)) {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) continue;
                try {
                    courses.add(Course.fromFileLine(line));
                    loaded++;
                } catch (RuntimeException ignored) { /* skip corrupt line */ }
            }
            refreshAll();
            filesStatus.setForeground(Palette.SUCCESS);
            filesStatus.setText("Loaded " + loaded + " course(s) from " + FILE_NAME + ".");
            toast.show("Loaded " + loaded + " course(s)", true);
        } catch (IOException ex) {
            filesStatus.setForeground(Palette.ERROR);
            filesStatus.setText("Load failed: " + ex.getMessage());
            toast.show("Load failed", false);
        }
    }

    // =========================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.messageFont", new Font(Typography.family(), Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont",  new Font(Typography.family(), Font.BOLD, 12));

        SwingUtilities.invokeLater(() -> new CourseManager().setVisible(true));
    }
}