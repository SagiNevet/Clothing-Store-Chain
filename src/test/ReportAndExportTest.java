package test;

import common.model.Branch;
import common.model.CustomerType;
import common.model.Product;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.model.Sale;
import common.util.IdGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.core.DataSeeder;
import server.core.ServerContext;
import server.report.JsonExporter;
import server.report.ReportExporter;
import server.report.WordRtfExporter;
import server.service.ReportService;
import server.storage.StoragePaths;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the sales reports and the two exporters.
 * <p>
 * The export tests do more than check that a file appeared: they read the file
 * back and look for the things that make it a valid document. A file that was
 * created but cannot be opened by Word would pass a weaker test and fail during
 * the defence.
 * </p>
 */
public class ReportAndExportTest {

    /** The service that builds the reports. */
    private final ReportService reportService = new ReportService();

    /**
     * Makes sure the folders and the demonstration data exist.
     *
     * @throws Exception if the data files cannot be prepared
     */
    @BeforeAll
    public static void prepareData() throws Exception {
        StoragePaths.createDirectoriesIfMissing();
        DataSeeder.seedIfEmpty();
        ServerContext.getInstance().initializeServices();
        recordSomeSales();
    }

    /**
     * Records a few sales so the reports have something to summarise.
     *
     * @throws Exception if the sales file cannot be written
     */
    private static void recordSomeSales() throws Exception {
        Product shirt = new Product("P-100", "Blue Cotton Shirt",
                ProductCategory.SHIRTS, 100.0, 50);
        Product belt = new Product("P-400", "Leather Belt",
                ProductCategory.ACCESSORIES, 80.0, 50);

        ServerContext.getInstance().getSalesRepository().append(
                new Sale(IdGenerator.nextSaleId(), Branch.TEL_AVIV, "1002",
                        "REPORT-TEST-1", CustomerType.NEW, shirt, 2, 180.0));
        ServerContext.getInstance().getSalesRepository().append(
                new Sale(IdGenerator.nextSaleId(), Branch.JERUSALEM, "2002",
                        "REPORT-TEST-2", CustomerType.VIP, belt, 1, 68.0));
    }

    @Test
    @DisplayName("The report by branch names every branch, even one that sold nothing")
    public void everyBranchAppearsInTheBranchReport() throws Exception {
        List<ReportRow> rows = reportService.addBranchesWithNoSales(
                reportService.buildReport(ReportType.SALES_BY_BRANCH, null, null));

        for (Branch branch : Branch.values()) {
            boolean branchAppears = rows.stream()
                    .anyMatch(row -> row.getGroupName().equals(branch.getDisplayName()));
            assertTrue(branchAppears, branch.getDisplayName() + " is missing from the report");
        }
    }

    @Test
    @DisplayName("A report line adds up the sales, the items and the money")
    public void aReportLineAddsEverythingUp() throws Exception {
        List<ReportRow> rows = reportService.buildReport(ReportType.SALES_BY_BRANCH, null, null);

        ReportRow telAvivRow = rows.stream()
                .filter(row -> row.getGroupName().equals(Branch.TEL_AVIV.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tel Aviv is missing from the report"));

        assertTrue(telAvivRow.getNumberOfSales() >= 1);
        assertTrue(telAvivRow.getItemsSold() >= 2);
        assertTrue(telAvivRow.getTotalRevenue() > 0.0);
        // The discount is the difference between the catalogue price and what was
        // actually paid, so a sale to a new customer must show one.
        assertTrue(telAvivRow.getTotalDiscount() > 0.0,
                "the purchase plans must show up as a discount in the report");
    }

    @Test
    @DisplayName("A category filter keeps only the sales of those categories")
    public void aCategoryFilterIsApplied() throws Exception {
        List<ReportRow> shirtsOnly = reportService.buildReport(ReportType.SALES_BY_CATEGORY,
                null, Arrays.asList(ProductCategory.SHIRTS));

        for (ReportRow row : shirtsOnly) {
            assertEquals(ProductCategory.SHIRTS.getDisplayName(), row.getGroupName(),
                    "a filtered report must not contain any other category");
        }
    }

    @Test
    @DisplayName("A report by product groups by the product that was sold")
    public void theProductReportGroupsByProduct() throws Exception {
        List<ReportRow> rows = reportService.buildReport(ReportType.SALES_BY_PRODUCT, null, null);

        boolean shirtAppears = rows.stream()
                .anyMatch(row -> row.getGroupName().contains("P-100"));
        assertTrue(shirtAppears, "the shirt that was sold is missing from the report");
    }

    @Test
    @DisplayName("The Word export produces a file Word can really open")
    public void theWordExportProducesAValidDocument() throws Exception {
        ReportExporter exporter = new WordRtfExporter();
        List<ReportRow> rows = reportService.buildReport(ReportType.SALES_BY_BRANCH, null, null);

        String writtenPath = exporter.export(ReportType.SALES_BY_BRANCH, rows);
        File writtenFile = new File(writtenPath);

        assertTrue(writtenFile.exists(), "the report file was not created");
        assertTrue(writtenFile.length() > 0, "the report file is empty");

        String content = new String(Files.readAllBytes(writtenFile.toPath()),
                StandardCharsets.US_ASCII);

        // These four are what make the file a document rather than a text file
        // that happens to end in .rtf.
        assertTrue(content.startsWith("{\\rtf1"), "the RTF header is missing");
        assertTrue(content.endsWith("}"), "the document is not closed");
        assertTrue(content.contains("\\trowd"), "the table is missing");
        assertTrue(content.contains("Sales by branch"), "the title is missing");
    }

    @Test
    @DisplayName("Braces and backslashes in a name cannot break the Word file")
    public void specialCharactersAreEscapedInTheWordFile() throws Exception {
        ReportExporter exporter = new WordRtfExporter();
        List<ReportRow> rows = new ArrayList<>();
        rows.add(new ReportRow("A {tricky} name \\ here", 1, 1, 10.0, 1.0));

        String writtenPath = exporter.export(ReportType.SALES_BY_PRODUCT, rows);
        String content = new String(Files.readAllBytes(new File(writtenPath).toPath()),
                StandardCharsets.US_ASCII);

        // The braces of the name must appear escaped. If they did not, Word would
        // read them as the end of the document and the file would be corrupt.
        assertTrue(content.contains("\\{tricky\\}"),
                "the braces of the name were not escaped");
        assertTrue(content.endsWith("}"), "the document is still closed correctly");
    }

    @Test
    @DisplayName("The JSON export produces a readable document")
    public void theJsonExportProducesAReadableDocument() throws Exception {
        ReportExporter exporter = new JsonExporter();
        List<ReportRow> rows = reportService.buildReport(ReportType.SALES_BY_BRANCH, null, null);

        String writtenPath = exporter.export(ReportType.SALES_BY_BRANCH, rows);
        String content = new String(Files.readAllBytes(new File(writtenPath).toPath()),
                StandardCharsets.UTF_8);

        assertTrue(content.trim().startsWith("{"), "the JSON document does not open");
        assertTrue(content.trim().endsWith("}"), "the JSON document does not close");
        assertTrue(content.contains("\"rows\""), "the rows array is missing");
        assertTrue(content.contains("\"totalRevenue\""), "a field is missing");
    }

    @Test
    @DisplayName("A quotation mark in a name cannot break the JSON file")
    public void quotesAreEscapedInTheJsonFile() throws Exception {
        ReportExporter exporter = new JsonExporter();
        List<ReportRow> rows = new ArrayList<>();
        rows.add(new ReportRow("A \"quoted\" name", 1, 1, 10.0, 1.0));

        String writtenPath = exporter.export(ReportType.SALES_BY_PRODUCT, rows);
        String content = new String(Files.readAllBytes(new File(writtenPath).toPath()),
                StandardCharsets.UTF_8);

        assertTrue(content.contains("\\\"quoted\\\""),
                "the quotation marks of the name were not escaped");
    }

    @Test
    @DisplayName("Both exporters name their own format")
    public void bothExportersNameTheirFormat() {
        assertEquals("Word", new WordRtfExporter().getFormatName());
        assertEquals("JSON", new JsonExporter().getFormatName());
    }
}
