package GUI;

import BigQuery.BQHandler;
import org.apache.log4j.Logger;

import javax.swing.*;

public class MainFrame extends JFrame {
    final static Logger logger = Logger.getLogger(MainFrame.class);
    public MainFrame(){
        setSize(1000, 1000);
        setLocation(200, 100);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Sales Extraction From X-kom");
        pack();
        setVisible(true);
        logger.info("Correctly create JFrame");
    }
}
