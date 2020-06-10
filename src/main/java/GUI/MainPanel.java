package GUI;

import BigQuery.BQHandler;
import com.google.cloud.bigquery.BigQueryOptions;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainPanel extends JPanel {
    final static Logger logger = Logger.getLogger(MainPanel.class);
    private final JList<String> brandsJList;
    private final JList<String> productsJList;
    private final BQHandler bq = new BQHandler(BigQueryOptions.getDefaultInstance().getService());
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));

    public MainPanel() throws IOException, InterruptedException {
        setLayout(new BorderLayout());

        // Create static elements for panel
        // brands
        JPanel brandsPanel = new JPanel();
        BoxLayout boxlayoutBrands = new BoxLayout(brandsPanel, BoxLayout.Y_AXIS);
        brandsPanel.setLayout(boxlayoutBrands);
        JLabel brandsLabel = new JLabel("Brands", JLabel.CENTER);
        ArrayList<String> brands = bq.getAllBrands();
        brandsJList = new JList<>(getListModel(brands));
        brandsPanel.add(brandsLabel);
        brandsPanel.add(brandsJList);
        brandsPanel.add(new JScrollPane(brandsJList));
        logger.info("Set up brands panel");

        // products
        JPanel productsPanel = new JPanel();
        BoxLayout boxlayoutProducts = new BoxLayout(productsPanel, BoxLayout.Y_AXIS);
        productsPanel.setLayout(boxlayoutProducts);
        JLabel productsLabel = new JLabel("Products", JLabel.CENTER);
        productsLabel.setMaximumSize(new Dimension(1000, 100));
        productsJList = new JList<>();

        productsPanel.add(productsLabel);
        productsPanel.add(productsJList);
        productsPanel.add(new JScrollPane(productsJList));
        logger.info("Setup products panel");

        // product info
        JPanel productInfoPanel = new JPanel();
        BoxLayout boxlayoutProductInfo = new BoxLayout(productInfoPanel, BoxLayout.Y_AXIS);
        productInfoPanel.setLayout(boxlayoutProductInfo);
        JLabel productInfoLabel = new JLabel("Product Info", JLabel.CENTER);
        ChartPanel chartPanel = new ChartPanel(getEmptyChart());

        productInfoPanel.add(productInfoLabel);
        productInfoPanel.add(chartPanel);
        logger.info("Setup product info panel");

        // add panels to main panel
        add(brandsPanel, BorderLayout.WEST);
        add(productsPanel, BorderLayout.CENTER);
        add(productInfoPanel, BorderLayout.EAST);

        // Listeners on lists
        brandsJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) {
//                    System.out.println(brandsJList.getSelectedValue());
                    try {
                        productsJList.setModel(getListModelProducts(bq.getBrandProducts(brandsJList.getSelectedValue())));
                    } catch (InterruptedException e) {
                        logger.error("Failed during choosing producent");
                        e.printStackTrace();
                    }
                }
            }
        });

        productsJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) {
                    String sel = productsJList.getSelectedValue();
                    int idEnd = sel.indexOf(" - ");
                    int id;
                    String name;
                    if(idEnd != -1){
                        id = Integer.parseInt(sel.substring(0, idEnd));
                        name = sel.substring(idEnd + 3, sel.length() - 1);
                    } else {
                        id = 0;
                        name = "";
                    }
                    XYDataset prices = null;
                    try {
                        prices = createDataset(id);
                    } catch (InterruptedException | ParseException e) {
                        logger.error("Failed during creating dataset for chart");
                        e.printStackTrace();
                    }
                    float price = 0;
                    try {
                        price = bq.getActualPrice(id);
                    } catch (InterruptedException e) {
                        logger.error("Failed furing getting actual price from BigQuery");
                        e.printStackTrace();
                    }
                    JFreeChart chart = ChartFactory.createTimeSeriesChart(
                            "Prices History of " + name + "\n Actual price: " + price + " zł",
                            "Date",
                            "Price [zł]",
                            prices);
                    XYPlot plot = chart.getXYPlot();
                    NumberAxis yAxis = new NumberAxis("Price [zł]");
                    yAxis.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(yAxis);
                    DateAxis axis = new DateAxis();
                    plot.setDomainAxis(axis);
                    axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
                    chartPanel.setChart(chart);
                    logger.info("Correctly added new product chart to panel");
                }
            }
        });

    }

    private DefaultListModel<String> getListModel(ArrayList<String> data){
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String value : data){
            listModel.addElement(value);
        }
        logger.info("Create list of producents");
        return listModel;
    }

    private DefaultListModel<String> getListModelProducts(Map<Integer, String> data){
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Map.Entry<Integer,String> entry : data.entrySet()){
            listModel.addElement(entry.getKey().toString() + " - " + entry.getValue());
        }
        logger.info("Create list of products");
        return listModel;
    }

    private XYDataset createDataset(Integer productId) throws InterruptedException, ParseException {

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        TimeSeries pricesSeries = new TimeSeries("Price");
        Map<Date, Float> history = bq.getPricesOfProduct(productId);
        for (Map.Entry<Date,Float> entry : history.entrySet()){
            cal.setTime(entry.getKey());
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            pricesSeries.add(new Day(day, month, year), entry.getValue());
        }
        dataset.addSeries(pricesSeries);
        logger.info("Create dataset for chart");
        return dataset;
    }

    private JFreeChart getEmptyChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart("History of product's prices",
                "Date",
                "Price",
                new TimeSeriesCollection());
        XYPlot plot = chart.getXYPlot();
        DateAxis axis = new DateAxis();
        plot.setDomainAxis(axis);
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        logger.info("Create empty chart for starting view");
        return chart;
    }
}
