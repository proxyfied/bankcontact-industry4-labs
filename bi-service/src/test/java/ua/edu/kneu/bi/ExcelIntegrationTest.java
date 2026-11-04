package ua.edu.kneu.bi;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class ExcelIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ContactRecordRepository repository;
 @BeforeEach void clear() { repository.deleteAll(); }
 private byte[] workbook(String[][] rows) throws Exception {
  try(XSSFWorkbook w=new XSSFWorkbook();ByteArrayOutputStream b=new ByteArrayOutputStream()) {
   Sheet sheet=w.createSheet("Звернення"); Row h=sheet.createRow(0);
   for(int i=0;i<6;i++) h.createCell(i).setCellValue(ExcelService.HEADERS.get(i));
   for(int r=0;r<rows.length;r++) {Row row=sheet.createRow(r+1);for(int c=0;c<rows[r].length;c++) row.createCell(c).setCellValue(rows[r][c]);}
   w.write(b);return b.toByteArray();
  }
 }
 private String[] row(String code) { return new String[]{code,"Питання щодо картки","Картки","RESOLVED","2026-11-01","5"}; }
 private MockMultipartFile file(byte[] data) { return new MockMultipartFile("file","data.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",data); }
 @Test void savesAndAggregatesInPostgres() throws Exception {
  String[] second=row("B");second[2]="Кредити";second[3]="NEW";second[4]="2026-11-02";
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{row("A"),second}))))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.imported").value(2));
  assertThat(repository.count()).isEqualTo(2);
  assertThat(repository.findAll().get(0).getRegisteredOn()).isEqualTo(LocalDate.of(2026,11,1));
  mvc.perform(get("/api/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
   .andExpect(jsonPath("$.totalMinutes").value(10)).andExpect(jsonPath("$.categories.length()").value(2)).andExpect(jsonPath("$.dates.length()").value(2));
 }
 @Test void invalidSecondRowRollsBackEntireImport() throws Exception {
  String[] invalid=row("B");invalid[3]="UNKNOWN";
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{row("A"),invalid}))))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Рядок 3")));
  assertThat(repository.count()).isZero();
 }
 @Test void rejectsDuplicateWithinFile() throws Exception {
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{row("A"),row("A")})))).andExpect(status().isBadRequest());
  assertThat(repository.count()).isZero();
 }
 @Test void rejectsExistingCodeWithoutAddingNewRows() throws Exception {
  byte[] first=workbook(new String[][]{row("A")});
  mvc.perform(multipart("/api/import/excel").file(file(first))).andExpect(status().isCreated());
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{row("B"),row("A")})))).andExpect(status().isConflict());
  assertThat(repository.count()).isEqualTo(1);
 }
 @Test void handlesTypedExcelDateAndNumericMinutes() throws Exception {
  try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(workbook(new String[][]{row("A")})));ByteArrayOutputStream b=new ByteArrayOutputStream()) {
   Row row=w.getSheetAt(0).getRow(1);Cell date=row.getCell(4);date.setCellValue(LocalDate.of(2026,11,3));
   CellStyle style=w.createCellStyle();style.setDataFormat(w.createDataFormat().getFormat("yyyy-mm-dd"));date.setCellStyle(style);
   row.getCell(5).setCellValue(12);w.write(b);
   mvc.perform(multipart("/api/import/excel").file(file(b.toByteArray()))).andExpect(status().isCreated());
   assertThat(repository.findAll().get(0).getRegisteredOn()).isEqualTo(LocalDate.of(2026,11,3));
   assertThat(repository.totalMinutes()).isEqualTo(12);
  }
 }
 @Test void rejectsFormulasRatherThanEvaluatingThem() throws Exception {
  try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(workbook(new String[][]{row("A")})));ByteArrayOutputStream b=new ByteArrayOutputStream()) {
   w.getSheetAt(0).getRow(1).getCell(5).setCellFormula("1+2");w.write(b);
   mvc.perform(multipart("/api/import/excel").file(file(b.toByteArray()))).andExpect(status().isBadRequest());
   assertThat(repository.count()).isZero();
  }
 }
 @Test void rejectsInvalidDateAndNegativeDuration() throws Exception {
  for(String[] invalid:List.of(new String[]{"A","Тема","Картки","NEW","2026-02-30","5"},new String[]{"A","Тема","Картки","NEW","2026-01-01","-1"})) {
   mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{invalid})))).andExpect(status().isBadRequest());
  }
  assertThat(repository.count()).isZero();
 }
 @Test void skipsBlankRows() throws Exception {
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{row("A"),new String[]{""," ","","","",""}}))))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.skipped").value(1));
 }
 @Test void rejectsMissingHeadersAndEmptyData() throws Exception {
  mvc.perform(multipart("/api/import/excel").file(file(workbook(new String[][]{})))).andExpect(status().isBadRequest());
  try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(workbook(new String[][]{row("A")})));ByteArrayOutputStream b=new ByteArrayOutputStream()) {
   w.getSheetAt(0).getRow(0).getCell(0).setCellValue("Wrong");w.write(b);
   mvc.perform(multipart("/api/import/excel").file(file(b.toByteArray()))).andExpect(status().isBadRequest());
  }
 }
 @Test void rejectsFakeXlsxMissingFileAndOversize() throws Exception {
  mvc.perform(multipart("/api/import/excel").file(file("not an excel".getBytes()))).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/import/excel")).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/import/excel").file(file(new byte[2*1024*1024+1]))).andExpect(status().isPayloadTooLarge());
 }
 @Test void templateCanBeImported() throws Exception {
  byte[] bytes=mvc.perform(get("/api/import/template")).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=template.xlsx"))
   .andReturn().getResponse().getContentAsByteArray();
  mvc.perform(multipart("/api/import/excel").file(file(bytes))).andExpect(status().isCreated()).andExpect(jsonPath("$.imported").value(1));
 }
 @Test void emptyStatisticsAndCors() throws Exception {
  mvc.perform(get("/api/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.categories").isEmpty());
  mvc.perform(options("/api/import/excel").header("Origin","http://localhost:8080").header("Access-Control-Request-Method","POST")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:8080"));
  mvc.perform(options("/api/import/excel").header("Origin","https://unknown.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
 }
}
