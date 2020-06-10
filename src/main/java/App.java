import GUI.MainFrame;
import GUI.MainPanel;

import javax.swing.*;
import java.io.IOException;

public class App{

    public App() throws IOException, InterruptedException {
        JFrame frame = new MainFrame();
        JPanel panel = new MainPanel();
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new App();
    }
}