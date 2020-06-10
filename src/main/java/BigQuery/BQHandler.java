package BigQuery;
import com.google.cloud.bigquery.*;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BQHandler {
    final static Logger logger = Logger.getLogger(BQHandler.class);
    BigQuery bigquery;
    String propFile = "config.properties";
    Properties prop;
    InputStream inputStream;
    TableId tableIdProducts, tableIdPrices, tableIdBrands;

    public BQHandler(BigQuery bq) throws IOException {
        bigquery = bq;
        prop = new Properties();
        inputStream = getClass().getClassLoader().getResourceAsStream(propFile);

        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + propFile + "' not found in the classpath");
        }
        tableIdProducts = TableId.of("xkomdb", "products");
        tableIdPrices = TableId.of("xkomdb", "productPrices");
        tableIdBrands = TableId.of("xkomdb", "brands");
    }

    private TableResult getResult(String query) throws InterruptedException {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .setUseLegacySql(false)
                        .build();
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        queryJob = queryJob.waitFor();
        if (queryJob == null) {
            logger.error("Job no longer exists");
            throw new RuntimeException();
        } else if (queryJob.getStatus().getError() != null) {
            logger.error("Errors during getting values from BigQuery: " + queryJob.getStatus().getError().toString());
            throw new RuntimeException();
        }
        logger.info("Correctly got table results from BigQuery");
        return queryJob.getQueryResults();
    }

    public Map<String, String> getBrandsCodes() throws InterruptedException {
        String query = String.format("Select brand, code from %s", prop.getProperty("brandsTable"));

        TableResult result = getResult(query);
        Map<String, String> brands = new HashMap<>();

        for (FieldValueList row : result.iterateAll()){
            brands.put(row.get("code").getStringValue(), row.get("brand").getStringValue());
        }
        logger.info("Correctly got brands codes");
        return brands;
    }

    public ArrayList<String> getAllBrands() throws InterruptedException {
        String query = String.format("Select distinct(productBrand) as b_dist from %s", prop.getProperty("productsTable"));

        TableResult result = getResult(query);
        ArrayList<String> brands = new ArrayList<>();

        for (FieldValueList row : result.iterateAll()){
            brands.add(row.get("b_dist").getStringValue());
        }
        logger.info("Correctly got all brands from database");
        return brands;
    }

    public Float getActualPrice(Integer productId) throws InterruptedException {
        String query = String.format("Select price from %s where productId=%d order by date desc limit 1",
                prop.getProperty("pricesTable"), productId);
        TableResult result = getResult(query);
        float price = 0;
        for (FieldValueList row : result.iterateAll()){
            price =row.get("price").getNumericValue().floatValue();
        }
        logger.info("Correctly got actual price of product");
        return price;
    }

    public Map<Integer, String> getBrandProducts(String brand) throws InterruptedException {
        String query = String.format("Select productId, productName from %s where productBrand='%s'",
                prop.getProperty("productsTable"), brand);
        TableResult result = getResult(query);
        Map<Integer, String> products = new TreeMap<>();

        for (FieldValueList row : result.iterateAll()){
            products.put(row.get("productId").getNumericValue().intValue(), row.get("productName").getStringValue());
        }
        logger.info("Correctly got products by brand");
        return products;
    }

    public Map<Date, Float> getPricesOfProduct(Integer id) throws InterruptedException, ParseException {
        String query = String.format("Select date, price from %s where productId=%d",
                prop.getProperty("pricesTable"), id);

        TableResult result = getResult(query);
        Map<Date, Float> products = new TreeMap<>();

        for (FieldValueList row : result.iterateAll()){
            Date rowDate = new SimpleDateFormat("yyyy-MM-dd").parse(row.get("date").getStringValue());
            products.put(rowDate, (float) row.get("price").getDoubleValue());
        }

        logger.info("Correctly got history of product's prices");
        return products;
    }

    private ArrayList<Integer> getAllProducts() throws InterruptedException {
        String query = String.format("Select productId from %s ",
                prop.getProperty("productsTable"));

        TableResult result = getResult(query);
        ArrayList<Integer> products = new ArrayList<>();

        for (FieldValueList row : result.iterateAll()){
            products.add(row.get("productId").getNumericValue().intValue());
        }
        logger.info("Correctly got all products");
        return products;
    }

    public ArrayList<String> getAllBrandProducts(String brand) throws InterruptedException {
        String query = String.format("Select productName from %s where productBrand='%s'",
                prop.getProperty("productsTable"), brand);

        TableResult result = getResult(query);
        ArrayList<String> products = new ArrayList<>();

        for (FieldValueList row : result.iterateAll()){
            products.add(row.get("productName").getStringValue());
        }
        logger.info("Correctly got all products");
        return products;
    }
}
