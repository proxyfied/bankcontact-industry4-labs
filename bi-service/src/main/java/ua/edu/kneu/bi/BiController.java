package ua.edu.kneu.bi;
import java.io.*;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api")
public class BiController {
 private final ExcelService excel; private final AnalyticsService analytics;
 public BiController(ExcelService excel,AnalyticsService analytics) { this.excel=excel;this.analytics=analytics; }
 @PostMapping(value="/import/excel",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<ExcelService.ImportResult> upload(@RequestPart("file") MultipartFile file) {
  return ResponseEntity.status(201).body(excel.save(file));
 }
 @GetMapping("/analytics") public AnalyticsService.Statistics statistics() { return analytics.statistics(); }
 @GetMapping("/import/template") public ResponseEntity<byte[]> template() throws IOException {
  try(XSSFWorkbook workbook=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
   Sheet sheet=workbook.createSheet("Звернення"); Row header=sheet.createRow(0);
   CellStyle style=workbook.createCellStyle();style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
   Font font=workbook.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());style.setFont(font);
   for(int i=0;i<6;i++) {Cell cell=header.createCell(i);cell.setCellValue(ExcelService.HEADERS.get(i));cell.setCellStyle(style);sheet.setColumnWidth(i,(i==1?40:22)*256);}
   List<String> example=List.of("BI-001","Питання щодо картки","Картки","NEW","2026-11-01","5");
   Row row=sheet.createRow(1);for(int i=0;i<6;i++) row.createCell(i).setCellValue(example.get(i));
   sheet.createFreezePane(0,1); workbook.write(output);
   return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=template.xlsx")
    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(output.toByteArray());
  }
 }
}
