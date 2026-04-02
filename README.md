#  Real Time Multiplayer Quiz Competition Application (Advanced Java Project)

##  Project Overview

The **Real time Multiplayer Quiz Competition Application** is a multiplayer Java-based application developed using concepts from Advanced Java Programming.
It allows multiple users to participate in a quiz, answer questions, and view results in real-time.

The project integrates **Applet, Socket Programming, and Servlets** to demonstrate a complete client-server architecture.

---

##  Features

*  Multiplayer quiz system
*  Timer-based questions
*  Score calculation and leaderboard
*  Web interface for server status
*  Socket-based communication
*  Interactive GUI using Swing/Applet

---

##  Technologies Used

* **Java (JDK 8)**
* **Java Applet (AWT + Swing)**
* **Socket Programming (Client-Server)**
* **Java Servlets**
* **Apache Tomcat Server**
* **HTML (for Applet execution)**

---

##  System Architecture

###  Quiz Server (Socket Programming)

* Runs on port **5000**
* Handles client connections
* Processes quiz logic and responses

###  Quiz Applet (Client UI)

* Provides graphical interface
* Displays questions and options
* Sends answers to server

###  Servlet (Web Interface)

* Displays server status (online/offline)
* Shows leaderboard (if implemented)
* Demonstrates HTTP request handling

---

## 📂 Project Structure

```
Quiz-Project/
│
├── QuizApplet.java
├── QuizServer.java
├── QuizServlet.java
├── web.xml
├── quiz.html
│
└── WEB-INF/
    └── classes/
        ├── QuizServlet.class
        └── QuizServlet$GameSnapshot.class
```

---

##  How to Run the Project

###  Step 1: Run Server

```bash
javac QuizServer.java
java QuizServer
```

---

###  Step 2: Run Applet

```bash
javac QuizApplet.java
java QuizApplet
```

OR using Applet Viewer:

```bash
appletviewer quiz.html
```

---

###  Step 3: Run Servlet (Tomcat)

1. Place files in:

```
apache-tomcat/webapps/MyApp/
```

2. Start Tomcat:

```bash
startup.bat
```

3. Open in browser:

```
http://localhost:8080/MyApp/leaderboard
```

---

##  Learning Outcomes

* Understanding of **Applet lifecycle**
* Implementation of **Socket Programming**
* Use of **Servlets for web applications**
* Handling **client-server communication**
* GUI design using **Swing & AWT**
* Deployment using **Apache Tomcat**

---

##  Output

* Quiz UI with timer and options
* Server connection status (Online/Offline)
* Player scores and leaderboard

---

##  Note

* Applets are deprecated in modern browsers, so use:

  * **JDK 8**
  * **Applet Viewer / IntelliJ execution**

---

##  Team Details

* Team Number : 24

* Slot:C11+C12

### Members and Registration Number:
* Heramb Krishna Arora        24BCE10154
* Aditya Seswani              24BCE11132
* Aryan Thombare              24BCE10278
* Aadhya Shukla               24BCE10302
* Saanvi Sharma               24BCE10237


---

##  Submission Details

* Subject: Advanced Java Programming
* Submission Date: 5 April 2026

---

##  Conclusion

This project demonstrates the integration of multiple Advanced Java concepts into a single real-world application, showcasing both GUI and backend communication effectively.

---

