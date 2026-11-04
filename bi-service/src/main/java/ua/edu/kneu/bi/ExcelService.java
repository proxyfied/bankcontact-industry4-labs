package ua.edu.kneu.bi;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service
public class ExcelService {
 public static final List<String> HEADERS=List.of("Код","Тема","Категорія","Статус","Дата","Тривалість хв");
 public static final Set<String> STATUSES=Set.of("NEW","IN_PROGRESS","RESOLVED","CLOSED");
 private final ContactRecordRepository repository;
 public ExcelService(ContactRecordRepository repository) { this.repository=repository; }
 public record ImportResult(int imported,int skipped) {}
 @Transactional
 public ImportResult save(MultipartFile file) {
  String name=file.getOriginalFilename();
  if(file.isEmpty() || name==null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
   throw bad("Оберіть непорожній файл .xlsx.");
  if(file.getSize()>2*1024*1024) throw new ImportProblem(413,"Файл перевищує 2 МБ.");
  List<ContactRecord> records=new ArrayList<>(); Set<String> codes=new HashSet<>(); int skipped=0;
  try(InputStream input=file.getInputStream(); XSSFWorkbook workbook=new XSSFWorkbook(input)) {
   if(workbook.getNumberOfSheets()!=1) throw bad("Файл має містити один аркуш із даними.");
   Sheet sheet=workbook.getSheetAt(0); Row header=sheet.getRow(0);
   if(header==null || header.getLastCellNum()!=6) throw bad("Потрібно 6 колонок згідно із шаблоном.");
   for(int c=0;c<6;c++) if(!HEADERS.get(c).equals(text(header,c)))
    throw bad("Заголовки або порядок колонок не відповідають шаблону.");
   if(sheet.getLastRowNum()>2000) throw bad("Допускається не більше 2000 рядків після заголовка.");
   for(int index=1;index<=sheet.getLastRowNum();index++) {
    Row row=sheet.getRow(index);
    if(blank(row)) { skipped++; continue; }
    try {
     if(row.getLastCellNum()>6) throw bad("Зайві колонки.");
     ContactRecord record=new ContactRecord();
     String code=required(row,0,40);
     if(!code.matches("[A-Za-z0-9_-]+")) throw bad("Код: лише латинські літери, цифри, _ та -.");
     if(!codes.add(code)) throw bad("Код повторюється у файлі: "+code+".");
     record.setExternalCode(code); record.setSubject(required(row,1,200));
     record.setCategory(required(row,2,80));
     String status=required(row,3,20);
     if(!STATUSES.contains(status)) throw bad("Статус має бути NEW, IN_PROGRESS, RESOLVED або CLOSED.");
     record.setStatus(status); record.setRegisteredOn(date(row.getCell(4)));
     String minutes=text(row,5);
     if(!minutes.matches("[0-9]{1,4}")) throw bad("Тривалість: ціле число від 0 до 1440 хв.");
     int value=Integer.parseInt(minutes);
     if(value>1440) throw bad("Тривалість перевищує 1440 хв.");
     record.setHandlingMinutes(value); records.add(record);
    } catch(ImportProblem ex) { throw bad("Рядок "+(index+1)+": "+ex.getMessage()); }
   }
  } catch(ImportProblem ex) { throw ex; }
    catch(IOException | RuntimeException ex) { throw bad("Не вдалося прочитати Excel. Перевірте формат .xlsx та відсутність пароля."); }
  if(records.isEmpty()) throw bad("У файлі немає записів для імпорту.");
  List<String> existing=repository.existingCodes(codes);
  if(!existing.isEmpty()) throw new ImportProblem(409,"Коди вже існують у базі. Жодного запису не додано.");
  repository.saveAll(records); repository.flush();
  return new ImportResult(records.size(),skipped);
 }
 private static boolean blank(Row row) {
  if(row==null) return true;
  for(Cell c:row) if(c.getCellType()!=CellType.BLANK && !(c.getCellType()==CellType.STRING && c.getStringCellValue().isBlank())) return false;
  return true;
 }
 private static String required(Row r,int c,int max) {
  String s=text(r,c); if(s.isBlank() || s.length()>max) throw bad("Поле «"+HEADERS.get(c)+"»: від 1 до "+max+" символів."); return s;
 }
 private static String text(Row r,int col) {
  Cell c=r.getCell(col); if(c==null || c.getCellType()==CellType.BLANK) return "";
  if(c.getCellType()==CellType.STRING) return c.getStringCellValue().strip();
  if(c.getCellType()==CellType.NUMERIC && !DateUtil.isCellDateFormatted(c)) {
   double n=c.getNumericCellValue(); if(n==Math.rint(n) && Math.abs(n)<1e12) return Long.toString((long)n);
  }
  throw bad("Неприпустимий тип клітинки. Формули та логічні значення не підтримуються.");
 }
 private static LocalDate date(Cell c) {
  if(c==null) throw bad("Дата обов’язкова.");
  LocalDate date;
  try {
   if(c.getCellType()==CellType.NUMERIC && DateUtil.isCellDateFormatted(c) && DateUtil.isValidExcelDate(c.getNumericCellValue())) {
    LocalDateTime time=c.getLocalDateTimeCellValue();
    if(!time.toLocalTime().equals(LocalTime.MIDNIGHT)) throw bad("У даті не повинно бути часу.");
    date=time.toLocalDate();
   } else if(c.getCellType()==CellType.STRING) date=LocalDate.parse(c.getStringCellValue().strip());
   else throw bad("Дата має бути датою Excel або текстом РРРР-ММ-ДД.");
  } catch(DateTimeParseException ex) { throw bad("Невірна дата. Формат РРРР-ММ-ДД."); }
  if(date.getYear()<2000 || date.getYear()>2100) throw bad("Рік має бути від 2000 до 2100.");
  return date;
 }
 private static ImportProblem bad(String text) { return new ImportProblem(400,text); }
}
