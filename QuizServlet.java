import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * QuizServlet.java
 * ─────────────────────────────────────────────────────────────────────────────
 * A simple HTTP Servlet that renders a live leaderboard web page for the
 * Real-Time Quiz Competition.
 *
 * Unit 5 coverage:
 *   • Building Web Applications  (Servlet API)
 *   • The Servlet API Introduction
 *   • Working with URLs / Networking
 *   • doGet / doPost lifecycle methods
 *
 * How it works:
 *   The servlet connects to QuizServer's status endpoint (port 5001) OR
 *   falls back to a built-in HTML auto-refresh page that re-fetches via
 *   meta-refresh so no WebSocket is needed.
 *
 * Deployment:
 *   1. Compile:  javac -cp jakarta.servlet-api-6.0.0.jar QuizServlet.java
 *   2. Package:  put QuizServlet.class into WEB-INF/classes/
 *   3. Map URL:  add to web.xml  (or use @WebServlet annotation below)
 *   4. Deploy:   drop WAR into Tomcat's webapps/ folder
 *
 * web.xml mapping (alternative to annotation):
 *   <servlet>
 *     <servlet-name>QuizServlet</servlet-name>
 *     <servlet-class>QuizServlet</servlet-class>
 *   </servlet>
 *   <servlet-mapping>
 *     <servlet-name>QuizServlet</servlet-name>
 *     <url-pattern>/leaderboard</url-pattern>
 *   </servlet-mapping>
 * ─────────────────────────────────────────────────────────────────────────────
 */
@WebServlet(name = "QuizServlet", urlPatterns = { "/leaderboard", "/status" })
public class QuizServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // QuizServer host/port (same machine or configurable via init-param)
    private String serverHost = "localhost";
    private int    serverPort = 5000;

    // ── Servlet lifecycle ─────────────────────────────────────────────────────

    /**
     * init() — called once when servlet loads.
     * Reads optional init-params from web.xml so the host/port are configurable
     * without recompiling.
     *
     * web.xml example:
     *   <init-param><param-name>serverHost</param-name><param-value>192.168.1.10</param-value></init-param>
     *   <init-param><param-name>serverPort</param-name><param-value>5000</param-value></init-param>
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String host = config.getInitParameter("serverHost");
        String port = config.getInitParameter("serverPort");
        if (host != null && !host.isEmpty()) serverHost = host;
        if (port != null && !port.isEmpty()) {
            try { serverPort = Integer.parseInt(port); } catch (NumberFormatException ignored) {}
        }
        log("QuizServlet initialized — server at " + serverHost + ":" + serverPort);
    }

    /** destroy() — called when servlet is unloaded (clean up resources). */
    @Override
    public void destroy() {
        log("QuizServlet destroyed.");
    }

    // ── HTTP handlers ─────────────────────────────────────────────────────────

    /**
     * doGet() — renders the leaderboard page.
     * Supports two output modes:
     *   /leaderboard        → full HTML leaderboard page (auto-refreshes every 5 s)
     *   /leaderboard?json=1 → JSON snapshot (for AJAX polling)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        boolean wantJson = "1".equals(req.getParameter("json"));

        // Fetch a status snapshot from the QuizServer
        GameSnapshot snap = fetchSnapshot();

        if (wantJson) {
            sendJson(res, snap);
        } else {
            sendHtml(res, snap);
        }
    }

    /**
     * doPost() — allows an admin to submit a chat message from the web page,
     * demonstrating POST handling as required by the Servlet API module.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String message = req.getParameter("message");
        String name    = req.getParameter("name");

        if (message != null && !message.trim().isEmpty()) {
            // In a real deployment you'd push this to the server via an admin socket.
            // Here we log it and redirect back to the leaderboard.
            log("Web chat from [" + name + "]: " + message);
            req.getSession().setAttribute("lastMsg",
                    "Message sent: \"" + message + "\"");
        }

        res.sendRedirect(req.getContextPath() + "/leaderboard");
    }

    // ── Server snapshot ───────────────────────────────────────────────────────

    /**
     * Attempts a lightweight probe connection to QuizServer to check if it
     * is alive. Since QuizServer sends WELCOME on connect, we read one line.
     * In a production setup QuizServer would expose a dedicated status port;
     * here we just report "online / offline" to keep QuizServer unchanged.
     */
    private GameSnapshot fetchSnapshot() {
        GameSnapshot snap = new GameSnapshot();
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(serverHost, serverPort), 800);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
            String welcome = br.readLine();   // e.g. "WELCOME|Connected to..."
            if (welcome != null && welcome.startsWith("WELCOME")) {
                snap.online = true;
                snap.statusLine = "Server is online and accepting players.";
            }
            // Politely close — server will log a disconnect, which is fine
        } catch (IOException e) {
            snap.online     = false;
            snap.statusLine = "Server is offline or not reachable at "
                    + serverHost + ":" + serverPort;
        }
        return snap;
    }

    // ── JSON response ─────────────────────────────────────────────────────────

    private void sendJson(HttpServletResponse res, GameSnapshot snap)
            throws IOException {
        res.setContentType("application/json; charset=UTF-8");
        res.setCharacterEncoding("UTF-8");

        PrintWriter out = res.getWriter();
        out.println("{");
        out.println("  \"online\": " + snap.online + ",");
        out.println("  \"status\": \"" + escapeJson(snap.statusLine) + "\",");
        out.println("  \"host\": \"" + escapeJson(serverHost) + "\",");
        out.println("  \"port\": " + serverPort + ",");
        out.println("  \"timestamp\": " + System.currentTimeMillis());
        out.println("}");
    }

    // ── HTML response ─────────────────────────────────────────────────────────

    private void sendHtml(HttpServletResponse res, GameSnapshot snap)
            throws IOException {

        res.setContentType("text/html; charset=UTF-8");
        res.setCharacterEncoding("UTF-8");
        // Prevent browser caching so every reload is fresh
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang='en'>");
        out.println("<head>");
        out.println("  <meta charset='UTF-8'>");
        // Auto-refresh every 5 seconds — live leaderboard without WebSocket
//        out.println("  <meta http-equiv='refresh' content='5'>");
        out.println("  <meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("  <title>Quiz Competition — Live Status</title>");
        out.println("  <style>");
        out.println(CSS);
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        // ── Header ────────────────────────────────────────────────────────────
        out.println("  <header>");
        out.println("    <div class='logo'>🎯</div>");
        out.println("    <h1>Quiz Competition</h1>");
        out.println("    <p class='subtitle'>Live Server Status</p>");
        out.println("  </header>");

        // ── Status card ───────────────────────────────────────────────────────
        out.println("  <main>");
        out.println("    <section class='status-card " + (snap.online ? "online" : "offline") + "'>");
        out.println("      <div class='status-dot'></div>");
        out.println("      <span class='status-text'>"
                + (snap.online ? "SERVER ONLINE" : "SERVER OFFLINE") + "</span>");
        out.println("    </section>");

        out.println("    <section class='info-card'>");
        out.println("      <h2>Server Info</h2>");
        out.println("      <table class='info-table'>");
        out.println("        <tr><td>Host</td><td><code>" + esc(serverHost) + "</code></td></tr>");
        out.println("        <tr><td>Port</td><td><code>" + serverPort + "</code></td></tr>");
        out.println("        <tr><td>Status</td><td>" + esc(snap.statusLine) + "</td></tr>");
        out.println("        <tr><td>Page refreshes</td><td>Every 5 seconds</td></tr>");
        out.println("      </table>");
        out.println("    </section>");

        // ── How to play card ──────────────────────────────────────────────────
        out.println("    <section class='info-card'>");
        out.println("      <h2>How to Play</h2>");
        out.println("      <ol class='steps'>");
        out.println("        <li>Run <code>QuizServer.java</code> on port 5000</li>");
        out.println("        <li>Open <code>QuizApplet</code> or <code>QuizClient</code></li>");
        out.println("        <li>Enter your name and wait for 2+ players</li>");
        out.println("        <li>Answer A / B / C / D — faster = more points!</li>");
        out.println("        <li>Highest score after 10 questions wins 🏆</li>");
        out.println("      </ol>");
        out.println("    </section>");

        // ── Web chat form (demonstrates doPost) ───────────────────────────────
        out.println("    <section class='info-card'>");
        out.println("      <h2>Send a Message</h2>");
        out.println("      <p class='dim'>Demonstrates HTTP POST handling (Servlet API)</p>");
        out.println("      <form method='post' action=''>");
        out.println("        <input type='text' name='name'    placeholder='Your name' required>");
        out.println("        <input type='text' name='message' placeholder='Message...' required>");
        out.println("        <button type='submit'>Send</button>");
        out.println("      </form>");
        out.println("    </section>");

        out.println("  </main>");

        // ── Footer ────────────────────────────────────────────────────────────
        out.println("  <footer>");
        out.println("    <p>CSE4019 · Advanced Java Programming · Unit 5 — Servlet Demo</p>");
        out.println("    <p class='dim'>Auto-refreshing · Last updated: " + new Date() + "</p>");
        out.println("  </footer>");

        out.println("</body>");
        out.println("</html>");
    }

    // ── CSS (inlined for single-file deployment) ──────────────────────────────

    private static final String CSS =
            "  :root {" +
                    "    --bg: #0d111e; --card: #161c30; --card2: #1e263e;" +
                    "    --blue: #5282ff; --cyan: #00d2d3; --green: #34d399;" +
                    "    --red: #f85252;  --gold: #fbbf24;" +
                    "    --text: #e6ebff; --dim: #788ab0; --border: #2d3759;" +
                    "  }" +
                    "  * { box-sizing: border-box; margin: 0; padding: 0; }" +
                    "  body { background: var(--bg); color: var(--text);" +
                    "         font-family: 'Segoe UI', system-ui, sans-serif;" +
                    "         min-height: 100vh; display: flex; flex-direction: column;" +
                    "         align-items: center; padding: 24px 16px; }" +
                    "  header { text-align: center; margin-bottom: 32px; }" +
                    "  header .logo { font-size: 52px; line-height: 1; }" +
                    "  header h1 { font-size: 2rem; color: var(--cyan); letter-spacing: 2px;" +
                    "              text-transform: uppercase; margin-top: 8px; }" +
                    "  header .subtitle { color: var(--dim); font-size: 0.9rem; margin-top: 4px; }" +
                    "  main { width: 100%; max-width: 600px; display: flex;" +
                    "         flex-direction: column; gap: 16px; }" +
                    "  .status-card { display: flex; align-items: center; gap: 12px;" +
                    "    padding: 16px 24px; border-radius: 12px; font-weight: 700;" +
                    "    letter-spacing: 1px; font-size: 0.95rem;" +
                    "    border: 1px solid var(--border); background: var(--card); }" +
                    "  .status-dot { width: 14px; height: 14px; border-radius: 50%;" +
                    "    flex-shrink: 0; animation: pulse 1.4s infinite; }" +
                    "  .status-card.online  .status-dot { background: var(--green); }" +
                    "  .status-card.offline .status-dot { background: var(--red); animation: none; }" +
                    "  .status-card.online  .status-text { color: var(--green); }" +
                    "  .status-card.offline .status-text { color: var(--red); }" +
                    "  @keyframes pulse { 0%,100%{box-shadow:0 0 0 0 rgba(52,211,153,.5)}" +
                    "    50%{box-shadow:0 0 0 7px rgba(52,211,153,0)} }" +
                    "  .info-card { background: var(--card); border: 1px solid var(--border);" +
                    "    border-radius: 12px; padding: 20px 24px; }" +
                    "  .info-card h2 { font-size: 1rem; text-transform: uppercase;" +
                    "    letter-spacing: 1px; color: var(--cyan); margin-bottom: 14px; }" +
                    "  .info-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }" +
                    "  .info-table td { padding: 7px 0; border-bottom: 1px solid var(--border); }" +
                    "  .info-table td:first-child { color: var(--dim); width: 40%; }" +
                    "  code { background: var(--card2); padding: 2px 7px; border-radius: 5px;" +
                    "    font-size: 0.85rem; color: var(--gold); }" +
                    "  .steps { padding-left: 20px; display: flex; flex-direction: column; gap: 8px;" +
                    "    font-size: 0.9rem; color: var(--dim); }" +
                    "  .steps li::marker { color: var(--blue); font-weight: bold; }" +
                    "  form { display: flex; flex-direction: column; gap: 10px; margin-top: 6px; }" +
                    "  input[type=text] { background: var(--card2); border: 1px solid var(--border);" +
                    "    color: var(--text); padding: 10px 14px; border-radius: 8px; font-size: 0.9rem;" +
                    "    outline: none; width: 100%; }" +
                    "  input[type=text]:focus { border-color: var(--blue); }" +
                    "  button[type=submit] { background: var(--blue); color: #fff; border: none;" +
                    "    padding: 10px 24px; border-radius: 8px; font-weight: 700; cursor: pointer;" +
                    "    align-self: flex-start; letter-spacing: 0.5px; }" +
                    "  button[type=submit]:hover { opacity: 0.85; }" +
                    "  .dim { color: var(--dim); font-size: 0.82rem; margin-bottom: 10px; }" +
                    "  footer { text-align: center; margin-top: 32px; color: var(--dim);" +
                    "    font-size: 0.8rem; line-height: 1.8; }";

    // ── Utility ───────────────────────────────────────────────────────────────

    /** HTML-escape user/server-provided strings before writing to response. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#x27;");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r");
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    /** Lightweight snapshot of server state passed from fetchSnapshot() to render methods. */
    private static class GameSnapshot {
        boolean online      = false;
        String  statusLine  = "Unknown";
        // In a full implementation: phase, currentQuestion, scores list, etc.
        // Extend here when QuizServer exposes a status port.
    }
}
